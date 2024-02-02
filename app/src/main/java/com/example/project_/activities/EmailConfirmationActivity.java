package com.example.project_.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.example.project_.R;
import com.example.project_.databinding.ActivityEmailConfirmationBinding;
import com.google.android.gms.auth.api.signin.SignInAccount;

public class EmailConfirmationActivity extends AppCompatActivity {

    private ActivityEmailConfirmationBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmailConfirmationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Intent intent = getIntent();
        binding.textView.setText("Dear " + intent.getStringExtra("name") + "\n" +
                "Thank you for signing up with us!\n To complete your registration, please go to your email address and confirm link that we send to you. After confirmation you can go to your account\n" +
                "This step ensures that the provided email address is accurate and belongs to you. \n" +
                "Thank you for choosing us!");

        binding.continues.setOnClickListener(v -> {
            Intent intent1 = new Intent(getApplicationContext(), HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent1);
        });
    }
}