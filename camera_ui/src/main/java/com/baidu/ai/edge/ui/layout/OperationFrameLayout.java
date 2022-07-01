package com.baidu.ai.edge.ui.layout;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by ruanshimin on 2018/5/3.
 */

public class OperationFrameLayout extends RelativeLayout {
    private int layoutHeight = 360;

    public OperationFrameLayout(@NonNull Context context) {
        super(context);
    }

    public OperationFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public OperationFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, layoutHeight);
        setBackgroundColor(Color.BLACK);
    }

}
