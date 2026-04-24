package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.widget.TextViewCompat;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.HomeNavItem;
import com.fongmi.android.tv.databinding.AdapterHomeNavBinding;

public class HomeNavPresenter extends Presenter {

    private final OnClickListener listener;

    public HomeNavPresenter(OnClickListener listener) {
        this.listener = listener;
    }

    public interface OnClickListener {
        void onItemClick(HomeNavItem item);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
        return new ViewHolder(AdapterHomeNavBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Presenter.ViewHolder viewHolder, Object object) {
        HomeNavItem item = (HomeNavItem) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.text.setText(item.getText());
        holder.binding.getRoot().setActivated(item.isActivated());
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(holder.binding.text, 0, 0, item.isActivated() && item.hasFilters() ? R.drawable.ic_vod_nav_filter_on : 0, 0);
        setOnClickListener(holder, view -> listener.onItemClick(item));
    }

    @Override
    public void onUnbindViewHolder(@NonNull Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterHomeNavBinding binding;

        public ViewHolder(@NonNull AdapterHomeNavBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
