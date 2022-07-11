package com.zhang.autotouch;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.util.ArraySet;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.baidu.ai.edge.core.base.BaseConfig;
import com.baidu.ai.edge.core.base.BaseException;
import com.baidu.ai.edge.core.base.CallException;
import com.baidu.ai.edge.core.classify.ClassificationResultModel;
import com.baidu.ai.edge.core.classify.ClassifyException;
import com.baidu.ai.edge.core.classify.ClassifyInterface;
import com.baidu.ai.edge.core.classify.ClassifyOnline;
import com.baidu.ai.edge.core.davinci.DDKDaVinciConfig;
import com.baidu.ai.edge.core.davinci.DDKDavinciManager;
import com.baidu.ai.edge.core.ddk.DDKConfig;
import com.baidu.ai.edge.core.ddk.DDKManager;
import com.baidu.ai.edge.core.detect.DetectInterface;
import com.baidu.ai.edge.core.detect.DetectOnline;
import com.baidu.ai.edge.core.detect.DetectionResultModel;
import com.baidu.ai.edge.core.infer.InferConfig;
import com.baidu.ai.edge.core.infer.InferManager;
import com.baidu.ai.edge.core.ocr.OcrInterface;
import com.baidu.ai.edge.core.ocr.OcrResultModel;
import com.baidu.ai.edge.core.segment.SegmentInterface;
import com.baidu.ai.edge.core.segment.SegmentationResultModel;
import com.baidu.ai.edge.core.snpe.SnpeConfig;
import com.baidu.ai.edge.core.snpe.SnpeManager;
import com.baidu.ai.edge.core.util.FileUtil;
import com.baidu.ai.edge.core.util.Util;
import com.baidu.ai.edge.ui.activity.ResultListener;
import com.baidu.ai.edge.ui.view.adapter.DetectResultAdapter;
import com.baidu.ai.edge.ui.view.model.BasePolygonResultModel;
import com.baidu.ai.edge.ui.view.model.BaseResultModel;
import com.baidu.ai.edge.ui.view.model.ClassifyResultModel;
import com.baidu.ai.edge.ui.view.model.DetectResultModel;
import com.baidu.ai.edge.ui.view.model.OcrViewResultModel;
import com.baidu.ai.edge.ui.view.model.SegmentResultModel;
import com.tamsiree.rxkit.RxActivityTool;
import com.tencent.mmkv.MMKV;
import com.yorhp.recordlibrary.ScreenShotUtil;
import com.zhang.autotouch.utils.ToastUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BaiduManager {

    private static final int REQUEST_PERMISSION_CODE_CAMERA = 100;
    private static final int REQUEST_PERMISSION_CODE_STORAGE = 101;

    private static final int INTENT_CODE_PICK_IMAGE = 100;

    protected static final int PAGE_CAMERA = 100;
    protected static final int PAGE_RESULT = 101;

    protected int pageCode;

    private float resultConfidence;

    private float realtimeConfidence;


    Handler uiHandler;

    private String name;

    private boolean isProcessingAutoTakePicture = false;

    protected boolean canAutoRun = false;

    private boolean autoTakeFlag = true;

    public static final int MODEL_OCR = 100;

    public static final int MODEL_SEGMENT = 6;

    public static final int MODEL_DETECT = 2;

    public static final int MODEL_CLASSIFY = 1;

    private Bitmap detectBitmapCache;

    private List<BasePolygonResultModel> detectResultModelCache;

    private List<BasePolygonResultModel> segmentResultModelCache;

    private List<BasePolygonResultModel> ocrResultModelCache;

    private List<ClassifyResultModel> classifyResultModelCache;

    // 是离线还是在线模式
    private boolean isOnline = false;
    protected int model = MODEL_OCR;
//    protected int model = MODEL_SEGMENT;


    static final int ACTION_TYPE_SCAN = 0;

    static final int ACTION_TYPE_TAKE = 1;

    private int actionType = ACTION_TYPE_TAKE;


    private String version = "";
    private String ak;
    private String sk;
    private String apiUrl;
    private String soc;
    private ArrayList<String> socList = new ArrayList<>();
    private int type;
    // 请替换为您的序列号
    private static final String SERIAL_NUM = null; //null

    public static BaiduManager getInstance() {
        if (instance == null) {
            instance = new BaiduManager();
        }
        return instance;
    }

    static BaiduManager instance;


    private String serialNum;

    ClassifyInterface mClassifyDLManager;
    ClassifyInterface mOnlineClassify;
    DetectInterface mDetectManager;
    DetectInterface mOnlineDetect;
    SegmentInterface mSegmentManager;
    OcrInterface mOcrManager;
    public static final int TYPE_INFER = 0;
    public static final int TYPE_DDK150 = 1;
    public static final int TYPE_DDK200 = 11;
    public static final int TYPE_SNPE = 2;
    public static final int TYPE_DDK_DAVINCI = 10;

    private static final int CODE_FOR_WRITE_PERMISSION = 0;

    private int platform = TYPE_INFER;

    private boolean isInitializing = false;

    private boolean hasOnlineApi = false;
    // 模型加载状态
    private boolean modelLoadStatus = false;

    Set<String> keySet = new ArraySet<>();
    public void init() {
        initConfig();
        final AlertDialog.Builder agreementDialog = new AlertDialog.Builder(getActivity())
                .setTitle("允许“百度EasyDL”使用数据？")
                .setMessage("可能同时包含无线局域网和蜂窝移动数据")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("允许", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences sp = getActivity().getSharedPreferences("demo_auth_info",
                                Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putBoolean("isAgree", true);
                        editor.commit();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                startUICameraActivity();
                            }
                        }).start();
                        dialog.cancel();
                    }
                })
                .setNegativeButton("不允许", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        SharedPreferences sp = getActivity().getSharedPreferences("demo_auth_info", Context.MODE_PRIVATE);
        boolean hasAgree = sp.getBoolean("isAgree", false);
        boolean checkChip = checkChip();
        if (hasAgree) {
            Log.i(this.getClass().getSimpleName(), "socList:" + socList.toString()
                    + ", Build.HARDWARE is :" + Build.HARDWARE + "soc:" + soc);
            if (checkChip) {
                startUICameraActivity();
            } else {
                Toast.makeText(getActivity(), "socList:" + socList.toString()
                                + ", Build.HARDWARE is :" + Build.HARDWARE,
                        Toast.LENGTH_LONG).show();
            }
        } else {
            agreementDialog.show();
        }
        Set<String> set = MMKV.defaultMMKV().getStringSet("KEY", null);
        if (set != null && !set.isEmpty()) {
            keySet = set;
        }

    }

    public static long DELAY_TIME = MMKV.defaultMMKV().getInt("DELAY_TIME",5);

    public void addKey(String key) {
        keySet.add(key);
        MMKV.defaultMMKV().putStringSet("KEY",keySet);
    }

    public void deleteKey(String key) {
        keySet.remove(key);
        MMKV.defaultMMKV().putStringSet("KEY",keySet);
    }


    public void editDelaytime(int time){
        if (time <= 0) {
            ToastUtil.show("时间必须大于0");
            return;
        }
        MMKV.defaultMMKV().getInt("DELAY_TIME",time);;
        DELAY_TIME = time;
        ToastUtil.show("修改成功，识别间隔时间已经修改为"+time+"秒一次");
    }
    public Set<String> getKeySet() {
        if (keySet == null) {
            keySet = MMKV.defaultMMKV().getStringSet("KEY",null);
        }
        return keySet;
    }



    /**
     * onCreate中调用
     */
    public void onActivityCreate() {
        int hasWriteStoragePermission =
                ActivityCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteStoragePermission == PackageManager.PERMISSION_GRANTED) {
            start();
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    CODE_FOR_WRITE_PERMISSION);
        }
        choosePlatform();
    }

    private void start() {
        // paddleLite需要保证初始化与预测在同一线程保证速度
        ThreadPoolManager.executeSingle(new Runnable() {
            @Override
            public void run() {
                initManager();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if ((model == MODEL_DETECT && mDetectManager != null) ||
                                (model == MODEL_CLASSIFY && mClassifyDLManager != null) ||
                                (model == MODEL_SEGMENT && mSegmentManager != null) ||
                                (model == MODEL_OCR && mOcrManager != null)) {
                            modelLoadStatus = true;
                            updateTakePictureButtonStatus();
                        }
                    }
                });
            }
        });
    }

    private void updateTakePictureButtonStatus() {
        if (modelLoadStatus || isOnline) {
//            setTakePictureButtonAvailable(true);
        }
        if (!isOnline && !modelLoadStatus) {
//            setTakePictureButtonAvailable(false);
        }
    }

    private void initManager() {
//        serialNum = getIntent().getStringExtra("serial_num");
//        String apiUrl = getIntent().getStringExtra("apiUrl");
//        String ak = getIntent().getStringExtra("ak");
//        String sk = getIntent().getStringExtra("sk");

        float threshold = BaseConfig.DEFAULT_THRESHOLD;

        if (apiUrl != null) {
            hasOnlineApi = true;
        }
//        UiLog.info("model type is" + model);
        if (model == MODEL_DETECT) {
            if (hasOnlineApi) {
                mOnlineDetect = new DetectOnline(apiUrl, ak, sk, getActivity());
            }
            try {
                switch (platform) {
                    case TYPE_DDK200:
                        DDKConfig mDetectConfig = new DDKConfig(getActivity().getAssets(),
                                "ddk-detect/config.json");
                        threshold = mDetectConfig.getRecommendedConfidence();
                        mDetectManager = new DDKManager(getActivity(), mDetectConfig, serialNum);
                        break;
                    case TYPE_SNPE:
                        SnpeConfig mSnpeClassifyConfig = new SnpeConfig(getActivity().getAssets(),
                                "snpe-detect/config.json");
                        threshold = mSnpeClassifyConfig.getRecommendedConfidence();
                        mDetectManager = new SnpeManager(getActivity(), mSnpeClassifyConfig, serialNum);
                        break;
                    case TYPE_DDK_DAVINCI:
                        DDKDaVinciConfig mDDKDaVinciConfig = new DDKDaVinciConfig(getActivity().getAssets(),
                                "ddkvinci-detect/config.json");
                        threshold = mDDKDaVinciConfig.getRecommendedConfidence();
                        mDetectManager = new DDKDavinciManager(getActivity(), mDDKDaVinciConfig, serialNum);
                        break;
                    case TYPE_INFER:
                    default:
                        InferConfig mInferConfig = new InferConfig(getActivity().getAssets(),
                                "infer-detect/config.json");
                        // 可修改ARM推断使用的CPU核数
                        mInferConfig.setThread(Util.getInferCores());
                        threshold = mInferConfig.getRecommendedConfidence();

                        mDetectManager = new InferManager(getActivity(), mInferConfig, serialNum);
                        break;
                }

                canAutoRun = true;
                isInitializing = true;
            } catch (BaseException e) {
                showError(e);
            }
        }
        if (model == MODEL_CLASSIFY) {
            if (hasOnlineApi) {
                mOnlineClassify = new ClassifyOnline(apiUrl, ak, sk, getActivity());
            }
            try {
                switch (platform) {
                    case TYPE_DDK150:
                    case TYPE_DDK200:
                        threshold = initDDK(platform);
                        break;
                    case TYPE_DDK_DAVINCI:
                        DDKDaVinciConfig mDDKDaVinciConfig = new DDKDaVinciConfig(getActivity().getAssets(),
                                "ddkvinci-classify/config.json");
                        threshold = mDDKDaVinciConfig.getRecommendedConfidence();
                        mClassifyDLManager = new DDKDavinciManager(getActivity(), mDDKDaVinciConfig, serialNum);
                        break;
                    case TYPE_SNPE:
                        SnpeConfig mSnpeClassifyConfig = new SnpeConfig(getActivity().getAssets(),
                                "snpe-classify/config.json");
                        threshold = mSnpeClassifyConfig.getRecommendedConfidence();
                        mClassifyDLManager = new SnpeManager(getActivity(), mSnpeClassifyConfig, serialNum);
                        break;
                    case TYPE_INFER:
                    default:
                        threshold = initInfer();
                        break;
                }

                canAutoRun = true;
                isInitializing = true;
            } catch (BaseException e) {
                showError(e);
                Log.e("CameraActivity", e.getClass().getSimpleName() + ":" + e.getErrorCode() + ":" + e.getMessage());
            }
        }

        if (model == MODEL_SEGMENT) {
            InferConfig mInferConfig = null;
            try {
                mInferConfig = new InferConfig(getActivity().getAssets(), "infer-segment/config.json");
                mInferConfig.setThread(Util.getInferCores());
                threshold = mInferConfig.getRecommendedConfidence();
                mSegmentManager = new InferManager(getActivity(), mInferConfig, serialNum);
                canAutoRun = true;
                isInitializing = true;
            } catch (CallException e) {
                showError(e);
            } catch (BaseException e) {
                showError(e);
            }
        }

        if (model == MODEL_OCR) {
            InferConfig mInferConfig = null;
            try {
                mInferConfig = new InferConfig(getActivity().getAssets(), "infer-ocr/config.json");
                mInferConfig.setThread(Util.getInferCores());
                threshold = mInferConfig.getRecommendedConfidence();
                mOcrManager = new InferManager(getActivity(), mInferConfig, null);
                canAutoRun = true;
                isInitializing = true;
            } catch (CallException e) {
                showError(e);
            } catch (BaseException e) {
                showError(e);
            }
        }

        setConfidence(threshold);
    }


    private void releaseEasyDL() {
        if (model == MODEL_DETECT) {
            if (mDetectManager != null) {
                try {
                    mDetectManager.destroy();
                } catch (BaseException e) {
                    showError(e);
                }
            }
        }
        if (model == MODEL_CLASSIFY) {
            if (mClassifyDLManager != null) {
                try {
                    mClassifyDLManager.destroy();
                } catch (ClassifyException e) {
                    showError(e);
                } catch (BaseException e) {
                    e.printStackTrace();
                }
            }
        }
        if (model == MODEL_SEGMENT) {
            if (mSegmentManager != null) {
                try {
                    mSegmentManager.destroy();
                } catch (ClassifyException e) {
                    showError(e);
                } catch (BaseException e) {
                    e.printStackTrace();
                }
            }
        }
        if (model == MODEL_OCR) {
            if (mOcrManager != null) {
                try {
                    mOcrManager.destroy();
                } catch (ClassifyException e) {
                    showError(e);
                } catch (BaseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void setConfidence(float value) {
        resultConfidence = value;
        realtimeConfidence = value;
//        confidenceSeekbar.setProgress((int) (resultConfidence * 100));
//        realtimeConfidenceSeekbar.setProgress((int) (realtimeConfidence * 100));
    }


    private float initDDK(int type) throws BaseException {
        DDKConfig mClassifyConfig = new DDKConfig(getActivity().getAssets(),
                "ddk-classify/config.json");
        switch (type) {
            case TYPE_DDK150:
                if (mClassifyConfig.getModelFileAssetPath() == null) {
                    return initInfer();
                }
                break;
            case TYPE_DDK200:
                if (mClassifyConfig.getModelFileAssetPathV200() == null) {
                    return initInfer();
                }
                break;
            default:
        }

        mClassifyDLManager = new DDKManager(getActivity(), mClassifyConfig, serialNum);
        return mClassifyConfig.getRecommendedConfidence();
    }

    private float initInfer() throws BaseException {
        InferConfig mInferConfig = new InferConfig(getActivity().getAssets(),
                "infer-classify/config.json");
        mInferConfig.setThread(Util.getInferCores());
        mClassifyDLManager = new InferManager(getActivity(), mInferConfig, serialNum);
        return mInferConfig.getRecommendedConfidence();
    }

    private void showError(BaseException e) {
        showMessage(e.getErrorCode(), e.getMessage());
        Log.e("CameraActivity", e.getMessage(), e);
    }

    protected void showMessage(final int errorCode, final String msg) {
        showMessage("errorcode: " + errorCode + ":" + msg);
    }


    protected void showMessage(final String msg) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                builder.setTitle("提示")
                        .setMessage(msg)
                        .setNegativeButton("关闭", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
            }
        });

    }

    private void choosePlatform() {
//        String soc = getIntent().getStringExtra("soc");
        switch (soc) {
            case "dsp":
                platform = TYPE_SNPE;
                break;
            case "npu-vinci":
                platform = TYPE_DDK_DAVINCI;
                break;
            case "npu150":
                platform = TYPE_DDK150;
                break;
            case "npu200":
                platform = TYPE_DDK200;
                break;
            default:
            case "arm":
                platform = TYPE_INFER;
        }
    }


    /**
     * 读取json配置
     */
    private void initConfig() {
        try {
            String configJson = FileUtil.readAssetFileUtf8String(getActivity().getAssets(), "demo/config.json");
            JSONObject jsonObject = new JSONObject(configJson);
            name = jsonObject.getString("model_name");
            version = jsonObject.getString("model_version");
            type = jsonObject.getInt("model_type");

            if (jsonObject.has("apiUrl")) {
                apiUrl = jsonObject.getString("apiUrl");
                ak = jsonObject.getString("ak");
                sk = jsonObject.getString("sk");
            }

            JSONArray jsonArray = jsonObject.getJSONArray("soc");
            for (int i = 0; i < jsonArray.length(); i++) {
                String s = jsonArray.getString(i);
                socList.add(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void startUICameraActivity() {
        if (soc.equals("xeye")) {
//            Intent xeyeIntent = new Intent((getActivity()), XeyeActivity.class);
//            xeyeIntent.putExtra("model_type", type);
//            xeyeIntent.putExtra("serial_num", SERIAL_NUM);
//            startActivity(xeyeIntent);
        } else {
//            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
//            intent.putExtra("name", name);
//            intent.putExtra("model_type", type);
//            intent.putExtra("serial_num", SERIAL_NUM);
//
//            if (apiUrl != "null") {
//                intent.putExtra("apiUrl", apiUrl);
//                intent.putExtra("ak", ak);
//                intent.putExtra("sk", sk);
//            }

//            intent.putExtra("soc", soc);
//            startActivityForResult(intent, 1);
        }


        onActivityCreate();
    }


    private boolean checkChip() {
        if (socList.contains("dsp") && Build.HARDWARE.equalsIgnoreCase("qcom")) {
            soc = "dsp";
            return true;
        }
        if (socList.contains("npu") && (Build.HARDWARE.contains("kirin970") || Build.HARDWARE.contains("kirin980"))) {
            if (Build.HARDWARE.contains("kirin970")) {
                soc = "npu150";
            }
            if (Build.HARDWARE.contains("kirin980")) {
                soc = "npu200";
            }
            return true;
        }
        if (socList.contains("npu-vinci") && (Build.HARDWARE.contains("kirin810")
                || Build.HARDWARE.contains("kirin820") || Build.HARDWARE.contains("kirin990"))) {
            soc = "npu-vinci";
            return true;
        }
        if (socList.contains("arm")) {
            soc = "arm";
            return true;
        }
        if (socList.contains("xeye")) {
            soc = "xeye";
            return true;
        }
        return false;
    }


    public void release() {
        releaseEasyDL();
        BaiduManager.getInstance().pause();
    }

    private MainActivity getActivity() {
        return (MainActivity) RxActivityTool.currentActivity();
    }



    private List<ClassifyResultModel> filterClassifyListByConfidence(List<ClassifyResultModel> results, float min) {
        List<ClassifyResultModel> filteredList = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < results.size(); i++) {
            ClassifyResultModel mBaseResultModel = results.get(i);
            if (mBaseResultModel.getConfidence() > min) {
                filteredList.add(new ClassifyResultModel(++j, mBaseResultModel.getName(),
                        mBaseResultModel.getConfidence()));
            }
        }
        return filteredList;
    }

    private void updateResultImageAndList(List<? extends BaseResultModel> results, final Bitmap bitmap, float min) {


        if (model == MODEL_DETECT) {
            List<DetectResultModel> filteredList
                    = filterDetectListByConfidence((List<DetectResultModel>) results, min);
            fillDetectList(filteredList);
            fillRectImageView(filteredList, bitmap);
        }

        if (model == MODEL_CLASSIFY) {
            List<ClassifyResultModel> filteredList
                    = filterClassifyListByConfidence((List<ClassifyResultModel>) results, min);
            fillDetectList(filteredList);
        }


        if (model == MODEL_SEGMENT) {
            List<SegmentResultModel> filteredList
                    = filterSegmentListByConfidence((List<SegmentResultModel>) results, min);
            fillDetectList(filteredList);
            fillRectImageView(filteredList, bitmap);
        }

        if (model == MODEL_OCR) {
            List<OcrViewResultModel> filteredList
                    = filterOcrListByConfidence((List<OcrViewResultModel>) results, min);
            fillDetectList(filteredList);
            fillRectImageView(filteredList, bitmap);
        }
    }

    private List<OcrViewResultModel> filterOcrListByConfidence(List<OcrViewResultModel> results, float min) {
        List<OcrViewResultModel> filteredList = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < results.size(); i++) {
            OcrViewResultModel mOcrResultModel = results.get(i);
            if (mOcrResultModel.getConfidence() > min) {
                filteredList.add(new OcrViewResultModel(++j, mOcrResultModel.getName(),
                        mOcrResultModel.getConfidence(), mOcrResultModel.getBounds()));
            }
        }
        return filteredList;
    }

    private void fillRectImageView(List<? extends BasePolygonResultModel> result, Bitmap bitmap) {
//        resultMaskView.setPolygonListInfo((List< BasePolygonResultModel>) result, bitmap.getWidth(),
//                bitmap.getHeight());
    }

    /**
     * 绘制 编号 名字 置信度的列表
     * @param results
     */
    private void fillDetectList(List<? extends BaseResultModel> results) {
        final DetectResultAdapter adapter = new DetectResultAdapter(getActivity(),
                com.baidu.ai.edge.ui.R.layout.result_detect_item, (List<BaseResultModel>) results);

        uiHandler.post(new Runnable() {
            @Override
            public void run() {
//                detectResultView.setAdapter(adapter);
//                detectResultView.invalidate();
            }
        });
    }

    private List<SegmentResultModel> filterSegmentListByConfidence(List<SegmentResultModel> results, float min) {
        List<SegmentResultModel> filteredList = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < results.size(); i++) {
            SegmentResultModel mSegmentResultModel = results.get(i);
            if (mSegmentResultModel.getConfidence() > min) {
                SegmentResultModel model = new SegmentResultModel(++j, mSegmentResultModel.getName(),
                        mSegmentResultModel.getConfidence(), mSegmentResultModel.getBounds(),
                        mSegmentResultModel.getMask());
                model.setColorId(mSegmentResultModel.getColorId());
                filteredList.add(model);
            }
        }
        return filteredList;
    }

    private List<DetectResultModel> filterDetectListByConfidence(List<DetectResultModel> results, float min) {
        List<DetectResultModel> filteredList = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < results.size(); i++) {
            DetectResultModel mDetectResultModel = results.get(i);
            if (mDetectResultModel.getConfidence() > min) {
                filteredList.add(new DetectResultModel(++j, mDetectResultModel.getName(),
                        mDetectResultModel.getConfidence(), mDetectResultModel.getRect()));
            }
        }
        return filteredList;
    }

    public void onSegmentBitmap(Bitmap bitmap, float confidence, final com.baidu.ai.edge.ui.activity.ResultListener.SegmentListener listener) {
        if (mSegmentManager == null) {
            showMessage("模型初始化中，请稍后");
            listener.onResult(null);
            return;
        }

        List<SegmentationResultModel> resultModels = null;
        try {
            resultModels = mSegmentManager.segment(bitmap, confidence);
            List<BasePolygonResultModel> results = new ArrayList<>();
            for (int i = 0; i < resultModels.size(); i++) {
                SegmentationResultModel mSegmentationResultModel = resultModels.get(i);
                SegmentResultModel mSegmentResultModel = new SegmentResultModel();
                mSegmentResultModel.setColorId(mSegmentationResultModel.getLabelIndex());
                mSegmentResultModel.setIndex(i + 1);
                mSegmentResultModel.setConfidence(mSegmentationResultModel.getConfidence());
                mSegmentResultModel.setName(mSegmentationResultModel.getLabel());
                mSegmentResultModel.setBounds(mSegmentationResultModel.getBox());
                mSegmentResultModel.setMask(mSegmentationResultModel.getMask());
                results.add(mSegmentResultModel);
            }

            listener.onResult(results);
        } catch (BaseException e) {
            showError(e);
            listener.onResult(null);
        }

    }

    public void onOcrBitmap(Bitmap bitmap, float confidence, com.baidu.ai.edge.ui.activity.ResultListener.OcrListener listener) {
        if (mOcrManager == null) {
            return;
        }
        List<OcrResultModel> modelList = null;
        try {
            modelList = mOcrManager.ocr(bitmap, confidence);
            List<BasePolygonResultModel> results = new ArrayList<>();
            for (int i = 0; i < modelList.size(); i++) {
                OcrResultModel mOcrResultModel = modelList.get(i);
                OcrViewResultModel mOcrViewResultModel = new OcrViewResultModel();
                mOcrViewResultModel.setColorId(mOcrResultModel.getLabelIndex());
                mOcrViewResultModel.setIndex(i + 1);
                mOcrViewResultModel.setConfidence(mOcrResultModel.getConfidence());
                mOcrViewResultModel.setName(mOcrResultModel.getLabel());
                mOcrViewResultModel.setBounds(mOcrResultModel.getPoints());
                mOcrViewResultModel.setTextOverlay(true);
                results.add(mOcrViewResultModel);
            }
            listener.onResult(results);
        } catch (BaseException e) {
            showError(e);
            listener.onResult(null);
        }
    }

    private List<ClassifyResultModel> fillClassificationResultModel(
            List<ClassificationResultModel> modelList) {
        List<ClassifyResultModel> results = new ArrayList<>();
        for (int i = 0; i < modelList.size(); i++) {
            ClassificationResultModel mClassificationResultModel = modelList.get(i);
            ClassifyResultModel mClassifyResultModel = new ClassifyResultModel();
            mClassifyResultModel.setIndex(i + 1);
            mClassifyResultModel.setConfidence(mClassificationResultModel.getConfidence());
            mClassifyResultModel.setName(mClassificationResultModel.getLabel());
            results.add(mClassifyResultModel);
        }
        return results;
    }

    private void resolveDetectResult(Bitmap bitmap, float confidence,
                                     final ResultListener.ListListener listener) {
        if (model == MODEL_DETECT) {
            onDetectBitmap(bitmap, confidence, new ResultListener.DetectListener() {
                @Override
                public void onResult(List<BasePolygonResultModel> models) {
                    if (models == null) {
                        listener.onResult(null);
                        return;
                    }
                    detectResultModelCache = models;
                    listener.onResult(models);
                }
            });

        }
        if (model == MODEL_CLASSIFY) {

            onClassifyBitmap(bitmap, confidence, new ResultListener.ClassifyListener() {
                @Override
                public void onResult(List<ClassifyResultModel> models) {
                    if (models == null) {
                        listener.onResult(null);
                        return;
                    }

                    classifyResultModelCache = models;
                    listener.onResult(models);
                }
            });
        }
        if (model == MODEL_SEGMENT) {

            onSegmentBitmap(bitmap, confidence, new ResultListener.SegmentListener() {

                @Override
                public void onResult(List<BasePolygonResultModel> models) {
                    if (models == null) {
                        listener.onResult(null);
                        return;
                    }
                    segmentResultModelCache = models;
                    listener.onResult(models);
                }
            });
        }

        if (model == MODEL_OCR) {

            onOcrBitmap(bitmap, confidence, new ResultListener.OcrListener() {

                @Override
                public void onResult(List<BasePolygonResultModel> models) {
                    if (models == null) {
                        listener.onResult(null);
                        return;
                    }
                    ocrResultModelCache = models;
                    listener.onResult(models);
                }
            });
        }
    }

    public void onClassifyBitmap(Bitmap bitmap, float confidence,
                                 final ResultListener.ClassifyListener listener) {
        if (isOnline) {
            mOnlineClassify.classify(bitmap, new ClassifyInterface.OnResultListener() {
                @Override
                public void onResult(List<ClassificationResultModel> result) {
                    listener.onResult(fillClassificationResultModel(result));
                }

                @Override
                public void onError(BaseException ex) {
                    listener.onResult(null);
                    showError(ex);
                }
            });
            return;
        }

        if (mClassifyDLManager == null) {
            showMessage("模型初始化中，请稍后");
            listener.onResult(null);
            return;
        }
        try {
            List<ClassificationResultModel> modelList = mClassifyDLManager.classify(bitmap, confidence);
            listener.onResult(fillClassificationResultModel(modelList));
        } catch (BaseException e) {
            showError(e);
            listener.onResult(null);
        }
    }

    public void onDetectBitmap(Bitmap bitmap, float confidence,
                               final com.baidu.ai.edge.ui.activity.ResultListener.DetectListener listener) {

        if (isOnline) {
            mOnlineDetect.detect(bitmap, confidence,
                    new DetectInterface.OnResultListener() {
                        @Override
                        public void onResult(List<DetectionResultModel> result) {
                            listener.onResult(fillDetectionResultModel(result));
                        }

                        @Override
                        public void onError(BaseException ex) {
                            listener.onResult(null);
                            showError(ex);
                        }
                    });
            return;
        }

        if (mDetectManager == null) {
            showMessage("模型初始化中，请稍后");
            listener.onResult(null);
            return;
        }
        try {
            List<DetectionResultModel> modelList = mDetectManager.detect(bitmap, confidence);
            listener.onResult(fillDetectionResultModel(modelList));
        } catch (BaseException e) {
            showError(e);
            listener.onResult(null);
        }
    }

    private List<BasePolygonResultModel> fillDetectionResultModel(
            List<DetectionResultModel> modelList) {
        List<BasePolygonResultModel> results = new ArrayList<>();
        for (int i = 0; i < modelList.size(); i++) {
            DetectionResultModel mDetectionResultModel = modelList.get(i);
            DetectResultModel mDetectResultModel = new DetectResultModel();
            mDetectResultModel.setIndex(i + 1);
            mDetectResultModel.setConfidence(mDetectionResultModel.getConfidence());
            mDetectResultModel.setName(mDetectionResultModel.getLabel());
            mDetectResultModel.setBounds(mDetectionResultModel.getBounds());
            results.add(mDetectResultModel);
        }
        return results;
    }


    public void doEdge(final Bitmap bitmap,final ResultCallback callback) {
        ThreadPoolManager.executeSingle(new Runnable() {
            @Override
            public void run() {
                resolveDetectResult(bitmap, 0,
                        new ResultListener.ListListener<DetectResultModel>() {
                            @Override
                            public void onResult(List<BasePolygonResultModel> results) {
                                if (results != null) {
                                    if (callback != null) {
                                        callback.onResult(results);
                                    }
//                                    updateResultImageAndList(results, bitmap, resultConfidence);
//                                    runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            toggleLoading(false);
//                                            mCameraPageView.setVisibility(View.GONE);
//                                            mResultPageView.setVisibility(View.VISIBLE);
//                                            resultImage.setImageBitmap(bitmap);
//                                            pageCode = PAGE_RESULT;
//                                            detectBitmapCache = bitmap;
//                                        }
//                                    });
                                } else {
//                                    toggleLoading(false);
                                }
                            }
                        });

            }
        });
    }



    public interface ResultCallback{
        void onResult(List<BasePolygonResultModel> results);
    }

    boolean start = false;

    public void pause() {
        start = false;
    }
    public void autoStart() {
        start = true;
    }



}
