package com.example.project_.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import com.example.project_.R;
import com.example.project_.databinding.ActivitySettingsBinding;

public class SettingsActivity extends BaseActivity {
    ActivitySettingsBinding binding;
    boolean nightMode;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE);
        nightMode = sharedPreferences.getBoolean("nightMode", false);

        setContentView(binding.getRoot());

        if (nightMode) {
            binding.switchMode.setChecked(true);
        }
        binding.switchMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nightMode) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    editor = sharedPreferences.edit();
                    editor.putBoolean("nightMode", false);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    editor = sharedPreferences.edit();
                    editor.putBoolean("nightMode", true);
                }
                editor.apply();
            }
        });

        binding.imageView6.setOnClickListener(v -> finish());
        binding.profileText.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, UpdateUserDataActivity.class));
        });
        binding.statisticsText.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, StatisticsActivity.class));
        });
    }
}