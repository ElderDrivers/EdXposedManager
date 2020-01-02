package com.solohsu.android.edxp.manager.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.solohsu.android.edxp.manager.R;

public class PreferenceMaterial extends Preference {

    private int titleColor;
    private int summaryColor;
    private int margin;
    private Context mContext;

    public PreferenceMaterial(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.PreferenceMaterial);
        titleColor = array.getColor(R.styleable.PreferenceMaterial_titleColor, 0xFF656565);
        summaryColor = array.getColor(R.styleable.PreferenceMaterial_summaryColor, 0xFF959595);
        margin = (int) array.getDimension(R.styleable.PreferenceMaterial_margin, dp(15));
        array.recycle();
    }

    public PreferenceMaterial(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.PreferenceMaterial);
        titleColor = array.getColor(R.styleable.PreferenceMaterial_titleColor, 0xFF656565);
        summaryColor = array.getColor(R.styleable.PreferenceMaterial_summaryColor, 0xFF959595);
        margin = (int) array.getDimension(R.styleable.PreferenceMaterial_margin, dp(15));
        array.recycle();
    }

    public PreferenceMaterial(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.PreferenceMaterial);
        titleColor = array.getColor(R.styleable.PreferenceMaterial_titleColor, 0xFF656565);
        summaryColor = array.getColor(R.styleable.PreferenceMaterial_summaryColor, 0xFF959595);
        margin = (int) array.getDimension(R.styleable.PreferenceMaterial_margin, dp(15));
        array.recycle();
    }

    public PreferenceMaterial(Context context) {
        super(context);
        mContext = context;
        titleColor = 0xFF656565;
        summaryColor = 0xFF959595;
        margin = dp(15);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView title = (TextView) holder.findViewById(android.R.id.title);
        TextView summary = (TextView) holder.findViewById(android.R.id.summary);
        summary.setPadding(summary.getPaddingLeft(), dp(10), summary.getPaddingRight(), summary.getPaddingBottom());
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

    public int dp(float value) {
        float density = mContext.getResources().getDisplayMetrics().density;
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }
}
