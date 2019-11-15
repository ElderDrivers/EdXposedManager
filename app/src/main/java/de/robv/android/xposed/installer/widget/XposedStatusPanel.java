package de.robv.android.xposed.installer.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.solohsu.android.edxp.manager.R;

public class XposedStatusPanel extends LinearLayout {

    private Drawable errorDrawable;
    private Drawable warnDrawable;
    private Drawable successDrawable;
    private int state = 0;

    public static final int XPOSED_STATE_ERROR = -2;
    public static final int XPOSED_STATE_WARN = -1;
    public static final int XPOSED_STATE_SUCCESS = 0;

    public XposedStatusPanel(Context context) {
        super(context);
        state = -2;
        errorDrawable = getResources().getDrawable(R.drawable.status_bg_error);
        warnDrawable = getResources().getDrawable(R.drawable.status_bg_warn);
        successDrawable = getResources().getDrawable(R.drawable.status_bg);
        setXposedState(state);
    }

    public XposedStatusPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public XposedStatusPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    public XposedStatusPanel(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.XposedStatusPanel);
        errorDrawable = a.getDrawable(R.styleable.XposedStatusPanel_errorBackground);
        warnDrawable = a.getDrawable(R.styleable.XposedStatusPanel_warnBackground);
        successDrawable = a.getDrawable(R.styleable.XposedStatusPanel_successBackground);
        state = a.getInt(R.styleable.XposedStatusPanel_xposedState, -2);
        a.recycle();
        setXposedState(state);
    }

    public void setXposedState(int state) {
        this.state = state;
        switch (state) {
            case XPOSED_STATE_ERROR:
                setBackground(errorDrawable);
                break;
            case XPOSED_STATE_WARN:
                setBackground(warnDrawable);
                break;
            case XPOSED_STATE_SUCCESS:
                setBackground(successDrawable);
                break;
        }
    }

    public Drawable getSuccessDrawable() {
        return successDrawable;
    }

    public Drawable getWarnDrawable() {
        return warnDrawable;
    }

    public Drawable getErrorDrawable() {
        return errorDrawable;
    }

    public void setWarnDrawable(Drawable warnDrawable) {
        this.warnDrawable = warnDrawable;
    }

    public void setSuccessDrawable(Drawable successDrawable) {
        this.successDrawable = successDrawable;
    }

    public void setErrorDrawable(Drawable errorDrawable) {
        this.errorDrawable = errorDrawable;
    }

    public int getXposedState() {
        return state;
    }
}
