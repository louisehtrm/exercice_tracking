package com.example.ass3;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Graph extends View implements ValueAnimator.AnimatorUpdateListener {

    Paint linePainter, circlePainter, guidePainter, yLabelPainter;
    float padding, minLabel, maxLabel, x, y, radius, fraction, yLabelWidth;
    List<Float> series;
    Path path;
    ValueAnimator animator;


    public Graph(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        path = new Path();

        linePainter = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePainter.setStyle(Paint.Style.STROKE);
        linePainter.setStrokeWidth(5f);
        linePainter.setColor(Color.parseColor("#32BC75"));

        circlePainter = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePainter.setStyle(Paint.Style.FILL);
        circlePainter.setColor(Color.parseColor("#32BC75"));

        guidePainter = new Paint(Paint.ANTI_ALIAS_FLAG);
        guidePainter.setStyle(Paint.Style.STROKE);
        guidePainter.setStrokeWidth(5f);
        guidePainter.setColor(Color.parseColor("#A59BC9"));

        yLabelPainter = new Paint(Paint.ANTI_ALIAS_FLAG);
        yLabelPainter.setTextSize(30f);
        yLabelPainter.setTextAlign(Paint.Align.RIGHT);

        padding = 20f;
        radius = 10f;

        series = MainActivity.points;

        minLabel = 0;
        maxLabel = 10;

        //animation of the line graph
        animator = new ValueAnimator();
        animator.setDuration(3000);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(this);
        animator.setFloatValues(0f, 1f);
        animator.start();
    }

    //draw the circles and path between the circles
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

            int height = getHeight();
            drawGuide(canvas, height - 2 * padding, getWidth() - padding);
            float gridHeight = getHeight() - 2 * padding;
            float drawnHeight = maxLabel - minLabel;
            float space = (getWidth() - 2 * padding - yLabelWidth) / series.size();

            //the first circle
            x = yLabelWidth + space;
            if(series.size() != 0)
                y = (height - padding - (series.get(0) - minLabel) * (gridHeight / drawnHeight)) * fraction;
            path.moveTo(x, y);
            canvas.drawCircle(x, y, radius, circlePainter);

            //rest of the circles
            for (int i = 1; i < series.size(); i++) {
                x += space;
                y = (height - padding - (series.get(i) - minLabel) * (gridHeight / drawnHeight)) * fraction;
                path.lineTo(x, y);
                canvas.drawCircle(x, y, radius, circlePainter);
            }
            canvas.drawPath(path, linePainter);
    }

    //draw horizontal lines and guide numbers
    private void drawGuide(Canvas canvas, float gridBottom, float gridRight) {
        float labelStep = (maxLabel - minLabel) / 9f;
        float currentLabel = maxLabel;
        float space = gridBottom / 9f;
        float y;

        for(int i = 0; i < 10; i++) {
            y = padding + i * space;

            String yLabel = String.format("%.02f", currentLabel);
            float width = yLabelPainter.measureText(yLabel);
            Rect bound = new Rect();
            yLabelPainter.getTextBounds(yLabel, 0, yLabel.length(), bound);
            if(yLabelWidth < width) yLabelWidth = width;

            canvas.drawText(yLabel, padding + yLabelWidth, y + bound.height() / 2, yLabelPainter);
            canvas.drawLine(padding + yLabelWidth + 10f, y, gridRight, y, guidePainter);
            currentLabel -= labelStep;
        }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        fraction = animation.getAnimatedFraction();
        path.reset();
        invalidate();
    }
}
