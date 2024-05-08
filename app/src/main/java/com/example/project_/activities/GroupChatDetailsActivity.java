package com.example.project_.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.project_.R;
import com.example.project_.adapters.UsersAdapter;
import com.example.project_.databinding.ActivityGroupChatDetailsBinding;
import com.example.project_.listeners.UserListener;
import com.example.project_.models.User;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GroupChatDetailsActivity extends BaseActivity {
    private ActivityGroupChatDetailsBinding binding;
    private PreferenceManager preferenceManager;
    private String groupChatId;
    private FirebaseFirestore firebaseFirestore;
    private List<User> users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupChatDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        firebaseFirestore = FirebaseFirestore.getInstance();
        users = new ArrayList<>();
        groupChatId = getIntent().getStringExtra("groupChatId");
        preferenceManager = new PreferenceManager(getApplicationContext());
        getGroupChatDetails();
    }

    private void getGroupChatDetails() {
        loading(true);
        firebaseFirestore.collection("groupChats").document(groupChatId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        List<String> imageUrls = (List<String>) documentSnapshot.get("imageUrls");
                        Picasso.get().load(imageUrls.get(0)).into(binding.groupChatImage);
                        binding.groupChatName.setText(documentSnapshot.getString("chatName"));
                        List<String> members = (List<String>) documentSnapshot.get("members");
                        int totalMembers = members.size();
                        AtomicInteger usersLoaded = new AtomicInteger(0);

                        for (String s : members) {
                            firebaseFirestore.collection("users").document(s).get()
                                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            User user = new User();
                                            user.name = documentSnapshot.getString(Constants.KEY_NAME);
                                            user.email = documentSnapshot.getString(Constants.KEY_EMAIL);
                                            user.image = documentSnapshot.getString(Constants.KEY_IMAGE);
                                            user.id = documentSnapshot.getId();
                                            users.add(user);

                                            int loadedCount = usersLoaded.incrementAndGet();
                                            if (loadedCount == totalMembers) {
                                                if (users.size() > 0) {
                                                    UsersAdapter usersAdapter = new UsersAdapter(users, new UserListener() {
                                                        @Override
                                                        public void onUserClicked(User user) {
                                                            Intent intent = new Intent(getApplicationContext(), UserProfileActivity.class);
                                                            intent.putExtra("userId", user.id);
                                                            startActivity(intent);
                                                        }
                                                    });
                                                    binding.usersRecyclerView.setAdapter(usersAdapter);
                                                    binding.usersRecyclerView.setVisibility(View.VISIBLE);
                                                    loading(false);
                                                }
                                            }
                                        }
                                    });
                        }
                    }
                });

    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }
}