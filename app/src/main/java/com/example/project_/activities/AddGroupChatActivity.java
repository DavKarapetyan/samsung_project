package com.example.project_.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;

import android.content.ClipData;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.project_.R;
import com.example.project_.databinding.ActivityAddGroupChatBinding;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
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

public class AddGroupChatActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String TAG = "AddGroupChatActivity";
    ActivityAddGroupChatBinding binding;
    FirebaseFirestore firebaseFirestore;
    PreferenceManager preferenceManager;
    StorageReference storageReference;
    List<Uri> imageUris;
    List<String> members;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddGroupChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        firebaseFirestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("uploads");
        members = new ArrayList<>();
        members.add(preferenceManager.getString(Constants.KEY_USER_ID));
        binding.fab.setOnClickListener(v -> {
            firebaseFirestore.collection("users").whereEqualTo("nickName", binding.member.getText().toString().trim())
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                if (task.getResult().size() > 0) {
                                    for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                                        String nickname = queryDocumentSnapshot.getString("nickName");

                                        // Create a new Chip
                                        Chip chip = new Chip(AddGroupChatActivity.this);
                                        chip.setText(nickname);
                                        chip.setTextSize(14);
                                        chip.setTextColor(getResources().getColor(R.color.primary__color));
                                        chip.setChipStrokeColorResource(R.color.primary__color);
                                        chip.setCloseIconVisible(true);
                                        chip.setCloseIconResource(R.drawable.delete);
                                        chip.setCloseIconTint(ColorStateList.valueOf(getColor(R.color.primary__color)));

                                        // Set OnClickListener to delete the Chip
                                        chip.setOnCloseIconClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                binding.members.removeView(chip); // Remove the Chip from the layout
                                                members.remove(nickname); // Remove the member from the list
                                            }
                                        });

                                        // Add the Chip to the layout
                                        binding.members.addView(chip);
                                        binding.member.setText(null);
                                        members.add(queryDocumentSnapshot.getId());
                                    }
                                } else {
                                    Toast.makeText(getApplicationContext(), "No user with this nickName", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(getApplicationContext(), "Failed to fetch user with this nickName", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });
        binding.uploadImage1.setOnClickListener(v -> openFileChooser());
        binding.save.setOnClickListener(v -> addGroupChat());
    }


    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        } else {
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

    private void addGroupChat() {
        loading(true);
        if (imageUris != null && !imageUris.isEmpty()) {
            List<String> downloadUrls = new ArrayList<>();

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
                                    saveGroupChatDetails(downloadUrls);
                                }
                            });
                        })
                        .addOnFailureListener(e -> {
                            MotionToast.Companion.createColorToast(this, "Error", "Failed to upload image", MotionToastStyle.ERROR, MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION, ResourcesCompat.getFont(getApplicationContext(), R.font.nunito_regular));
                            Log.e(TAG, "Failed to upload image", e);
                        });
            }
        }
    }
    private void saveGroupChatDetails(List<String> downloadUrls) {
        String chatName = binding.chatName.getText().toString();
        Map<String, Object> groupChat = new HashMap<>();
        groupChat.put("chatName", chatName);
        groupChat.put("members", members);
        groupChat.put("imageUrls", downloadUrls);
        groupChat.put("creatorId", preferenceManager.getString(Constants.KEY_USER_ID));

        firebaseFirestore.collection("groupChats")
                .add(groupChat)
                .addOnSuccessListener(documentReference -> {
                    //Toast.makeText(AddPostActivity.this, "Post added successfully", Toast.LENGTH_SHORT).show();
                    MotionToast.Companion.createColorToast(this, "Group chat is added", "Your group chat is added", MotionToastStyle.SUCCESS, MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION, ResourcesCompat.getFont(getApplicationContext(), R.font.nunito_regular));
                    loading(false);
                    finish();
                })
                .addOnFailureListener(e -> {
                    //Toast.makeText(AddPostActivity.this, "Failed to add post", Toast.LENGTH_SHORT).show();
                    MotionToast.Companion.createColorToast(this, "Error", "Failed to add group chat", MotionToastStyle.ERROR, MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION, ResourcesCompat.getFont(getApplicationContext(), R.font.nunito_regular));
                    loading(false);
                    Log.e(TAG, "Failed to add group chat", e);
                });    }
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
