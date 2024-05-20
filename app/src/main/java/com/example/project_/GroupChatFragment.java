package com.example.project_;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.project_.adapters.GroupChatAdapter;
import com.example.project_.databinding.FragmentGroupChatBinding;
import com.example.project_.models.GroupChat;
import com.example.project_.utilities.Constants;
import com.example.project_.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link GroupChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GroupChatFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    FragmentGroupChatBinding binding;
    private GroupChatAdapter groupChatAdapter;
    private List<GroupChat> groupChats;
    private PreferenceManager preferenceManager;

    public GroupChatFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment GroupChatFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GroupChatFragment newInstance(String param1, String param2) {
        GroupChatFragment fragment = new GroupChatFragment();
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
        binding = FragmentGroupChatBinding.inflate(getLayoutInflater());
        preferenceManager = new PreferenceManager(getActivity());
        groupChats = new ArrayList<>();
        groupChatAdapter = new GroupChatAdapter(groupChats, getActivity());
        binding.groupChatsRecyclerView.setAdapter(groupChatAdapter);
        retrieveGroupChats();
        binding.groupChatsRecyclerView.setVisibility(View.VISIBLE);
        return binding.getRoot();
    }
    private void retrieveGroupChats() {
        FirebaseFirestore.getInstance().collection("groupChats")
                //.whereEqualTo("creatorId", preferenceManager.getString(Constants.KEY_USER_ID))
                .whereArrayContains("members", preferenceManager.getString(Constants.KEY_USER_ID))
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().size() > 0) {
                                groupChats.clear();
                                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                    GroupChat groupChat = new GroupChat();
                                    groupChat.chatName = documentSnapshot.getString("chatName");
                                    groupChat.creatorId = documentSnapshot.getString("creatorId");
                                    groupChat.imageUrls = (List<String>) documentSnapshot.get("imageUrls");
                                    groupChat.members = (List<String>) documentSnapshot.get("members");
                                    groupChat.id = documentSnapshot.getId();
                                    groupChats.add(groupChat);
                                }
                                groupChatAdapter.notifyDataSetChanged();
                                binding.progressBar.setVisibility(View.GONE);
                                if (groupChats.isEmpty() || groupChats == null) {
                                    binding.noMessages.setVisibility(View.VISIBLE);
                                    binding.progressBar.setVisibility(View.INVISIBLE);
                                } else {
                                    binding.noMessages.setVisibility(View.GONE);
                                }
                            }
                        }
                    }
                });
    }
}