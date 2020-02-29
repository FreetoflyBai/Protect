## 使用步骤

#### 1：配置参数
    所有参数配置均在gradle.properties文件中，配置完成再执行以下步骤

#### 2：修改壳路径
    命令行 gradle modifyPackagePath 
    如果不需要修改壳部分代码，可省略此步骤 
    如果手动修改壳项目包路径，请同时修改以下内容：
      - 修改jni中包路径对应字符串（.cpp .h）
      - 修改lib模块中JiaGu.java中变量PROXY_APPLICATION_NAME的值

#### 2：编译壳
    命令行 gradle clean build 
    编译app模块，生成apk文件，用来制作壳文件 


#### 3：生成壳
    命令行 gradle zipToShell 
    将apk文件打包成壳文件（jiagu.zip）

#### 4：加固apk
    命令行 gradle zipToApk 
