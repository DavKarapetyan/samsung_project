package com.example.project_.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.project_.databinding.ItemContainerNotificationBinding;
import com.example.project_.models.Notification;

import java.util.List;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationsViewHolder>{
    private final List<Notification> notifications;
    private FragmentManager fragmentManager;

    public NotificationsAdapter(List<Notification> notifications, FragmentManager parentFragmentManager) {
        this.notifications = notifications;
        this.fragmentManager = parentFragmentManager;
    }
    @NonNull
    @Override
    public NotificationsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerNotificationBinding itemContainerNotificationBinding = ItemContainerNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new NotificationsViewHolder(itemContainerNotificationBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationsViewHolder holder, int position) {
        holder.setNotificationData(notifications.get(position));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    class NotificationsViewHolder extends RecyclerView.ViewHolder {
        ItemContainerNotificationBinding binding;

        NotificationsViewHolder(ItemContainerNotificationBinding itemContainerNotificationBinding) {
            super(itemContainerNotificationBinding.getRoot());
            binding = itemContainerNotificationBinding;
        }

        void setNotificationData(Notification notification) {
            binding.notificationName.setText(notification.name);
            binding.notificationMessage.setText(notification.message);
            if (notification.senderImage != null) {
                binding.senderImage.setImageBitmap(getUserImage(notification.senderImage));
            }
        }
    }
    private Bitmap getUserImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
