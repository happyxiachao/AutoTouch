package com.zhang.autotouch;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.baidu.ai.edge.core.base.BaseManager;
import com.tencent.mmkv.MMKV;

import java.io.File;

import xcrash.XCrash;

public class MyApplication extends Application {

    public static String baseDir = Environment.getExternalStorageDirectory() + "/ASceenUtil/";

    @Override
    public void onCreate() {
        super.onCreate();
        initDir();


    }


    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        String basePath = Environment.getExternalStorageDirectory().toString() + "/" + context.getPackageName();
        XCrash.InitParameters params = new XCrash.InitParameters();
        params.setAppVersion(BaseManager.VERSION);
        params.setLogDir(basePath + "/xcCrash");
        XCrash.init(this, params);
        MMKV.initialize(this);
        // XCrash.testJavaCrash(true); // 测试JAVA报错日志
        // XCrash.testNativeCrash(true); // 测试NATIVE报错日志
    }
    void initDir() {
        File file = new File(baseDir);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

}
