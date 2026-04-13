package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.databinding.DialogEpisodeListBinding;
import com.fongmi.android.tv.ui.adapter.EpisodeAdapter;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.utils.EpisodeWidth;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.sidesheet.SideSheetDialog;

import java.util.List;

public class EpisodeListDialog implements EpisodeAdapter.OnClickListener {

    private final EpisodeAdapter.OnClickListener listener;
    private final FragmentActivity activity;
    private DialogEpisodeListBinding binding;
    private SideSheetDialog dialog;
    private EpisodeAdapter adapter;
    private List<Episode> episodes;
    private String vodName = "";
    private boolean anyTitle;

    public static EpisodeListDialog create(FragmentActivity activity) {
        return new EpisodeListDialog(activity);
    }

    public EpisodeListDialog(FragmentActivity activity) {
        this.listener = (EpisodeAdapter.OnClickListener) activity;
        this.activity = activity;
    }

    public EpisodeListDialog episodes(List<Episode> episodes) {
        this.episodes = episodes;
        return this;
    }

    public EpisodeListDialog vodName(String vodName) {
        this.vodName = vodName == null ? "" : vodName;
        return this;
    }

    public EpisodeListDialog anyTitle(boolean anyTitle) {
        this.anyTitle = anyTitle;
        return this;
    }

    public void show() {
        initDialog();
        initView();
    }

    private void initDialog() {
        binding = DialogEpisodeListBinding.inflate(LayoutInflater.from(activity));
        dialog = new SideSheetDialog(activity);
        dialog.setContentView(binding.getRoot());
        dialog.getBehavior().setDraggable(false);
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
        setWidth();
    }

    private void setWidth() {
        int minWidth = ResUtil.dp2px(200);
        int maxWidth = ResUtil.getScreenWidth() / 3;
        for (Episode item : episodes) minWidth = Math.max(minWidth, getItemWidth(item));
        FrameLayout sheet = dialog.findViewById(com.google.android.material.R.id.m3_side_sheet);
        ViewGroup.LayoutParams params = sheet.getLayoutParams();
        params.width = Math.min(minWidth, maxWidth);
        sheet.setLayoutParams(params);
    }

    private int getItemWidth(Episode item) {
        return EpisodeWidth.measure(item, vodName, 14, 11, 10, 28, 24, 18);
    }

    private void initView() {
        setRecyclerView();
        setEpisode();
    }

    private void setRecyclerView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setLayoutManager(new LinearLayoutManager(activity));
        binding.recycler.setAdapter(adapter = new EpisodeAdapter(this, ViewType.GRID));
    }

    private void setEpisode() {
        adapter.setVodName(vodName);
        adapter.setAnyTitle(anyTitle);
        adapter.addAll(episodes);
        binding.recycler.scrollToPosition(adapter.getPosition());
    }

    @Override
    public void onItemClick(Episode item) {
        listener.onItemClick(item);
        dialog.dismiss();
    }
}
