package com.android.echocutter;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.android.echocutter.utils.AESHelper;
import com.android.echocutter.utils.DexReplace;

public class BaseApplication extends Application {

    private  final String TAG = BaseApplication.class.getSimpleName();
    private String srcAppClassName = null;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        AESHelper.loadLibrary();
        srcAppClassName= DexReplace.getInstance().getSrc(this);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        // 签名检查
        // 注意：不要在attachBaseContext方法中调用，因为应用上下文还没初始化完成
//        SignatureUtils.checkSign(getApplicationContext());
        if (!TextUtils.isEmpty(srcAppClassName)) {
            Application app = DexReplace.getInstance().changeTopApplication(srcAppClassName);
            if (app != null) {
                app.onCreate();
            } else {
                Log.e(TAG, "changeTopApplication failure!!!");
            }
        }

    }

}
