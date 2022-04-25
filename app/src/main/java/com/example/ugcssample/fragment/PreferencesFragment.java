package com.example.ugcssample.fragment;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.example.ugcssample.R;

public class PreferencesFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
