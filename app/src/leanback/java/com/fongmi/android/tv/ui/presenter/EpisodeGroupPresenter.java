package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.bean.EpisodeGroup;
import com.fongmi.android.tv.databinding.AdapterPartBinding;
public class EpisodeGroupPresenter extends Presenter {

    private final OnClickListener listener;
    private int nextFocusUp;
    private int nextFocusDown;

    public EpisodeGroupPresenter(OnClickListener listener) {
        this.listener = listener;
    }

    public interface OnClickListener {
        void onItemClick(EpisodeGroup item);
    }

    public void setNextFocusUp(int nextFocusUp) {
        this.nextFocusUp = nextFocusUp;
    }

    public void setNextFocusDown(int nextFocusDown) {
        this.nextFocusDown = nextFocusDown;
    }

    @NonNull
    @Override
    public Presenter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
        return new ViewHolder(AdapterPartBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Presenter.ViewHolder viewHolder, Object object) {
        EpisodeGroup group = (EpisodeGroup) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.text.setText(group.getGroupName());
        holder.binding.text.setActivated(group.isActivated());
        holder.binding.text.setNextFocusUpId(nextFocusUp);
        holder.binding.text.setNextFocusDownId(nextFocusDown);
        setOnClickListener(holder, view -> listener.onItemClick(group));
    }

    @Override
    public void onUnbindViewHolder(@NonNull Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterPartBinding binding;

        public ViewHolder(@NonNull AdapterPartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
