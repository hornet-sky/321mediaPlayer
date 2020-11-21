package com.example.mobileplayer.component;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mobileplayer.entity.Lyric;
import com.example.mobileplayer.utils.SizeUtils;

import java.util.ArrayList;
import java.util.List;

public class LyricsView extends androidx.appcompat.widget.AppCompatTextView {
    private int viewHeight, viewWidth;
    private List<Lyric> lyrics;
    private int position, index, oldIndex;
    private Paint paint;
    private TranslateAnimation tranAnim;

    public LyricsView(@NonNull Context context) {
        super(context);
    }
    public LyricsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        paint = new Paint();
        paint.setAntiAlias(true);
        // paint.setTextAlign(Paint.Align.CENTER); // 也可以自己算：x = (viewWidth - rect.width()) / 2.0F
        paint.setTextSize(SizeUtils.getInstance(getContext()).dip2px(20));
        //paint.setLetterSpacing(0.2F);
    }

    public void setPosition(int position) {
        Log.w("myTag14", "LyricsView.setPosition[position=" + position + "]");
        if(position < 0) {
            position = 0;
        }
        this.position = position;
        if(position == 0) {
            oldIndex = index = 0;
        }
        if(lyrics == null || lyrics.isEmpty()) {
            return;
        }
        for(int i = 0, len = lyrics.size(); i < len; i++) {
            if(position >= lyrics.get(i).getPosition() && (i == len - 1 || position < lyrics.get(i + 1).getPosition())) {
                oldIndex = index;
                index = i;
                break;
            }
        }
        if(index != oldIndex) {
            Lyric currLyric = lyrics.get(index);
            int lineHeight;
            if(index < lyrics.size() - 1) {
                Lyric nextLyric = lyrics.get(index + 1);
                lineHeight = -nextLyric.getLineHeight();
            } else {
                lineHeight = -currLyric.getLineHeight();
            }
            clearTranAnimation();
            if(getVisibility() == View.VISIBLE) {
                tranAnim = new TranslateAnimation(0, 0, 0, lineHeight);
                tranAnim.setDuration(currLyric.getDuration());
                tranAnim.setFillAfter(true);
                startAnimation(tranAnim); // 自带 invalidate() 效果
            }
        }
    }

    public void setLyrics(List<Lyric> lyrics) {
        this.lyrics = lyrics;
        if(lyrics == null) {
            lyrics = new ArrayList<>(1);
        }
        if(lyrics.isEmpty()) {
            lyrics.add(new Lyric(0, "暂无歌词"));
        }
        // 设置行高、时长
        Rect rect = new Rect();
        String content;
        long duration;
        int offset = SizeUtils.getInstance(getContext()).dip2px(10); // 增加一些行距
        Lyric lyric;
        for(int i = 0, len = lyrics.size(); i < len; i++) {
            lyric = lyrics.get(i);
            if(lyric.getLineHeight() == 0) {
                content = lyric.getContent();
                paint.getTextBounds(content, 0, content.length(), rect);
                lyric.setLineHeight(rect.height() + offset);
                lyric.setWidth(rect.width());
            }
            if(lyric.getDuration() == 0) {
                duration = i >= len - 1 ? 3000 : lyrics.get(i + 1).getPosition() - lyric.getPosition();
                lyric.setDuration(duration);
            }
        }
        clearTranAnimation();
        setPosition(0);
        invalidate();
    }

    private void clearTranAnimation() {
        if(tranAnim != null && !tranAnim.hasEnded() || getVisibility() != View.VISIBLE) { // 上一个动画如果没执行完就得立即结束，不然会与接下来的新动画重叠
            this.clearAnimation();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.w("myTag14", "LyricsView.onMeasure[measuredWidth=" + getMeasuredWidth() + ", measuredHeight=" + getMeasuredHeight() + "]");
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.w("myTag14", "LyricsView.onSizeChanged[w=" + w + ", h=" + h + ", oldW=" + oldw + ", oldH=" + oldh + "]");
        this.viewWidth = w;
        this.viewHeight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.w("myTag14", "LyricsView.onDraw");
        if(lyrics == null || lyrics.isEmpty()) {
            return;
        }

        // 1、绘制当前正在进行的那句歌词
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setColor(Color.GREEN);
        Lyric lyric = lyrics.get(index);
        String content = lyric.getContent();
        float x = viewWidth / 2.0F, y = viewHeight / 2.0F;
        canvas.drawText(content, x - lyric.getWidth() / 2.0F, y, paint);

        // 2、绘制上面已经进行过的歌词
        if(index > 0) {
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setColor(Color.rgb(200, 200, 200));
            float tx = x, ty = y;
            for(int i = index - 1; i >= 0; i--) {
                lyric = lyrics.get(i);
                ty -= lyrics.get(i + 1).getLineHeight();
//                if(ty - lyric.getLineHeight() < 0) {
//                    Log.w("myTag14", "上部分歌词终止绘制：" + content);
//                    break;
//                }
                content = lyric.getContent();
                canvas.drawText(content, tx - lyric.getWidth() / 2.0F, ty, paint);
            }
        }

        // 3、绘制下面即将进行的歌词
        if(index < lyrics.size() - 1) {
            paint.setTypeface(Typeface.DEFAULT);
            paint.setColor(Color.rgb(220, 220, 220));
            float bx = x, by = y;
            for(int i = index + 1; i < lyrics.size(); i++) {
                lyric = lyrics.get(i);
                by += lyric.getLineHeight();
//                if(by > viewHeight) {
//                    Log.w("myTag14", "下部分歌词终止绘制：" + content);
//                    break;
//                }
                content = lyric.getContent();
                canvas.drawText(content, bx - lyric.getWidth() / 2.0F, by, paint);
            }
        }

        // 绘制参考线（歌词没居中的时候可以用来调试）
        // canvas.drawLine(0, y, viewWidth, y, paint); // 横线
        // canvas.drawLine(x, 0, x, viewHeight, paint); // 纵线
    }
}
