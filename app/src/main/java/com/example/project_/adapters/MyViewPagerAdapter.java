package com.example.project_.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.project_.GroupChatFragment;
import com.example.project_.SingleChatFragment;

public class MyViewPagerAdapter extends FragmentStateAdapter {
    public MyViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new SingleChatFragment();
            case 1:
                return new GroupChatFragment();
            default:
                return new SingleChatFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
