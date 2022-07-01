package com.zhang.autotouch.dialog;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baidu.ai.edge.ui.view.model.BasePolygonResultModel;
import com.tamsiree.rxkit.RxActivityTool;
import com.tamsiree.rxkit.RxDeviceTool;
import com.yorhp.recordlibrary.OnScreenShotListener;
import com.yorhp.recordlibrary.ScreenRecordUtil;
import com.yorhp.recordlibrary.ScreenShotUtil;
import com.yorhp.recordlibrary.ScreenUtil;
import com.zhang.autotouch.BaiduManager;
import com.zhang.autotouch.MainActivity;
import com.zhang.autotouch.R;
import com.zhang.autotouch.TouchEventManager;
import com.zhang.autotouch.adapter.TouchPointAdapter;
import com.zhang.autotouch.bean.TouchEvent;
import com.zhang.autotouch.bean.TouchPoint;
import com.zhang.autotouch.utils.AccessibilityUtil;
import com.zhang.autotouch.utils.DensityUtil;
import com.zhang.autotouch.utils.DialogUtils;
import com.zhang.autotouch.utils.GsonUtils;
import com.zhang.autotouch.utils.SpUtils;
import com.zhang.autotouch.utils.ToastUtil;

import java.util.ArrayList;
import java.util.List;

import permison.PermissonUtil;

import static com.yorhp.recordlibrary.ScreenRecordActivity.REQUEST_MEDIA_PROJECTION;

public class MenuDialog extends BaseServiceDialog implements View.OnClickListener {

    private Button btStop;
    private RecyclerView rvPoints;

    private AddPointDialog addPointDialog;
    private Listener listener;
    private TouchPointAdapter touchPointAdapter;
    private RecordDialog recordDialog;

    public MenuDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_menu;
    }

    @Override
    protected int getWidth() {
        return DensityUtil.dip2px(getContext(), 350);
    }

    @Override
    protected int getHeight() {
        return WindowManager.LayoutParams.WRAP_CONTENT;
    }

    @Override
    protected void onInited() {
        setCanceledOnTouchOutside(true);
        findViewById(R.id.bt_exit).setOnClickListener(this);
        findViewById(R.id.bt_add).setOnClickListener(this);
        findViewById(R.id.bt_record).setOnClickListener(this);
        btStop = findViewById(R.id.bt_stop);
        btStop.setOnClickListener(this);
        rvPoints = findViewById(R.id.rv);
        touchPointAdapter = new TouchPointAdapter();
        touchPointAdapter.setOnItemClickListener(new TouchPointAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, String key) {
//                btStop.setVisibility(View.VISIBLE);
//                dismiss();
//                TouchEvent.postStartAction(touchPoint);
                ToastUtil.show(key);
            }
        });
        rvPoints.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPoints.setAdapter(touchPointAdapter);
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (TouchEventManager.getInstance().isPaused()) {
                    TouchEvent.postContinueAction();
                    ToastUtil.show("继续识别和控制");
                    BaiduManager.getInstance().autoStart();
                }
            }
        });
        findViewById(R.id.saveDelay).setOnClickListener(this);
//        ((MainActivity)RxActivityTool.currentActivity()).setOnActivityCallback(new IOnActivityResult() {
//            @Override
//            public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//                MediaProjectionManager mMediaProjectionManager = (MediaProjectionManager) RxActivityTool.currentActivity().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
//                RxActivityTool.currentActivity().startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(),REQUEST_MEDIA_PROJECTION);
//                MediaProjection mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
//                VirtualDisplay mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
//                        RxDeviceTool.getScreenWidth(getActivity()), RxDeviceTool.getScreenHeight(getActivity()), RxDeviceTool.getScreenDensity(getActivity()),
//                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
//                        mImageReader.getSurface(), null, null);
//
//                if (mVirtualDisplay == null) {
//                    return;
//                }
//                mVirtualDisplay.release();
//                mVirtualDisplay = null;
//            }
//        });
//        TouchEvent.postStartAction(new TouchPoint(null,0,0,0));
    }

    private MainActivity getActivity() {
        return (MainActivity) RxActivityTool.currentActivity();
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d("啊实打实", "onStart");
        //如果正在触控，则暂停
        ToastUtil.show("暂停识别和控制");
        BaiduManager.getInstance().pause();
        TouchEvent.postPauseAction();
        if (touchPointAdapter != null) {
//            List<TouchPoint> touchPoints = SpUtils.getTouchPoints(getContext());
//            Log.d("啊实打实", GsonUtils.beanToJson(touchPoints));
            List<String> list = new ArrayList<>();
            for (String key : BaiduManager.getInstance().getKeySet()) {
                list.add(key);
            }
            touchPointAdapter.setTouchPointList(list);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_add:
                DialogUtils.dismiss(addPointDialog);
                addPointDialog = new AddPointDialog(getContext());
                addPointDialog.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        MenuDialog.this.show();
                    }
                });
                addPointDialog.show();
                dismiss();
                test();
                break;
            case R.id.bt_record:
                dismiss();
                if (listener != null) {
                    listener.onFloatWindowAttachChange(false);
                    if (recordDialog == null) {
                        recordDialog = new RecordDialog(getContext());
                        recordDialog.setOnDismissListener(new OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                listener.onFloatWindowAttachChange(true);
                                MenuDialog.this.show();
                            }
                        });
                        recordDialog.show();
                    }
                }
                break;
            case R.id.bt_stop:
                btStop.setVisibility(View.GONE);
                TouchEvent.postStopAction();
                ToastUtil.show("已停止触控");
                break;
            case R.id.bt_exit:
                TouchEvent.postStopAction();
                if (listener != null) {
                    listener.onExitService();
                }
                break;

            case R.id.saveDelay: {
                try {
                    String delay = ((EditText) (findViewById(R.id.delayEt))).getText().toString();
                    int delayInt = Integer.parseInt(delay);
                    BaiduManager.getInstance().editDelaytime(delayInt);
                } catch (Exception e) {
                    if (e != null) {
                        Log.e("", "" + e.toString());
                    }
                }


                break;
            }

        }
    }

    int inn = 1;

    private void test() {
//        getWindow().getDecorView().setDrawingCacheEnabled(true);
//        Bitmap bmp = getWindow().getDecorView().getDrawingCache();
        try {
//            Bitmap bmp = RxDeviceTool.captureWithoutStatusBar(RxActivityTool.currentActivity());
//            PermissonUtil.checkPermission(getActivity(), null, Manifest.permission.WRITE_EXTERNAL_STORAGE);
//            ScreenUtil.getScreenSize(getActivity());
//            Bitmap bitmap = ScreenRecordUtil.getInstance().getScreenShot();
//            AccessibilityUtil.saveMyBitmap("xiachao" + inn, bitmap);
            //第一次会自动申请录屏权限
//            ScreenRecordUtil.getInstance().screenShot(RxActivityTool.currentActivity(), new OnScreenShotListener() {
//                @Override
//                public void screenShot() {
//                    //可以获取截图，可以多次调用
//                    try {
//                        Bitmap bitmap = ScreenRecordUtil.getInstance().getScreenShot();
//                        AccessibilityUtil.saveMyBitmap("xiachao" + inn, bitmap);
//                        //最后关闭录屏服务
//                        ScreenRecordUtil.getInstance().destroy();
//                    } catch (Exception e) {
//                        if (e != null) {
//                            Log.e("",""+e.toString());
//                        }
//                    }
//
//                }
//            });

            /**
             * 初始化成功
             */
//            ScreenShotUtil.getInstance().screenShot(RxActivityTool.currentActivity(), new OnScreenShotListener() {
//                @Override
//                public void screenShot() {
//                    AccessibilityUtil.saveMyBitmap("xiachao", ScreenShotUtil.getInstance().getScreenShot());
////                iv_pre.setImageBitmap(ScreenShotUtil.getInstance().getScreenShot());
//                }
//            });

            Bitmap bitmap = ScreenShotUtil.getInstance().getScreenShot();
            BaiduManager.getInstance().doEdge(bitmap, new BaiduManager.ResultCallback() {
                @Override
                public void onResult(List<BasePolygonResultModel> results) {
                    if (results != null) {
                        for (int i = 0; i < results.size(); i++) {
                            Log.i("OCR识别结果", "i" + " " + results.get(i).getName());
                        }
                    }
                }
            });
//            AccessibilityUtil.saveMyBitmap("xiachao", ScreenShotUtil.getInstance().getScreenShot());


        } catch (Exception e) {
            if (e != null) {
                Log.e("", "" + e.toString());
            }
        }

    }


    private void testShoot() {

    }


    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        /**
         * 悬浮窗显示状态变化
         *
         * @param attach
         */
        void onFloatWindowAttachChange(boolean attach);

        /**
         * 关闭辅助
         */
        void onExitService();
    }

    public interface IOnActivityResult {
        void onActivityResult(int requestCode, int resultCode, @Nullable Intent data);
    }

}
