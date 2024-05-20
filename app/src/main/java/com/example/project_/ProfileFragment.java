package com.example.project_;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.project_.activities.AddPostActivity;
import com.example.project_.activities.MainActivity;
import com.example.project_.activities.SettingsActivity;
import com.example.project_.activities.UpdateUserDataActivity;
import com.example.project_.adapters.PostAdapter;
import com.example.project_.adapters.ProfilePostsAdapter;
import com.example.project_.databinding.FragmentProfileBinding;
import com.example.project_.models.Post;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.android.flexbox.AlignContent;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.android.flexbox.FlexboxLayoutManager;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProfileFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private FragmentProfileBinding binding;
    boolean nightMode;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore firebaseFirestore;
    private ProfilePostsAdapter profilePostsAdapter;
    private List<Post> posts;
    private String api = "AIzaSyAaY036WM5OntGLeHgWkxNmuMl3IfEri5Q";


    public ProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ProfileFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ProfileFragment newInstance(String param1, String param2) {
        ProfileFragment fragment = new ProfileFragment();
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
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        sharedPreferences = getActivity().getSharedPreferences("MODE", Context.MODE_PRIVATE);
        nightMode = sharedPreferences.getBoolean("nightMode", false);
        firebaseFirestore = FirebaseFirestore.getInstance();

//        binding.start.setOnClickListener(v -> {
////            String targetLang = "en"; // for Arabic
////            String sourceLang = "ja"; // for japan
////            Translate translate = (Translate) TranslateOptions.newBuilder().setApiKey(api).build().getService();
////            //Translation translation = translate.translate("Hello", translate.TranslateOption.targetLanguage(targetLang), Translate.TranslateOption.sourceLanguage(sourceLang));
////            Tran
////            String translatedText = translation.getTranslatedText();
//            signOut();
//        });
        binding.menu.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), SettingsActivity.class));
        });
        binding.editProfile.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), UpdateUserDataActivity.class));
        });
        binding.followings.setOnClickListener(v -> {
            FollowListDialogFragment followListDialogFragment = FollowListDialogFragment.newInstance("followings", preferenceManager.getString(Constants.KEY_USER_ID));
            followListDialogFragment.show(getParentFragmentManager(), "TAG");
        });
        binding.followers.setOnClickListener(v -> {
            FollowListDialogFragment followListDialogFragment = FollowListDialogFragment.newInstance("followers", preferenceManager.getString(Constants.KEY_USER_ID));
            followListDialogFragment.show(getParentFragmentManager(), "TAG");
        });

        preferenceManager = new PreferenceManager(getContext());
//
        loadUserDetails();
        getToken();
//
        posts = new ArrayList<>();
        profilePostsAdapter = new ProfilePostsAdapter(posts, new PreferenceManager(getActivity()), getActivity());
        binding.profilePostsRecyclerView.setAdapter(profilePostsAdapter);

        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(getActivity());
        layoutManager.setFlexWrap(FlexWrap.WRAP);
        retrievePosts();
        binding.profilePostsRecyclerView.setLayoutManager(layoutManager);
        binding.profilePostsRecyclerView.addItemDecoration(new DividerItemDecoration(binding.profilePostsRecyclerView.getContext(), DividerItemDecoration.HORIZONTAL));
        binding.profilePostsRecyclerView.setVisibility(View.VISIBLE);

        // Inflate the layout for this fragment
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        retrievePosts();
    }

    private void retrievePosts() {
        FirebaseFirestore.getInstance().collection("posts").whereEqualTo("userId", preferenceManager.getString(Constants.KEY_USER_ID))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    posts.clear(); // Clear previous posts
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Post post = createPostFromDocument(documentSnapshot);
                        post.id = documentSnapshot.getId();
                        post.userId = documentSnapshot.getString("userId");
                        posts.add(post);
                    }
                    binding.postsCount.setText(Integer.toString(posts.size()));
                    profilePostsAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("Home", "Failed to retrieve posts: " + e.getMessage());
                });
    }

    private Post createPostFromDocument(QueryDocumentSnapshot documentSnapshot) {
        Post post = new Post();
        post.hashTag = documentSnapshot.getString("hashTag");
        post.content = documentSnapshot.getString("content");
        // Get the imageUris field from the DocumentSnapshot
        List<String> imageUris = (List<String>) documentSnapshot.get("imageUrls");
        if (imageUris != null) {
            post.imageUris = imageUris;
        }

        // Get user details using userId
        String userId = documentSnapshot.getString("userId");
        retrieveUserDetails(userId, post);

        return post;
    }

    private void retrieveUserDetails(String userId, Post post) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User document exists, retrieve user details
                        String userName = documentSnapshot.getString("name");
                        String userImage = documentSnapshot.getString("image");

                        // Set user details to post object
                        post.userName = userName;
                        post.userImage = userImage;

                        // Notify adapter about changes
                        profilePostsAdapter.notifyDataSetChanged();
                    } else {
                        Log.e("Home", "User document does not exist for userId: " + userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Home", "Failed to retrieve user details for userId: " + userId + ", Error: " + e.getMessage());
                });
    }

    private void loadUserDetails() {
        //binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        //binding.textNickName.setText(preferenceManager.getString(Constants.KEY_NICKNAME));
        //byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        firebaseFirestore.collection("users").document(preferenceManager.getString(Constants.KEY_USER_ID))
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        binding.textName.setText(documentSnapshot.getString("name"));
                        binding.textNickName.setText(documentSnapshot.getString("nickName"));
                        byte[] bytes = Base64.decode(documentSnapshot.getString("image"), Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        binding.imageProfile.setImageBitmap(bitmap);
                    }
                });

        firebaseFirestore.collection("users").document(preferenceManager.getString(Constants.KEY_USER_ID)).collection("followers").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        binding.followersCount.setText(Integer.toString(queryDocumentSnapshots.size()));
                    }
                });
        firebaseFirestore.collection("users").document(preferenceManager.getString(Constants.KEY_USER_ID)).collection("followings").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        binding.followingsCount.setText(Integer.toString(queryDocumentSnapshots.size()));
                    }
                });
    }

    private void showToast(String message) {
        //Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        } else {
            Log.e("ProfileAct", "getContext() returned null");
        }

    }

    private void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token) {
        preferenceManager.putString(Constants.KEY_FCM_TOKEN, token);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnSuccessListener(unused -> showToast("Token updated successfully"))
                .addOnFailureListener(e -> showToast("Unable to update token"));
    }

    private void signOut() {
        showToast("Signing out...");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getActivity(), MainActivity.class));
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }
}