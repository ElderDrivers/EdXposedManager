package de.robv.android.xposed.installer.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.solohsu.android.edxp.manager.R;

public class BottomNavigationViewCompact extends BottomNavigationView {

    private int selectedTextColor;
    private int unSelectedTextColor;
    private int selectedIconColor;
    private int unSelectedIconColor;

    public BottomNavigationViewCompact(@NonNull Context context) {
        super(context);
    }

    public BottomNavigationViewCompact(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public BottomNavigationViewCompact(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BottomNavigationViewCompact);
        selectedTextColor = a.getColor(R.styleable.BottomNavigationViewCompact_bItemSelectedTextColor, getResources().getColor(R.color.colorAccent));
        unSelectedTextColor = a.getColor(R.styleable.BottomNavigationViewCompact_bItemUnSelectedTextColor, getResources().getColor(R.color.tab_unselected_color));
        selectedIconColor = a.getColor(R.styleable.BottomNavigationViewCompact_bItemSelectedIconColor, getResources().getColor(R.color.colorAccent));
        unSelectedIconColor = a.getColor(R.styleable.BottomNavigationViewCompact_bItemUnSelectedIconColor, getResources().getColor(R.color.tab_unselected_color));
        a.recycle();
        setItemIconTintList(createColorStateList(selectedIconColor, unSelectedIconColor));
        setItemTextColor(createColorStateList(selectedTextColor, unSelectedTextColor));
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

    public void setItemSelectedTextColor(@ColorInt int color) {
        this.selectedTextColor = color;
        setItemTextColor(createColorStateList(color, unSelectedTextColor));
    }

    public void setItemUnSelectedTextColor(@ColorInt int color) {
        this.unSelectedTextColor = color;
        setItemTextColor(createColorStateList(selectedTextColor, color));
    }

    public int getSelectedTextColor() {
        return selectedTextColor;
    }

    public int getUnSelectedTextColor() {
        return unSelectedTextColor;
    }

    public void setItemSelectedIconColor(int color) {
        this.selectedIconColor = color;
        setItemIconTintList(createColorStateList(color, unSelectedIconColor));
    }

    public void setItemUnSelectedIconColor(int color) {
        this.unSelectedIconColor = color;
        setItemIconTintList(createColorStateList(selectedIconColor, color));
    }

    public int getSelectedIconColor() {
        return selectedIconColor;
    }

    public int getUnSelectedIconColor() {
        return unSelectedIconColor;
    }
}
