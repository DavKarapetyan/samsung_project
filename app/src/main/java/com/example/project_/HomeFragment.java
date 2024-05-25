package com.example.project_;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.Dialog;
import android.app.Notification;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.dsphotoeditor.sdk.activity.DsPhotoEditorActivity;
import com.dsphotoeditor.sdk.utils.DsPhotoEditorConstants;
import com.example.project_.activities.AugmentedFacesActivity;
import com.example.project_.activities.NotificationsActivity;
import com.example.project_.adapters.MomentsAdapter;
import com.example.project_.adapters.PostAdapter;
import com.example.project_.databinding.FragmentHomeBinding;
import com.example.project_.models.Post;
import com.example.project_.models.User;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private PostAdapter postAdapter;
    private MomentsAdapter momentsAdapter;
    private List<Post> posts;
    private List<User> momentsUsers;
    private SwipeRefreshLayout swipeRefreshLayout;
    private PreferenceManager preferenceManager;
    private boolean isUserMomentExisted;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        preferenceManager = new PreferenceManager(getActivity());

        // Set up SwipeRefreshLayout
        swipeRefreshLayout = binding.swipeRefreshLayout;
        swipeRefreshLayout.setOnRefreshListener(this::refreshPosts);
        loadUserImage();

        // Initialize posts list and adapter
        posts = new ArrayList<>();
        postAdapter = new PostAdapter(posts, getParentFragmentManager(), new PreferenceManager(getActivity()), getActivity());
        binding.postsRecyclerView.setAdapter(postAdapter);

        momentsUsers = new ArrayList<>();
        momentsAdapter = new MomentsAdapter(momentsUsers, getActivity());
        binding.momentsRecyclerView.setAdapter(momentsAdapter);

        // Retrieve initial posts
        retrievePosts();
        retrieveMoments();
        binding.postsRecyclerView.setVisibility(View.VISIBLE);
        binding.momentsRecyclerView.setVisibility(View.VISIBLE);
        binding.addMoment.setOnClickListener(v -> {
            //checkPermission();
            showPopupMenu(v);
        });
        binding.notifications.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), NotificationsActivity.class));
        });

        View view = inflater.inflate(R.layout.story_dialog, null);
        Dialog dialog = new StoryDialog(getActivity(), preferenceManager.getString(Constants.KEY_USER_ID));
        dialog.getWindow().getAttributes().windowAnimations = R.style.StoryDialogAnimation;
        dialog.setContentView(view);

        binding.myImageProfile.setOnClickListener(v -> {
            if (isUserMomentExisted) {
                dialog.show();
            }
        });


//        binding.imageView2.setOnClickListener(v -> {
//            dialog.show();
//        });

        return binding.getRoot();
    }

    public void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.menu_popup, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                return onPopupMenuClick(menuItem);
            }
        });

        popupMenu.show();
    }

    private boolean onPopupMenuClick(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.story:
                checkPermission();
                break;
            case R.id.mask:
                startActivity(new Intent(getActivity(), AugmentedFacesActivity.class));
                break;
        }

        return true;
    }


    private void checkPermission() {
        int permission = ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pickImage();
        } else {
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            } else {
                pickImage();
            }
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/**");
        startActivityForResult(intent, 100);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickImage();
        } else {
            Toast.makeText(getActivity(), "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();
            switch (requestCode) {
                case 100:
                    Intent intent = new Intent(getActivity(), DsPhotoEditorActivity.class);
                    intent.setData(uri);
                    intent.putExtra(DsPhotoEditorConstants.DS_PHOTO_EDITOR_OUTPUT_DIRECTORY, "Images");
                    intent.putExtra(DsPhotoEditorConstants.DS_TOOL_BAR_BACKGROUND_COLOR, Color.parseColor("#2D61AD"));
                    intent.putExtra(DsPhotoEditorConstants.DS_MAIN_BACKGROUND_COLOR, Color.parseColor("#FFFFFF"));
                    intent.putExtra(DsPhotoEditorConstants.DS_PHOTO_EDITOR_TOOLS_TO_HIDE, new int[]{DsPhotoEditorActivity.TOOL_WARMTH, DsPhotoEditorActivity.TOOL_PIXELATE, DsPhotoEditorActivity.TOOL_FILTER, DsPhotoEditorActivity.TOOL_FRAME, DsPhotoEditorActivity.TOOL_ROUND, DsPhotoEditorActivity.TOOL_EXPOSURE, DsPhotoEditorActivity.TOOL_CONTRAST, DsPhotoEditorActivity.TOOL_VIGNETTE, DsPhotoEditorActivity.TOOL_ORIENTATION, DsPhotoEditorActivity.TOOL_SATURATION, DsPhotoEditorActivity.TOOL_SHARPNESS});
                    startActivityForResult(intent, 101);
                    break;
                case 101:
                    //binding.addMoment.setImageURI(uri);
                    StorageReference storageRef = FirebaseStorage.getInstance().getReference();

                    StorageReference imageRef = storageRef.child("images/" + UUID.randomUUID().toString());

                    imageRef.putFile(uri)
                            .addOnSuccessListener(taskSnapshot -> {
                                imageRef.getDownloadUrl().addOnSuccessListener(uri1 -> {
                                    HashMap<String, Object> moment = new HashMap<>();
                                    Calendar calendar = Calendar.getInstance();
                                    calendar.setTime(new Date());
                                    Calendar newCalendar = Calendar.getInstance();
                                    newCalendar.setTime(new Date());

                                    FirebaseFirestore.getInstance().collection("users").document(preferenceManager.getString(Constants.KEY_USER_ID)).get()
                                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                @Override
                                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                    if (documentSnapshot.getString("momentExpirationDate") != null) {
                                                        String timeHour = documentSnapshot.getString("momentExpirationDate");
                                                        String cleanedString = timeHour.replace(" hour", "").trim();
                                                        int time = Integer.parseInt(cleanedString);
                                                        newCalendar.add(Calendar.HOUR, time);
                                                        moment.put("imageURL", uri1.toString());
                                                        moment.put("publishDate", calendar.getTime());
                                                        moment.put("expirationDate", newCalendar.getTime());
                                                    } else {
                                                        newCalendar.add(Calendar.HOUR, 6);
                                                        moment.put("imageURL", uri1.toString());
                                                        moment.put("publishDate", calendar.getTime());
                                                        moment.put("expirationDate", newCalendar.getTime());
                                                    }
                                                    FirebaseFirestore.getInstance().collection("users")
                                                            .document(preferenceManager.getString(Constants.KEY_USER_ID))
                                                            .collection("moments")
                                                            .add(moment)
                                                            .addOnSuccessListener(documentReference -> {
                                                                Toast.makeText(getActivity(), "Moment is added", Toast.LENGTH_SHORT).show();
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Toast.makeText(getActivity(), "Error adding moment", Toast.LENGTH_SHORT).show();
                                                            });
                                                }
                                            });
                                });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getActivity(), "Error uploading image", Toast.LENGTH_SHORT).show();
                            });

                    Toast.makeText(getActivity(), "Photo saved", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private void retrieveMoments() {
        momentsUsers.clear();
        FirebaseFirestore.getInstance().collection("users").document(preferenceManager.getString(Constants.KEY_USER_ID)).collection("followings")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                            FirebaseFirestore.getInstance().collection("users").document(queryDocumentSnapshot.getString("userId"))
                                    .get()
                                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            User user = new User();
                                            user.name = documentSnapshot.getString("name");
                                            user.image = documentSnapshot.getString("image");
                                            user.id = documentSnapshot.getId();
                                            momentsUsers.add(user);
                                            momentsAdapter.notifyDataSetChanged();
                                        }
                                    });
                        }
                        momentsAdapter.notifyDataSetChanged();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void retrievePosts() {
        FirebaseFirestore.getInstance().collection("posts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    posts.clear(); // Clear previous posts
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Post post = createPostFromDocument(documentSnapshot);
                        post.id = documentSnapshot.getId();
                        post.userId = documentSnapshot.getString("userId");
                        posts.add(post);
                    }
                    Collections.shuffle(posts);
                    postAdapter.notifyDataSetChanged();
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Log.e("Home", "Failed to retrieve posts: " + e.getMessage());
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private Post createPostFromDocument(QueryDocumentSnapshot documentSnapshot) {
        Post post = new Post();
        post.hashTag = documentSnapshot.getString("hashTag");
        post.content = documentSnapshot.getString("content");
        List<String> imageUris = (List<String>) documentSnapshot.get("imageUrls");
        if (imageUris != null) {
            post.imageUris = imageUris;
        }
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
                        postAdapter.notifyDataSetChanged();
                    } else {
                        Log.e("Home", "User document does not exist for userId: " + userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Home", "Failed to retrieve user details for userId: " + userId + ", Error: " + e.getMessage());
                });
    }

    private void refreshPosts() {
        swipeRefreshLayout.setRefreshing(true); // Show refreshing animation
        retrievePosts(); // Fetch posts again
        retrieveMoments();
    }

    private void loadUserImage() {
        FirebaseFirestore.getInstance().collection("users").document(preferenceManager.getString(Constants.KEY_USER_ID)).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        byte[] bytes = Base64.decode(documentSnapshot.getString("image"), Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        binding.myImageProfile.setImageBitmap(bitmap);
                    }
                });
        CollectionReference momentsRef = FirebaseFirestore.getInstance().collection("users").document(preferenceManager.getString(Constants.KEY_USER_ID)).collection("moments");
        momentsRef.whereGreaterThan("expirationDate", new Date()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                boolean momentsExist = querySnapshot != null && !querySnapshot.isEmpty();
                isUserMomentExisted = momentsExist;
                if (momentsExist) {
                    binding.myImageProfile.setBorderColor(ContextCompat.getColor(getActivity(), R.color.primary__color));
                    binding.myImageProfile.setBorderWidth(com.intuit.sdp.R.dimen._2sdp);
                }
            }
        });
    }
}
