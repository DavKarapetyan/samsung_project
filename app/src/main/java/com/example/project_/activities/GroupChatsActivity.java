package com.example.project_.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.project_.R;
import com.example.project_.adapters.GroupChatAdapter;
import com.example.project_.databinding.ActivityGroupChatsBinding;
import com.example.project_.models.GroupChat;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class GroupChatsActivity extends AppCompatActivity {
    ActivityGroupChatsBinding binding;
    private GroupChatAdapter groupChatAdapter;
    private List<GroupChat> groupChats;
    private PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupChatsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        groupChats = new ArrayList<>();
        groupChatAdapter = new GroupChatAdapter(groupChats, getApplicationContext());
        binding.groupChatsRecyclerView.setAdapter(groupChatAdapter);
        retrieveGroupChats();
        binding.groupChatsRecyclerView.setVisibility(View.VISIBLE);
    }
    private void retrieveGroupChats() {
        FirebaseFirestore.getInstance().collection("groupChats")
                //.whereEqualTo("creatorId", preferenceManager.getString(Constants.KEY_USER_ID))
                .whereArrayContains("members", preferenceManager.getString(Constants.KEY_USER_ID))
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().size() > 0) {
                                groupChats.clear();
                                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                    GroupChat groupChat = new GroupChat();
                                    groupChat.chatName = documentSnapshot.getString("chatName");
                                    groupChat.creatorId = documentSnapshot.getString("creatorId");
                                    groupChat.imageUrls = (List<String>) documentSnapshot.get("imageUrls");
                                    groupChat.members = (List<String>) documentSnapshot.get("members");
                                    groupChat.id = documentSnapshot.getId();
                                    groupChats.add(groupChat);
                                }
                                groupChatAdapter.notifyDataSetChanged();
                                binding.progressBar.setVisibility(View.INVISIBLE);
                            } else {
                                Toast.makeText(getApplicationContext(), "No Chats", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }
}