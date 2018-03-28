package com.library.dexknife;
import com.library.dexknife.shell.jiagu.JiaGu;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import com.library.dexknife.doaction.UpdateZipTask;


/**
 * @author wangjun
 * @version 1.0
 */

public class BuildSimpleApkTask extends DefaultTask {
    @Input
    File file;
    @Input
    File copy2file;
    @Input
    String buildDirName="myapplication";
    @TaskAction
    void buildSimple() {
        UpdateZipTask task = new UpdateZipTask();

        task.setProjectDir(new File(buildDirName));

        String packagePath = JiaGu.class.getPackage().getName().replaceAll("\\.","/");
        //copy
        if(copy2file==null){
            task.addOutFile(new File("dexknife-wj/src/" + packagePath + "/" + UpdateZipTask.JIAGU_ZIP));
        }else{
            task.addOutFile(copy2file);
        }
        //rebuild
        if(file==null){
            task.addOutFile(new File("../qianfandu/src/main"+packagePath+"/" + UpdateZipTask.JIAGU_ZIP));
        }else {
            task.addOutFile(file);
        }


        task.execute();
    }
}
