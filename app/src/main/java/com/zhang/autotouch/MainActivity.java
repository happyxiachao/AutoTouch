package com.zhang.autotouch;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.baidu.ai.edge.core.base.BaseException;
import com.baidu.ai.edge.core.classify.ClassifyException;
import com.baidu.ai.edge.core.ocr.OcrInterface;
import com.tamsiree.rxkit.RxActivityTool;
import com.tamsiree.rxkit.RxTool;
import com.yorhp.recordlibrary.OnScreenShotListener;
import com.yorhp.recordlibrary.ScreenRecordUtil;
import com.yorhp.recordlibrary.ScreenShotUtil;
import com.yorhp.recordlibrary.ScreenUtil;
import com.zhang.autotouch.bean.TouchEvent;
import com.zhang.autotouch.bean.TouchPoint;
import com.zhang.autotouch.dialog.MenuDialog;
import com.zhang.autotouch.fw_permission.FloatWinPermissionCompat;
import com.zhang.autotouch.service.AutoTouchService;
import com.zhang.autotouch.service.FloatingService;
import com.zhang.autotouch.utils.AccessibilityUtil;
import com.zhang.autotouch.utils.ToastUtil;

import permison.PermissonUtil;


@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {

    private TextView tvStart;
    private final String STRING_START = "开始";
    private final String STRING_ACCESS = "无障碍服务";
    private final String STRING_ALERT = "悬浮窗权限";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RxTool.init(this);

        RxActivityTool.addActivity(this);
        tvStart = findViewById(R.id.tv_start);
        tvStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (tvStart.getText().toString()) {
                    case STRING_START:
                        ToastUtil.show(getString(R.string.app_name) + "已启用");
                        startService(new Intent(MainActivity.this, FloatingService.class));
                        moveTaskToBack(true);
//                        BaiduManager.getInstance().edgeStart();
                        TouchEvent.postStartAction(new TouchPoint(null,0,0,0));
                        break;
                    case STRING_ALERT:
                        requestPermissionAndShow();
                        break;
                    case STRING_ACCESS:
                        requestAcccessibility();
                        break;
                    default:
                        break;
                }
            }
        });
        ToastUtil.init(this);

        PermissonUtil.checkPermission(this, null, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        ScreenUtil.getScreenSize(this);
        /**
         * 初始化成功
         */
        ScreenShotUtil.getInstance().screenShot(this, new OnScreenShotListener() {
            @Override
            public void screenShot() {
                Log.i("","asdasdasd");
                Bitmap bitmap = ScreenShotUtil.getInstance().getScreenShot();
                get().doEdge(bitmap,null);
                AccessibilityUtil.saveMyBitmap("xiachao", bitmap);
//                iv_pre.setImageBitmap(ScreenShotUtil.getInstance().getScreenShot());
            }
        });

        get().init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkState();
    }

    private void checkState() {
        boolean hasAccessibility = AccessibilityUtil.isSettingOpen(AutoTouchService.class, MainActivity.this);
        boolean hasWinPermission = FloatWinPermissionCompat.getInstance().check(this);
        if (hasAccessibility) {
            if (hasWinPermission) {
                tvStart.setText(STRING_START);
            } else {
                tvStart.setText(STRING_ALERT);
            }
        } else {
            tvStart.setText(STRING_ACCESS);
        }
    }

    private void requestAcccessibility() {
        new AlertDialog.Builder(this).setTitle("无障碍服务未开启")
                .setMessage("你的手机没有开启无障碍服务，" + getString(R.string.app_name) + "将无法正常使用")
                .setPositiveButton("去开启", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 显示授权界面
                        try {
                            AccessibilityUtil.jumpToSetting(MainActivity.this);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton("取消", null).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ScreenRecordUtil.getInstance().destroy();
        get().release();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (callback != null) {
            callback.onActivityResult(requestCode, resultCode, data);
        }

    }

    /**
     * 开启悬浮窗权限
     */
    private void requestPermissionAndShow() {
        new AlertDialog.Builder(this).setTitle("悬浮窗权限未开启")
                .setMessage(getString(R.string.app_name) + "获得悬浮窗权限，才能正常使用应用")
                .setPositiveButton("去开启", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 显示授权界面
                        try {
                            FloatWinPermissionCompat.getInstance().apply(MainActivity.this);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton("取消", null).show();
    }
    MenuDialog.IOnActivityResult callback;
    public void setOnActivityCallback(MenuDialog.IOnActivityResult callback) {
        this.callback = callback;
    }




    private void initBaiduAi() {

    }

    OcrInterface mOcrManager;



    private void release() {
        get().release();
    }

    private BaiduManager get() {
        return BaiduManager.getInstance();
    }
}
