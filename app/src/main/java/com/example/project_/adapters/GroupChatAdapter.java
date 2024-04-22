package com.example.project_.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_.activities.GroupChatMain2Activity;
import com.example.project_.databinding.ItemContainerRecentConversionBinding;
import com.example.project_.models.GroupChat;
import com.squareup.picasso.Picasso;

import java.util.List;

public class GroupChatAdapter extends RecyclerView.Adapter<GroupChatAdapter.GroupChatViewHolder> {
    private final List<GroupChat> groupChats;
    private static Context context;

    public GroupChatAdapter(List<GroupChat> groupChats, Context context) {
        this.groupChats = groupChats;
        this.context = context;
    }
    @NonNull

    @Override
    public GroupChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GroupChatViewHolder(
                ItemContainerRecentConversionBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull GroupChatViewHolder holder, int position) {
        holder.setData(groupChats.get(position));
    }

    @Override
    public int getItemCount() {
        return groupChats.size();
    }

    class GroupChatViewHolder extends RecyclerView.ViewHolder {
        ItemContainerRecentConversionBinding binding;

        public GroupChatViewHolder(ItemContainerRecentConversionBinding itemContainerRecentConversionBinding) {
            super(itemContainerRecentConversionBinding.getRoot());
            binding = itemContainerRecentConversionBinding;
        }
        void setData(GroupChat groupChat) {
            binding.textName.setText(groupChat.chatName);
            if (groupChat.imageUrls != null && !groupChat.imageUrls.isEmpty()) {
                Picasso.get().load(groupChat.imageUrls.get(0)).into(binding.imageProfile);
            }
            binding.getRoot().setOnClickListener(v -> {
                Intent intent = new Intent(context, GroupChatMain2Activity.class);
                intent.putExtra("groupChatId", groupChat.id);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            });
        }
    }
}
