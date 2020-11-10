package com.example.mobileplayer.component;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.aspsine.swipetoloadlayout.SwipeRefreshTrigger;
import com.aspsine.swipetoloadlayout.SwipeTrigger;
import com.example.mobileplayer.R;
import com.example.mobileplayer.utils.SizeUtils;


public class RefreshHeaderView extends LinearLayout implements SwipeTrigger, SwipeRefreshTrigger {
    private ProgressBar refreshProgressBar;
    private ImageView arrowImageView;
    private TextView refreshGuideTextView;
    private SizeUtils sizeUtils;

    private boolean isArrowUp;

    public RefreshHeaderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        sizeUtils = SizeUtils.getInstance(getContext());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        refreshProgressBar = findViewById(R.id.pb_indicator_header);
        arrowImageView = findViewById(R.id.iv_indicator_header);
        refreshGuideTextView = findViewById(R.id.tv_guide_header);
    }

    @Override
    public void onRefresh() {
        Log.w("myTag", "onRefresh");
        refreshGuideTextView.setText("加载中");
        arrowImageView.clearAnimation();
        arrowImageView.setVisibility(View.GONE);
        refreshProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPrepare() {
        Log.w("myTag", "onPrepare");

    }

    @Override
    public void onMove(int y, boolean isComplete, boolean automatic) {
        Log.w("myTag", "onMove - " + y + " - " + isComplete);
        if(y > sizeUtils.dip2px(80F)) {
            if(!isArrowUp) {
                refreshGuideTextView.setText("释放更新");
                arrowImageView.clearAnimation();
                Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.arrow_rotate_to_up);
                arrowImageView.startAnimation(animation);
                isArrowUp = true;
            }
        } else {
            if(isArrowUp) {
                refreshGuideTextView.setText("下拉刷新");
                arrowImageView.clearAnimation();
                Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.arrow_rotate_to_down);
                arrowImageView.startAnimation(animation);
                isArrowUp = false;
            }
        }
    }

    @Override
    public void onRelease() {
        Log.w("myTag", "onRelease");

    }

    @Override
    public void onComplete() {
        Log.w("myTag", "onComplete");
        refreshProgressBar.setVisibility(View.GONE);
        refreshGuideTextView.setText("刷新完成");
    }

    @Override
    public void onReset() {
        Log.w("myTag", "onReset");
        refreshProgressBar.setVisibility(View.GONE);
        arrowImageView.setVisibility(View.VISIBLE);
        refreshGuideTextView.setText("下拉刷新");
    }
}
