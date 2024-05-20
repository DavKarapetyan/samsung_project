package com.example.project_.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_.databinding.ItemContainerReceivedMessageBinding;
import com.example.project_.databinding.ItemContainerSentMessageBinding;
import com.example.project_.models.ChatMessage;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> chatMessages;
    private Bitmap receiverProfileImage;
    private final String senderId;

    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;

    public void setReceiverProfileImage(Bitmap bitmap) {
        receiverProfileImage = bitmap;
    }

    public ChatAdapter(List<ChatMessage> chatMessages, Bitmap receiverProfileImage, String senderId) {
        this.chatMessages = chatMessages;
        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            return new SentMessageViewHolder(
                    ItemContainerSentMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        } else {
            return new ReceivedMessageViewHolder(
                    ItemContainerReceivedMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).setData(message);
        } else {
            ((ReceivedMessageViewHolder) holder).setData(message, receiverProfileImage);
        }
    }


    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if(chatMessages.get(position).senderId.equals(senderId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerSentMessageBinding binding;

        SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding) {
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
        }

        void setData(ChatMessage chatMessage) {
            binding.textMessage.setText(chatMessage.message);
            binding.textDateTime.setText(chatMessage.dateTime);
            DocumentReference chatMessageRef = FirebaseFirestore.getInstance().collection("chat").document(chatMessage.id);

            if (chatMessage.isLiked == true) {
                binding.likeImage.setVisibility(View.VISIBLE);
            } else {
                binding.likeImage.setVisibility(View.GONE);
            }

            GestureDetector gestureDetector = new GestureDetector(itemView.getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    // Handle double tap event
                    chatMessageRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.getBoolean("isLiked") != null && !documentSnapshot.getBoolean("isLiked")) {
                                FirebaseFirestore.getInstance().collection("chat").document(chatMessage.id).update("isLiked", true);
                                binding.likeImage.setVisibility(View.GONE);
                            } else {
                                FirebaseFirestore.getInstance().collection("chat").document(chatMessage.id).update("isLiked", false);
                                binding.likeImage.setVisibility(View.GONE);
                            }
                        }
                    });
                    return true;
                }
            });
            binding.senderMessage.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                return true;
            });

            if (chatMessage.image != null) {
                Picasso.get().load(chatMessage.image).into(binding.sendImage);
                binding.sendImage.setVisibility(View.GONE);
            } else {
                // If no image, hide the ImageView
                binding.sendImage.setVisibility(View.GONE);
            }
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerReceivedMessageBinding binding;

        ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding) {
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
        }

        void setData(ChatMessage chatMessage, Bitmap receiverProfileImage) {
            binding.textMessage.setText(chatMessage.message);
            binding.textDateTime.setText(chatMessage.dateTime);
            if (receiverProfileImage != null) {
                binding.imageProfile.setImageBitmap(receiverProfileImage);
            }
            if (chatMessage.groupChatId != null) {
                FirebaseFirestore.getInstance().collection("users").document(chatMessage.senderId).get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                binding.imageProfile.setImageBitmap(getUserImage(documentSnapshot.getString("image")));
                            }
                        });
            }
            DocumentReference chatMessageRef = FirebaseFirestore.getInstance().collection("chat").document(chatMessage.id);

            if (chatMessage.isLiked == true) {
                binding.likeImage.setVisibility(View.GONE);
            } else {
                binding.likeImage.setVisibility(View.GONE);
            }

            GestureDetector gestureDetector = new GestureDetector(itemView.getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    // Handle double tap event
                    chatMessageRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.getBoolean("isLiked") != null && !documentSnapshot.getBoolean("isLiked")) {
                                FirebaseFirestore.getInstance().collection("chat").document(chatMessage.id).update("isLiked", true);
                                binding.likeImage.setVisibility(View.GONE);
                            } else {
                                FirebaseFirestore.getInstance().collection("chat").document(chatMessage.id).update("isLiked", false);
                                binding.likeImage.setVisibility(View.GONE);
                            }
                        }
                    });
                    return true;
                }
            });
            binding.receiverMessageContainer.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                return true;
            });

            if (chatMessage.image != null) {
                Picasso.get().load(chatMessage.image).into(binding.sendImage);
                binding.sendImage.setVisibility(View.VISIBLE);
            } else {
                // If no image, hide the ImageView
                binding.sendImage.setVisibility(View.GONE);
            }
        }

        private Bitmap getUserImage(String encodedImage) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
    }

}
