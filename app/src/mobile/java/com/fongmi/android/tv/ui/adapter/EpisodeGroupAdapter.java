package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.EpisodeGroup;
import com.fongmi.android.tv.databinding.AdapterFlagBinding;

import java.util.ArrayList;
import java.util.List;

public class EpisodeGroupAdapter extends RecyclerView.Adapter<EpisodeGroupAdapter.ViewHolder> {

    private final OnClickListener listener;
    private final List<EpisodeGroup> mItems;
    private int mActiveIndex;

    public EpisodeGroupAdapter(OnClickListener listener) {
        this.listener = listener;
        this.mItems = new ArrayList<>();
        this.mActiveIndex = 0;
    }

    public interface OnClickListener {
        void onItemClick(EpisodeGroup item);
    }

    public void addAll(List<EpisodeGroup> items) {
        mItems.clear();
        mItems.addAll(items);
        mActiveIndex = 0;
        notifyDataSetChanged();
    }

    public void setActivated(EpisodeGroup group) {
        int index = mItems.indexOf(group);
        if (index >= 0) {
            mActiveIndex = index;
            notifyDataSetChanged();
        }
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterFlagBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EpisodeGroup item = mItems.get(position);
        holder.binding.text.setText(item.getGroupName());
        holder.binding.text.setActivated(position == mActiveIndex);
        holder.binding.text.setOnClickListener(v -> listener.onItemClick(item));
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterFlagBinding binding;

        ViewHolder(@NonNull AdapterFlagBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
