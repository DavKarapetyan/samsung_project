package com.example.project_.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.project_.R;
import com.example.project_.adapters.NotificationsAdapter;
import com.example.project_.databinding.ActivityNotificationsBinding;
import com.example.project_.models.Notification;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {
    private ActivityNotificationsBinding binding;
    private NotificationsAdapter notificationsAdapter;
    private List<Notification> notifications;
    private SwipeRefreshLayout swipeRefreshLayout;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());

        swipeRefreshLayout = binding.swipeRefreshLayout;
        swipeRefreshLayout.setOnRefreshListener(this::refreshNotifications);

        notifications = new ArrayList<>();
        notificationsAdapter = new NotificationsAdapter(notifications, getSupportFragmentManager());
        binding.notificationsRecyclerView.setAdapter(notificationsAdapter);

        retrieveNotifications();
        binding.notificationsRecyclerView.setVisibility(View.VISIBLE);
    }
    private void retrieveNotifications() {
        FirebaseFirestore.getInstance().collection("notifications")
                .whereEqualTo("receiverId", preferenceManager.getString(Constants.KEY_USER_ID))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notifications.clear();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Notification notification = new Notification();
                        notification.senderImage = documentSnapshot.getString("senderImage");
                        notification.name = documentSnapshot.getString("name");
                        notification.message = documentSnapshot.getString("message");
                        notification.timeStamp = documentSnapshot.get("timestamp").toString();
                        notification.receiverId = documentSnapshot.getString("receiverId");

                        notifications.add(notification);
                    }
                    notificationsAdapter.notifyDataSetChanged();
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Log.e("Notifications", "Failed to retrieve notifications: " + e.getMessage());
                    swipeRefreshLayout.setRefreshing(false); // Stop refreshing animation
                });

    }
    private void refreshNotifications() {
        swipeRefreshLayout.setRefreshing(true); // Show refreshing animation
        retrieveNotifications(); // Fetch posts again
    }
}