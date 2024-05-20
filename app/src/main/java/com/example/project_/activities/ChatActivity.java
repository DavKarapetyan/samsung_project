package com.example.project_.activities;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.project_.adapters.ChatAdapter;
import com.example.project_.databinding.ActivityChatBinding;
import com.example.project_.models.ChatMessage;
import com.example.project_.models.TimeModel;
import com.example.project_.models.User;
import com.example.project_.network.ApiClient;
import com.example.project_.network.ApiService;
import com.example.project_.repository.MainRepository;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.permissionx.guolindev.PermissionX;
import com.squareup.picasso.Picasso;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends BaseActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String conversionId = null;
    private Boolean isReceiverAvailable = false;
    private static final String NTP_SERVER = "pool.ntp.org";
    private StorageReference storageReference;
    private Uri imageUri;
    private MainRepository mainRepository;
    private String currentTime1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        storageReference = FirebaseStorage.getInstance().getReference("uploadsChat");
        setContentView(binding.getRoot());
        setListeners();
        loadReceiverDetails();
        init();
        listenMessages();
        //12new NtpTask().execute();
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

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                imageUri = data.getClipData().getItemAt(0).getUri();
            } else {
                imageUri = data.getData();
            }
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
                getBitmapFromEncodedString(receiverUser.image),
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void sendMessage() {
        HashMap<String, Object> message = new HashMap<>();
        //new NtpTask().execute();
        ApiService apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
        Call<TimeModel> timeModelCall = apiService.getTime();
        if (binding.inputMessage.getText().toString() != null && !binding.inputMessage.getText().toString().isEmpty()) {
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
                                            message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                                            message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
                                            message.put(Constants.KEY_TIMESTAMP, currentTime1);
                                            message.put(Constants.KEY_SEND_IMAGE, uri.toString());
                                            // Add message text only after image upload is completed
                                            message.put(Constants.KEY_MESSAGE, messageText);

                                            database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
                                        });
                                    });
                        } else {
                            message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                            message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
                            message.put(Constants.KEY_TIMESTAMP, currentTime1);
                            // Add message text directly if no image is selected
                            message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());

                            database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
                        }
                        imageUri = null;
                        if (conversionId != null) {
                            updateConversion(binding.inputMessage.getText().toString());
                        } else {
                            HashMap<String, Object> conversion = new HashMap<>();
                            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
                            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
                            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
                            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
                            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
                            conversion.put(Constants.KEY_LAST_MESSAGE, binding.inputMessage.getText().toString());
                            conversion.put(Constants.KEY_TIMESTAMP, currentTime1);
                            addConversion(conversion);
                        }
                        if (!isReceiverAvailable) {
                            try {
                                JSONArray tokens = new JSONArray();
                                tokens.put(receiverUser.token);

                                JSONObject data = new JSONObject();
                                data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                                data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
                                data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                                data.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
                                if (imageUri != null) {
                                    data.put(Constants.KEY_SEND_IMAGE, imageUri.toString());
                                }
                                JSONObject body = new JSONObject();
                                body.put(Constants.REMOTE_MSG_DATA, data);
                                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

                                sendNotification(body.toString());
                            } catch (Exception exception) {
                                showToast(exception.getMessage());
                            }
                        }
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
        } else {
            showToast("Message is empty");
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void sendNotification(String messageBody) {
        HashMap<String, Object> message = new HashMap<>();
        //new NtpTask().execute();
        ApiClient.getClient().create(ApiService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    try {
                        if (response.body() != null) {
                            JSONObject responseJson = new JSONObject(response.body());
                            JSONArray results = responseJson.getJSONArray("results");
                            if (responseJson.getInt("failure") == 1) {
                                JSONObject error = (JSONObject) results.get(0);
                                showToast(error.getString("error"));
                                return;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    showToast("Notification sent successfully");
                } else {
                    showToast("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                showToast(t.getMessage());
            }
        });
        try {
            JSONObject jsonObject = new JSONObject(messageBody);
            JSONObject dataObject = jsonObject.getJSONObject("data");

            String name = dataObject.getString("name");

            HashMap<String, Object> notification = new HashMap<>();
            notification.put("name", "New message");
            notification.put("receiverId", receiverUser.id);
            notification.put("senderImage", preferenceManager.getString(Constants.KEY_IMAGE));
            notification.put("timestamp", new Date());
            notification.put("message", name + " sent you a message");

            database.collection("notifications").add(notification);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void listenAvailabilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTION_USERS).document(
                receiverUser.id
        ).addSnapshotListener(ChatActivity.this, (value, error) -> {
            if (error != null) {
                return;
            }
            if (value != null) {
                if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                    int availability = Objects.requireNonNull(
                            value.getLong(Constants.KEY_AVAILABILITY)
                    ).intValue();
                    isReceiverAvailable = availability == 1;
                }
                receiverUser.token = value.getString(Constants.KEY_FCM_TOKEN);
                if (receiverUser.image == null) {
                    receiverUser.image = value.getString(Constants.KEY_IMAGE);
                    chatAdapter.setReceiverProfileImage(getBitmapFromEncodedString(receiverUser.image));
                    chatAdapter.notifyItemRangeChanged(0, chatMessages.size());
                }
            }
            if (isReceiverAvailable) {
                binding.textAvailability.setVisibility(View.VISIBLE);
            } else {
                binding.textAvailability.setVisibility(View.GONE);
            }
        });
    }

    private void listenMessages() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
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
                    chatMessage.id = documentChange.getDocument().getId();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.image = documentChange.getDocument().getString(Constants.KEY_SEND_IMAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getString(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getString(Constants.KEY_TIMESTAMP);
                    if (documentChange.getDocument().getBoolean("isLiked") != null) {
                        chatMessage.isLiked = documentChange.getDocument().getBoolean("isLiked");
                    }
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
        if (conversionId == null) {
            checkForConversion();
        }
    };

    private Bitmap getBitmapFromEncodedString(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }
    }

    private void loadReceiverDetails() {
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiverUser.name);
        binding.imageProfile.setImageBitmap(getBitmapFromEncodedString(receiverUser.image));
    }

    private void setListeners() {
        binding.textName.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), UserProfileActivity.class);
            intent.putExtra("userId", receiverUser.id);
            startActivity(intent);
        });
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> sendMessage());
        startVideoCall();
        binding.uploadImage.setOnClickListener(v -> openFileChooser());
    }

    private void checkForConversion() {
        if (chatMessages.size() != 0) {
            checkForConversionRemotely(
                    preferenceManager.getString(Constants.KEY_USER_ID),
                    receiverUser.id
            );
            checkForConversionRemotely(
                    receiverUser.id,
                    preferenceManager.getString(Constants.KEY_USER_ID)
            );
        }
    }

    private void addConversion(HashMap<String, Object> conversion) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }

    private void updateConversion(String message) {
        ApiService apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
        Call<TimeModel> timeModelCall = apiService.getTime();

        timeModelCall.enqueue(new Callback<TimeModel>() {
            @Override
            public void onResponse(Call<TimeModel> call, Response<TimeModel> response) {
                currentTime1 = response.body().getDateTime();

                Toast.makeText(getApplicationContext(), "Date is getted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<TimeModel> call, Throwable t) {
                String a = t.getMessage();
                Toast.makeText(getApplicationContext(), "Cant get date" + a, Toast.LENGTH_SHORT).show();
            }
        });

        if (currentTime1 != null) {
            DocumentReference documentReference =
                    database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
            documentReference.update(
                    Constants.KEY_LAST_MESSAGE, message,
                    Constants.KEY_TIMESTAMP, new Date()
            );
        } else {
            Toast.makeText(getApplicationContext(), "Date is null", Toast.LENGTH_SHORT).show();
        }
    }

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

    private void startVideoCall() {
        mainRepository = MainRepository.getInstance();
        binding.video.setOnClickListener(v -> {
            PermissionX.init(this)
                    .permissions(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO)
                    .request((allGranted, grantedList, deniedList) -> {
                        if (allGranted) {
                            //login to firebase here

                            mainRepository.login(
                                    preferenceManager.getString(Constants.KEY_USER_ID), getApplicationContext(), () -> {
                                        //if success then we want to move to call activity
                                        Intent intent = new Intent(ChatActivity.this, CallActivity.class);
                                        intent.putExtra("receiverId", receiverUser.id);
                                        startActivity(intent);
                                    }
                            );
                        }
                    });


        });
    }

    private void checkForConversionRemotely(String senderId, String receiverId) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, senderId)
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversionId = documentSnapshot.getId();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }
}