package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.HomeHistoryItem;
import com.fongmi.android.tv.databinding.AdapterVodBinding;
import com.fongmi.android.tv.utils.ImgUtil;

public class HomeHistoryPresenter extends Presenter {

    private final OnClickListener listener;
    private final int width;
    private final int height;
    private boolean delete;

    public HomeHistoryPresenter(OnClickListener listener, int[] spec) {
        this.listener = listener;
        this.width = spec == null || spec.length < 2 ? 0 : spec[0];
        this.height = spec == null || spec.length < 2 ? 0 : spec[1];
    }

    public interface OnClickListener {

        void onItemClick(HomeHistoryItem item);

        void onItemDelete(History item);

        boolean onLongClick();
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    private void setClickListener(View root, HomeHistoryItem item) {
        root.setOnLongClickListener(view -> item.isMore() ? false : listener.onLongClick());
        root.setOnClickListener(view -> {
            if (item.isMore()) {
                if (!isDelete()) listener.onItemClick(item);
            } else if (isDelete()) {
                listener.onItemDelete(item.getHistory());
            } else {
                listener.onItemClick(item);
            }
        });
    }

    @NonNull
    @Override
    public Presenter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
        ViewHolder holder = new ViewHolder(AdapterVodBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        holder.binding.getRoot().getLayoutParams().width = width;
        holder.binding.image.getLayoutParams().height = height;
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull Presenter.ViewHolder viewHolder, Object object) {
        HomeHistoryItem item = (HomeHistoryItem) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        setClickListener(holder.view, item);
        holder.binding.delete.setVisibility(!delete || item.isMore() ? View.GONE : View.VISIBLE);
        if (item.isMore()) bindMore(holder);
        else bindHistory(holder, item.getHistory());
    }

    private void bindHistory(ViewHolder holder, History item) {
        boolean same = item.getVodName().equals(item.getVodRemarks());
        holder.binding.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        holder.binding.name.setText(item.getVodName());
        holder.binding.site.setText(item.getSiteName());
        holder.binding.remark.setText(item.getVodRemarks());
        holder.binding.site.setVisibility(item.getSiteVisible());
        holder.binding.remark.setVisibility(delete || same ? View.GONE : View.VISIBLE);
        holder.binding.name.setSelected(holder.view.hasFocus());
        holder.view.setOnFocusChangeListener((view, hasFocus) -> {
            holder.binding.name.setSelected(hasFocus);
            applyFocusScale(view, hasFocus);
        });
        ImgUtil.load(item.getVodName(), item.getVodPic(), holder.binding.image);
    }

    private void bindMore(ViewHolder holder) {
        Glide.with(holder.binding.image).clear(holder.binding.image);
        holder.binding.image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        holder.binding.image.setImageResource(R.drawable.ic_setting_history);
        holder.binding.name.setText(R.string.home_more_history);
        holder.binding.site.setVisibility(View.GONE);
        holder.binding.remark.setVisibility(View.VISIBLE);
        holder.binding.remark.setText(R.string.home_history);
    }

    @Override
    public void onUnbindViewHolder(@NonNull Presenter.ViewHolder viewHolder) {
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.name.setSelected(false);
        holder.view.setScaleX(1.0f);
        holder.view.setScaleY(1.0f);
        Glide.with(holder.binding.image).clear(holder.binding.image);
    }

    private void applyFocusScale(View view, boolean hasFocus) {
        view.animate().scaleX(hasFocus ? 1.06f : 1.0f).scaleY(hasFocus ? 1.06f : 1.0f).setDuration(120).start();
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterVodBinding binding;

        public ViewHolder(@NonNull AdapterVodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
