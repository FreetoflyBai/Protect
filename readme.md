## 使用步骤

#### 1：配置参数
    所有参数配置均在gradle.properties文件中，配置完成再执行以下步骤</br></br>

#### 2：修改壳路径
    命令行 gradle modifyPackagePath </br></br>
    如果不需要修改壳部分代码，可省略此步骤</br>
    如果手动修改壳项目包路径，请同时修改以下内容：</br>
      - 修改jni中包路径对应字符串（.cpp .h）</br>
      - 修改lib模块中JiaGu.java中变量PROXY_APPLICATION_NAME的值</br></br>

#### 2：编译壳
    命令行 gradle clean build</br></br>
    编译app模块，生成apk文件，用来制作壳文件</br></br>


#### 3：生成壳
    命令行 gradle zipToShell</br></br>
    将apk文件打包成壳文件（jiagu.zip）</br></br>

#### 4：加固apk
    命令行 gradle zipToApk</br></br>

### 以上为分步执行，也可以直接执行以下组合命令：</br></br>

    gradle modifyPackagePath clean build zipToShell zipToApk
