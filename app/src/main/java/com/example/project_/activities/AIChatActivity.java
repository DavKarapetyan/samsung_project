package com.example.project_.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.project_.R;
import com.example.project_.adapters.ChatAdapter;
import com.example.project_.adapters.RecentConversationsAdapter;
import com.example.project_.databinding.ActivityAichatBinding;
import com.example.project_.models.ChatMessage;
import com.example.project_.models.TimeModel;
import com.example.project_.network.ApiClient;
import com.example.project_.network.ApiService;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.InputStream;
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

public class AIChatActivity extends AppCompatActivity {
    ActivityAichatBinding binding;
    private static final int PICK_IMAGE_REQUEST = 1;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private StorageReference storageReference;
    private Uri imageUri;
    private String currentTime1;
    private Bitmap imageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAichatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        storageReference = FirebaseStorage.getInstance().getReference("uploadsChat");
        binding.layoutSend.setOnClickListener(v -> {
            if (binding.inputMessage.getText().toString() != null && !binding.inputMessage.getText().toString().isEmpty()) {
                sendMessage();
                callGemini();
            } else {
                showToast("Message is empty");
            }
        });
        binding.imageBack.setOnClickListener(v -> {
            finish();
        });
        binding.uploadImage.setOnClickListener(v -> openFileChooser());
        init();
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

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                imageUri = data.getClipData().getItemAt(0).getUri();
            } else {
                imageUri = data.getData();
            }
            InputStream inputStream = null;
            try {
                inputStream = getContentResolver().openInputStream(imageUri);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            imageBitmap = BitmapFactory.decodeStream(inputStream);
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
                drawableToBitmap(getApplicationContext(), R.drawable.ai),
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
                                            message.put(Constants.KEY_RECEIVER_ID, "ai");
                                            message.put(Constants.KEY_TIMESTAMP, currentTime1);
                                            message.put(Constants.KEY_SEND_IMAGE, uri.toString());
                                            // Add message text only after image upload is completed
                                            message.put(Constants.KEY_MESSAGE, messageText);

                                            database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
                                        });
                                    });
                        } else {
                            message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                            message.put(Constants.KEY_RECEIVER_ID, "ai");
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
        } else {
            showToast("Message is empty");
        }
    }

    private void listenMessages() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, "ai")
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, "ai")
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
    };


    private void callGemini() {
        if (imageBitmap != null) {
            GenerativeModel gm = new GenerativeModel(/* modelName */ "gemini-pro-vision",
// Access your API key as a Build Configuration variable (see "Set up your API key" above)
                    /* apiKey */ "AIzaSyDdRlxj8SZZ_M7JRw7AZsQQuIhDZIRqMls");
            GenerativeModelFutures model = GenerativeModelFutures.from(gm);

            Content content = new Content.Builder()
                    .addText(binding.inputMessage.getText().toString())
                    .addImage(imageBitmap)
                    .build();
            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    String resultText = result.getText();
                    HashMap<String, Object> message = new HashMap<>();
                    message.put(Constants.KEY_SENDER_ID, "ai");
                    message.put(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                    message.put(Constants.KEY_TIMESTAMP, currentTime1);
                    // Add message text directly if no image is selected
                    message.put(Constants.KEY_MESSAGE, resultText);

                    database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
                    System.out.println(resultText);
                }

                @Override
                public void onFailure(Throwable t) {
                    t.printStackTrace();
                }
            }, this.getMainExecutor());
        } else {
            GenerativeModel gm = new GenerativeModel(/* modelName */ "gemini-pro",
// Access your API key as a Build Configuration variable (see "Set up your API key" above)
                    /* apiKey */ "AIzaSyDdRlxj8SZZ_M7JRw7AZsQQuIhDZIRqMls");
            GenerativeModelFutures model = GenerativeModelFutures.from(gm);

            Content content = new Content.Builder()
                    .addText(binding.inputMessage.getText().toString())
                    .build();
            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    String resultText = result.getText();
                    HashMap<String, Object> message = new HashMap<>();
                    message.put(Constants.KEY_SENDER_ID, "ai");
                    message.put(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                    message.put(Constants.KEY_TIMESTAMP, currentTime1);
                    // Add message text directly if no image is selected
                    message.put(Constants.KEY_MESSAGE, resultText);

                    database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
                    System.out.println(resultText);
                }

                @Override
                public void onFailure(Throwable t) {
                    t.printStackTrace();
                }
            }, this.getMainExecutor());
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

    public static Bitmap drawableToBitmap(Context context, int drawableId) {
        // Decode the drawable resource into a Bitmap object
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), drawableId);
        return bitmap;
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}