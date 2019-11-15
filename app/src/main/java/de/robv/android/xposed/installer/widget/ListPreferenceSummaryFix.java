package de.robv.android.xposed.installer.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;

import com.solohsu.android.edxp.manager.R;

public class ListPreferenceSummaryFix extends ListPreference {

    private int titleColor;

    public ListPreferenceSummaryFix(Context context) {
        super(context);
    }

    public ListPreferenceSummaryFix(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ListPreferenceSummaryFix);
        titleColor = array.getColor(R.styleable.ListPreferenceSummaryFix_titleColorListSummaryFix, 0xFF323232);
        array.recycle();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView title = (TextView) holder.findViewById(android.R.id.title);
        TextView summary = (TextView) holder.findViewById(android.R.id.summary);
        summary.setPadding(summary.getPaddingLeft(), dp(10), summary.getPaddingRight(), summary.getPaddingBottom());
        title.setTextColor(titleColor);
        summary.setTextColor(0xFF959595);
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        notifyChanged();
    }

    private int dp(float value) {
        float density = getContext().getResources().getDisplayMetrics().density;

        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }
}