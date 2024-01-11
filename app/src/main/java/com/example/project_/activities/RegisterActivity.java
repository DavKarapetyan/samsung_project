package com.example.project_.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.project_.R;
import com.example.project_.databinding.ActivityRegisterBinding;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private PreferenceManager preferenceManager;
    private String encodedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
    }
    private void setListeners()
    {
        binding.signIn.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        binding.signUp.setOnClickListener(v -> {
            if(isValidSignUpDetails()) {
                signUp();
            }
        });
        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void signUp() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_NAME, binding.fullName.getText().toString());
        user.put(Constants.KEY_NICKNAME, binding.nickName.getText().toString());
        user.put(Constants.KEY_PHONENUMBER, binding.phoneNumber.getText().toString());
        user.put(Constants.KEY_EMAIL, binding.email.getText().toString());
        user.put(Constants.KEY_PASSWORD, binding.password.getText().toString());
        user.put(Constants.KEY_IMAGE, encodedImage);
        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    loading(false);
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(Constants.KEY_NAME, binding.fullName.getText().toString());
                    preferenceManager.putString(Constants.KEY_NICKNAME, binding.nickName.getText().toString());
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                    Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(exception -> {
                    loading(false);
                    showToast(exception.getMessage());
                });

    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK) {
                    if(result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            binding.textAddImage.setVisibility(View.GONE);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private Boolean isValidSignUpDetails() {
        if(encodedImage == null) {
            showToast("Select profile image");
            return false;
        } else if (binding.fullName.getText().toString().trim().isEmpty()) {
            showToast("Enter name");
            return false;
        } else if (binding.nickName.getText().toString().trim().isEmpty()) {
            showToast("Enter nick name");
            return false;
        } else if (binding.email.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.email.getText().toString()).matches()) {
            showToast("Enter valid email");
            return false;
        } else if (binding.phoneNumber.getText().toString().trim().isEmpty()) {
            showToast("Enter phone number");
            return false;
        } else if (binding.password.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        } else if (binding.passwordConfirm.getText().toString().trim().isEmpty()) {
            showToast("Confirm your password");
            return false;
        } else if (!binding.password.getText().toString().equals(binding.passwordConfirm.getText().toString())) {
            showToast("Password & confirm password must be same");
            return false;
        } else {
            return true;
        }
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.signUp.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.signUp.setVisibility(View.VISIBLE);
        }
    }
}