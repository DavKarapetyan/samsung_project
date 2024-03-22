package com.example.project_.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.project_.R;
import com.example.project_.databinding.ActivityEmailConfirmationBinding;
import com.google.android.gms.auth.api.signin.SignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

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
            FirebaseAuth.getInstance().signInWithEmailAndPassword(intent.getStringExtra("email"), intent.getStringExtra("password"))
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                if (FirebaseAuth.getInstance().getCurrentUser().isEmailVerified()) {
                                    Intent intent1 = new Intent(getApplicationContext(), HomeActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent1);
                                } else {
                                    MotionToast.Companion.createColorToast(EmailConfirmationActivity.this, "Error", "Email is not verified", MotionToastStyle.ERROR, MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION, ResourcesCompat.getFont(getApplicationContext(), R.font.nunito_regular));
                                }
                            }
                        }
                    });
            //Toast.makeText(EmailConfirmationActivity.this, "Email is not verified", Toast.LENGTH_SHORT).show();
        });
    }
}