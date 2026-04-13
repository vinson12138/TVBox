package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.EpisodePage;
import com.fongmi.android.tv.databinding.DialogEpisodeGridBinding;
import com.fongmi.android.tv.ui.adapter.EpisodeAdapter;
import com.fongmi.android.tv.ui.fragment.EpisodeFragment;
import com.fongmi.android.tv.utils.EpisodeWidth;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class EpisodeGridDialog extends BaseDialog {

    private final List<String> titles;
    private final List<EpisodePage> pages;
    private EpisodeAdapter.OnClickListener listener;
    private DialogEpisodeGridBinding binding;
    private List<Episode> episodes;
    private String vodName;
    private boolean anyTitle;
    private boolean reverse;
    private int spanCount;
    private int pageSize;

    public static EpisodeGridDialog create() {
        return new EpisodeGridDialog();
    }

    public EpisodeGridDialog() {
        this.titles = new ArrayList<>();
        this.pages = new ArrayList<>();
        this.spanCount = 5;
        this.pageSize = 20;
    }

    public EpisodeGridDialog reverse(boolean reverse) {
        this.reverse = reverse;
        return this;
    }

    public EpisodeGridDialog episodes(List<Episode> episodes) {
        this.episodes = episodes;
        return this;
    }

    public EpisodeGridDialog vodName(String vodName) {
        this.vodName = vodName == null ? "" : vodName;
        return this;
    }

    public EpisodeGridDialog anyTitle(boolean anyTitle) {
        this.anyTitle = anyTitle;
        return this;
    }

    public EpisodeGridDialog pageSize(int pageSize) {
        this.pageSize = Math.max(pageSize, 1);
        return this;
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        this.listener = (EpisodeAdapter.OnClickListener) activity;
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogEpisodeGridBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        setSpanCount();
        setTitles();
        setPager();
    }

    @Override
    protected void initEvent() {
        getChildFragmentManager().setFragmentResultListener("result", this, (requestKey, bundle) -> {
            listener.onItemClick(bundle.getParcelable("episode"));
            dismiss();
        });
    }

    private void setSpanCount() {
        int available = ResUtil.getScreenWidth() - ResUtil.dp2px(32);
        int width = EpisodeWidth.measure(episodes, vodName, 14, 11, 10, 28, 24, 18);
        spanCount = Math.max(1, available / Math.max(width, 1));
    }

    private void setTitles() {
        titles.clear();
        pages.clear();
        pages.addAll(EpisodePage.create(episodes, pageSize, reverse));
        for (EpisodePage page : pages) titles.add(page.getTitle());
    }

    private void setPager() {
        binding.pager.setAdapter(new PageAdapter(this));
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(titles.get(position))).attach();
        setCurrentPage();
    }

    private void setCurrentPage() {
        for (int i = 0; i < episodes.size(); i++) {
            if (episodes.get(i).isActivated()) {
                for (int page = 0; page < pages.size(); page++) {
                    EpisodePage item = pages.get(page);
                    if (i >= item.getStartIndex() && i <= item.getEndIndex()) {
                        binding.pager.setCurrentItem(page);
                        return;
                    }
                }
                break;
            }
        }
    }

    class PageAdapter extends FragmentStateAdapter {

        public PageAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            EpisodePage page = pages.get(position);
            return EpisodeFragment.newInstance(spanCount, episodes.subList(page.getStartIndex(), page.getEndIndex() + 1), vodName, anyTitle);
        }

        @Override
        public int getItemCount() {
            return titles.size();
        }
    }
}
