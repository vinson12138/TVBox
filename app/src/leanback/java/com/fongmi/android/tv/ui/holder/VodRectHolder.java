package com.fongmi.android.tv.ui.holder;

import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterVodRectBinding;
import com.fongmi.android.tv.ui.base.BaseVodHolder;
import com.fongmi.android.tv.ui.presenter.VodPresenter;
import com.fongmi.android.tv.utils.ImgUtil;

public class VodRectHolder extends BaseVodHolder {

    private final VodPresenter.OnClickListener listener;
    private final AdapterVodRectBinding binding;

    public VodRectHolder(@NonNull AdapterVodRectBinding binding, VodPresenter.OnClickListener listener) {
        super(binding.getRoot());
        this.binding = binding;
        this.listener = listener;
    }

    public VodRectHolder size(int[] size) {
        binding.image.getLayoutParams().height = size[1];
        binding.getRoot().getLayoutParams().width = size[0];
        return this;
    }

    @Override
    public void initView(Vod item) {
        String score = getScoreText(item);
        String remark = getRemarkText(item, score);
        binding.name.setText(item.getName());
        binding.year.setText(score);
        binding.site.setText(item.getSiteName());
        binding.remark.setText(remark);
        binding.site.setVisibility(item.getSiteVisible());
        binding.year.setVisibility(TextUtils.isEmpty(score) ? View.GONE : View.VISIBLE);
        binding.name.setVisibility(item.getNameVisible());
        binding.remark.setVisibility(TextUtils.isEmpty(remark) ? View.GONE : View.VISIBLE);
        binding.name.setSelected(binding.getRoot().hasFocus());
        binding.getRoot().setOnFocusChangeListener((view, hasFocus) -> {
            binding.name.setSelected(hasFocus);
            applyFocusScale(view, hasFocus);
        });
        binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        binding.getRoot().setOnLongClickListener(v -> listener.onLongClick(item));
        ImgUtil.load(item.getName(), item.getPic(), binding.image);
    }

    @Override
    public void unbind() {
        binding.name.setSelected(false);
        binding.getRoot().setScaleX(1.0f);
        binding.getRoot().setScaleY(1.0f);
        Glide.with(binding.image).clear(binding.image);
    }

    private String getScoreText(Vod item) {
        String score = parseScore(item.getRemarks());
        if (!TextUtils.isEmpty(score)) return score;
        return parseScore(item.getYear());
    }

    private String getRemarkText(Vod item, String score) {
        return TextUtils.isEmpty(score) ? item.getRemarks() : "";
    }

    private String parseScore(String text) {
        if (TextUtils.isEmpty(text)) return "";
        String value = text.trim().replace("评分", "").replace("：", "").replace(":", "").replace("分", "").trim();
        if (value.isEmpty()) return "";
        try {
            float score = Float.parseFloat(value);
            return score >= 0f && score <= 10f ? value : "";
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private void applyFocusScale(View view, boolean hasFocus) {
        view.animate().scaleX(hasFocus ? 1.06f : 1.0f).scaleY(hasFocus ? 1.06f : 1.0f).setDuration(120).start();
    }
}
