package com.zhang.autotouch.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import androidx.annotation.RequiresApi;

import com.baidu.ai.edge.ui.view.model.BasePolygonResultModel;
import com.tamsiree.rxkit.RxActivityTool;
import com.tamsiree.rxkit.RxDeviceTool;
import com.yorhp.recordlibrary.ScreenShotUtil;
import com.zhang.autotouch.BaiduManager;
import com.zhang.autotouch.BuildConfig;
import com.zhang.autotouch.R;
import com.zhang.autotouch.TouchEventManager;
import com.zhang.autotouch.bean.TouchEvent;
import com.zhang.autotouch.bean.TouchPoint;
import com.zhang.autotouch.utils.DataUtil;
import com.zhang.autotouch.utils.DensityUtil;
import com.zhang.autotouch.utils.ToastUtil;
import com.zhang.autotouch.utils.WindowUtils;

import java.text.DecimalFormat;
import java.util.List;

import static com.zhang.autotouch.BaiduManager.DELAY_TIME;
import static com.zhang.autotouch.BaiduManager.getInstance;

/**
 * 无障碍服务-自动点击
 *
 * @date 2019/9/6 16:23
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class AutoTouchService extends AccessibilityService {

    private final String TAG = "AutoTouchService+++";
    //自动点击事件
    private TouchPoint autoTouchPoint;
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private TextView tvTouchPoint;
    //倒计时
    private float countDownTime;
    private DecimalFormat floatDf = new DecimalFormat("#0.0");
    //修改点击文本的倒计时
    private Runnable touchViewRunnable;
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        handler = new Handler();
        EventBus.getDefault().register(this);
        windowManager = WindowUtils.getWindowManager(this);
        DataUtil.createTestData();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReciverTouchEvent(TouchEvent event) {
        Log.d(TAG, "onReciverTouchEvent: " + event.toString());
        TouchEventManager.getInstance().setTouchAction(event.getAction());
        handler.removeCallbacks(autoTouchRunnable);
        switch (event.getAction()) {
            case TouchEvent.ACTION_START:
                autoTouchPoint = event.getTouchPoint();
                onAutoClick();
                break;
            case TouchEvent.ACTION_CONTINUE:
                if (autoTouchPoint != null) {
                    onAutoClick();
                }
                break;
            case TouchEvent.ACTION_PAUSE:
                handler.removeCallbacks(autoTouchRunnable);
                handler.removeCallbacks(touchViewRunnable);
                break;
            case TouchEvent.ACTION_STOP:
                handler.removeCallbacks(autoTouchRunnable);
                handler.removeCallbacks(touchViewRunnable);
                removeTouchView();
                autoTouchPoint = null;
                break;
        }
    }

    /**
     * 执行自动点击
     */
    private void onAutoClick() {
        if (autoTouchPoint != null) {
            handler.postDelayed(autoTouchRunnable, getDelayTime());
            showTouchView();
        }
    }

    // xiachao  aosdasdkj 这了执行自动
    private Runnable autoTouchRunnable = new Runnable() {
        @Override
        public void run() {
//            ToastUtil.show("执行");
            try {
                Bitmap bitmap = ScreenShotUtil.getInstance().getScreenShot();
                BaiduManager.getInstance().doEdge(bitmap, new BaiduManager.ResultCallback() {

                    @Override
                    public void onResult(List<BasePolygonResultModel> results) {
//                        if (BaiduManager.getInstance().getKeySet() == null || BaiduManager.getInstance().getKeySet().isEmpty()) {
//                            return;
//                        }
//                        boolean find = false;
//                        for (String key : BaiduManager.getInstance().getKeySet()) {
//                            for (BasePolygonResultModel resultModel : results) {
//                                Log.i("识别结果","识别结果："+resultModel.getName());//品哥，核心比对代码在这里
//                                if (key != null && resultModel.getName() != null && resultModel.getName().contains(key)) {
//                                    Log.i("KEYRESULT", "识别到了：" + resultModel.getName() +" " +
//                                            resultModel.getBounds().get(0).x +" " +
//                                            resultModel.getBounds().get(0).y +" " +
//                                            resultModel.getBounds().get(1).x +" " +
//                                            resultModel.getBounds().get(1).y +" " +
//                                            resultModel.getBounds().get(2).x +" " +
//                                            resultModel.getBounds().get(2).y +" " +
//                                            resultModel.getBounds().get(3).x +" " +
//                                            resultModel.getBounds().get(3).y +" " );
////                                    doClick(resultModel.getBounds())
//                                    final Point point = getCenterOfGravityPoint(resultModel.getBounds());
//                                    Log.i("KEYRESULT", "点击x:"+point.x + " y:"+point.y);
//                                    find = true;
//                                    RxActivityTool.currentActivity().runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            doClick(point.x , point.y);
//                                        }
//                                    });
//                                    break;
//                                }
//                            }
//                        }

                        if (DataUtil.getKeySet() == null || DataUtil.getKeySet().isEmpty()) {
                            return;
                        }
                        boolean find = false;
                        for (String key : DataUtil.getKeySet()) {
                            for (BasePolygonResultModel resultModel : results) {
                                Log.i("识别结果","识别结果："+resultModel.getName());//品哥，核心比对代码在这里
                                if (key != null && resultModel.getName() != null && resultModel.getName().contains(key)) {
                                    Log.i("KEYRESULT", "识别到了：" + resultModel.getName() +" " +
                                            resultModel.getBounds().get(0).x +" " +
                                            resultModel.getBounds().get(0).y +" " +
                                            resultModel.getBounds().get(1).x +" " +
                                            resultModel.getBounds().get(1).y +" " +
                                            resultModel.getBounds().get(2).x +" " +
                                            resultModel.getBounds().get(2).y +" " +
                                            resultModel.getBounds().get(3).x +" " +
                                            resultModel.getBounds().get(3).y +" " );
//                                    doClick(resultModel.getBounds())
                                    final Point point = getCenterOfGravityPoint(resultModel.getBounds());
                                    Log.i("KEYRESULT", "点击x:"+point.x + " y:"+point.y);
                                    find = true;
                                    RxActivityTool.currentActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            doClick(point.x , point.y);
                                        }
                                    });
                                    break;
                                }
                            }
                        }
                        if (!find) {
                            RxActivityTool.currentActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    onAutoClick();
                                }
                            });

                        }
                    }
                });
            } catch (Exception e) {
                if (e != null) {
                    ToastUtil.show("异常" + e.toString());
                    Log.e("", "" + e.toString());
                }
            }
        }
    };


    /**
     * 获取不规则多边形重心点
     *
     * @param mPoints
     * @return
     */
    public static Point getCenterOfGravityPoint(List<Point> mPoints) {
        Float area = 0.0f;//多边形面积
        Float x = 0.0f, y = 0.0f;// 重心的x、y
        for (int i = 1; i <= mPoints.size(); i++) {
            float iLat = mPoints.get(i % mPoints.size()).x;
            float iLng = mPoints.get(i % mPoints.size()).y;
            float nextLat = mPoints.get(i - 1).x;
            float nextLng = mPoints.get(i - 1).y;
            float temp = (iLat * nextLng - iLng * nextLat) / 2.0f;
            area += temp;
            x += temp * (iLat + nextLat) / 3.0f;
            y += temp * (iLng + nextLng) / 3.0f;
        }
        x = x / area;
        y = y / area;
        Point point = new Point(Math.round(x), Math.round(y));
        return point;
    }


    private void doClick(int x, int y) {
//        Log.d(TAG, "onAutoClick: " + "x=" + autoTouchPoint.getX() + " y=" + autoTouchPoint.getY());
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        GestureDescription gestureDescription = builder.addStroke(
                new GestureDescription.StrokeDescription(path, 0, 100))
                .build();
        dispatchGesture(gestureDescription, new AccessibilityService.GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d("AutoTouchService", "点击结束" + gestureDescription.getStrokeCount());
                if (BuildConfig.DEBUG) {
                    ToastUtil.show("点击OK");
//                    Bundle arguments = new Bundle();
//                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, Constents.mobile[mobile_j]);
//                    info.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                    RxActivityTool.currentActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                            //查找到聚焦的文本
                            AccessibilityNodeInfo accessibilityNodeInfo = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                            Bundle arguments = new Bundle();
                            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "android");
                            accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                        }
                    });

                }
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.d("AutoTouchService", "滑动取消");
            }
        }, null);
        onAutoClick();
    }


    private long getDelayTime() {
//        int random = (int) (Math.random() * (30 - 1) + 1);
//        return autoTouchEvent.getDelay() * 1000L + random;
//        return autoTouchPoint.getDelay() * 1000L;
        return DELAY_TIME * 1000L;
    }
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if(nodeInfo != null && "com.xhlis.lis.transport.transport".equals(nodeInfo.getPackageName().toString())){
            AccessibilityNodeInfo source = event.getSource();
            if (source != null & event.getClassName().equals("android.widget.EditText")) {
                Bundle arguments = new Bundle();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    String hintText  =event.getSource().getHintText().toString();
                            arguments.putCharSequence(AccessibilityNodeInfo
                            .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, DataUtil.get(hintText));
                    DataUtil.remove(hintText);
                }
                source.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                Log.d("AutoTouchService", "点击结束 输入完成");
            }
        }
//        if(nodeInfo != null && "com.xhlis.lis.transport.transport".equals(nodeInfo.getPackageName().toString())) {
//            List<AccessibilityNodeInfo> nodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.xhlis.lis.transport.transport:id/select");
//            List<AccessibilityNodeInfo> nodesInsert = nodeInfo.findAccessibilityNodeInfosByViewId("com.xhlis.lis.transport.transport:id/insert");
//            //回收ondeInfo,避免重复创建
//            nodeInfo.recycle();
//
////            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
////            //查找到聚焦的文本
////            AccessibilityNodeInfo accessibilityNodeInfo = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
//            //点击操作
//            AccessibilityNodeInfo checkinfo = nodes.get(0);
//            if(!checkinfo.isChecked()){
//                checkinfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                checkinfo.recycle();
//            }
//            //文本输入内容
//            Bundle arguments = new Bundle();
//            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "测试");
//            nodesInsert.get(0).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,arguments);
//        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        removeTouchView();
    }

    /**
     * 显示倒计时
     */
    private void showTouchView() {
        if (autoTouchPoint != null) {
            //创建触摸点View
            if (tvTouchPoint == null) {
                tvTouchPoint = (TextView) LayoutInflater.from(this).inflate(R.layout.window_touch_point, null);
            }
            //显示触摸点View
            if (windowManager != null && !tvTouchPoint.isAttachedToWindow()) {
                int width = DensityUtil.dip2px(this, 40);
                int height = DensityUtil.dip2px(this, 40);
                WindowManager.LayoutParams params = WindowUtils.newWmParams(width, height);
                params.gravity = Gravity.START | Gravity.TOP;
//                params.x = autoTouchPoint.getX() - width / 2;
//                params.y = autoTouchPoint.getY() - width;

                params.x = RxDeviceTool.getScreenWidth(RxActivityTool.currentActivity()) - 100;
                params.y = RxDeviceTool.getScreenHeight(RxActivityTool.currentActivity()) - 100;
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                windowManager.addView(tvTouchPoint, params);
            }
            //开启倒计时
//            countDownTime = autoTouchPoint.getDelay();
            countDownTime = DELAY_TIME;
            if (touchViewRunnable == null) {
                touchViewRunnable = new Runnable() {
                    @Override
                    public void run() {
                        handler.removeCallbacks(touchViewRunnable);
                        Log.d("触摸倒计时", countDownTime + "");
                        if (countDownTime > 0) {
                            float offset = 0.1f;
                            tvTouchPoint.setText(floatDf.format(countDownTime));
                            countDownTime -= offset;
                            handler.postDelayed(touchViewRunnable, (long) (1000L * offset));
                        } else {
                            removeTouchView();
                        }
                    }
                };
            }
            handler.post(touchViewRunnable);
        }
    }

    private void removeTouchView() {
        if (windowManager != null && tvTouchPoint.isAttachedToWindow()) {
            windowManager.removeView(tvTouchPoint);
        }
    }


}
