package com.example.project_.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.project_.ChatFragment;
import com.example.project_.HomeFragment;
import com.example.project_.ProfileFragment;
import com.example.project_.R;
import com.example.project_.SearchFragment;
import com.example.project_.databinding.ActivityHomeBinding;
import com.example.project_.repository.MainRepository;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private MainRepository mainRepository;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());

        // Set default fragment
        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment());
        }

        mainRepository = MainRepository.getInstance();
        mainRepository.login(
                preferenceManager.getString(Constants.KEY_USER_ID), getApplicationContext(), () -> {
                    // On successful login
                    Toast.makeText(HomeActivity.this, "Login for call is successful", Toast.LENGTH_SHORT).show();
                }
        );

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            switch (item.getItemId()) {
                case R.id.home:
                    selectedFragment = new HomeFragment();
                    break;
                case R.id.chat:
                    selectedFragment = new ChatFragment();
                    break;
                case R.id.search:
                    selectedFragment = new SearchFragment();
                    break;
                case R.id.profile:
                    selectedFragment = new ProfileFragment();
                    break;
            }
            if (selectedFragment != null) {
                replaceFragment(selectedFragment);
            }
            return true;
        });

        binding.fab.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), AddPostActivity.class);
            startActivity(intent);
        });
    }

    private void replaceFragment(@NonNull Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.addToBackStack(null); // Optional, for back navigation
        fragmentTransaction.commit();
    }
}
