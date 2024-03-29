package com.example.project_.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.project_.R;
import com.example.project_.adapters.ChatAdapter;
import com.example.project_.adapters.GroupChatAdapter;
import com.example.project_.adapters.GroupChatMessageAdapter;
import com.example.project_.databinding.ActivityGroupChatMainBinding;
import com.example.project_.models.ChatMessage;
import com.example.project_.models.Comment;
import com.example.project_.models.GroupChatMessage;
import com.example.project_.models.TimeModel;
import com.example.project_.network.ApiClient;
import com.example.project_.network.ApiService;
import com.example.project_.repository.MainRepository;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupChatMainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    ActivityGroupChatMainBinding binding;
    String groupChatId;
    private Uri imageUri;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private StorageReference storageReference;
    private String currentTime1;
    private List<GroupChatMessage> groupChatMessages;
    private GroupChatMessageAdapter groupChatMessageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupChatMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        groupChatId = getIntent().getStringExtra("groupChatId");
        init();
        groupChatMessages = new ArrayList<>();
        groupChatMessageAdapter = new GroupChatMessageAdapter(groupChatMessages, preferenceManager);
        binding.chatRecyclerView.setAdapter(groupChatMessageAdapter);
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> sendMessage());
        binding.uploadImage.setOnClickListener(v -> openFileChooser());
        retrieveGroupChatMessages();
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
            Toast.makeText(this, "No app available to pick images", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Picasso.get().load(imageUri).into(binding.image);
            binding.image.setVisibility(View.VISIBLE);
            binding.viewImage.setVisibility(View.VISIBLE);
        }
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        database = FirebaseFirestore.getInstance();
    }

    private void sendMessage() {
        HashMap<String, Object> message = new HashMap<>();
        //new NtpTask().execute();
        ApiService apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
        Call<TimeModel> timeModelCall = apiService.getTime();
        timeModelCall.enqueue(new Callback<TimeModel>() {
            @Override
            public void onResponse(Call<TimeModel> call, Response<TimeModel> response) {
                currentTime1 = response.body().getDateTime();
                if (currentTime1 != null) {
                    String messageText = binding.inputMessage.getText().toString();
                    if (imageUri != null) {
                        StorageReference fileReference = storageReference.child(System.currentTimeMillis() + ".jpg");

                        fileReference.putFile(imageUri)
                                .addOnSuccessListener(taskSnapshot -> {
                                    fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                                        //add message
                                        message.put("content", messageText);
                                        message.put("senderId", preferenceManager.getString(Constants.KEY_USER_ID));
                                        message.put(Constants.KEY_TIMESTAMP, currentTime1);
                                        message.put(Constants.KEY_SEND_IMAGE, uri.toString());

                                        database.collection("groupChats").document(groupChatId).collection("messages").add(message);
                                    });
                                });
                    } else {
                        // add message without image
                        message.put("content", messageText);
                        message.put("senderId", preferenceManager.getString(Constants.KEY_USER_ID));
                        message.put(Constants.KEY_TIMESTAMP, currentTime1);

                        database.collection("groupChats").document(groupChatId).collection("messages").add(message);
                    }
                    imageUri = null;
                    binding.image.setVisibility(View.GONE);
                    binding.viewImage.setVisibility(View.GONE);
                    binding.chatRecyclerView.smoothScrollToPosition(groupChatMessages.size());
                    binding.inputMessage.setText(null);

                } else {
                    Toast.makeText(getApplicationContext(), "Date is null", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TimeModel> call, Throwable t) {
                String a = t.getMessage();
                Toast.makeText(getApplicationContext(), "Cant get date" + a, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void retrieveGroupChatMessages() {
        binding.chatRecyclerView.setVisibility(View.GONE);
        database.collection("groupChats").document(groupChatId).collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e("RetrieveMessages", "Error fetching messages", error);
                            return;
                        }
                        if (value != null) {
                            int count = groupChatMessages.size();
                            for (DocumentChange documentChange : value.getDocumentChanges()) {
                                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                                    GroupChatMessage groupChatMessage = new GroupChatMessage();
                                    groupChatMessage.senderId = documentChange.getDocument().getString("senderId");
                                    groupChatMessage.content = documentChange.getDocument().getString("content");
                                    groupChatMessage.timeStamp = documentChange.getDocument().getString("timestamp");
                                    groupChatMessage.senderImage = "";
                                    groupChatMessages.add(groupChatMessage);
                                }
                            }
                            Collections.sort(groupChatMessages, new Comparator<GroupChatMessage>() {
                                @Override
                                public int compare(GroupChatMessage e1, GroupChatMessage e2) {
                                    return e1.timeStamp.compareTo(e2.timeStamp);
                                }
                            });
                            if (count == 0) {
                                groupChatMessageAdapter.notifyDataSetChanged();
                            } else {
                                groupChatMessageAdapter.notifyItemRangeInserted(count, groupChatMessages.size() - count);
                                if (isRecyclerViewAtBottom()) {
                                    binding.chatRecyclerView.smoothScrollToPosition(groupChatMessages.size());
                                }
                            }
                            binding.chatRecyclerView.setVisibility(View.VISIBLE);
                        }
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });
    }
    private boolean isRecyclerViewAtBottom() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) binding.chatRecyclerView.getLayoutManager();
        int lastVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition();
        int itemCount = groupChatMessages.size();
        return lastVisibleItemPosition == itemCount - 1;
    }
}