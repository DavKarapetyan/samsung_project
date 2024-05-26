package com.example.project_.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.project_.R;
import com.example.project_.adapters.PostAdapter;
import com.example.project_.databinding.ActivitySavedPostsBinding;
import com.example.project_.models.Post;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class SavedPostsActivity extends AppCompatActivity {

    private ActivitySavedPostsBinding binding;
    private FirebaseFirestore firebaseFirestore;
    private PreferenceManager preferenceManager;
    private PostAdapter postAdapter;
    private List<Post> posts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySavedPostsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        firebaseFirestore = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(getApplicationContext());
        posts = new ArrayList<>();
        postAdapter = new PostAdapter(posts, getSupportFragmentManager(), preferenceManager, getApplicationContext());
        binding.postsRecyclerView.setAdapter(postAdapter);
        binding.imageView6.setOnClickListener(v -> finish());
        getPosts();
        if (posts.isEmpty()) {
            binding.headerLottie.setVisibility(View.VISIBLE);
            binding.postsRecyclerView.setVisibility(View.GONE);
        }
    }

    private void getPosts()
    {
        firebaseFirestore.collection("users").document(preferenceManager.getString(Constants.KEY_USER_ID))
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        List<String> postsIds = (List<String>) documentSnapshot.get("savedPosts");
                        if (postsIds != null) {
                            for (String postId : postsIds) {
                                firebaseFirestore.collection("posts").document(postId).get()
                                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(DocumentSnapshot documentSnapshot1) {
                                                Post post = createPostFromDocument(documentSnapshot1);
                                                post.id = documentSnapshot1.getId();
                                                post.userId = documentSnapshot1.getString("userId");
                                                posts.add(post);
                                                binding.postsRecyclerView.setVisibility(View.VISIBLE);
                                                binding.headerLottie.setVisibility(View.GONE);
                                            }
                                        });
                                postAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
    }
    private Post createPostFromDocument(DocumentSnapshot documentSnapshot) {
        Post post = new Post();
        post.hashTag = documentSnapshot.getString("hashTag");
        post.content = documentSnapshot.getString("content");
        List<String> imageUris = (List<String>) documentSnapshot.get("imageUrls");
        if (imageUris != null) {
            post.imageUris = imageUris;
        }
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

                        // Notify adapter about changes
                        postAdapter.notifyDataSetChanged();
                    } else {
                        Log.e("Home", "User document does not exist for userId: " + userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Home", "Failed to retrieve user details for userId: " + userId + ", Error: " + e.getMessage());
                });
    }
}