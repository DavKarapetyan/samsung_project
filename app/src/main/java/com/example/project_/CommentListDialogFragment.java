package com.example.project_;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.codebyashish.autoimageslider.ExceptionsClass;
import com.example.project_.models.Comment;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.project_.databinding.FragmentCommentListDialogListDialogItemBinding;
import com.example.project_.databinding.FragmentCommentListDialogListDialogBinding;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>A fragment that shows a list of items as a modal bottom sheet.</p>
 * <p>You can show this modal bottom sheet from your activity like this:</p>
 * <pre>
 *     CommentListDialogFragment.newInstance(30).show(getSupportFragmentManager(), "dialog");
 * </pre>
 */
public class CommentListDialogFragment extends BottomSheetDialogFragment {

    // TODO: Customize parameter argument names
    private static final String ARG_POST_ID = "postId";
    private static final String ARG_USER_PROFILE_IMAGE = "postProfileImage";
    private FragmentCommentListDialogListDialogBinding binding;
    private FirebaseFirestore firebaseFirestore;
    private ItemAdapter itemAdapter;
    private List<Comment> comments;
    private PreferenceManager preferenceManager;

    // TODO: Customize parameters
    public static CommentListDialogFragment newInstance(String postId, String userProfileImage) {
        final CommentListDialogFragment fragment = new CommentListDialogFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_POST_ID, postId);
        args.putString(ARG_USER_PROFILE_IMAGE, userProfileImage);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        firebaseFirestore = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(getActivity());
        binding = FragmentCommentListDialogListDialogBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final RecyclerView recyclerView = (RecyclerView) binding.list;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        Bundle args = getArguments();
        String postId = args != null ? args.getString(ARG_POST_ID) : null;
        String userProfileImage = args != null ? args.getString(ARG_USER_PROFILE_IMAGE) : null;
        comments = new ArrayList<>();
        itemAdapter = new ItemAdapter(comments);
        recyclerView.setAdapter(itemAdapter);
        loading(true);
        retrieveData(postId);
        binding.layoutSend.setOnClickListener(v -> addComment(postId, userProfileImage, preferenceManager.getString(Constants.KEY_USER_ID)));
    }
    private void retrieveData(String postId) {
        binding.list.setVisibility(View.GONE);
        loading(true);
        firebaseFirestore.collection("posts")
                .document(postId)
                .collection("comments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    comments.clear();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Comment comment = new Comment();
                        comment.id = documentSnapshot.getId();
                        comment.content = documentSnapshot.getString("content");
                        comment.userId = documentSnapshot.getString("userId");
                        comment.postId = documentSnapshot.getString("postId");
                        comment.userProfileImage = documentSnapshot.getString("userProfileImage");
                        comments.add(comment);
                        itemAdapter.notifyDataSetChanged();
                        binding.list.setVisibility(View.VISIBLE);
                        loading(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Home", "Failed to retrieve comments: " + e.getMessage());
                });
    }

    public void addComment(String postId, String userProfileImage, String userId) {
        loading(true);
        Comment comment = new Comment();
        comment.userProfileImage = userProfileImage;
        comment.userId = userId;
        comment.postId = postId;
        comment.content = binding.inputMessage.getText().toString();

        CollectionReference commentsRef = firebaseFirestore.collection("posts").document(postId).collection("comments");
        commentsRef.add(comment)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Comment is added", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), e.toString(), Toast.LENGTH_SHORT).show();
                });
        binding.inputMessage.setText(null);
        retrieveData(postId);
        loading(false);
    }

    private void loading(boolean isLoading) {
        if (isLoading) {
            binding.loading.setVisibility(View.VISIBLE);
        } else {
            binding.loading.setVisibility(View.GONE);
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private class ViewHolder extends RecyclerView.ViewHolder {

        //final TextView text;
        FragmentCommentListDialogListDialogItemBinding binding;

        ViewHolder(FragmentCommentListDialogListDialogItemBinding fragmentCommentListDialogListDialogItemBinding) {
            super(fragmentCommentListDialogListDialogItemBinding.getRoot());
            //text = binding.text;
            binding = fragmentCommentListDialogListDialogItemBinding;
        }

        void setCommentData(Comment comment) throws ExceptionsClass {
            binding.textMessage.setText(comment.content);
            if (comment.userProfileImage != null) {
                binding.roundedImageView.setImageBitmap(getUserImage(comment.userProfileImage));
            }
            if (preferenceManager.getString(Constants.KEY_USER_ID).equals(comment.userId)) {
                binding.removeComment.setVisibility(View.VISIBLE);
                binding.removeComment.setOnClickListener(v -> {
                    deleteComment(comment.id, comment.postId);
                    retrieveData(comment.postId);
                });
            } else {
                binding.removeComment.setVisibility(View.GONE);
            }

        }

        private Bitmap getUserImage(String encodedImage) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        private void deleteComment(String commentId, String postId) {
            FirebaseFirestore.getInstance().collection("posts").document(postId).collection("comments").document(commentId).delete()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(getActivity(), "Comment is deleted", Toast.LENGTH_SHORT).show();
                            retrieveData(postId);
                        }
                    });
        }
    }

    private class ItemAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final List<Comment> comments;

        ItemAdapter(List<Comment> comments) {
            this.comments = comments;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            return new ViewHolder(FragmentCommentListDialogListDialogItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            //holder.text.setText(String.valueOf(position));
            try {
                holder.setCommentData(comments.get(position));
            } catch (ExceptionsClass e) {
                throw new RuntimeException(e);
            }

        }

        @Override
        public int getItemCount() {
            return comments.size();
        }
    }
}