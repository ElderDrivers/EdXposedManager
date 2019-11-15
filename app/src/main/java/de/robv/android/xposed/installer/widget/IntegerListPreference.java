package de.robv.android.xposed.installer.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;

import com.solohsu.android.edxp.manager.R;

public class IntegerListPreference extends ListPreference {

    private int titleColor;

    public IntegerListPreference(Context context) {
        super(context);
    }

    public IntegerListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.IntegerListPreference);
        titleColor = array.getColor(R.styleable.IntegerListPreference_titleColorIntegerList, 0xFF323232);
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

    private int dp(float value) {
        float density = getContext().getResources().getDisplayMetrics().density;

        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }

    public static int getIntValue(String value) {
        if (value == null)
            return 0;

        return (int) ((value.startsWith("0x"))
                ? Long.parseLong(value.substring(2), 16)
                : Long.parseLong(value));
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        notifyChanged();
    }

    @Override
    protected boolean persistString(String value) {
        return value != null && persistInt(getIntValue(value));

    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        SharedPreferences pref = getPreferenceManager().getSharedPreferences();
        String key = getKey();
        if (!shouldPersist() || !pref.contains(key))
            return defaultReturnValue;

        return String.valueOf(pref.getInt(key, 0));
    }

    @Override
    public int findIndexOfValue(String value) {
        CharSequence[] entryValues = getEntryValues();
        int intValue = getIntValue(value);
        if (value != null && entryValues != null) {
            for (int i = entryValues.length - 1; i >= 0; i--) {
                if (getIntValue(entryValues[i].toString()) == intValue) {
                    return i;
                }
            }
        }
        return -1;
    }
}