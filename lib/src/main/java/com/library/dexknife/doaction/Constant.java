package com.library.dexknife.doaction;

/**
 * author   : kevin.bai
 * date      : 2017/9/7 下午4:21
 * qq        :904869397@qq.com
 */

public class Constant {


    /**
     * 注意:
     * 代码执行(一个点)
     * 命令行执行(两个点)
     */

    public static String FileDir="../";


    /**
     * GradleFind
     */
    public static String GRADLE_FILE=FileDir+"gradle.properties";

    /**
     * zipToShell
     */

    public static String APKTOOL_PATH=FileDir+"apktool.jar";
    public static String SHELL_FROM=FileDir+"app/build/outputs/apk/app-debug.apk";
    public static String SHELL_TO=FileDir + UpdateZipTask.JIAGU_ZIP;


    /**
     * jiagu
     */
//    public static String KEY_PATH=FileDir+"com.android.echocutter.jks";
//    public static String KEY_PASSWORD="123456";
//    public static String KEY_ALIAS="com.android.echocutter";
//    public static String KEY_ALIAS_PASSWORD="123456";
//    public static String APK_FROM=FileDir+"app-release.apk";

    public static String KEY_PATH=GradleFind.getValue("KEY_PATH");
    public static String KEY_PASSWORD=GradleFind.getValue("KEY_PASSWORD");
    public static String KEY_ALIAS=GradleFind.getValue("KEY_ALIAS");
    public static String KEY_ALIAS_PASSWORD=GradleFind.getValue("KEY_ALIAS_PASSWORD");
    public static String APK_FROM=GradleFind.getValue("APK_FROM");
    public static String ANDRES_PZ=FileDir+"andreshuard.xml";
    public static String ANDRES_MAP=FileDir+"resource_mapping.txt";
    public static String APK_SUFFIX="jiagu";
    public static boolean ANDRESGUARD=GradleFind.getValue("ANDRESGUARD").equals("true")?true:false;



}
