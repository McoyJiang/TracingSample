package com.danny_jiang.tracinglibrary.util;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Window;
import android.view.WindowManager;

/**
 * @author danny.jiang
 */
public class ScreenUtils {

    private static ScreenUtils instance;

    public static ScreenUtils getInstance() {
        if (instance == null) {
            instance = new ScreenUtils();
        }
        return instance;
    }

    public enum EScreenDensity {
        XXHDPI,
        XHDPI,
        HDPI,
        MDPI,
    }

    public  EScreenDensity getDisply(Context context) {
        EScreenDensity eScreenDensity;
        //init screen intensity
        DisplayMetrics dm = context.getApplicationContext().getResources().getDisplayMetrics();
        int densityDpi = dm.densityDpi;

        if (densityDpi <= 160) {
            eScreenDensity = EScreenDensity.MDPI;
        } else if (densityDpi <= 240) {
            eScreenDensity = EScreenDensity.HDPI;
        } else if (densityDpi < 400) {
            eScreenDensity = EScreenDensity.XHDPI;
        } else {
            eScreenDensity = EScreenDensity.XXHDPI;
        }
        return eScreenDensity;
    }

    /**
     * @return
     */
    public int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 获取屏幕高度
     *
     * @return
     */
    public int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * @param dp
     * @return
     */
    public float dpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    public int dpToPxInt(Context context, float dp) {
        return (int) (dpToPx(context, dp) + 0.5f);
    }

    /**
     * @param px
     * @return
     */
    public float pxToDp(Context context, float px) {
        return px / context.getResources().getDisplayMetrics().density;
    }

    public int pxToDpInt(Context context, float px) {
        return (int) (pxToDp(context, px) + 0.5f);
    }

    public int getActionBarSize(Context context) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            int actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
            return actionBarHeight;
        }
        return 0;
    }

    /**
     * @param context context
     * @return boolean
     */
    public  final boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * @param context context
     * @return boolean
     */
    public  final boolean isPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    /**
     * @param from    from>=0&&from<=1.0f
     * @param to      to>=0&&to<=1.0f
     * @param context 当前的activity
     */
    public  void dimBackground(final float from, final float to, Activity context) {
        final Window window = context.getWindow();
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(from, to);
        valueAnimator.setDuration(500);
        valueAnimator.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        WindowManager.LayoutParams params = window.getAttributes();
                        params.alpha = (Float) animation.getAnimatedValue();
                        window.setAttributes(params);
                    }
                });
        valueAnimator.start();
    }

    /**
     * @param activity
     * @return
     */
    public  boolean isAutoBrightness(Activity activity) {
        boolean isAutoAdjustBright = false;
        try {
            isAutoAdjustBright = Settings.System.getInt(
                    activity.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return isAutoAdjustBright;
    }

    /**
     * @param activity
     */
    public  void stopAutoBrightness(Activity activity) {
        Settings.System.putInt(activity.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    /**
     * @param activity
     */

    public  void startAutoBrightness(Activity activity) {
        Settings.System.putInt(activity.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }

    /**
     * @param mContext
     * @return 0~100
     */
    public  float getScreenBrightness(Context mContext) {
        int screenBrightness = 255;
        try {
            screenBrightness = Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return screenBrightness / 255.0F * 100;
    }

    /**
     * @param paramInt 0~100
     * @param mContext
     */
    public  void saveScreenBrightness(int paramInt, Context mContext) {
        if (paramInt <= 5) {
            paramInt = 5;
        }
        try {
            float f = paramInt / 100.0F * 255;
            Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, (int) f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param paramInt
     * @param mActivity
     */
    public void setScreenBrightness(int paramInt, Activity mActivity) {
        if (paramInt <= 5) {
            paramInt = 5;
        }
        Window localWindow = mActivity.getWindow();
        WindowManager.LayoutParams localLayoutParams = localWindow.getAttributes();
        float f = paramInt / 100.0F;
        localLayoutParams.screenBrightness = f;
        localWindow.setAttributes(localLayoutParams);
    }

    public float getScreenRatio(Context context) {
        return getScreenWidth(context) * 1.0f / getScreenHeight(context);
    }
}
