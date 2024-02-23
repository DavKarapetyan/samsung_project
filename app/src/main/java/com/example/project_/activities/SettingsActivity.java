package com.example.project_.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.example.project_.R;
import com.example.project_.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {
    ActivitySettingsBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.profileText.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, UpdateUserDataActivity.class));
        });
    }
}