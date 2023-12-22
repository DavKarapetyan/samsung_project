package com.example.project_;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class RegisterActivity extends AppCompatActivity {
    Button sign_up;
    TextView sign_in;
    FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        sign_up = findViewById(R.id.sign_up);
        sign_in = findViewById(R.id.sign_in);
        fab = findViewById(R.id.fab);
        sign_up.setOnClickListener(view -> {
            Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
            startActivity(intent);
        });
        sign_in.setOnClickListener(view -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });
        fab.setOnClickListener(view -> {
            finish();
        });
    }
}