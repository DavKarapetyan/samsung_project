package com.example.project_.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.codebyashish.autoimageslider.Enums.ImageScaleType;
import com.codebyashish.autoimageslider.ExceptionsClass;
import com.codebyashish.autoimageslider.Models.ImageSlidesModel;
import com.example.project_.CommentListDialogFragment;
import com.example.project_.R;
import com.example.project_.databinding.ActivityPostDetailsBinding;
import com.example.project_.models.Post;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class PostDetailsActivity extends BaseActivity {
    ActivityPostDetailsBinding binding;
    Post post;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        retrievePosts(getIntent().getStringExtra("postId"));
        if (!getIntent().getBooleanExtra("editable", true)) {
            binding.save.setVisibility(View.GONE);
            binding.imageView5.setVisibility(View.GONE);
            binding.content.setEnabled(false);
        }
        binding.save.setOnClickListener(v -> {
            updatePost(getIntent().getStringExtra("postId"));
        });
        binding.imageView5.setOnClickListener(v -> {
            deletePost(getIntent().getStringExtra("postId"));
        });
    }

    private void retrievePosts(String postId) {
        FirebaseFirestore.getInstance().collection("posts").document(postId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        post = createPostFromDocument(documentSnapshot);
                        post.id = documentSnapshot.getId();
                        if (post.imageUris != null && !post.imageUris.isEmpty()) {
                            //Picasso.get().load(post.imageUris.get(0)).into(binding.postImage);
                            ArrayList<ImageSlidesModel> autoImageList = new ArrayList<>();
                            for (String str : post.imageUris) {
                                autoImageList.add(new ImageSlidesModel(str, ImageScaleType.CENTER_CROP));
                            }
                            binding.postImages.setImageList(autoImageList);
                            binding.postImages.setDefaultAnimation();
                        }
                        binding.content.setText(post.content);
                        binding.showComments.setOnClickListener(v -> {
                            CommentListDialogFragment commentListDialogFragment = CommentListDialogFragment.newInstance(post.id);
                            commentListDialogFragment.show(getSupportFragmentManager(), "TAG");
                        });
                    }
                });
    }

    private Bitmap getUserImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private Post createPostFromDocument(DocumentSnapshot documentSnapshot) {
        Post post = new Post();
        post.hashTag = documentSnapshot.getString("hashTag");
        post.content = documentSnapshot.getString("content");
        // Get the imageUris field from the DocumentSnapshot
        List<String> imageUris = (List<String>) documentSnapshot.get("imageUrls");
        if (imageUris != null) {
            post.imageUris = imageUris;
        }

        // Get user details using userId
        String userId = documentSnapshot.getString("userId");
        retrieveUserDetails(userId, post);

        return post;
    }

    private void retrieveUserDetails(String userId, Post post) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User document exists, retrieve user details
                        String userName = documentSnapshot.getString("name");
                        String userImage = documentSnapshot.getString("image");

                        // Set user details to post object
                        post.userName = userName;
                        post.userImage = userImage;
                        binding.fullName.setText(post.userName);
                        if (post.userImage != null) {
                            binding.profileImage.setImageBitmap(getUserImage(post.userImage));
                        }
                    } else {
                        Log.e("Home", "User document does not exist for userId: " + userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Home", "Failed to retrieve user details for userId: " + userId + ", Error: " + e.getMessage());
                });
    }
    private void updatePost(String postId) {
        loading(true);
        DocumentReference postRef = FirebaseFirestore.getInstance().collection("posts").document(postId);
        postRef.update("content", binding.content.getText().toString());
        loading(false);
        //Toast.makeText(PostDetailsActivity.this, "Post data is updated", Toast.LENGTH_SHORT).show();
        MotionToast.Companion.createColorToast(this, "Post update", "Post data is successfully updated", MotionToastStyle.SUCCESS, MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION, ResourcesCompat.getFont(getApplicationContext(), R.font.nunito_regular));
    }
    private void deletePost(String postId) {
        loading(true);
        FirebaseFirestore.getInstance().collection("posts").document(postId).delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //Toast.makeText(PostDetailsActivity.this, "Post is deleted", Toast.LENGTH_SHORT).show();
                        MotionToast.Companion.createColorToast(PostDetailsActivity.this, "Delete", "Post is successfully deleted", MotionToastStyle.DELETE, MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION, ResourcesCompat.getFont(getApplicationContext(), R.font.nunito_regular));
                        loading(false);
                        finish();
                    }
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