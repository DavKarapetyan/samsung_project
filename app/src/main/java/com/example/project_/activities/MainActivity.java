package com.example.project_.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.example.project_.R;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;

public class MainActivity extends AppCompatActivity {
    Button start__btn;
    PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start__btn = findViewById(R.id.start);
        start__btn.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
        preferenceManager = new PreferenceManager(getApplicationContext());
        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
            startActivity(intent);
            finish();
        }
    }
}