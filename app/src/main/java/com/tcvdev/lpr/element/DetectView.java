package com.tcvdev.lpr.element;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.tcvdev.lpr.model.CARPLATEDATA;
import com.crashlytics.android.Crashlytics;
import com.tcvdev.lpr.R;
import com.tcvdev.lpr.common.Util;

public class DetectView extends View {


    private Paint mRectPaint;
    private Paint mTextNumberPaint;
    private Paint mTextConfPaint;
    private Paint mNumberBGPaint;
    private Paint mConfBGPaint;


    private CARPLATEDATA mCarPlateData;
    private int m_nImgWidth;
    private int m_nImgHeight;
    private int m_nCntPlate;
    private Context mContext;

    public DetectView(Context context) {
        super(context);
        this.mContext = context;
        this.m_nCntPlate = 0;
    }

    public DetectView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.mContext = context;
        this.m_nCntPlate = 0;

        mRectPaint = new Paint();
        mRectPaint.setColor(context.getResources().getColor(R.color.colorRect));
        mRectPaint.setStrokeWidth(4);
        mRectPaint.setAntiAlias(true);
        mRectPaint.setStyle(Paint.Style.STROKE);

        mTextNumberPaint = new Paint();
        mTextNumberPaint.setTextSize(50);
        mTextNumberPaint.setColor(context.getResources().getColor(R.color.colorText));

        mTextConfPaint = new Paint();
        mTextConfPaint.setTextSize(30);
        mTextConfPaint.setColor(context.getResources().getColor(R.color.colorText));

        mNumberBGPaint = new Paint();
        mNumberBGPaint.setColor(context.getResources().getColor(R.color.colorTextBG));
        mNumberBGPaint.setAntiAlias(true);
        mNumberBGPaint.setStyle(Paint.Style.FILL);
    }

    public DetectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        try {
            if (mCarPlateData != null && mCarPlateData.pPlate != null) {

                int cnt = mCarPlateData.nPlate;

                float scaleX = (float) width / m_nImgWidth;
                float scaleY = (float) height / m_nImgHeight;
                float scale = Math.min(scaleX, scaleY);

                int offsetLeft = (int) (width - m_nImgWidth * scale) / 2;
                int offsetTop = (int) (height - m_nImgHeight * scale) / 2;

                for (int i = 0; i < cnt; i++) {

                    CARPLATEDATA.LICENSE plateData = mCarPlateData.pPlate[i];
                    if (plateData != null) {

                        RectF rectPlate = new RectF(mCarPlateData.pPlate[i].rtPlate.left * scale, mCarPlateData.pPlate[i].rtPlate.top * scale,
                                mCarPlateData.pPlate[i].rtPlate.right * scale, mCarPlateData.pPlate[i].rtPlate.bottom * scale);

                        rectPlate.offset(offsetLeft, offsetTop);
                        canvas.drawRect(rectPlate, mRectPaint);

                        String strResult = Util.getValidString(mCarPlateData.pPlate[i].szLicense, 20);

                        Rect rectNumberBound = Util.getTextBounds(mTextNumberPaint, strResult);
                        int textPadding = 20;
                        int offset = 50;
                        RectF rectNumberBG = new RectF(rectPlate.left, rectPlate.bottom + offset,
                                rectPlate.left + rectNumberBound.width() + textPadding * 2, rectPlate.bottom + offset + rectNumberBound.height() + textPadding * 2);

                        canvas.drawRect(rectNumberBG, mNumberBGPaint);
                        canvas.drawText(strResult, rectNumberBG.left + textPadding, rectNumberBG.bottom - textPadding, mTextNumberPaint);

                        // Draw Conf;
                        String strConf = String.format("Conf: %.3f%%", plateData.pfDist);
                        Rect rectConfBound = Util.getTextBounds(mTextConfPaint, strConf);
                        RectF rectConfBG = new RectF(rectPlate.left, rectPlate.top - offset - rectConfBound.height() - textPadding * 2,
                                rectPlate.left + rectConfBound.width() + textPadding * 2, rectPlate.top - offset);

                        canvas.drawRect(rectConfBG, mNumberBGPaint);
                        canvas.drawText(strConf, rectConfBG.left + textPadding, rectConfBG.bottom - textPadding, mTextConfPaint);

                    }
                }
            }
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            Crashlytics.logException(ex);
            Crashlytics.log(1, "LPR", "NullPointException");
        }

    }

    public void setLPRResult(CARPLATEDATA carplatedata) {

        this.mCarPlateData = carplatedata;
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }

    public void setImageSize(int imgWidth, int imgHeight) {

        this.m_nImgWidth = imgWidth;
        this.m_nImgHeight = imgHeight;
    }


}
