package com.fongmi.android.tv.ui.holder;

import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.databinding.AdapterEpisodeGridBinding;
import com.fongmi.android.tv.ui.adapter.EpisodeAdapter;
import com.fongmi.android.tv.ui.base.BaseEpisodeHolder;

import java.util.function.Supplier;

public class EpisodeGridHolder extends BaseEpisodeHolder {

    private final EpisodeAdapter.OnClickListener listener;
    private final AdapterEpisodeGridBinding binding;
    private final Supplier<String> vodNameProvider;
    private final Supplier<Boolean> anyTitleProvider;

    public EpisodeGridHolder(@NonNull AdapterEpisodeGridBinding binding, EpisodeAdapter.OnClickListener listener) {
        this(binding, listener, () -> "", () -> false);
    }

    public EpisodeGridHolder(@NonNull AdapterEpisodeGridBinding binding, EpisodeAdapter.OnClickListener listener, Supplier<String> vodNameProvider) {
        this(binding, listener, vodNameProvider, () -> false);
    }

    public EpisodeGridHolder(@NonNull AdapterEpisodeGridBinding binding, EpisodeAdapter.OnClickListener listener, Supplier<String> vodNameProvider, Supplier<Boolean> anyTitleProvider) {
        super(binding.getRoot());
        this.binding = binding;
        this.listener = listener;
        this.vodNameProvider = vodNameProvider;
        this.anyTitleProvider = anyTitleProvider;
    }

    @Override
    public void initView(Episode item) {
        binding.getRoot().setActivated(item.isActivated());
        binding.getRoot().setSelected(item.isSelected());
        String numberText = item.getPrimaryText();
        binding.number.setText(numberText);
        String titleText = item.getCleanSubtitle(vodNameProvider.get());
        boolean hasTitle = !titleText.isEmpty() && !titleText.equals(numberText);
        boolean singleLine = !anyTitleProvider.get();
        binding.content.setGravity(singleLine ? Gravity.CENTER : Gravity.START);
        binding.number.setGravity(singleLine ? Gravity.CENTER_HORIZONTAL : Gravity.START);
        if (hasTitle) {
            binding.title.setVisibility(View.VISIBLE);
            binding.title.setText(titleText);
        } else if (anyTitleProvider.get()) {
            binding.title.setVisibility(View.INVISIBLE);
        } else {
            binding.title.setVisibility(View.GONE);
        }
        String tagText = item.getVersionKey();
        binding.tag.setVisibility(tagText.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tag.setText(tagText);
        binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
    }
}
