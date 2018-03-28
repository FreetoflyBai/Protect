package com.library.dexknife.doaction;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * author   : kevin.bai
 * date      : 2018/3/16 下午4:29
 * qq        :904869397@qq.com
 */

public class GradleFind {

    private static Properties mProperties = null;
    private static String gradleFile=Constant.GRADLE_FILE;

    private static void initProperties() {
        if (mProperties == null) {
            mProperties = new Properties();
        }
        InputStream is = null;
        try {
            is = new FileInputStream(gradleFile);
            mProperties.load(is);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getValue(String key) {
        String value = "";
        try {
            initProperties();
            value = mProperties.getProperty(key);
            return value;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }


}
