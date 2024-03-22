package com.example.project_.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DividerItemDecoration;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.project_.FollowListDialogFragment;
import com.example.project_.R;
import com.example.project_.adapters.ProfilePostsAdapter;
import com.example.project_.databinding.ActivityUserProfileBinding;
import com.example.project_.models.Post;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class UserProfileActivity extends AppCompatActivity {
    ActivityUserProfileBinding binding;
    FirebaseFirestore firebaseFirestore;
    String userId;
    ProfilePostsAdapter profilePostsAdapter;
    PreferenceManager preferenceManager;
    private List<Post> posts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        firebaseFirestore = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra("userId");
        preferenceManager = new PreferenceManager(getApplicationContext());
        binding.fab.setOnClickListener(v -> finish());
        loadUserDetails();
        posts = new ArrayList<>();
        profilePostsAdapter = new ProfilePostsAdapter(posts, new PreferenceManager(getApplicationContext()), getApplicationContext());
        binding.profilePostsRecyclerView.setAdapter(profilePostsAdapter);

        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(getApplicationContext());
        layoutManager.setFlexWrap(FlexWrap.WRAP);
        retrievePosts();
        binding.profilePostsRecyclerView.setLayoutManager(layoutManager);
        binding.profilePostsRecyclerView.addItemDecoration(new DividerItemDecoration(binding.profilePostsRecyclerView.getContext(), DividerItemDecoration.HORIZONTAL));
        binding.profilePostsRecyclerView.setVisibility(View.VISIBLE);
        binding.follow.setOnClickListener(v -> {
            follow();
        });
        binding.unFollow.setOnClickListener(v -> {
            unFollow();
        });
        binding.followings.setOnClickListener(v -> {
            FollowListDialogFragment followListDialogFragment = FollowListDialogFragment.newInstance("followings", userId);
            followListDialogFragment.show(getSupportFragmentManager(), "TAG");
        });
        binding.followers.setOnClickListener(v -> {
            FollowListDialogFragment followListDialogFragment = FollowListDialogFragment.newInstance("followers", userId);
            followListDialogFragment.show(getSupportFragmentManager(), "TAG");
        });
    }

    private void retrievePosts() {
        FirebaseFirestore.getInstance().collection("posts").whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    posts.clear(); // Clear previous posts
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Post post = createPostFromDocument(documentSnapshot);
                        post.id = documentSnapshot.getId();
                        post.userId = userId;
                        posts.add(post);
                    }
                    binding.postsCount.setText(Integer.toString(posts.size()));
                    profilePostsAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("Home", "Failed to retrieve posts: " + e.getMessage());
                });
    }

    private Post createPostFromDocument(QueryDocumentSnapshot documentSnapshot) {
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
        firebaseFirestore.collection("users").document(userId)
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
                        profilePostsAdapter.notifyDataSetChanged();
                    } else {
                        Log.e("Home", "User document does not exist for userId: " + userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Home", "Failed to retrieve user details for userId: " + userId + ", Error: " + e.getMessage());
                });
    }

    private void loadUserDetails() {
//        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
//        binding.textNickName.setText(preferenceManager.getString(Constants.KEY_NICKNAME));
//        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
//        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//        binding.imageProfile.setImageBitmap(bitmap);
        firebaseFirestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        binding.textName.setText(documentSnapshot.getString("name"));
                        binding.textNickName.setText(documentSnapshot.getString("nickName"));
                        byte[] bytes = Base64.decode(documentSnapshot.getString("image"), Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        binding.imageProfile.setImageBitmap(bitmap);
                    }
                });
        firebaseFirestore.collection("users").document(preferenceManager.getString(Constants.KEY_USER_ID)).collection("followings").whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            binding.follow.setVisibility(View.GONE);
                            binding.unFollow.setVisibility(View.VISIBLE);
                        } else {
                            binding.follow.setVisibility(View.VISIBLE);
                            binding.unFollow.setVisibility(View.GONE);
                        }
                    }
                });
        firebaseFirestore.collection("users").document(userId).collection("followers").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        binding.followersCount.setText(Integer.toString(queryDocumentSnapshots.size()));
                    }
                });
        firebaseFirestore.collection("users").document(userId).collection("followings").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        binding.followingsCount.setText(Integer.toString(queryDocumentSnapshots.size()));
                    }
                });
    }

    private void follow() {
        DocumentReference currentUserRef = firebaseFirestore.collection("users").document(preferenceManager.getString(Constants.KEY_USER_ID));
        DocumentReference userToFollowRef = firebaseFirestore.collection("users").document(userId);
        HashMap<String, Object> followerId = new HashMap<>();
        followerId.put("userId", preferenceManager.getString(Constants.KEY_USER_ID));
        HashMap<String, Object> followingId = new HashMap<>();
        followingId.put("userId", userId);

        userToFollowRef.collection("followers").add(followerId)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        //Toast.makeText(UserProfileActivity.this, "You are followed", Toast.LENGTH_SHORT).show();
                        MotionToast.Companion.createColorToast(UserProfileActivity.this, "Success", "You are followed", MotionToastStyle.SUCCESS, MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION, ResourcesCompat.getFont(getApplicationContext(), R.font.nunito_regular));
                    }
                });
        currentUserRef.collection("followings").add(followingId)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Toast.makeText(UserProfileActivity.this, "You are in my followings", Toast.LENGTH_SHORT).show();
                    }
                });
        loadUserDetails();
        binding.follow.setVisibility(View.GONE);
        binding.unFollow.setVisibility(View.VISIBLE);
    }
    private void unFollow()  {
        DocumentReference currentUserRef = firebaseFirestore.collection("users").document(preferenceManager.getString(Constants.KEY_USER_ID));
        DocumentReference userToFollowRef = firebaseFirestore.collection("users").document(userId);

        userToFollowRef.collection("followers").whereEqualTo("userId", preferenceManager.getString(Constants.KEY_USER_ID))
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            userToFollowRef.collection("followers").document(documentSnapshot.getId()).delete();
                            Toast.makeText(UserProfileActivity.this, "You are deleted from followers", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        currentUserRef.collection("followings").whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            currentUserRef.collection("followings").document(documentSnapshot.getId()).delete();
                            Toast.makeText(UserProfileActivity.this, "User is deleted from your followings", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        loadUserDetails();
        binding.follow.setVisibility(View.VISIBLE);
        binding.unFollow.setVisibility(View.GONE);
    }
}