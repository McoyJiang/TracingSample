package com.danny_jiang.tracinglibrary.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
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
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.support.annotation.Nullable;
import android.text.TextUtils;
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
    private Bitmap anchorBitmap;
    private Bitmap letterBitmap;
    private Bitmap drawingBp;
    private Canvas drawingCanvas;

    private LetterStrokeBean strokeBean;
    private PointF letterStarterPos = new PointF();
    // this is used to point the position of anchor
    private PointF anchorPos = new PointF();
    private String letterAssets;
    private TracingListener listener;
    private int viewWidth = -1;
    private int viewHeight = -1;
    private float toleranceArea;


    public TracingLetterView(Context context) {
        this(context, null);
    }

    public TracingLetterView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TracingLetterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);

        processingPaint = new Paint();
        processingPaint.setAntiAlias(true);
        processingPaint.setDither(true);
        processingPaint.setStyle(Paint.Style.STROKE);
        processingPaint.setStrokeCap(Paint.Cap.ROUND);
        processingPaint.setStrokeJoin(Paint.Join.ROUND);
        processingPaint.setPathEffect(new CornerPathEffect(ScreenUtils.getInstance().dpToPx(getContext(),60)));
        processingPaint.setColor(Color.parseColor("#DA609F"));

        toleranceArea = getResources().getDimension(R.dimen.dp_30);
    }

    /**
     * @param listener callback tracing letter finish state
     */
    public void setListener(TracingListener listener) {
        this.listener = listener;
    }

    public void setLetterChar(@LetterFactory.Letter int letterChar) {
        LetterFactory letterFactory = new LetterFactory();
        letterFactory.setLetter(letterChar);
        initializeLetterAssets(letterFactory.getLetterAssets(),
                letterFactory.getTracingAssets(),
                letterFactory.getStrokeAssets());
    }

    private void initializeLetterAssets(String letterAssets, String tracingAssets, String strokeAssets) {
        this.letterAssets = letterAssets;
        InputStream stream = null;
        try {
            anchorBitmap = getBitmapByAssetName("crayon.png");
            letterBitmap = getBitmapByAssetName(letterAssets);
            Bitmap traceBitmap = getBitmapByAssetName(tracingAssets);

            Canvas canvas = new Canvas(letterBitmap);
            canvas.drawBitmap(traceBitmap, 0, 0, mPaint);

            traceBitmap.recycle();

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

    private Path pathToCheck;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (viewWidth == -1 || viewHeight == -1) {
            viewWidth = getWidth();
            viewHeight = getHeight();

            // don't move the following code's to init()
            // because viewWidth and viewHeight not initialized
            if (strokeBean != null) {
                String pointStr = strokeBean.strokes.get(0).points.get(0);
                String[] pointArray = pointStr.split(",");
                letterStarterPos.set((float) (viewWidth * Double.valueOf(pointArray[0])), (float) (viewHeight * Double.valueOf(pointArray[1])));
                anchorPos.set(letterStarterPos);
                pathToCheck = createPath(strokeBean.strokes.get(currentStroke).points);
                setupPathAnimation(pathToCheck);
            }
        }
    }

    private Rect letterRect;
    private RectF viewRect = null;
    private Rect anchorRect;
    private RectF scaleRect = new RectF();
    private float scale = 0f;
    private float anchorScale = 0f;


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (drawingBp == null) {
            processingPaint.setStrokeWidth(viewHeight / 9f);
            drawingBp = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
            drawingCanvas = new Canvas(drawingBp);
            viewRect = new RectF();

            scale = viewHeight / (letterBitmap.getHeight() * 1f);
            anchorScale = scale * 1.2f;

            viewRect.set(0, 0, viewWidth, viewHeight);
            drawingCanvas.drawBitmap(letterBitmap, letterRect, viewRect, mPaint);
            processingPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        }

        canvas.drawBitmap(drawingBp, 0, 0, mPaint);
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

    private Path currentDrawingPath;
    private int currentStroke = 0;
    private int currentStokeProgress = 0;
    private List<Path> paths = new ArrayList<>();
    private boolean needInstruct = true;
    private boolean letterTracingFinished = false;
    private boolean hasFinishOneStroke = false;
    private PointF startPoint = new PointF();

    private float[] toPoint(String pointStr) {
        String[] pointArray = pointStr.split(",");
        return new float[]{Float.parseFloat(pointArray[0]) * viewWidth, Float.parseFloat(pointArray[1]) * viewHeight};
    }

    private boolean isValidPoint(String trackStr, float x, float y) {
        float[] points = toPoint(trackStr);
        boolean valid = Math.abs(x - points[0]) < toleranceArea
                && Math.abs(y - points[1]) < toleranceArea;
        return valid;
    }

    private boolean overlapped(int x, int y) {
        RectF touchPoint = new RectF(x, y, x + 20, y + 20);
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
        return Math.abs(x - startPoint.x) < toleranceArea + 15
                && Math.abs(y - startPoint.y) < toleranceArea + 15;
    }

    private boolean isStartTracingPoint(String trackStr, float x, float y) {
        float[] points = toPoint(trackStr);
        return Math.abs(x - points[0]) < toleranceArea + 15
                && Math.abs(y - points[1]) < toleranceArea + 15;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (currentStroke >= strokeBean.strokes.size()) {
            LogUtils.i(TAG, "touch event break");
            return false;
        }

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
        float x = event.getX();
        float y = event.getY();
        if (overlapped((int) x, (int) y)) {
            LogUtils.i("ABC", "overlap true");
        } else {
            LogUtils.i("ABC", "overlap false");
        }

        List<String> points = strokeBean.getCurrentStrokePoints(currentStroke);
        String[] pointStr = points.get(0).split(",");
        float[] startPoint = new float[]{Float.parseFloat(pointStr[0]) * viewWidth,
                Float.parseFloat(pointStr[1]) * viewHeight};

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!isTracingStartPoint(x, y)) {
                    return false;
                }
                LogUtils.i(TAG, "event: down");
                currentDrawingPath = new Path();
                currentDrawingPath.moveTo(startPoint[0], startPoint[1]);
                currentStokeProgress = 1;
                break;
            case MotionEvent.ACTION_MOVE:
                LogUtils.i(TAG, "event: move");
                if (currentStokeProgress < points.size() && overlapped((int)x, (int)y)) {
                    if(isValidPoint(points.get(currentStokeProgress), x, y)) currentStokeProgress++;
                    this.startPoint.x = x;
                    this.startPoint.y = y;
                    float[] point = toPoint(points.get(currentStokeProgress - 1));
                    if (needInstruct) {
                        currentDrawingPath.lineTo(point[0], point[1]);
                        //currentDrawingPath.lineTo(x, y);
                    } else {
                        currentDrawingPath.lineTo(x, y);
                    }
                    drawingCanvas.drawPath(currentDrawingPath, processingPaint);
                } else {
                    if (currentStokeProgress == points.size() && isValidPoint(points.get(currentStokeProgress - 1), x, y)) {
                        float[] point = toPoint(points.get(currentStokeProgress - 1));
                        currentDrawingPath.lineTo(point[0], point[1]);
                        drawingCanvas.drawPath(currentDrawingPath, processingPaint);
                    }

                    if (currentStokeProgress == points.size()
                            && currentStroke < strokeBean.strokes.size() - 1) {
                        paths.add(currentDrawingPath);
                        currentStroke++;
                        pathToCheck = createPath(strokeBean.strokes.get(currentStroke).points);
                        currentStokeProgress = 1;

                        String stepStartStr = strokeBean.strokes.get(currentStroke % strokeBean.strokes.size()).points.get(0);
                        float[] stepPoints = toPoint(stepStartStr);
                        anchorPos.set(stepPoints[0], stepPoints[1]);
                        this.startPoint.x = stepPoints[0];
                        this.startPoint.y = stepPoints[1];
                        hasFinishOneStroke = true;
                        invalidate();
                        return false;
                    }
                }
                anchorPos.set(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                LogUtils.i(TAG, "event: up");
                if (currentStokeProgress == points.size()) {

                    paths.add(currentDrawingPath);
                    if (currentStroke < strokeBean.strokes.size()) {
                        currentStroke++;
                    }

                    if (currentStroke == strokeBean.strokes.size()) {
                        mPaint.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#DA609F"),
                                PorterDuff.Mode.SRC_ATOP));
                        invalidate();

                        needInstruct = false;
                        currentStroke = 0;

                        if (!letterTracingFinished) {
                            letterTracingFinished = true;
                            anchorToSmall(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    outOffScreen();
                                }
                            });
                        } else {
                            anchorToSmall(null);
                            postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (listener != null) {
                                        listener.onFinish();
                                        listener = null;
                                    }
                                }
                            }, 1500);

                        }
                    } else if (needInstruct && drawingCanvas != null) {
                        String stepStartStr = strokeBean.strokes.get(currentStroke % strokeBean.strokes.size()).points.get(0);
                        float[] stepPoints = toPoint(stepStartStr);
                        anchorPos.set(stepPoints[0], stepPoints[1]);

                        /**
                         * deal with some track path just a point
                         */
                        if (strokeBean.strokes.get(currentStroke % strokeBean.strokes.size()).points.size() == 1) {
                            invalidate();
                        }

                    } else {
                        // for stroke correct
                        String stepStartStr = strokeBean.strokes.get(currentStroke % strokeBean.strokes.size()).points.get(0);
                        float[] stepPoints = toPoint(stepStartStr);
                        anchorPos.set(stepPoints[0], stepPoints[1]);
                        invalidate();
                    }

                } else {
                    //anchorPos.set(startPoint[0], startPoint[1]);
                    //anchorPos.set(x, y);
//                    this.startPoint.x = anchorPos.x;
//                    this.startPoint.y = anchorPos.y;
//                    drawingCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
//                    drawingCanvas.drawBitmap(letterBitmap, letterRect, viewRect, mPaint);
//                    for (Path item : paths) {
//                        drawingCanvas.drawPath(item, processingPaint);
//                    }
//                    invalidate();
                }
                //currentStokeProgress = 0;

                break;
        }
        return true;
    }

    private ValueAnimator setupPathAnimation(final Path path) {
        final PathMeasure measure = new PathMeasure(path, false);
        final float len = measure.getLength();
        final Path drawingPath = new Path();
        final float[] startPoint = new float[2];
        measure.getPosTan(0, startPoint, null);
        drawingPath.moveTo(startPoint[0], startPoint[1]);
        this.startPoint.x = startPoint[0];
        this.startPoint.y = startPoint[1];
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                float[] point = new float[2];
                measure.getPosTan(len * val, point, null);
                anchorPos.set(point[0], point[1]);
                drawingPath.lineTo(point[0], point[1]);
                invalidate();
            }
        });

        int dpi = getResources().getDisplayMetrics().densityDpi;
        animator.setDuration((long) (len / dpi * 1500));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                anchorPos.set(startPoint[0], startPoint[1]);
                invalidate();
                processingPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                anchorPos.set(startPoint[0], startPoint[1]);
                invalidate();
                processingPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            }
        });
        return animator;
    }

    public Path createPath(List<String> pathStr) {
        Path path = new Path();
        path.setFillType(Path.FillType.WINDING);
        for (int i = 0; i < pathStr.size(); i++) {
            String item = pathStr.get(i);
            String[] pointArray = item.split(",");
            if (i == 0) {
                path.moveTo((float) (viewWidth * Double.valueOf(pointArray[0])), (float) (viewHeight * Double.valueOf(pointArray[1])));
            } else if (i == pathStr.size() - 1) {
                path.setLastPoint((float) (viewWidth * Double.valueOf(pointArray[0])), (float) (viewHeight * Double.valueOf(pointArray[1])));
            } else {
                path.lineTo((float) (viewWidth * Double.valueOf(pointArray[0])), (float) (viewHeight * Double.valueOf(pointArray[1])));
            }

        }

        return path;
    }

    private void outOffScreen() {
        ObjectAnimator translateAnimatorOut = ObjectAnimator.ofFloat(this, "translationY", 0, ScreenUtils.getInstance().getScreenHeight(getContext()));
        translateAnimatorOut.setDuration(500);
        translateAnimatorOut.setStartDelay(300);
        translateAnimatorOut.setInterpolator(new AccelerateInterpolator());
        translateAnimatorOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (letterBitmap != null) {
                    letterBitmap.recycle();
                    letterBitmap = null;
                }
                letterBitmap = getBitmapByAssetName(letterAssets);
                paths.clear();
                mPaint.setColorFilter(null);
                drawingCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
                drawingCanvas.drawBitmap(letterBitmap, letterRect, viewRect, mPaint);
                invalidate();
            }
        });

        ObjectAnimator translateAnimatorIn = ObjectAnimator.ofFloat(this, "translationY", ScreenUtils.getInstance().getScreenHeight(getContext()), 0);
        translateAnimatorIn.setDuration(500);
        translateAnimatorIn.setInterpolator(new AccelerateDecelerateInterpolator());
        translateAnimatorIn.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                List<String> points = strokeBean.getCurrentStrokePoints(currentStroke);
                float[] startPoint = toPoint(points.get(0));
                pathToCheck = createPath(strokeBean.strokes.get(currentStroke).points);
                setupPathAnimation(pathToCheck);
                anchorScale = scale * 1.2f;
                anchorPos.set(startPoint[0], startPoint[1]);
                invalidate();
            }
        });
        AnimatorSet set = new AnimatorSet();
        set.playSequentially(translateAnimatorOut, translateAnimatorIn);
        set.start();

    }

    private void scaleAnimation(Animator.AnimatorListener listener) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, SCALE_X, 1.0f, 1.15f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, SCALE_Y, 1.0f, 1.15f, 1.0f);
        AnimatorSet scaleAnimation = new AnimatorSet();
        scaleAnimation.playTogether(scaleX, scaleY);
        scaleAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleAnimation.setDuration(800);
        scaleAnimation.start();

        if (listener != null) {
            scaleAnimation.addListener(listener);
        }

    }

    private void anchorToSmall(Animator.AnimatorListener listener) {
        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(1f, 0f);
        scaleAnimator.setInterpolator(new AccelerateInterpolator());
        scaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                anchorScale *= val;
                invalidate();
            }
        });
        scaleAnimator.setStartDelay(200);
        scaleAnimator.setDuration(800);

        if (listener != null) {
            scaleAnimator.addListener(listener);
        }

        scaleAnimator.start();
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (anchorBitmap != null) {
            anchorBitmap.recycle();
            anchorBitmap = null;
        }


        if (letterBitmap != null) {
            letterBitmap.recycle();
            letterBitmap = null;
        }

        if (drawingBp != null) {
            drawingBp.recycle();
            drawingBp = null;
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
    }
}
