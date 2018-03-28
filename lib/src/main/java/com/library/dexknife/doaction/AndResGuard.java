package com.library.dexknife.doaction;

import com.library.dexknife.shell.jiagu.KeystoreConfig;
import com.library.dexknife.shell.res.ApkDecoder;
import com.library.dexknife.shell.res.Configuration;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import brut.androlib.AndrolibException;
import brut.directory.DirectoryException;

/**
 * @author Admin
 * @version 1.0
 * @date 2017/6/10
 */

public class AndResGuard {
    public static void main(String[] args) {
        KeystoreConfig keystoreConfig=new KeystoreConfig();
        keystoreConfig.keystorePassword="123456";
        keystoreConfig.aliasPassword="123456";
        keystoreConfig.alias="test";
        keystoreConfig.keystorePath="./test.jks";
        //资源混淆
        try {
            Configuration configuration=new Configuration(
                    new File("./andreshuard.xml"),
                    new File("./resource_mapping.txt"),
                    new File(keystoreConfig.keystorePath),keystoreConfig.keystorePassword,keystoreConfig.aliasPassword,keystoreConfig.aliasPassword);
            ApkDecoder apkDecoder=new ApkDecoder(configuration);
            apkDecoder.setApkFile(new File("./app/build/outputs/apk/app-debug.apk"));
            apkDecoder.setOutDir(new File("./app/build/outputs/apk/after"));
            apkDecoder.decode();
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
        }
    }
}
