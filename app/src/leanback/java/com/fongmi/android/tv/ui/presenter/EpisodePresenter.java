package com.fongmi.android.tv.ui.presenter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.databinding.AdapterEpisodeBinding;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;

public class EpisodePresenter extends Presenter {

    private final OnClickListener listener;
    private int itemWidth;
    private int nextFocusDown;
    private int nextFocusUp;
    private String vodName = "";
    private boolean anyTitle = false;

    public EpisodePresenter(OnClickListener listener) {
        this.listener = listener;
        this.itemWidth = (ResUtil.getScreenWidth() - ResUtil.dp2px(60)) / 4;
    }

    public void setVodName(String name) {
        this.vodName = name == null ? "" : name;
    }

    public void setItemWidth(int width) {
        this.itemWidth = width;
    }

    public void setAnyTitle(boolean anyTitle) {
        this.anyTitle = anyTitle;
    }

    public interface OnClickListener {
        void onItemClick(Episode item);
    }

    public void setNextFocusDown(int nextFocus) {
        this.nextFocusDown = nextFocus;
    }

    public void setNextFocusUp(int nextFocus) {
        this.nextFocusUp = nextFocus;
    }

    @NonNull
    @Override
    public Presenter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
        return new ViewHolder(AdapterEpisodeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Presenter.ViewHolder viewHolder, Object object) {
        Episode item = (Episode) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        ViewGroup.LayoutParams params = holder.view.getLayoutParams();
        if (params != null) {
            params.width = itemWidth;
            holder.view.setLayoutParams(params);
        }
        holder.view.setNextFocusUpId(nextFocusUp);
        holder.view.setNextFocusDownId(nextFocusDown);
        holder.view.setActivated(item.isActivated());
        String numberText = item.getPrimaryText();
        holder.binding.number.setText(numberText);
        String titleText = Util.cleanTitle(item.getName(), vodName);
        boolean hasTitle = !titleText.isEmpty() && !titleText.equals(numberText);
        boolean singleLine = !anyTitle;
        holder.binding.content.setGravity(singleLine ? Gravity.CENTER : Gravity.START);
        holder.binding.number.setGravity(singleLine ? Gravity.CENTER_HORIZONTAL : Gravity.START);
        if (hasTitle) {
            holder.binding.title.setVisibility(View.VISIBLE);
            holder.binding.title.setText(titleText);
        } else if (anyTitle) {
            holder.binding.title.setVisibility(View.INVISIBLE);
        } else {
            holder.binding.title.setVisibility(View.GONE);
        }
        String tagText = item.getVersionKey();
        holder.binding.tag.setVisibility(tagText.isEmpty() ? View.GONE : View.VISIBLE);
        holder.binding.tag.setText(tagText);
        setOnClickListener(holder, view -> listener.onItemClick(item));
    }

    @Override
    public void onUnbindViewHolder(@NonNull Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterEpisodeBinding binding;

        public ViewHolder(@NonNull AdapterEpisodeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
