/**
 * This is sample code provided by Wowza Media Systems, LLC.  All sample code is intended to be a reference for the
 * purpose of educating developers, and is not intended to be used in any production environment.
 *
 * IN NO EVENT SHALL WOWZA MEDIA SYSTEMS, LLC BE LIABLE TO YOU OR ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL,
 * OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION,
 * EVEN IF WOWZA MEDIA SYSTEMS, LLC HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * WOWZA MEDIA SYSTEMS, LLC SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. ALL CODE PROVIDED HEREUNDER IS PROVIDED "AS IS".
 * WOWZA MEDIA SYSTEMS, LLC HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * Copyright © 2015 Wowza Media Systems, LLC. All rights reserved.
 */

package com.wowza.gocoder.sdk.sampleapp.config;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

public class AutoCompletePreference extends EditTextPreference {
    private final String TAG = AutoCompletePreference.class.getSimpleName();

    private AutoCompleteTextView mAutoCompleteEditText = null;

    public AutoCompletePreference(Context context) {
        super(context);
    }

    public AutoCompletePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoCompletePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        // find the current EditText object
        final EditText editText = (EditText) view.findViewById(android.R.id.edit);

        // construct a new editable autocomplete object with the appropriate params
        // and id that the TextEditPreference is expecting
        mAutoCompleteEditText = new AutoCompleteTextView(getContext());

        mAutoCompleteEditText.setLayoutParams(editText.getLayoutParams());
        mAutoCompleteEditText.setImeOptions(editText.getImeOptions());
        mAutoCompleteEditText.setInputType(editText.getInputType());
        mAutoCompleteEditText.setKeyListener(editText.getKeyListener());
        mAutoCompleteEditText.setMaxLines(editText.getMaxLines());
        mAutoCompleteEditText.setText(editText.getText());

        mAutoCompleteEditText.setSelectAllOnFocus(true);
        mAutoCompleteEditText.setThreshold(1);

        // remove old edit text field from the existing layout hierarchy
        ViewGroup vg = (ViewGroup) editText.getParent();
        vg.removeView(editText);

        mAutoCompleteEditText.setId(android.R.id.edit);

        //mAutoCompleteEditText.setSelection(mAutoCompleteEditText.getText().length());

        // Set auto complete values stored in shared preferences as a string set
        String[] autoCompleteList = ConfigPrefs.getAutoCompleteList(getSharedPreferences(), getKey());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, autoCompleteList);
        mAutoCompleteEditText.setAdapter(adapter);

        mAutoCompleteEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focused) {
                if (focused && mAutoCompleteEditText != null && mAutoCompleteEditText.getAdapter().getCount() > 0)
                    mAutoCompleteEditText.showDropDown();
            }
        });

        mAutoCompleteEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (((AutoCompleteTextView)view).getText().length() == 0)
                    mAutoCompleteEditText.showDropDown();
            }
        });

        // add the new view to the layout
        vg.addView(mAutoCompleteEditText);
    }

    /**
     * Because the base class does not handle this correctly
     * we need to query our injected AutoCompleteTextView for
     * the value to save
     */
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult && mAutoCompleteEditText != null) {
            String value = mAutoCompleteEditText.getText().toString();
            if (callChangeListener(value)) {
                setText(value);
            }
        }
    }

    /**
     * again we need to override methods from the base class
     */
    @Override
    public EditText getEditText() {
        return mAutoCompleteEditText;
    }
}
