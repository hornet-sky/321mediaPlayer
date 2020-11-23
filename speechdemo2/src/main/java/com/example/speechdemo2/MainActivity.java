package com.example.speechdemo2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.MemoryFile;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.msc.util.FileUtil;
import com.iflytek.cloud.msc.util.log.DebugLog;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Vector;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private EditText editText;
    private Button iatButton;
    private Button iseButton;
    private Button switchLanguageButton;
    private RecognizerDialog mIatDialog;
    private Toast mToast;

    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<>();

    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;

    private String language="zh_cn";

    private String resultType = "json";

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败，错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            }
        }
    };

    /**
     * 听写UI监听器
     */
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        public void onResult(RecognizerResult results, boolean isLast) {
            printResult(results);
        }

        /**
         * 识别回调错误.
         */
        public void onError(SpeechError error) {
            showTip(error.getPlainDescription(true));
        }

    };


    // 默认发音人
    private String voicer = "xiaoyan";
    // 默认发音文本
    private String defTexts = "请先写入语句";
    private String[] mCloudVoicersEntries;
    private String[] mCloudVoicersValue ;
    private SpeechSynthesizer mTts;
    // 缓冲进度
    private int mPercentForBuffering = 0;
    // 播放进度
    private int mPercentForPlaying = 0;
    MemoryFile memFile;
    public volatile long   mTotalSize = 0;

    private Vector<byte[]> container = new Vector<> ();
    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {
        @Override
        public void onSpeakBegin() {
            showTip("开始播放");
        }
        @Override
        public void onSpeakPaused() {
            showTip("暂停播放");
        }
        @Override
        public void onSpeakResumed() {
            showTip("继续播放");
        }
        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度
            Log.e("MscSpeechLog_", "percent =" + percent);
            mPercentForBuffering = percent;
            showTip(String.format(getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
            Log.e("MscSpeechLog_", "percent =" + percent);
            mPercentForPlaying = percent;
            showTip(String.format(getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));

            SpannableStringBuilder style = new SpannableStringBuilder(editText.getText().toString());
            Log.e(TAG,"beginPos = " + beginPos + "  endPos = " + endPos);
            style.setSpan(new BackgroundColorSpan(Color.RED), beginPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            editText.setText(style);
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                //	showTip("播放完成");
                try {
                    for(int i = 0; i < container.size() ; i++) {
                        writeToFile(container.get(i));
                    }
                } catch (IOException e) {

                }
                FileUtil.saveFile(memFile, mTotalSize, getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC) + "/1.pcm");
            } else if (error != null) {
                showTip(error.getPlainDescription(true));
            }
        }

        private void writeToFile(byte[] data) throws IOException {
            if (data == null || data.length == 0)
                return;
            try {
                if(memFile == null) {
                    String mFilepath = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC) + "/1.pcm";
                    memFile = new MemoryFile(mFilepath,1920000);
                    memFile.allowPurging(false);
                }
                memFile.writeBytes(data, 0, (int) mTotalSize, data.length);
                mTotalSize += data.length;
            } finally {
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            //	 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            //	 若使用本地能力，会话id为null
            if (SpeechEvent.EVENT_SESSION_ID == eventType) {
                String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
                Log.d(TAG, "session id =" + sid);
            }

            //当设置SpeechConstant.TTS_DATA_NOTIFY为1时，抛出buf数据
            if (SpeechEvent.EVENT_TTS_BUFFER == eventType) {
                byte[] buf = obj.getByteArray(SpeechEvent.KEY_EVENT_TTS_BUFFER);
                Log.e("MscSpeechLog_", "bufis =" + buf.length);
                container.add(buf);
            }
        }
    };

    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());
        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
            Log.w(TAG, "printResult[text=" + text + ", sn=" + sn + "]"); // printResult[text=今天的天气真不错, sn=1]    printResult[text=是吧, sn=2]
        } catch (JSONException e) {
            Log.e(TAG, "printResult: " + e.getMessage(), e);
        }

        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            if(resultBuffer.length() > 0) {
                resultBuffer.append(" ");
            }
            resultBuffer.append(mIatResults.get(key));
        }

        editText.setText(resultBuffer.toString());
        editText.setSelection(editText.length());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        initView();
        initEvent();
        initData();
        requestPermissions();
    }

    private void initView() {
        editText = findViewById(R.id.editText);
        iatButton = findViewById(R.id.iatButton);
        iseButton = findViewById(R.id.iseButton);
        switchLanguageButton = findViewById(R.id.switchLanguageButton);
    }

    private void initEvent() {
        iatButton.setOnClickListener(this);
        iseButton.setOnClickListener(this);
        switchLanguageButton.setOnClickListener(this);
    }

    private void initData() {
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        // 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
        // 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
        mIatDialog = new RecognizerDialog(this, mInitListener);

        //以下为dialog设置听写参数
        setIatParam();

        //开始识别并设置监听器
        mIatDialog.setListener(mRecognizerDialogListener);


        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(this, mInitListener);

        // 云端发音人名称列表
        mCloudVoicersEntries = getResources().getStringArray(R.array.voicer_cloud_entries);
        mCloudVoicersValue = getResources().getStringArray(R.array.voicer_cloud_values);

        setIseParam();
    }

    public void setIatParam() {
        // 清空参数
        mIatDialog.setParameter(SpeechConstant.PARAMS, null);

        // 设置听写引擎
        mIatDialog.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIatDialog.setParameter(SpeechConstant.RESULT_TYPE, resultType);


        if(language.equals("zh_cn")) {
            mIatDialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            // 设置语言区域
            mIatDialog.setParameter(SpeechConstant.ACCENT, "mandarin"); // 普通话
        }else {
            mIatDialog.setParameter(SpeechConstant.LANGUAGE, language);
        }
        //此处用于设置dialog中不显示错误码信息
        //mIat.setParameter("view_tips_plain","false");

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIatDialog.setParameter(SpeechConstant.VAD_BOS, "4000");

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIatDialog.setParameter(SpeechConstant.VAD_EOS, "1000");

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIatDialog.setParameter(SpeechConstant.ASR_PTT, "0");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mIatDialog.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mIatDialog.setParameter(SpeechConstant.ASR_AUDIO_PATH, getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC) + "/msc/iat.wav");
    }

    private void setIseParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        // 根据合成引擎设置相应参数
        if(mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            //支持实时音频返回，仅在synthesizeToUri条件下支持
            mTts.setParameter(SpeechConstant.TTS_DATA_NOTIFY, "1");
            //	mTts.setParameter(SpeechConstant.TTS_BUFFER_TIME,"1");

            // 设置在线合成发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, voicer);
            //设置合成语速
            mTts.setParameter(SpeechConstant.SPEED, "50");
            //设置合成音调
            mTts.setParameter(SpeechConstant.PITCH, "50");
            //设置合成音量
            mTts.setParameter(SpeechConstant.VOLUME, "50");
        } else {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            mTts.setParameter(SpeechConstant.VOICE_NAME, "");

        }

        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "false");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "pcm");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC) + "/msc/tts.pcm");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iatButton:
                //显示听写对话框
                mIatDialog.show();
                break;
            case R.id.iseButton:
                String texts = editText.getText().toString();
                if(TextUtils.isEmpty(texts)) {
                    texts = this.defTexts;
                }
                // 设置参数
                int code = mTts.startSpeaking(texts, mTtsListener);
                /**
                 * 只保存音频不进行播放接口,调用此接口请注释startSpeaking接口
                 * text:要合成的文本，uri:需要保存的音频全路径，listener:回调接口
                */
                // String path = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC) + "/tts.pcm";
                // int code = mTts.synthesizeToUri(texts, path, mTtsListener);

                if (code != ErrorCode.SUCCESS) {
                    showTip("语音合成失败,错误码: " + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
                }
                break;
            case R.id.switchLanguageButton:
                showPresonSelectDialog();
                break;
        }
    }

    private int selectedNum = 0;
    /**
     * 发音人选择。
     */
    private void showPresonSelectDialog() {
        new AlertDialog.Builder(this).setTitle("在线合成发音人选项")
                .setSingleChoiceItems(mCloudVoicersEntries, // 单选框有几项,各是什么名字
                        selectedNum, // 默认的选项
                        new DialogInterface.OnClickListener() { // 点击单选框后的处理
                            public void onClick(DialogInterface dialog, int which) { // 点击了哪一项
                                voicer = mCloudVoicersValue[which];
                                /*
                                if ("catherine".equals(voicer) || "henry".equals(voicer) || "vimary".equals(voicer)) {
                                    editText.setText("Today is a nice day!");
                                } else {
                                    editText.setText("今天天气不错！");
                                }
                                 */
                                selectedNum = which;
                                mTts.setParameter(SpeechConstant.VOICE_NAME, voicer);
                                dialog.dismiss();
                            }
                        }).show();
    }

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permission = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO);
            if(permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,new String[]
                        { Manifest.permission.RECORD_AUDIO },0x0010);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if( null != mTts ){
            mTts.stopSpeaking();
            // 退出时释放连接
            mTts.destroy();
        }
    }
}