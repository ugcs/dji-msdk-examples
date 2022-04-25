package com.example.ugcssample.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import com.example.ugcssample.R;
import com.example.ugcssample.fragment.PreferencesFragment;


public class PreferencesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.preferences_fragment_container, new PreferencesFragment())
                .commit();
    }
}