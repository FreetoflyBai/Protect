package com.library.dexknife.doaction;

import com.library.dexknife.shell.AppManager;
import com.library.dexknife.shell.Callback;
import com.library.dexknife.shell.jiagu.JiaGu;
import com.library.dexknife.shell.jiagu.KeystoreConfig;

import java.io.File;

/**
 * 加固
 */
public class ToJiaGu {

    public static void main(String[] args) {
        KeystoreConfig keystoreConfig=new KeystoreConfig();
        keystoreConfig.keystorePath=Constant.KEY_PATH;
        keystoreConfig.keystorePassword=Constant.KEY_PASSWORD;
        keystoreConfig.alias=Constant.KEY_ALIAS;
        keystoreConfig.aliasPassword=Constant.KEY_ALIAS_PASSWORD;
        JiaGu.ANDRESGUARD=Constant.ANDRESGUARD;
        JiaGu.andres_pz=Constant.ANDRES_PZ;
        JiaGu.andres_map=Constant.ANDRES_MAP;
        JiaGu.ISSHELL=true;
        JiaGu.SHELLAPKNAME=Constant.APK_SUFFIX;
        AppManager.APKTOOLJARPATH=Constant.APKTOOL_PATH;
        JiaGu.JIAGU_ZIP_PATH=Constant.SHELL_TO;
        encryptApk(new File(Constant.APK_FROM),keystoreConfig);


    }
    public static File encryptApk(File apk, KeystoreConfig keystoreConfig){

        return JiaGu.encrypt(apk, keystoreConfig, new Callback<JiaGu.Event>() {
            @Override
            public void callback(JiaGu.Event event) {
                switch (event){
                    case DECOMPILEING:
                        System.out.println("正在反编译");
                        break;
                    case ENCRYPTING:
                        System.out.println("正在加固");
                        break;
                    case ANDROIDRES:
                        System.out.println("正在混淆资源");
                        break;
                    case RECOMPILING:
                        System.out.println("正在回编译");
                        break;
                    case SIGNING:
                        System.out.println("正在签名");
                        break;
                    case ZIPALIGN:
                        System.out.println("正在zipalign");
                        break;
                    case DECOMPILE_FAIL:
                        System.out.println("反编译失败");
                        break;
                    case RECOMPILE_FAIL:
                        System.out.println("回编译失败");
                        break;
                    case ENCRYPT_FAIL:
                        System.out.println("加固失败");
                        break;
                    case MENIFEST_FAIL:
                        System.out.println("清单文件解析失败");
                        break;


                }
            }
        });
    }

}
