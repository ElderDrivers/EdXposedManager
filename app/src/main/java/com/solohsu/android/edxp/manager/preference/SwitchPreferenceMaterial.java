package com.solohsu.android.edxp.manager.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;

import com.solohsu.android.edxp.manager.R;

public class SwitchPreferenceMaterial extends SwitchPreference {

    private int titleColor;
    private int summaryColor;
    private int margin;
    private Context mContext;

    public SwitchPreferenceMaterial(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SwitchPreferenceMaterial);
        titleColor = array.getColor(R.styleable.SwitchPreferenceMaterial_titleColor, 0xFF656565);
        summaryColor = array.getColor(R.styleable.SwitchPreferenceMaterial_summaryColor, 0xFF959595);
        margin = (int) array.getDimension(R.styleable.SwitchPreferenceMaterial_margin, dp(mContext, 15));
        array.recycle();
    }

    public SwitchPreferenceMaterial(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SwitchPreferenceMaterial);
        titleColor = array.getColor(R.styleable.SwitchPreferenceMaterial_titleColor, 0xFF656565);
        summaryColor = array.getColor(R.styleable.SwitchPreferenceMaterial_summaryColor, 0xFF959595);
        margin = (int) array.getDimension(R.styleable.SwitchPreferenceMaterial_margin, dp(mContext, 15));
        array.recycle();
    }

    public SwitchPreferenceMaterial(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SwitchPreferenceMaterial);
        titleColor = array.getColor(R.styleable.SwitchPreferenceMaterial_titleColor, 0xFF656565);
        summaryColor = array.getColor(R.styleable.SwitchPreferenceMaterial_summaryColor, 0xFF959595);
        margin = (int) array.getDimension(R.styleable.SwitchPreferenceMaterial_margin, dp(mContext, 15));
        array.recycle();
    }

    public SwitchPreferenceMaterial(Context context) {
        super(context);
        mContext = context;
        titleColor = 0xFF656565;
        summaryColor = 0xFF959595;
        margin = dp(mContext, 15);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView title = (TextView) holder.findViewById(android.R.id.title);
        TextView summary = (TextView) holder.findViewById(android.R.id.summary);
        summary.setPadding(summary.getPaddingLeft(), dp(mContext, 10), summary.getPaddingRight(), summary.getPaddingBottom());
        title.setTextColor(titleColor);
        summary.setTextColor(summaryColor);
        holder.itemView.setPadding(margin, holder.itemView.getPaddingTop(), margin, holder.itemView.getPaddingBottom());
    }

    public void setTitleColor(@ColorInt int color) {
        titleColor = color;
    }

    public void setSummaryColor(@ColorInt int color) {
        summaryColor = color;
    }

    public int dp(Context context, float value) {
        float density = context.getResources().getDisplayMetrics().density;

        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }
}
