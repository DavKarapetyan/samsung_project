package com.example.project_.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;

import com.example.project_.ChatFragment;
import com.example.project_.HomeFragment;
import com.example.project_.NotificationsFragment;
import com.example.project_.ProfileFragment;
import com.example.project_.R;
import com.example.project_.databinding.ActivityHomeBinding;

public class HomeActivity extends BaseActivity {

    ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        replaceFragment(new HomeFragment());

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.home:
                    replaceFragment(new HomeFragment());
                    break;
                case R.id.chat:
                    replaceFragment(new ChatFragment());
                    break;
                case R.id.notifications:
                    replaceFragment(new NotificationsFragment());
                    break;
                case R.id.profile:
                    replaceFragment(new ProfileFragment());
                    break;
            }

            return true;
        });
    }
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }
}