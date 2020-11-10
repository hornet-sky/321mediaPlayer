package com.example.mobileplayer.component;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.aspsine.swipetoloadlayout.SwipeLoadMoreFooterLayout;
import com.example.mobileplayer.R;

public class LoadMoreFooterView extends SwipeLoadMoreFooterLayout {
    private TextView loadMoreGuideTextView;
    private boolean noMoreData;
    public LoadMoreFooterView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        loadMoreGuideTextView = findViewById(R.id.tv_guide_footer);
    }

    @Override
    public void onLoadMore() {
        Log.w("myTag", "onLoadMore");
        loadMoreGuideTextView.setText(noMoreData ? "没有更多数据" : "加载中...");
    }

    @Override
    public void onComplete() {
        Log.w("myTag", "onComplete");
        loadMoreGuideTextView.setText(noMoreData ? "没有更多数据" : "加载完成");
    }

    @Override
    public void onReset() {
        Log.w("myTag", "onReset");
        loadMoreGuideTextView.setText(noMoreData ? "没有更多数据" : "加载更多");
    }

    public void setNoMoreData(boolean noMoreData) {
        this.noMoreData = noMoreData;
        onReset();
    }

    public boolean isNoMoreData() {
        return this.noMoreData;
    }
}
