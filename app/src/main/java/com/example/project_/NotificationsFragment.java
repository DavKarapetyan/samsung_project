package com.example.project_;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.project_.adapters.NotificationsAdapter;
import com.example.project_.databinding.FragmentNotificationsBinding;
import com.example.project_.models.Notification;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {
    private FragmentNotificationsBinding binding;
    private NotificationsAdapter notificationsAdapter;
    private List<Notification> notifications;
    private SwipeRefreshLayout swipeRefreshLayout;
    private PreferenceManager preferenceManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        preferenceManager = new PreferenceManager(getActivity());

        swipeRefreshLayout = binding.swipeRefreshLayout;
        swipeRefreshLayout.setOnRefreshListener(this::refreshNotifications);

        notifications = new ArrayList<>();
        notificationsAdapter = new NotificationsAdapter(notifications, getParentFragmentManager());
        binding.notificationsRecyclerView.setAdapter(notificationsAdapter);

        retrieveNotifications();
        binding.notificationsRecyclerView.setVisibility(View.VISIBLE);

        // Inflate the layout for this fragment
        return binding.getRoot();
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