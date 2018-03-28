package com.library.dexknife.shell.jiagu;

import com.library.dexknife.shell.ApkToolPlus;
import com.library.dexknife.shell.AppManager;
import com.library.dexknife.shell.Callback;
import com.library.dexknife.shell.apkparser.ApkParser;
import com.library.dexknife.shell.apkparser.bean.CertificateMeta;
import com.library.dexknife.shell.res.util.FileOperation;
import com.library.dexknife.shell.utils.ClassHelper;
import com.library.dexknife.shell.utils.Cmd;
import com.library.dexknife.shell.utils.DataProtector;
import com.library.dexknife.shell.utils.Debug;
import com.library.dexknife.shell.utils.ZipHelper;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

import org.apache.commons.io.FileUtils;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import brut.androlib.AndrolibException;
import brut.directory.DirectoryException;

public class JiaGu {

    public static final String TAG = JiaGu.class.getSimpleName();

    public static final String JIAGU_ZIP = "jiagu.zip";
    private static final String JIAGU_DATA_BIN = "libprotected.jar";
    public static  String JIAGU_ZIP_PATH= JiaGu.class.getPackage().getName().replaceAll("\\.","/") + "/" + JIAGU_ZIP;

    private static String PROXY_APPLICATION_NAME = "com.android.echocutter.BaseApplication";
    private static final String METADATA_SRC_APPLICATION = "apktoolplus_jiagu_app";
    public static boolean ISSHELL=false;//是否开启加固
    public static boolean ANDRESGUARD=false;//是否开启资源混淆
    public static String SHELLAPKNAME;

    //加固文件工作文件夹
    public static File workDir = new File(AppManager.getTempDir(), "jiagu");
    private static File andResGuardFile = null;
    private static File jiaguZip = new File(workDir, JIAGU_ZIP);
    //资源混淆的配置文件
    public static String andres_pz="";//配置
    public static String andres_map="";//map路径

    public enum Event {
        DECOMPILEING,
        ENCRYPTING,
        RECOMPILING,
        SIGNING,
        DECOMPILE_FAIL,
        RECOMPILE_FAIL,
        ENCRYPT_FAIL,
        MENIFEST_FAIL,
        ANDROIDRES,
        ZIPALIGN
    }

    public static boolean isEncrypted(File apk) {
        return ZipHelper.hasFile(apk, "assets/" + JIAGU_DATA_BIN);
    }

    /**
     * 开始加固
     * @param apk
     * @param config
     * @param callback
     * @return
     */
    public static File encrypt(File apk, KeystoreConfig config, com.library.dexknife.shell.Callback<Event> callback) {

        if (!com.library.dexknife.shell.utils.FileHelper.exists(apk) || isEncrypted(apk)) {
            return null;
        }
        workDir.mkdir();
        /**
         * 1.decompile apk
         */
        handleCallback(callback, Event.DECOMPILEING);
        File decompile = new File(workDir, "decompile");
        com.library.dexknife.shell.utils.FileHelper.cleanDirectory(decompile);
        boolean decompileResult = ApkToolPlus.decompile(apk, decompile, new com.library.dexknife.shell.Callback<Exception>() {
            @Override
            public void callback(Exception e) {
                if (callback != null) {
                    callback.callback(Event.DECOMPILE_FAIL);
                }
            }
        });
        if (!decompileResult) {
            return null;
        }
        /**
         * 2.加固
         */
        handleCallback(callback, Event.ENCRYPTING);

        //加密dex
        if (!encryptDex(apk, decompile)) {
            handleCallback(callback, Event.ENCRYPT_FAIL);
            return null;
        }
        //加固转移
        if (!jiagu(decompile)) {
            handleCallback(callback, Event.ENCRYPT_FAIL);
            return null;
        }
        //保存签名信息
        signatureProtect(apk, decompile);

        //修改配置文件
        if (!updateMenifest(new File(decompile, "AndroidManifest.xml"))) {
            handleCallback(callback, Event.MENIFEST_FAIL);
            return null;
        }

        /**
         * 3.recompile apk
         */
        handleCallback(callback, Event.RECOMPILING);
        String shellNanme = com.library.dexknife.shell.utils.FileHelper.getNoSuffixName(apk) + "_encrypted.apk";
        if (ISSHELL == true && SHELLAPKNAME != null && SHELLAPKNAME.length() > 0) {
            shellNanme = com.library.dexknife.shell.utils.FileHelper.getNoSuffixName(apk) + "_" + SHELLAPKNAME + ".apk";
        }
        File encryptedApk = new File(apk.getParentFile(), shellNanme);
        boolean recompileResult = ApkToolPlus.recompile(decompile, encryptedApk, new com.library.dexknife.shell.Callback<Exception>() {
            @Override
            public void callback(Exception e) {
                handleCallback(callback, Event.RECOMPILE_FAIL);
            }
        });
        if (!recompileResult) {
            return null;
        }
        /**
         * 4.资源混淆
         */
        if(ANDRESGUARD){
            handleCallback(callback, Event.ANDROIDRES);
            if(!new File(andres_pz).exists()){
                System.err.println("请添加资源混淆文件andresguard.xml，具体见说明");
            }else if(!new File(andres_map).exists()){
                System.err.println("请添加资源混淆文件resource_mapping.txt，具体见说明");
            }else{
                andResGuard(encryptedApk,config);
            }
        }
        /**
         * 5.sign apk
         */
        if (config != null) {
            handleCallback(callback, Event.SIGNING);
            File signedApk = ApkToolPlus.signApk(encryptedApk, config);
            //FileHelper.cleanDirectory(decompile);
            if (ISSHELL == true && (SHELLAPKNAME == null || SHELLAPKNAME.length() < 1)) {
                apk.delete();
                com.library.dexknife.shell.utils.FileHelper.copyFile(signedApk,new File(apk.getAbsolutePath()));
                signedApk.delete();
                /**
                 * 6.zipalign 需要配置环境变量
                 */
                handleCallback(callback, Event.ZIPALIGN);
                Cmd.exec("zipalign -v 4 "+apk.getAbsolutePath()+" "+apk.getAbsolutePath().replace(".apk","_zipalign.apk"));
                System.out.println("加固apk完成地址==:" + apk.getAbsolutePath().replace(".apk","_zipalign.apk"));
                apk.delete();
            } else {
                /**
                 * 6.zipalign 需要配置环境变量
                 */
                handleCallback(callback, Event.ZIPALIGN);
                Cmd.exec("zipalign -v 4 "+signedApk.getAbsolutePath()+" "+signedApk.getAbsolutePath().replace(".apk","_zipalign.apk"));
                System.out.println("加固apk完成地址==:" + signedApk.getAbsolutePath().replace(".apk","_zipalign.apk"));
                signedApk.delete();
            }

            return signedApk;
        }

        com.library.dexknife.shell.utils.FileHelper.cleanDirectory(decompile);
        return encryptedApk;
    }


    /**
     * 资源混淆
     * @param apkFile
     */
    private static void andResGuard(File apkFile,KeystoreConfig config){
        andResGuardFile=new File(workDir+"\\com.library.dexknife.doaction.AndResGuard");
        if(!andResGuardFile.exists()){
            andResGuardFile.mkdir();
        }
        //资源混淆
        try {
            com.library.dexknife.shell.res.Configuration configuration=new com.library.dexknife.shell.res.Configuration(new File(andres_pz),new File(andres_map),new File(config.keystorePath),config.keystorePassword,config.alias,config.aliasPassword);
            com.library.dexknife.shell.res.ApkDecoder apkDecoder=new com.library.dexknife.shell.res.ApkDecoder(configuration);
            apkDecoder.setApkFile(apkFile);
            apkDecoder.setOutDir(andResGuardFile);
            apkDecoder.decode();

            generalUnsignApk(apkDecoder.getCompressData(),apkDecoder.getOutDir(),apkFile,apkDecoder.getConfig());
        } catch (AndrolibException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DirectoryException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成apk文件
     * @param compressData
     * @param mOutDir
     * @param apk
     * @param config
     * @throws IOException
     * @throws InterruptedException
     */
    private static void generalUnsignApk(HashMap<String, Integer> compressData, File mOutDir, File apk, com.library.dexknife.shell.res.Configuration config) throws IOException, InterruptedException {
        File tempOutDir = new File(mOutDir.getAbsolutePath(), com.library.dexknife.shell.res.util.TypedValue.UNZIP_FILE_PATH);
        if (!tempOutDir.exists()) {
            System.err.printf("Missing apk unzip files, path=%s\n", tempOutDir.getAbsolutePath());
            System.exit(-1);
        }

        File[] unzipFiles = tempOutDir.listFiles();
        List<File> collectFiles = new ArrayList<>();
        for (File f : unzipFiles) {
            String name = f.getName();
            if (name.equals("res") || name.equals(config.mMetaName) || name.equals("resources.arsc")) {
                continue;
            }
            collectFiles.add(f);
        }

        File destResDir = new File(mOutDir.getAbsolutePath(), "res");
        //添加修改后的res文件
        if (!config.mKeepRoot && FileOperation.getlist(destResDir) == 0) {
            destResDir = new File(mOutDir.getAbsolutePath(), com.library.dexknife.shell.res.util.TypedValue.RES_FILE_PATH);
        }

        /**
         * NOTE:文件数量应该是一样的，如果不一样肯定有问题
         */
        File rawResDir = new File(tempOutDir.getAbsolutePath() + File.separator + "res");
        System.out.printf("DestResDir %d rawResDir %d\n", FileOperation.getlist(destResDir), FileOperation.getlist(rawResDir));
        if (FileOperation.getlist(destResDir) != FileOperation.getlist(rawResDir)) {
            throw new IOException(String.format(
                    "the file count of %s, and the file count of %s is not equal, there must be some problem\n",
                    rawResDir.getAbsolutePath(), destResDir.getAbsolutePath()));
        }
        if (!destResDir.exists()) {
            System.err.printf("Missing res files, path=%s\n", destResDir.getAbsolutePath());
            System.exit(-1);
        }
        //这个需要检查混淆前混淆后，两个res的文件数量是否相等
        collectFiles.add(destResDir);
        File rawARSCFile = new File(mOutDir.getAbsolutePath() + File.separator + "resources.arsc");
        if (!rawARSCFile.exists()) {
            System.err.printf("Missing resources.arsc files, path=%s\n", rawARSCFile.getAbsolutePath());
            System.exit(-1);
        }
        collectFiles.add(rawARSCFile);
//        apk.delete();//删除临时文件
        FileOperation.zipFiles(collectFiles,apk , compressData);

        if (!apk.exists()) {
            throw new IOException(String.format(
                    "can not found the unsign apk file path=%s",
                    apk.getAbsolutePath()));
        }
    }
    /**
     * 获取签名存入
     * @param apk
     * @param decompile
     */
    private static void signatureProtect(File apk, File decompile) {
        try (ApkParser parser = new ApkParser(apk)) {
            List<CertificateMeta> certList = parser.getCertificateMetaList();
            String certMD5 = certList.get(0).getCertMd5();
            byte[] encryptData = com.library.dexknife.shell.utils.DataProtector.encryptXXTEA(certMD5.getBytes());
            FileUtils.writeByteArrayToFile(new File(decompile, "assets/app.key"), encryptData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleCallback(com.library.dexknife.shell.Callback<Event> callback, Event event) {
        if (callback != null) {
            callback.callback(event);
        }
    }

    /**
     * 更新xml
     * @param menifest
     * @return
     */
    private static boolean updateMenifest(File menifest) {
        XMLWriter writer = null;
        try {
            SAXReader reader = new SAXReader();
            org.dom4j.Document document = reader.read(menifest);
            Element rootElement = document.getRootElement();

            Element applicationElement = rootElement.element("application");
            Attribute appNameAttribute = applicationElement.attribute("name");
            if (appNameAttribute != null) {
                String appName = appNameAttribute.getValue();
                appNameAttribute.setValue(PROXY_APPLICATION_NAME);
                applicationElement.addElement("meta-data")
                        .addAttribute("android:name", METADATA_SRC_APPLICATION)
                        .addAttribute("android:value", appName);
            } else {
                applicationElement.addAttribute("android:name", PROXY_APPLICATION_NAME);
            }
            OutputFormat format = OutputFormat.createPrettyPrint();
            // OutputFormat format = OutputFormat.createCompactFormat();
            format.setEncoding("UTF-8");
            writer = new XMLWriter(new FileOutputStream(menifest), format);
            writer.write(document);
            writer.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 加固
     * @param decompileDir
     * @return
     */
    private static boolean jiagu(File decompileDir) {
        if (!jiaguZip.exists()) {
            if (!JIAGU_ZIP_PATH.contains(":")) {
                if (!ClassHelper.releaseResourceToFile(JIAGU_ZIP_PATH, jiaguZip)) {
                    return false;
                }
            } else {
                if (!com.library.dexknife.shell.utils.FileHelper.copy(new File(JIAGU_ZIP_PATH), jiaguZip)) {
                    return false;
                }
            }
        }
        if (com.library.dexknife.shell.utils.FileHelper.exists(jiaguZip)) {
            jiaguZip.deleteOnExit();
        }

        File smali = new File(decompileDir, "smali");
        com.library.dexknife.shell.utils.FileHelper.delete(smali);
        File lib = new File(decompileDir, "lib");
        String[] platforms = lib.list();
        boolean isHasLib = lib.exists() && platforms != null && platforms.length > 0;

        ZipHelper.list(jiaguZip, new ZipHelper.FileFilter() {
            @Override
            public void handle(ZipFile zipFile, FileHeader fileHeader) {
                if (fileHeader.getFileName().startsWith("smali")) {
                    if (!ZipHelper.unzip(zipFile, fileHeader, smali.getParentFile())) {
                        Debug.e(fileHeader.getFileName() + " unzip failure from " + zipFile.getFile().getAbsolutePath());
                    }
                } else if (fileHeader.getFileName().startsWith("libs")) {
                    if (!ZipHelper.unzip(zipFile, fileHeader, decompileDir)) {
                        Debug.e(fileHeader.getFileName() + " unzip failure from " + zipFile.getFile().getAbsolutePath());
                    }
                }
            }
        });

        File libs = new File(decompileDir, "libs");
        if (isHasLib) {
            for (String platform : platforms) {
                File libFile = new File(libs, platform + "/libprotected.so");
                File libSOFile =new File(lib, platform + "/" + libFile.getName());
                if (libFile.exists() &&  !libSOFile.exists()) {
                    com.library.dexknife.shell.utils.FileHelper.move(libFile, new File(lib, platform + "/" + libFile.getName()));
                }
            }
        } else {
            com.library.dexknife.shell.utils.FileHelper.move(libs, lib);
        }
        com.library.dexknife.shell.utils.FileHelper.delete(libs);
        return true;
    }

    /**
     * 删除文件
     * @param file
     */
    private static void deleteFile(File file) {
        if (file.exists()) {//判断文件是否存在
            if (file.isFile()) {//判断是否是文件
                file.delete();//删除文件
            } else if (file.isDirectory()) {//否则如果它是一个目录
                File[] files = file.listFiles();//声明目录下所有的文件 files[];
                for (int i = 0;i < files.length;i ++) {//遍历目录下所有的文件
                    JiaGu.deleteFile(files[i]);//把每个文件用这个方法进行迭代
                }
                file.delete();//删除文件夹
            }
        } else {
            System.out.println("所删除的文件不存在");
        }
    }

    /**
     * 加密dex
     * @param apk
     * @param decompileDir
     * @return
     */
    private static boolean encryptDex(File apk, File decompileDir) {
        if(decompileDir.isDirectory()){
            String[] filenames=decompileDir.list();
            int index=0;
            for (int i=0;i<filenames.length;i++){
                if(filenames[i].contains("smali")){
                    index+=1;
                    String dexName="classes2.dex";//加固之后的dex
                    String unzipDexName="classes.dex";//解压之后的dex
                    if(index!=1){
                        dexName="classes"+index+".dex";
                        unzipDexName="classes"+index+".dex";
                        deleteFile(new File(decompileDir, filenames[i]));//删除问价夹
                    }
                    File dexFile = new File(decompileDir, unzipDexName);
                    if (dexFile.exists()) {
                        dexFile.delete();
                    }
                    try {
                        ZipFile zipFile = new ZipFile(apk);
                        ZipHelper.unzip(zipFile,unzipDexName, dexFile.getParentFile());
                    } catch (ZipException e) {
                        e.printStackTrace();
                        return false;
                    }

                    File assets = new File(decompileDir, "assets");
                    assets.mkdirs();
                    assets = new File(decompileDir, "assets");

                    File encryptFile = new File(assets, JIAGU_DATA_BIN);
                    if(encryptFile.exists()){
                        encryptFile.delete();
                    }
                    encryptFile.mkdirs();
                    //加密
                    DataProtector.encrypt(dexFile,  new File(assets, JIAGU_DATA_BIN+"/"+"classes"+(index+1)+".dex"));
                    //修改加密文件后缀名
//                    DataProtector.encrypt(dexFile,  new File(assets, JIAGU_DATA_BIN+"/"+"libletao"+(index+1)+".jar"));
                    dexFile.delete();
                }
            }
        }

        return true;
    }

    /**
     * 开始加固apk
     * @param apk
     * @param keystoreConfig
     * @return
     */
    public static File encryptApk(File apk, KeystoreConfig keystoreConfig){

        return JiaGu.encrypt(apk, keystoreConfig, new Callback<Event>() {
            @Override
            public void callback(Event event) {
                switch (event){
                    case DECOMPILEING:
                        break;
                    case ENCRYPTING:
                        break;
                    case RECOMPILING:
                        break;
                    case SIGNING:
                        break;
                    case DECOMPILE_FAIL:
                        break;
                    case RECOMPILE_FAIL:
                        break;
                    case ENCRYPT_FAIL:
                        break;
                    case MENIFEST_FAIL:
                        break;
                }
            }
        });
    }
}
