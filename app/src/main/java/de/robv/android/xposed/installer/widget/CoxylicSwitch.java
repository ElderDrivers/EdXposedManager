package de.robv.android.xposed.installer.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.Switch;

import com.solohsu.android.edxp.manager.R;

public class CoxylicSwitch extends Switch {

    private int thumbOnColor;
    private int thumbOffColor;
    private int trackOnColor;
    private int trackOffColor;

    public CoxylicSwitch(Context context) {
        super(context);
    }

    public CoxylicSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs);
    }

    public CoxylicSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
    }

    public CoxylicSwitch(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttrs(context, attrs);
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CoxylicSwitch);
        thumbOnColor = a.getColor(R.styleable.CoxylicSwitch_thumbOnColor, getResources().getColor(R.color.colorAccent));
        thumbOffColor = a.getColor(R.styleable.CoxylicSwitch_thumbOffColor, 0xFFF5F5F5);
        trackOnColor = a.getColor(R.styleable.CoxylicSwitch_trackOnColor, getResources().getColor(R.color.colorAccentLight));
        trackOffColor = a.getColor(R.styleable.CoxylicSwitch_trackOnColor, 0xFFBFBFBF);
        setThumbTintList(createColorStateList(thumbOnColor, thumbOffColor));
        setTrackTintList(createColorStateList(trackOnColor, trackOffColor));
        a.recycle();
    }

    private ColorStateList createColorStateList(int checked, int normal) {
        //状态
        int[][] states = new int[2][];
        //按下
        states[0] = new int[]{android.R.attr.state_checked};
        //默认
        states[1] = new int[]{};

        //状态对应颜色值（按下，默认）
        int[] colors = new int[]{checked, normal};
        return new ColorStateList(states, colors);
    }

    public void setThumbOnColor(int color) {
        this.thumbOnColor = color;
        setThumbTintList(createColorStateList(color, thumbOffColor));
    }

    public void setThumbOffColor(int color) {
        this.thumbOffColor = color;
        setThumbTintList(createColorStateList(thumbOnColor, color));
    }

    public void setTrackOnColor(int color) {
        this.trackOnColor = color;
        setTrackTintList(createColorStateList(color, trackOnColor));
    }

    public void setTrackOffColor(int color) {
        this.trackOffColor = color;
        setTrackTintList(createColorStateList(color, trackOffColor));
    }

    public int getThumbOnColor() {
        return thumbOnColor;
    }

    public int getThumbOffColor() {
        return thumbOffColor;
    }

    public int getTrackOnColor() {
        return trackOnColor;
    }

    public int getTrackOffColor() {
        return trackOffColor;
    }
}
