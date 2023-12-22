package com.example.project_;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class ChatMainActivity extends AppCompatActivity {
    ImageView goBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_main);
        goBack = findViewById(R.id.goBack);
        goBack.setOnClickListener(view -> {
            finish();
        });
    }
}