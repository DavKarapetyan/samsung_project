package com.example.project_.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.transition.Slide;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.codebyashish.autoimageslider.Enums.ImageAnimationTypes;
import com.codebyashish.autoimageslider.Enums.ImageScaleType;
import com.codebyashish.autoimageslider.ExceptionsClass;
import com.codebyashish.autoimageslider.Models.ImageSlidesModel;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.example.project_.CommentListDialogFragment;
import com.example.project_.R;
import com.example.project_.activities.PostDetailsActivity;
import com.example.project_.activities.UserProfileActivity;
import com.example.project_.databinding.ItemContainerPostBinding;
import com.example.project_.models.Post;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.like.LikeButton;
import com.like.OnLikeListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private final List<Post> posts;
    private FragmentManager fragmentManager;
    private PreferenceManager preferenceManager;
    private static Context context;


    public PostAdapter(List<Post> posts, FragmentManager fragmentManager, PreferenceManager preferenceManager, Context context) {
        this.posts = posts;
        this.fragmentManager = fragmentManager;
        this.preferenceManager = preferenceManager;
        this.context = context;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerPostBinding itemContainerPostBinding = ItemContainerPostBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new PostViewHolder(itemContainerPostBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        try {
            holder.setPostData(posts.get(position));
            if (posts.get(position).content != null) {
                // Call HttpRequestTask to fetch translated text
                Locale locale = Locale.getDefault();
                HttpRequestTask httpRequestTask = new HttpRequestTask(holder.binding);
                httpRequestTask.execute(locale.getLanguage(), posts.get(position).content);
            }
        } catch (ExceptionsClass e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        ItemContainerPostBinding binding;
        int count;

        PostViewHolder(ItemContainerPostBinding itemContainerPostBinding) {
            super(itemContainerPostBinding.getRoot()); // Use the root view of the binding object
            binding = itemContainerPostBinding;
        }

        void setPostData(Post post) throws ExceptionsClass {
            count = 0;
            AtomicBoolean atomicBoolean = new AtomicBoolean();

            DocumentReference userRef = FirebaseFirestore.getInstance().collection("users").document(preferenceManager.getString(Constants.KEY_USER_ID));

            userRef.get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            List<String> savedPostsIds = (List<String>) documentSnapshot.get("savedPosts");
                            if (savedPostsIds != null && savedPostsIds.contains(post.id)) {
                                binding.savePost.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.save));
                                atomicBoolean.set(true);
                            } else {
                                binding.savePost.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.save_off));
                                atomicBoolean.set(false);
                            }
                        }
                    });

            binding.savePost.setOnClickListener(v -> {
                if (atomicBoolean.get()) {
                    userRef.update("savedPosts", FieldValue.arrayRemove(post.id))
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    binding.savePost.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.save_off));
                                    atomicBoolean.set(false);
                                }
                            });
                } else {
                    userRef.update("savedPosts", FieldValue.arrayUnion(post.id))
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    binding.savePost.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.save));
                                    atomicBoolean.set(true);
                                }
                            });
                }
            });

            FirebaseFirestore.getInstance().collection("posts").document(post.id)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            List<String> likedUsers = (List<String>) documentSnapshot.get("likedUsers");
                            if (likedUsers != null && likedUsers.contains(preferenceManager.getString(Constants.KEY_USER_ID))) {
                                binding.likeButton.setLiked(true);
                                count = likedUsers.size();
                                binding.likeCount.setText(Integer.toString(count));
                            } else if (likedUsers == null) {
                                binding.likeButton.setLiked(false);
                                count = 0;
                                binding.likeCount.setText(Integer.toString(count));
                            } else if (likedUsers != null) {
                                binding.likeButton.setLiked(false);
                                count = likedUsers.size();
                                binding.likeCount.setText(Integer.toString(count));
                            }
                        }
                    });
            if (post.imageUris != null && !post.imageUris.isEmpty()) {
                //Picasso.get().load(post.imageUris.get(0)).into(binding.postImage);
                List<SlideModel> imageList = new ArrayList<SlideModel>();

                for (String str : post.imageUris) {
                    imageList.add(new SlideModel(str, null, ScaleTypes.CENTER_CROP));
                }
                binding.postImages.setImageList(imageList);


//                ArrayList<ImageSlidesModel> autoImageList = new ArrayList<>();
//                for (String str : post.imageUris) {
//                    autoImageList.add(new ImageSlidesModel(str, ImageScaleType.CENTER_CROP));
//                }
//                binding.postImages.setImageList(autoImageList);
//                binding.postImages.setSlideAnimation(ImageAnimationTypes.DEPTH_SLIDE);
            }
            binding.fullName.setText(post.userName);
            //String content = new HttpRequestTask().execute(locale.getLanguage(), post.content).toString();
            binding.content.setText(post.content);
            if (post.content.trim().length() < 50) {
                binding.moreText.setVisibility(View.GONE);
            }

            if (post.userImage != null) {
                binding.profileImage.setImageBitmap(getUserImage(post.userImage));
            }
            binding.showComments.setOnClickListener(v -> {
                CommentListDialogFragment commentListDialogFragment = CommentListDialogFragment.newInstance(post.id);
                commentListDialogFragment.show(fragmentManager, "TAG");
            });
            binding.fullName.setOnClickListener(v -> {
                if (!post.userId.equals(preferenceManager.getString(Constants.KEY_USER_ID))) {
                    Intent intent = new Intent(context.getApplicationContext(), UserProfileActivity.class);
                    intent.putExtra("userId", post.userId);
                    context.startActivity(intent);
                }
            });
            binding.likeButton.setOnLikeListener(new OnLikeListener() {
                @Override
                public void liked(LikeButton likeButton) {
                    addLike(post.id);
                    count++;
                    binding.likeCount.setText(Integer.toString(count));
                }

                @Override
                public void unLiked(LikeButton likeButton) {
                    removeLike(post.id);
                    count--;
                    binding.likeCount.setText(Integer.toString(count));
                }
            });
        }

    }

    private Bitmap getUserImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private void addLike(String postId) {
        DocumentReference docRef = FirebaseFirestore.getInstance().collection("posts").document(postId);
        docRef
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            List<String> likedUsers = (List<String>) documentSnapshot.get("likedUsers");
                            if (likedUsers == null) {
                                likedUsers = new ArrayList<>();
                            }

                            likedUsers.add(preferenceManager.getString(Constants.KEY_USER_ID));

                            docRef.update("likedUsers", likedUsers);
                        } else {
                            System.out.println("Error");
                        }
                    }
                });
    }

    private void removeLike(String postId) {
        DocumentReference docRef = FirebaseFirestore.getInstance().collection("posts").document(postId);
        docRef
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            List<String> likedUsers = (List<String>) documentSnapshot.get("likedUsers");
                            if (likedUsers != null) {
                                likedUsers.remove(preferenceManager.getString(Constants.KEY_USER_ID));

                                docRef.update("likedUsers", likedUsers);
                            }
                        } else {
                            System.out.println("Error");
                        }
                    }
                });
    }

    public class HttpRequestTask extends AsyncTask<String, Void, String> {
        private final WeakReference<ItemContainerPostBinding> bindingRef;

        private static final String API_URL = "https://google-translate113.p.rapidapi.com/api/v1/translator/text";
        private static final String API_KEY = "5f01d37347mshc09148e8be0818ap199403jsn9e56a4870367";
        private static final String API_HOST = "google-translate113.p.rapidapi.com";

        public HttpRequestTask(ItemContainerPostBinding bindingRef) {
            this.bindingRef = new WeakReference<>(bindingRef);
        }

        @Override
        protected String doInBackground(String... params) {
            String convertTo = params[0];
            String text = params[1];
            try {
                // Create URL
                URL url = new URL(API_URL);

                // Create connection
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("X-RapidAPI-Key", API_KEY);
                connection.setRequestProperty("X-RapidAPI-Host", API_HOST);
                connection.setDoOutput(true);

                // Build request body
                String requestBody = "from=auto&to=" + convertTo + "&text=" + text;

                // Send request
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(requestBody);
                wr.flush();
                wr.close();

                // Get Response
                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\r');
                }
                rd.close();

                // Extract detected source language from the response, if needed
                // For example, you can parse the JSON response to extract the detected language
                JSONObject jsonResponse = new JSONObject(response.toString());
                String translation = jsonResponse.optString("trans");

                return translation;

            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }


        @Override
        protected void onPostExecute(String result) {
            ItemContainerPostBinding binding = bindingRef.get();
            if (binding != null && result != null) {
                binding.content.setText(result);
            }
        }
    }

}
