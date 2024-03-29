package com.example.project_.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_.databinding.ItemContainerGroupChatMessageBinding;
import com.example.project_.models.GroupChatMessage;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GroupChatMessageAdapter extends RecyclerView.Adapter<GroupChatMessageAdapter.GroupChatMessageViewHolder> {
    private final List<GroupChatMessage> groupChatMessages;
    private final PreferenceManager preferenceManager;

    public GroupChatMessageAdapter(List<GroupChatMessage> groupChatMessages, PreferenceManager preferenceManager) {
        this.groupChatMessages = groupChatMessages;
        this.preferenceManager = preferenceManager;
    }

    @NonNull
    @Override
    public GroupChatMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GroupChatMessageViewHolder(
                ItemContainerGroupChatMessageBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull GroupChatMessageViewHolder holder, int position) {
        holder.setData(groupChatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return groupChatMessages.size();
    }

    class GroupChatMessageViewHolder extends RecyclerView.ViewHolder {
        ItemContainerGroupChatMessageBinding binding;
        public GroupChatMessageViewHolder(ItemContainerGroupChatMessageBinding itemContainerGroupChatMessageBinding) {
            super(itemContainerGroupChatMessageBinding.getRoot());
            binding = itemContainerGroupChatMessageBinding;
        }
        void setData(GroupChatMessage groupChatMessage) {
            if (groupChatMessage.senderId.equals(preferenceManager.getString(Constants.KEY_USER_ID))) {
                binding.receivedMessage.setVisibility(View.GONE);
                binding.textMessage.setText(groupChatMessage.content);
                binding.textDateTime.setText(getReadableDateTime(groupChatMessage.timeStamp));
            } else {
                binding.sendMessage.setVisibility(View.GONE);
                binding.textMessage1.setText(groupChatMessage.content);
                binding.textDateTime1.setText(getReadableDateTime(groupChatMessage.timeStamp));

            }
        }
        private Bitmap getUserImage(String encodedImage) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
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
    }
}
