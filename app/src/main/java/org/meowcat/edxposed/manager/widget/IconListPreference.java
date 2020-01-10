package org.meowcat.edxposed.manager.widget;

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceViewHolder;

import org.meowcat.edxposed.manager.R;

import java.util.ArrayList;
import java.util.List;

public class IconListPreference extends ListPreference {

    private List<Drawable> mEntryDrawables = new ArrayList<>();

    public IconListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.IconListPreference, 0, 0);

        CharSequence[] drawables;

        try {
            drawables = a.getTextArray(R.styleable.IconListPreference_icons);
        } finally {
            a.recycle();
        }

        for (CharSequence drawable : drawables) {
            int resId = context.getResources().getIdentifier(drawable.toString(), "mipmap", context.getPackageName());

            Drawable d = context.getResources().getDrawable(resId);

            mEntryDrawables.add(d);
        }

        setWidgetLayoutResource(R.layout.color_icon_preview);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        String selectedValue = getValue();
        int selectedIndex = findIndexOfValue(selectedValue);

        Drawable drawable = mEntryDrawables.get(selectedIndex);

        ((ImageView) holder.findViewById(R.id.preview)).setImageDrawable(drawable);
    }

    public static class IconListPreferenceDialog extends PreferenceDialogFragmentCompat {

        private static final java.lang.String KEY_CLICKED_ENTRY_INDEX
                = "settings.CustomListPrefDialog.KEY_CLICKED_ENTRY_INDEX";
        private int mClickedDialogEntryIndex;

        public IconListPreferenceDialog() {

        }

        public static IconListPreferenceDialog newInstance(String key) {
            IconListPreferenceDialog dialog = new IconListPreferenceDialog();
            final Bundle b = new Bundle(1);
            b.putString(ARG_KEY, key);
            dialog.setArguments(b);
            return dialog;
        }

        protected IconListPreference getCustomizablePreference() {
            return (IconListPreference) getPreference();
        }

        protected ListAdapter createListAdapter() {
            final String selectedValue = getCustomizablePreference().getValue();
            int selectedIndex = getCustomizablePreference().findIndexOfValue(selectedValue);
            return new AppArrayAdapter(getContext(), R.layout.icon_preference_item,
                    getCustomizablePreference().getEntries(), getCustomizablePreference().mEntryDrawables, selectedIndex);
        }

        @Override
        protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
            builder.setAdapter(createListAdapter(), this);
            super.onPrepareDialogBuilder(builder);
            mClickedDialogEntryIndex = getCustomizablePreference()
                    .findIndexOfValue(getCustomizablePreference().getValue());
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dialog = super.onCreateDialog(savedInstanceState);
            if (savedInstanceState != null) {
                mClickedDialogEntryIndex = savedInstanceState.getInt(KEY_CLICKED_ENTRY_INDEX,
                        mClickedDialogEntryIndex);
            }
            return dialog;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt(KEY_CLICKED_ENTRY_INDEX, mClickedDialogEntryIndex);
        }

        protected void setClickedDialogEntryIndex(int which) {
            mClickedDialogEntryIndex = which;
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            Log.d("solo", "onDialogClosed:" + positiveResult);
            final ListPreference preference = getCustomizablePreference();
            final String value = getValue();
            if (value != null) {
                if (preference.callChangeListener(value)) {
                    preference.setValue(value);
                }
            }
        }

        private String getValue() {
            final ListPreference preference = getCustomizablePreference();
            if (mClickedDialogEntryIndex >= 0 && preference.getEntryValues() != null) {
                return preference.getEntryValues()[mClickedDialogEntryIndex].toString();
            } else {
                return null;
            }
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            setClickedDialogEntryIndex(which);
        }
    }

    public static class AppArrayAdapter extends ArrayAdapter<CharSequence> {
        private List<Drawable> mImageDrawables = null;
        private int mSelectedIndex = 0;

        public AppArrayAdapter(Context context, int textViewResourceId,
                               CharSequence[] objects, List<Drawable> imageDrawables,
                               int selectedIndex) {
            super(context, textViewResourceId, objects);
            mSelectedIndex = selectedIndex;
            mImageDrawables = imageDrawables;
        }

        @Override
        @SuppressLint("ViewHolder")
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            View view = inflater.inflate(R.layout.icon_preference_item, parent, false);
            CheckedTextView textView = view.findViewById(R.id.label);
            textView.setText(getItem(position));
            textView.setChecked(position == mSelectedIndex);

            ImageView imageView = view.findViewById(R.id.icon);
            imageView.setImageDrawable(mImageDrawables.get(position));
            return view;
        }
    }
}