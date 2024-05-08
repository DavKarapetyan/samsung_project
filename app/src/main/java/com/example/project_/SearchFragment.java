package com.example.project_;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.project_.activities.UserProfileActivity;
import com.example.project_.adapters.UsersAdapter;
import com.example.project_.databinding.FragmentSearchBinding;
import com.example.project_.listeners.UserListener;
import com.example.project_.models.User;
import com.example.project_.utilities.Constants;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;
    private FragmentSearchBinding binding;
    private List<User> users;
    private FirebaseFirestore firebaseFirestore;

    public SearchFragment() {
        // Required empty public constructor
    }

    public static SearchFragment newInstance(String param1, String param2) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentSearchBinding.inflate(getLayoutInflater());
        firebaseFirestore = FirebaseFirestore.getInstance();
        users = new ArrayList<>();
        getUsers();
        binding.searchView.clearFocus();
        binding.searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                filterUsers(s.toString());
            }
        });

        return binding.getRoot();
    }
    private void getUsers() {
        loading(true);
        firebaseFirestore.collection("users").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            User user = new User();
                            user.name = documentSnapshot.getString(Constants.KEY_NAME);
                            user.email = documentSnapshot.getString(Constants.KEY_EMAIL);
                            user.image = documentSnapshot.getString(Constants.KEY_IMAGE);
                            user.id = documentSnapshot.getId();
                            users.add(user);
                        }
                    }
                });
        loading(false);
    }
    private void filterUsers(String text) {
        loading(true);
        List<User> filteredUsers = new ArrayList<>();
        for (User user : users) {
            if (user.name.trim().toLowerCase().contains(text.trim().toLowerCase())) {
                filteredUsers.add(user);
            }
        }
        if (filteredUsers.size() > 0) {
            UsersAdapter usersAdapter = new UsersAdapter(filteredUsers, new UserListener() {
                @Override
                public void onUserClicked(User user) {
                    Intent intent = new Intent(getActivity(), UserProfileActivity.class);
                    intent.putExtra("userId", user.id);
                    startActivity(intent);
                }
            });
            binding.usersRecyclerView.setAdapter(usersAdapter);
            binding.usersRecyclerView.setVisibility(View.VISIBLE);
        }
        loading(false);
    }
    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.loading.setVisibility(View.VISIBLE);
        } else {
            binding.loading.setVisibility(View.INVISIBLE);
        }
    }
}