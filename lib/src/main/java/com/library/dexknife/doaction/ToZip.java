package com.library.dexknife.doaction;

import com.library.dexknife.shell.AppManager;
import com.library.dexknife.shell.jiagu.JiaGu;

import java.io.File;

/**
 * 输出壳zip
 *
 * @author Admin
 * @version 1.0
 * @date 2017/6/24
 */

public class ToZip {

    /**
     * 注意代码执行与命令行执行根目录区分(一个点与两个点区别)
     * @param args
     */
    public static void main(String[] args) {

        UpdateZipTask task = new UpdateZipTask();
        AppManager.APKTOOLJARPATH=Constant.APKTOOL_PATH;
        task.setProjectDir(new File(Constant.SHELL_FROM));
        String packagePath = JiaGu.class.getPackage().getName().replaceAll("\\.", "/");
        task.addOutFile(new File(Constant.SHELL_TO));
        task.execute();

    }

}
