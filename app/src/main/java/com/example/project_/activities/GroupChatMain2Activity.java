package com.example.project_.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.example.project_.R;
import com.example.project_.adapters.ChatAdapter;
import com.example.project_.databinding.ActivityChatBinding;
import com.example.project_.databinding.ActivityGroupChatMain2Binding;
import com.example.project_.models.ChatMessage;
import com.example.project_.models.TimeModel;
import com.example.project_.models.User;
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
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupChatMain2Activity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private ActivityGroupChatMain2Binding binding;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String groupChataId = null;
    private Boolean isReceiverAvailable = false;
    private static final String NTP_SERVER = "pool.ntp.org";
    private StorageReference storageReference;
    private Uri imageUri;
    private MainRepository mainRepository;
    private String currentTime1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupChatMain2Binding.inflate(getLayoutInflater());
        storageReference = FirebaseStorage.getInstance().getReference("uploadsChat");
        setContentView(binding.getRoot());
        groupChataId = getIntent().getStringExtra("groupChatId");
        setListeners();
        init();
        loadGroupChatDetails();
        listenMessages();
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
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                getBitmapFromEncodedString(""),
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }
    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> sendMessage());
        binding.uploadImage.setOnClickListener(v -> openFileChooser());
        binding.textName.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), GroupChatDetailsActivity.class);
            intent.putExtra("groupChatId", groupChataId);
            startActivity(intent);
        });
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
                    if(imageUri != null) {
                        StorageReference fileReference = storageReference.child(System.currentTimeMillis() + ".jpg");

                        fileReference.putFile(imageUri)
                                .addOnSuccessListener(taskSnapshot -> {
                                    fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                                        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                                        message.put(Constants.KEY_RECEIVER_ID, "");
                                        message.put("groupChatId", groupChataId);
                                        message.put(Constants.KEY_TIMESTAMP, currentTime1);
                                        message.put(Constants.KEY_SEND_IMAGE, uri.toString());

                                        // Add message text only after image upload is completed
                                        message.put(Constants.KEY_MESSAGE, messageText);

                                        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
                                    });
                                });
                    } else {
                        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                        message.put(Constants.KEY_RECEIVER_ID, "");
                        message.put("groupChatId", groupChataId);
                        message.put(Constants.KEY_TIMESTAMP, currentTime1);
                        // Add message text directly if no image is selected
                        message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());

                        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
                    }
                    imageUri = null;
                    binding.image.setVisibility(View.GONE);
                    binding.viewImage.setVisibility(View.GONE);
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
    private void listenMessages() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo("groupChatId", groupChataId)
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.groupChatId = documentChange.getDocument().getString("groupChatId");
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.image = documentChange.getDocument().getString(Constants.KEY_SEND_IMAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getString(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getString(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }
//            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            chatMessages.sort(new Comparator<ChatMessage>() {
                @Override
                public int compare(ChatMessage e1, ChatMessage e2) {
                    return e1.dateObject.compareTo(e2.dateObject);
                }
            });
            if (count == 0) {
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
    };

    private String getReadableDateTime(String timestamp) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        try {
            // Parse the timestamp
            Date date = inputFormat.parse(timestamp);

            // Format the parsed date
            String formattedDate = outputFormat.format(date);

            // Output the formatted date
            return formattedDate;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
    private Bitmap getBitmapFromEncodedString(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }
    }
    private void loadGroupChatDetails() {
        database.collection("groupChats").document(groupChataId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        List<String> imageUrls = (List<String>) documentSnapshot.get("imageUrls");
                        Picasso.get().load(imageUrls.get(0)).into(binding.imageProfile);
                        binding.textName.setText(documentSnapshot.getString("chatName"));
                    }
                });
    }
}