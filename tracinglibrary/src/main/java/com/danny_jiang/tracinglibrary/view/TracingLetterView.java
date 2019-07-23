package com.danny_jiang.tracinglibrary.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;

import com.danny_jiang.tracinglibrary.R;
import com.danny_jiang.tracinglibrary.bean.LetterFactory;
import com.danny_jiang.tracinglibrary.bean.LetterStrokeBean;
import com.danny_jiang.tracinglibrary.util.LogUtils;
import com.danny_jiang.tracinglibrary.util.ScreenUtils;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author danny.jiang
 */
public class TracingLetterView extends View {
    public static String TAG = TracingLetterView.class.getSimpleName();

    private Paint mPaint;
    private Paint processingPaint;
    private Paint pointPaint;
    private Bitmap anchorBitmap;
    private Bitmap letterBitmap;
    private Bitmap traceBitmap;

    private LetterStrokeBean strokeBean;
    // this is used to point the position of anchor
    private PointF anchorPos = new PointF();
    private TracingListener tracingListener;
    private int viewWidth = -1;
    private int viewHeight = -1;
    private float validArea;
    private float toleranceArea;

    private Rect letterRect;
    private RectF viewRect = null;
    private Rect anchorRect;
    private RectF scaleRect = new RectF();
    private float anchorScale = 0f;
    private float[] pathPoints;

    private Path pathToCheck;
    private Path currentDrawingPath;
    private int currentStroke = 0;
    private int currentStokeProgress = -1;
    private List<Path> paths = new ArrayList<>();
    private boolean needInstruct;
    private boolean letterTracingFinished = false;
    private boolean hasFinishOneStroke = false;
    private String tracingAssets;

    public TracingLetterView(Context context) {
        this(context, null);
    }

    public TracingLetterView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TracingLetterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.TracingLetterView);
        final Resources res = getResources();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);

        pointPaint = new Paint();
        pointPaint.setAntiAlias(true);
        pointPaint.setStrokeWidth(15);
        pointPaint.setColor(typedArray.getColor(R.styleable.TracingLetterView_pointColor, Color.YELLOW));
        pointPaint.setStyle(Paint.Style.STROKE);
        pointPaint.setStrokeCap(Paint.Cap.ROUND);
        pointPaint.setStrokeJoin(Paint.Join.ROUND);

        processingPaint = new Paint();
        processingPaint.setAntiAlias(true);
        processingPaint.setDither(true);
        processingPaint.setStyle(Paint.Style.STROKE);
        processingPaint.setStrokeCap(Paint.Cap.ROUND);
        processingPaint.setStrokeJoin(Paint.Join.ROUND);
        processingPaint.setPathEffect(new CornerPathEffect(ScreenUtils.getInstance().dpToPx(getContext(),60)));
        processingPaint.setColor(typedArray.getColor(R.styleable.TracingLetterView_strokeColor, Color.parseColor("#DA609F")));

        needInstruct = typedArray.getBoolean(R.styleable.TracingLetterView_instructionMode, true);
        validArea = res.getDimension(R.dimen.dp_30);
        if (needInstruct) {
            toleranceArea = 60;
        } else {
            toleranceArea = 50;
        }

        Drawable drawable = typedArray.getDrawable(R.styleable.TracingLetterView_anchorDrawable);
        if (drawable != null)
            anchorBitmap = ((BitmapDrawable) drawable).getBitmap();
        else
            anchorBitmap = getBitmapByAssetName("crayon.png");
        typedArray.recycle();

    }

    /**
     * @param tracingListener callback tracing letter finish state
     */
    public void setTracingListener(TracingListener tracingListener) {
        this.tracingListener = tracingListener;
    }

    public void setLetterChar(@LetterFactory.Letter int letterChar) {
        LetterFactory letterFactory = new LetterFactory();
        letterFactory.setLetter(letterChar);
        initializeLetterAssets(letterFactory.getLetterAssets(),
                letterFactory.getTracingAssets(),
                letterFactory.getStrokeAssets());
    }

    private void initializeLetterAssets(String letterAssets, String tracingAssets, String strokeAssets) {
        this.tracingAssets = tracingAssets;
        InputStream stream = null;
        try {
            letterBitmap = getBitmapByAssetName(letterAssets);
            if (needInstruct) {
                traceBitmap = getBitmapByAssetName(tracingAssets);
            }
            letterRect = new Rect(0, 0, letterBitmap.getWidth(), letterBitmap.getHeight());
            anchorRect = new Rect(0, 0, anchorBitmap.getWidth(), anchorBitmap.getHeight());

            stream = getContext().getAssets().open(strokeAssets);
            Gson gson = new Gson();
            strokeBean = gson.fromJson(new InputStreamReader(stream), LetterStrokeBean.class);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (viewWidth == -1 || viewHeight == -1) {
            viewWidth = getWidth();
            viewHeight = getHeight();

            processingPaint.setStrokeWidth(viewHeight / 9f);
            viewRect = new RectF();
            viewRect.set(0, 0, viewWidth, viewHeight);
            anchorScale = viewHeight / (8.5f * anchorBitmap.getHeight());

            // don't move the following code's to constructor()
            // because viewWidth and viewHeight not initialized
            if (strokeBean != null) {
                String pointStr = strokeBean.strokes.get(0).points.get(0);
                String[] pointArray = pointStr.split(",");
                anchorPos.set((float) (viewWidth * Double.valueOf(pointArray[0])), (float) (viewHeight * Double.valueOf(pointArray[1])));
                pathToCheck = createPath(strokeBean.strokes.get(currentStroke).points);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // draw Letter
        canvas.drawBitmap(letterBitmap, letterRect, viewRect, mPaint);
        if (needInstruct) {
            canvas.drawBitmap(traceBitmap, letterRect, viewRect, mPaint);
        } else {
            // draw points along path
            canvas.drawPoints(pathPoints, pointPaint);
        }
        // draw all finished paths
        for (Path item : paths) {
            canvas.drawPath(item, processingPaint);
        }
        // draw the current unfinished path
        if (currentDrawingPath != null) canvas.drawPath(currentDrawingPath, processingPaint);
        // draw anchor
        scaleRect.set(
                anchorPos.x - anchorBitmap.getWidth() * anchorScale / 2,
                anchorPos.y - anchorBitmap.getHeight() * anchorScale / 2,
                anchorPos.x + anchorBitmap.getWidth() * anchorScale / 2,
                anchorPos.y + anchorBitmap.getHeight() * anchorScale / 2);
        canvas.drawBitmap(anchorBitmap, anchorRect, scaleRect, mPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (letterBitmap != null) {
            float scale = letterBitmap.getWidth() / (letterBitmap.getHeight() * 1f);
            int width = (int) (height * scale);
            setMeasuredDimension(width, height);
            return;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setInstructMode(boolean instructMode) {
        needInstruct = instructMode;
        traceBitmap = getBitmapByAssetName(tracingAssets);
    }

    public void setStrokeColor(@ColorInt int strokeColor) {
        if (processingPaint != null) processingPaint.setColor(strokeColor);
    }

    public void setPointColor(@ColorInt int pointColor) {
        if (pointPaint != null) pointPaint.setColor(pointColor);
    }

    private float[] toPoint(String pointStr) {
        String[] pointArray = pointStr.split(",");
        return new float[]{Float.parseFloat(pointArray[0]) * viewWidth, Float.parseFloat(pointArray[1]) * viewHeight};
    }

    private boolean isValidPoint(String trackStr, float x, float y) {
        float[] points = toPoint(trackStr);
        return Math.abs(x - points[0]) < validArea
                && Math.abs(y - points[1]) < validArea;
    }

    private boolean overlapped(int x, int y) {
        RectF touchPoint = new RectF(x - toleranceArea, y - toleranceArea,
                x + toleranceArea, y + toleranceArea);
        Path touchPointPath = new Path();
        touchPointPath.addRect(touchPoint, Path.Direction.CW);
        touchPointPath.addCircle(x, y, 20, Path.Direction.CW);
        touchPointPath.close();
        Path hourPathCopy = new Path(pathToCheck);
        hourPathCopy.op(touchPointPath, Path.Op.INTERSECT);
        touchPointPath.reset();
        RectF bounds = new RectF();
        hourPathCopy.computeBounds(bounds, true);
        return bounds.left != 0.0 && bounds.top != 0.0 && bounds.right != 0.0 && bounds.bottom != 0.0;
    }

    private boolean isTracingStartPoint(float x, float y) {
        boolean rightArea = Math.abs(anchorPos.x + anchorBitmap.getWidth() - x) < toleranceArea
                        && Math.abs(anchorPos.y - y) < anchorBitmap.getHeight() + toleranceArea;
        boolean leftArea = Math.abs(x - anchorPos.x) < toleranceArea
                && Math.abs(y - anchorPos.y) < toleranceArea;
        return leftArea || rightArea;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                hasFinishOneStroke = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (hasFinishOneStroke) return false;
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (currentStroke >= strokeBean.strokes.size()) {
            LogUtils.i(TAG, "touch event break");
            return false;
        }

        float x = event.getX();
        float y = event.getY();

        List<String> points = strokeBean.getCurrentStrokePoints(currentStroke);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!isTracingStartPoint(x, y)) {
                    return false;
                }
                LogUtils.i(TAG, "event: down");
                if (currentStokeProgress == -1) {
                    currentDrawingPath = new Path();
                    currentDrawingPath.moveTo(anchorPos.x, anchorPos.y);
                    currentStokeProgress = 1;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                LogUtils.i(TAG, "event: move");
                if (currentStokeProgress < points.size() && overlapped((int)x, (int)y)) {
                    if(isValidPoint(points.get(currentStokeProgress), x, y)) currentStokeProgress++;
                    float[] point = toPoint(points.get(currentStokeProgress - 1));
                    if (needInstruct) {
                        currentDrawingPath.lineTo(point[0], point[1]);
                    } else {
                        currentDrawingPath.lineTo(x, y);
                    }
                    if (tracingListener != null) tracingListener.onTracing(x, y);
                } else if (currentStokeProgress == points.size()) {
                    if (isValidPoint(points.get(currentStokeProgress - 1), x, y)) {
                        float[] point = toPoint(points.get(currentStokeProgress - 1));
                        currentDrawingPath.lineTo(point[0], point[1]);
                    }

                    if (currentStroke < strokeBean.strokes.size() - 1) {
                        paths.add(currentDrawingPath);
                        currentStroke++;
                        pathToCheck = createPath(strokeBean.strokes.get(currentStroke).points);
                        currentStokeProgress = -1;

                        String stepStartStr = strokeBean.strokes.get(currentStroke % strokeBean.strokes.size()).points.get(0);
                        float[] stepPoints = toPoint(stepStartStr);
                        anchorPos.set(stepPoints[0], stepPoints[1]);
                        hasFinishOneStroke = true;
                        invalidate();
                        return false;
                    } else {
                        if (!letterTracingFinished) {
                            letterTracingFinished = true;
                            hasFinishOneStroke = true;
                            if (tracingListener != null) {
                                tracingListener.onFinish();
                            }
                        }
                    }
                } else {
                    String stepStartStr = strokeBean.strokes.get(currentStroke % strokeBean.strokes.size()).points.get(0);
                    float[] stepPoints = toPoint(stepStartStr);
                    anchorPos.set(stepPoints[0], stepPoints[1]);
                    currentStokeProgress = -1;
                    currentDrawingPath = null;
                    invalidate();
                    hasFinishOneStroke = true;
                    return false;
                }
                anchorPos.set(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                LogUtils.i(TAG, "event: up");
                break;
        }
        return true;
    }


    public Path createPath(List<String> pathStr) {
        Path path = new Path();
        path.setFillType(Path.FillType.WINDING);
        List<Float> points = new ArrayList<>();
        for (int i = 0; i < pathStr.size(); i++) {
            String item = pathStr.get(i);
            String[] pointArray = item.split(",");
            float x = (float) (viewWidth * Double.valueOf(pointArray[0]));
            float y = (float) (viewHeight * Double.valueOf(pointArray[1]));
            points.add(x);
            points.add(y);
            if (i == 0) {
                path.moveTo(x, y);
            } else if (i == pathStr.size() - 1) {
                path.setLastPoint(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        pathPoints = new float[points.size()];
        for(int i = 0; i < pathPoints.length; i++){
            pathPoints[i] = points.get(i);
        }
        return path;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (traceBitmap != null) {
            traceBitmap.recycle();
            traceBitmap = null;
        }
        if (anchorBitmap != null) {
            anchorBitmap.recycle();
            anchorBitmap = null;
        }
        if (letterBitmap != null) {
            letterBitmap.recycle();
            letterBitmap = null;
        }
    }

    public Bitmap getBitmapByAssetName(String path) {
        Bitmap bitmap;
        InputStream is = null;
        try {
            AssetFileDescriptor assetFileDescriptor = getContext().getAssets().openFd(path);
            is = assetFileDescriptor.createInputStream();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true;
            bitmap = BitmapFactory.decodeStream(is, null, options);
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public interface TracingListener {
        void onFinish();
        void onTracing(float x, float y);
    }
}
