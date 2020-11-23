package com.example.mobileplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.mobileplayer.component.AudioPlayer;
import com.example.mobileplayer.entity.NewsItem;
import com.example.mobileplayer.entity.NewsItem;
import com.example.mobileplayer.ui.AudioFragment;
import com.example.mobileplayer.utils.JsonParser;
import com.example.mobileplayer.utils.MediaUtils;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class SearchFromNetActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = SearchFromNetActivity.class.getSimpleName();
    private EditText searchEditText;
    private ImageView voiceImageView;
    private TextView searchBtn;
    private RecyclerView recyclerView;
    private TextView searchNoDataTextView;
    private ProgressBar loadingProgressBar;

    private MyAdapter adapter;

    private MediaUtils.NewsLoadCallback callback = new MediaUtils.NewsLoadCallback() {
        @Override
        public void beforeRequest() {
            loadingProgressBar.setVisibility(View.VISIBLE);
        }
        @Override
        public void onSuccess(List<NewsItem> items) {
            adapter.submitList(items);
            searchNoDataTextView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        }
        @Override
        public void onError(Throwable ex) {
            Log.e("myTag18", ex.getMessage(), ex);
        }
        @Override
        public void onFinished() {
            loadingProgressBar.setVisibility(View.GONE);
        }
    };

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

    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<>();

    private RecognizerDialog mIatDialog;
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;

    private String language="zh_cn";

    private String resultType = "json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_from_net);

        initView();
        initEvent();
        initData();
    }

    private void initView() {
        searchEditText = findViewById(R.id.searchTextView);
        voiceImageView = findViewById(R.id.voiceImageView);
        searchBtn = findViewById(R.id.searchBtn);
        recyclerView = findViewById(R.id.recyclerView);
        searchNoDataTextView = findViewById(R.id.searchNoDataTextView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
    }

    private void initEvent() {
        searchBtn.setOnClickListener(this);
        voiceImageView.setOnClickListener(this);
    }

    private void initData() {
        adapter = new MyAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
        // 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
        mIatDialog = new RecognizerDialog(this, mInitListener);

        //以下为dialog设置听写参数
        setIatParam();

        //开始识别并设置监听器
        mIatDialog.setListener(mRecognizerDialogListener);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.searchBtn:
                searchNews();
                break;
            case R.id.voiceImageView:
                mIatDialog.show();
                break;
        }
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

        searchEditText.setText(resultBuffer.toString());
        searchEditText.setSelection(searchEditText.length());
    }

    private void showTip(final String str) {
        Toast toast = Toast.makeText(this, str, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private void searchNews() {
        String searchKey = searchEditText.getText().toString();
        hideKeyboard();
        int pageNo = 1, pageSize = 20;
        MediaUtils.listNews(pageNo, pageSize, searchKey, callback);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        // 隐藏软键盘
        imm.hideSoftInputFromWindow(this.getWindow().getDecorView().getWindowToken(), 0);
    }

    static class MyAdapter extends ListAdapter<NewsItem, MyViewHolder> {
        protected MyAdapter() {
            super(new DiffUtil.ItemCallback<NewsItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull NewsItem oldItem, @NonNull NewsItem newItem) {
                    return oldItem == newItem;
                }
                @Override
                public boolean areContentsTheSame(@NonNull NewsItem oldItem, @NonNull NewsItem newItem) {
                    return oldItem.getDetailsId().equals(newItem.getDetailsId());
                }
            });
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_news_item_layout, parent, false);
            return new MyViewHolder(itemView);
        }
        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            NewsItem item = getItem(position);
            holder.pubTimeTextView.setText(item.getPubTime());
            holder.durationTextView.setText(MediaUtils.formatDuration(item.getDuration() * 1000));
            holder.titleTextView.setText(item.getDreTitle());
            Glide.with(holder.itemView)
                    .load(item.getImageLink())
                    .thumbnail(Glide.with(holder.itemView).load(R.drawable.loading))
                    .error(R.drawable.pictures_no) // 设置了thumbnail后error就没效果了
                    .into(holder.coverImageView);
        }
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        private ImageView coverImageView;
        private TextView pubTimeTextView;
        private TextView titleTextView;
        private TextView durationTextView;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImageView = itemView.findViewById(R.id.coverImageView);
            pubTimeTextView = itemView.findViewById(R.id.pubTimeTextView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            durationTextView = itemView.findViewById(R.id.durationTextView);
        }
    }
}