package com.example.project_.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.project_.ChatFragment;
import com.example.project_.HomeFragment;
import com.example.project_.NotificationsFragment;
import com.example.project_.ProfileFragment;
import com.example.project_.R;
import com.example.project_.databinding.ActivityHomeBinding;
import com.example.project_.repository.MainRepository;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.DataModelType;
import com.example.project_.utilities.PreferenceManager;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends BaseActivity {

    ActivityHomeBinding binding;
    private MainRepository mainRepository;
    private PreferenceManager preferenceManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        replaceFragment(new HomeFragment());
        preferenceManager = new PreferenceManager(getApplicationContext());
        BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);

        MaterialShapeDrawable bottomBarBackground = (MaterialShapeDrawable) bottomAppBar.getBackground();
        bottomBarBackground.setShapeAppearanceModel(
                bottomBarBackground.getShapeAppearanceModel()
                        .toBuilder()
                        .setTopRightCorner(CornerFamily.ROUNDED,50)
                        .setTopLeftCorner(CornerFamily.ROUNDED,50)
                        .build());
        mainRepository = MainRepository.getInstance();
        mainRepository.login(
                preferenceManager.getString(Constants.KEY_USER_ID), getApplicationContext(), () -> {
                    //if success then we want to move to call activity
                    Toast.makeText(HomeActivity.this, "Login for call is successfull", Toast.LENGTH_SHORT).show();
                }
        );
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
        binding.fab.setOnClickListener(v -> {
            //addPost(post);
            Intent intent = new Intent(getApplicationContext(), AddPostActivity.class);
            startActivity(intent);
        });
    }
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }
}