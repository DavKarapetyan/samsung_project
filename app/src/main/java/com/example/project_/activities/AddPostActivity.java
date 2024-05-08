package com.example.project_.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.project_.R;
import com.example.project_.databinding.ActivityAddPostBinding;
import com.example.project_.databinding.ActivityRegisterBinding;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class AddPostActivity extends BaseActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String TAG = "AddPostActivity";
    private ActivityAddPostBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore firebaseFirestore;
    private StorageReference storageReference;
    private List<Uri> imageUris;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddPostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        firebaseFirestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("uploads");

        binding.uploadImage1.setOnClickListener(v -> openFileChooser());
        binding.save.setOnClickListener(v -> addPost());
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Allow multiple image selection
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // Check if there are any activities available to handle the intent
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        } else {
            //Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
            MotionToast.Companion.createColorToast(this, "Error", "No app available to pick images", MotionToastStyle.ERROR, MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION, ResourcesCompat.getFont(getApplicationContext(), R.font.nunito_regular));

        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
//            imageUri = data.getData();
//            Picasso.get().load(imageUri).into(binding.uploadedImage);
            imageUris = new ArrayList<>();
            if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri uri = clipData.getItemAt(i).getUri();
                    imageUris.add(uri);
                }
            } else if(data.getData() != null) {
                Uri uri = data.getData();
                imageUris.add(uri);
            }
            int i = 0;
            for (Uri uri : imageUris) {
                if (imageUris.size() > 4) {
                    //Toast.makeText(AddPostActivity.this, "Cant show more than 4 images", Toast.LENGTH_SHORT).show();
                    MotionToast.Companion.createColorToast(this, "Error", "Cant show more than 4 images", MotionToastStyle.ERROR, MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION, ResourcesCompat.getFont(getApplicationContext(), R.font.nunito_regular));
                }
                if (i == 0) {
                    Picasso.get().load(uri).into(binding.uploadedImage2);
                    binding.uploadedImage2.setVisibility(View.VISIBLE);
                } else if (i == 1) {
                    Picasso.get().load(uri).into(binding.uploadedImage3);
                    binding.uploadedImage3.setVisibility(View.VISIBLE);
                }else if (i == 2) {
                    Picasso.get().load(uri).into(binding.uploadedImage4);
                    binding.uploadedImage4.setVisibility(View.VISIBLE);
                }else if (i == 3) {
                    Picasso.get().load(uri).into(binding.uploadedImage5);
                    binding.uploadedImage5.setVisibility(View.VISIBLE);
                }
                i++;
            }
        }
    }

    private void addPost() {
        loading(true);
        if (imageUris != null && !imageUris.isEmpty()) {
            List<String> downloadUrls = new ArrayList<>(); // List to store download URLs of uploaded images

            // Counter to keep track of uploaded images
            AtomicInteger uploadCounter = new AtomicInteger(0);

            for (Uri imageUri : imageUris) {
                StorageReference fileReference = storageReference.child(System.currentTimeMillis() + ".jpg");

                fileReference.putFile(imageUri)
                        .addOnSuccessListener(taskSnapshot -> {
                            fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                                String downloadUrl = uri.toString();
                                downloadUrls.add(downloadUrl); // Add download URL to the list

                                // Check if all images are uploaded
                                if (uploadCounter.incrementAndGet() == imageUris.size()) {
                                    // All images uploaded, now save post details with multiple image URLs
                                    savePostDetails(downloadUrls);
                                }
                            });
                        })
                        .addOnFailureListener(e -> {
                            //Toast.makeText(AddPostActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                            MotionToast.Companion.createColorToast(this, "Error", "Failed to upload image", MotionToastStyle.ERROR, MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION, ResourcesCompat.getFont(getApplicationContext(), R.font.nunito_regular));
                            Log.e(TAG, "Failed to upload image", e);
                        });
            }
        } else {
            //Toast.makeText(this, "No images selected", Toast.LENGTH_SHORT).show();
            MotionToast.Companion.createColorToast(this, "Error", "No images selected", MotionToastStyle.ERROR, MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION, ResourcesCompat.getFont(getApplicationContext(), R.font.nunito_regular));
        }
    }
    private void savePostDetails(List<String> downloadUrls) {
        String content = binding.content.getText().toString();
        String hashTag = binding.hashTag.getText().toString();

        Map<String, Object> post = new HashMap<>();
        post.put("content", content);
        post.put("hashTag", hashTag);
        post.put("imageUrls", downloadUrls); // Save list of image download URLs
        post.put("userId", preferenceManager.getString(Constants.KEY_USER_ID));

        firebaseFirestore.collection("posts")
                .add(post)
                .addOnSuccessListener(documentReference -> {
                    //Toast.makeText(AddPostActivity.this, "Post added successfully", Toast.LENGTH_SHORT).show();
                    MotionToast.Companion.createColorToast(this, "Post is added", "Your post is added", MotionToastStyle.SUCCESS, MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION, ResourcesCompat.getFont(getApplicationContext(), R.font.nunito_regular));
                    loading(false);
                    finish();
                })
                .addOnFailureListener(e -> {
                    //Toast.makeText(AddPostActivity.this, "Failed to add post", Toast.LENGTH_SHORT).show();
                    MotionToast.Companion.createColorToast(this, "Error", "Failed to add post", MotionToastStyle.ERROR, MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION, ResourcesCompat.getFont(getApplicationContext(), R.font.nunito_regular));
                    loading(false);
                    Log.e(TAG, "Failed to add post", e);
                });
    }
    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.save.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.save.setVisibility(View.VISIBLE);
        }
    }
}