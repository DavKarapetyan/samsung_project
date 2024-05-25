package com.example.project_.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.project_.R;
import com.example.project_.databinding.ActivitySettingsBinding;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class SettingsActivity extends BaseActivity {
    ActivitySettingsBinding binding;
    PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        preferenceManager = new PreferenceManager(getApplicationContext());

        setContentView(binding.getRoot());


        binding.imageView6.setOnClickListener(v -> finish());
        binding.profileText.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, UpdateUserDataActivity.class));
        });
        binding.statisticsText.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, StatisticsActivity.class));
        });
        binding.savedPostsText.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, SavedPostsActivity.class));
        });
        binding.logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
            }
        });
    }
    private void signOut() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                })
                .addOnFailureListener(e -> Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show());
    }
}