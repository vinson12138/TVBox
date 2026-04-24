package com.fongmi.android.tv.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.core.splashscreen.SplashScreen;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.bean.Cache;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Filter;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.HomeHistoryItem;
import com.fongmi.android.tv.bean.HomeNavItem;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.bean.Value;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.bean.Word;
import com.fongmi.android.tv.databinding.ActivityHomeBinding;
import com.fongmi.android.tv.databinding.ViewHomeKeepBinding;
import com.fongmi.android.tv.databinding.ViewHomeSearchBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.CastEvent;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.player.exo.CacheManager;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.ui.adapter.BaseDiffCallback;
import com.fongmi.android.tv.ui.adapter.KeepAdapter;
import com.fongmi.android.tv.ui.adapter.RecordAdapter;
import com.fongmi.android.tv.ui.adapter.WordAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.CustomKeyboard;
import com.fongmi.android.tv.ui.custom.CustomRowPresenter;
import com.fongmi.android.tv.ui.custom.CustomScroller;
import com.fongmi.android.tv.ui.custom.CustomSelector;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.fongmi.android.tv.ui.custom.CustomTitleView;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.ui.presenter.HeaderPresenter;
import com.fongmi.android.tv.ui.presenter.FilterPresenter;
import com.fongmi.android.tv.ui.presenter.HomeHistoryPresenter;
import com.fongmi.android.tv.ui.presenter.HomeNavPresenter;
import com.fongmi.android.tv.ui.presenter.VodPresenter;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Task;
import com.fongmi.android.tv.utils.Util;
import com.fongmi.android.tv.utils.UrlUtil;
import com.fongmi.android.tv.utils.ZhuToPin;
import com.github.catvod.net.OkHttp;
import com.google.common.collect.Lists;
import com.google.common.net.HttpHeaders;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Response;

public class HomeActivity extends BaseActivity implements SiteCallback, CustomTitleView.Listener, VodPresenter.OnClickListener, HomeNavPresenter.OnClickListener, HomeHistoryPresenter.OnClickListener {

    private static final Style HOME_ROW_STYLE = new Style("rect", 1.33f);
    private static final int HOME_HISTORY_LIMIT = 4;
    private static final int HOME_SECTION_LIMIT = 12;

    private ActivityHomeBinding mBinding;
    private ViewHomeSearchBinding mSearchBinding;
    private ViewHomeKeepBinding mKeepBinding;
    private ArrayObjectAdapter mNavAdapter;
    private ArrayObjectAdapter mAdapter;
    private ArrayObjectAdapter mRecentAdapter;
    private ArrayObjectAdapter mCategoryLastAdapter;
    private int[] mHomeRowSpec;
    private HomeHistoryPresenter mHistoryPresenter;
    private SiteViewModel mViewModel;
    private RecordAdapter mRecordAdapter;
    private WordAdapter mWordAdapter;
    private KeepAdapter mKeepAdapter;
    private CustomScroller mCategoryScroller;
    private Result mHomeResult;
    private Result mCategoryResult;
    private Clock mClock;
    private HomeNavItem mActiveNavItem;
    private ConnectivityManager mConnectivityManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private boolean mNetworkRegistered;
    private int mSectionToken;
    private int mPendingSections;

    private final Map<HomeSection, com.fongmi.android.tv.bean.Class> mSectionTypes = new EnumMap<>(HomeSection.class);
    private final Map<HomeSection, List<Vod>> mSectionVideos = new EnumMap<>(HomeSection.class);
    private final HashMap<String, String> mCategoryExtends = new HashMap<>();
    private final List<Vod> mCategoryItems = new ArrayList<>();
    private List<Filter> mCategoryFilters = new ArrayList<>();
    private boolean mCategoryFilterVisible;
    private boolean mCategoryLoadingMore;
    private boolean mCategoryContentOnlyRefresh;

    private enum ContentMode {HOME, CATEGORY, SEARCH, KEEP}

    private ContentMode mMode = ContentMode.HOME;

    private enum HomeSection {
        SERIES(R.string.home_latest_series, "剧集", "电视剧", "连续剧", "短剧", "劇集", "電視劇", "連續劇", "短劇"),
        ANIME(R.string.home_latest_anime, "动漫", "动画", "国漫", "日漫", "番剧", "動漫", "動畫", "番劇"),
        MOVIE(R.string.home_latest_movie, "电影", "電影"),
        VARIETY(R.string.home_latest_variety, "综艺", "綜藝");

        private final int titleRes;
        private final List<String> keywords;

        HomeSection(int titleRes, String... keywords) {
            this.titleRes = titleRes;
            this.keywords = Arrays.asList(keywords);
        }

        public int getTitleRes() {
            return titleRes;
        }

        public boolean matches(String name) {
            String target = normalize(name);
            for (String keyword : keywords) if (target.contains(normalize(keyword))) return true;
            return false;
        }

        private static String normalize(String value) {
            return value == null ? "" : value.replace(" ", "").toLowerCase();
        }
    }

    private Site getHome() {
        return VodConfig.get().getHome();
    }

    private Config getConfig() {
        return VodConfig.get().getConfig();
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkAction(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initView() {
        mHomeResult = Result.empty();
        mCategoryResult = Result.empty();
        mClock = Clock.create(mBinding.clock);
        mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        Updater.create().start(this);
        setNavRecyclerView();
        setContentRecyclerView();
        setPanels();
        setViewModel();
        initConfig();
        setTitle();
        setLogo();
        updateNetworkIcon();
        mBinding.progressLayout.showProgress();
    }

    @Override
    protected void initEvent() {
        mBinding.title.setListener(this);
        mBinding.actionSetting.setOnClickListener(view -> SettingActivity.start(this));
        mBinding.nav.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                HomeNavItem item = getNavItem(position);
                if (item != null && item.supportsContent() && !item.equals(mActiveNavItem)) activateNav(item, false);
            }
        });
        mBinding.recycler.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (mHistoryPresenter != null && mHistoryPresenter.isDelete()) setHistoryDelete(false);
            }
        });
    }

    private void checkAction(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            VideoActivity.push(this, intent.getStringExtra(Intent.EXTRA_TEXT));
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            PermissionUtil.requestFile(this, allGranted -> checkType(intent));
        }
    }

    private void checkType(Intent intent) {
        if ("text/plain".equals(intent.getType()) || UrlUtil.path(intent.getData()).endsWith(".m3u")) {
            loadLive("file:/" + FileChooser.getPathFromUri(intent.getData()));
        } else {
            VideoActivity.push(this, intent.getData().toString());
        }
    }

    @SuppressLint("RestrictedApi")
    private void setNavRecyclerView() {
        mBinding.nav.setAdapter(new ItemBridgeAdapter(mNavAdapter = new ArrayObjectAdapter(new HomeNavPresenter(this))));
        mBinding.nav.setVerticalSpacing(ResUtil.dp2px(8));
    }

    @SuppressLint("RestrictedApi")
    private void setContentRecyclerView() {
        mHomeRowSpec = getHomeGridSpec(HOME_ROW_STYLE);
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(String.class, new HeaderPresenter());
        selector.addPresenter(Vod.class, new VodPresenter(this, Style.list()));
        selector.addPresenter(ListRow.class, new CustomRowPresenter(12, FocusHighlight.ZOOM_FACTOR_SMALL, HorizontalGridView.FOCUS_SCROLL_ITEM, 6, 4), VodPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(6, FocusHighlight.ZOOM_FACTOR_NONE, HorizontalGridView.FOCUS_SCROLL_ALIGNED, 4, 2), FilterPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(12, FocusHighlight.ZOOM_FACTOR_SMALL, HorizontalGridView.FOCUS_SCROLL_ALIGNED, 6, 4), HomeHistoryPresenter.class);
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(10));
        mBinding.recycler.addOnScrollListener(mCategoryScroller = new CustomScroller(this::onCategoryLoadMore));
    }

    private void setPanels() {
        mSearchBinding = ViewHomeSearchBinding.bind(findViewById(R.id.searchPanelRoot));
        mKeepBinding = ViewHomeKeepBinding.bind(findViewById(R.id.keepPanelRoot));
        setSearchPanel();
        setKeepPanel();
        showMode(ContentMode.HOME);
    }

    private void setSearchPanel() {
        CustomKeyboard.init(new CustomKeyboard.Callback() {
            @Override
            public void showDialog() {
                HomeActivity.this.showDialog();
            }

            @Override
            public void onRemote() {
                PushActivity.start(HomeActivity.this, 1);
            }

            @Override
            public void onSearch() {
                searchFromPanel();
            }
        }, mSearchBinding.homeSearchKeyboard, mSearchBinding.homeSearchKeyword);
        mSearchBinding.homeSearchWordRecycler.setItemAnimator(null);
        mSearchBinding.homeSearchWordRecycler.setHasFixedSize(false);
        mSearchBinding.homeSearchWordRecycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        mSearchBinding.homeSearchWordRecycler.setAdapter(mWordAdapter = new WordAdapter(this::setSearchKeyword));
        mSearchBinding.homeSearchRecordRecycler.setHasFixedSize(false);
        mSearchBinding.homeSearchRecordRecycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        mSearchBinding.homeSearchRecordRecycler.setAdapter(mRecordAdapter = new RecordAdapter(new RecordAdapter.OnClickListener() {
            @Override
            public void onItemClick(String text) {
                setSearchKeyword(text);
            }

            @Override
            public void onDataChanged(int size) {
                mSearchBinding.homeSearchRecordLayout.setVisibility(size == 0 ? View.GONE : View.VISIBLE);
            }
        }));
        mSearchBinding.homeSearchKeyword.setOnEditorActionListener((textView, actionId, event) -> {
            searchFromPanel();
            return true;
        });
        mSearchBinding.homeSearchKeyword.addTextChangedListener(new CustomTextListener() {
            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (s.toString().isEmpty()) loadSearchHot();
                else loadSearchSuggest(s.toString());
            }
        });
        mSearchBinding.homeSearchMic.setOnClickListener(v -> mSearchBinding.homeSearchMic.start());
        mSearchBinding.homeSearchMic.setListener(this, new CustomTextListener() {
            @Override
            public void onEndOfSpeech() {
                mSearchBinding.homeSearchKeyword.requestFocus();
                mSearchBinding.homeSearchMic.stop();
            }

            @Override
            public void onResults(String result) {
                mSearchBinding.homeSearchKeyword.setText(result);
                mSearchBinding.homeSearchKeyword.setSelection(mSearchBinding.homeSearchKeyword.length());
            }
        });
        loadSearchHot();
    }

    private void setKeepPanel() {
        int contentWidth = ResUtil.getScreenWidth() - ResUtil.dp2px(12 + 120 + 6 + 8 + 8);
        mKeepBinding.homeKeepRecycler.setHasFixedSize(true);
        mKeepBinding.homeKeepRecycler.setItemAnimator(null);
        mKeepBinding.homeKeepRecycler.setAdapter(mKeepAdapter = new KeepAdapter(new KeepAdapter.OnClickListener() {
            @Override
            public void onItemClick(Keep item) {
                openKeepItem(item);
            }

            @Override
            public void onItemDelete(Keep item) {
                mKeepAdapter.remove(item.delete(), () -> {
                    if (mKeepAdapter.getItemCount() == 0) mKeepAdapter.setDelete(false);
                    mKeepBinding.getRoot().showContent(true, mKeepAdapter.getItemCount());
                });
            }

            @Override
            public boolean onLongClick() {
                mKeepAdapter.setDelete(true);
                return true;
            }
        }, contentWidth));
        mKeepBinding.homeKeepRecycler.setLayoutManager(new GridLayoutManager(this, Product.getColumn()));
        mKeepBinding.homeKeepRecycler.addItemDecoration(new SpaceItemDecoration(Product.getColumn(), 16));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.getResult().observe(this, result -> {
            if (result == null) return;
            if (isCategoryMode()) renderCategoryContent(result);
            else if (isHomeMode()) renderHomeResult(result);
        });
        mViewModel.getAction().observe(this, result -> Notify.show(result.getMsg()));
    }

    private void initConfig() {
        VodConfig.get().init().load(getCallback());
        LiveConfig.get().init().load();
        WallConfig.get().init();
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                showContent();
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
                showContent();
            }
        };
    }

    private void showContent() {
        checkAction(getIntent());
        setFocus();
    }

    private void loadLive(String url) {
        LiveConfig.load(Config.find(url, 1), new Callback() {
            @Override
            public void success() {
                LiveActivity.start(getActivity());
            }
        });
    }

    private void setFocus() {
        mBinding.title.setSelected(true);
        App.post(() -> {
            mBinding.title.setFocusable(true);
            if (!mBinding.nav.hasFocus() && !mBinding.title.hasFocus() && !mBinding.actionSetting.hasFocus()) mBinding.nav.requestFocus();
        }, 300);
    }

    private void setTitle() {
        List<String> items = Arrays.asList(getHome().getName(), getConfig().getName(), getString(R.string.app_name));
        Optional<String> optional = items.stream().filter(s -> !TextUtils.isEmpty(s)).findFirst();
        optional.ifPresent(s -> mBinding.title.setText(s));
    }

    private void showMode(ContentMode mode) {
        mMode = mode;
        applyModeVisibility();
    }

    private void applyModeVisibility() {
        boolean showRecycler = mMode == ContentMode.HOME || mMode == ContentMode.CATEGORY;
        mBinding.recycler.setVisibility(showRecycler ? View.VISIBLE : View.GONE);
        mSearchBinding.getRoot().setVisibility(mMode == ContentMode.SEARCH ? View.VISIBLE : View.GONE);
        mKeepBinding.getRoot().setVisibility(mMode == ContentMode.KEEP ? View.VISIBLE : View.GONE);
    }

    private void showMainContent() {
        mBinding.progressLayout.showContent();
        applyModeVisibility();
    }

    private void showSearchContent() {
        showMode(ContentMode.SEARCH);
        showMainContent();
        loadSearchHot();
    }

    private void showKeepContent() {
        showMode(ContentMode.KEEP);
        showMainContent();
        renderKeepContent();
    }

    private void renderKeepContent() {
        mKeepBinding.getRoot().showProgress();
        mKeepAdapter.setItems(Keep.getVod(), () -> mKeepBinding.getRoot().showContent(true, mKeepAdapter.getItemCount()));
    }

    private void openKeepItem(Keep item) {
        Config config = Config.find(item.getCid());
        if (config == null) CollectActivity.start(this, item.getVodName());
        else if (item.getCid() != VodConfig.getCid()) {
            VodConfig.load(config, new Callback() {
                @Override
                public void success() {
                    VideoActivity.start(getActivity(), item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
                }

                @Override
                public void error(String msg) {
                    Notify.show(msg);
                }
            });
        } else {
            VideoActivity.start(this, item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
        }
    }

    private void setSearchKeyword(String text) {
        mSearchBinding.homeSearchKeyword.setText(text);
        mSearchBinding.homeSearchKeyword.setSelection(mSearchBinding.homeSearchKeyword.length());
        searchFromPanel();
    }

    private void searchFromPanel() {
        String keyword = mSearchBinding.homeSearchKeyword.getText().toString().trim();
        mSearchBinding.homeSearchKeyword.setSelection(mSearchBinding.homeSearchKeyword.length());
        Util.hideKeyboard(mSearchBinding.homeSearchKeyword);
        if (TextUtils.isEmpty(keyword)) return;
        CollectActivity.start(this, keyword);
        App.post(() -> mRecordAdapter.add(keyword), 250);
    }

    private void loadSearchHot() {
        mSearchBinding.homeSearchWord.setText(R.string.search_hot);
        mWordAdapter.setItems(Word.objectFrom(Setting.getHot()).getData());
        OkHttp.newCall("https://api.web.360kan.com/v1/rank?cat=1", Map.of(HttpHeaders.REFERER, "https://www.360kan.com/rank/general")).enqueue(getSearchCallback(true));
    }

    private void loadSearchSuggest(String text) {
        mSearchBinding.homeSearchWord.setText(R.string.search_suggest);
        OkHttp.newCall("https://suggest.video.iqiyi.com/?if=mobile&key=" + URLEncoder.encode(ZhuToPin.get(text))).enqueue(getSearchCallback(false));
    }

    private Callback getSearchCallback(boolean hot) {
        return new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String result = response.body().string();
                if (TextUtils.isEmpty(result)) return;
                App.post(() -> setSearchWords(result, hot));
            }
        };
    }

    private void setSearchWords(String result, boolean save) {
        if (!save && mSearchBinding.homeSearchKeyword.getText().toString().trim().isEmpty()) return;
        mWordAdapter.setItems(Word.objectFrom(result).getData(), () -> mSearchBinding.homeSearchWordRecycler.scrollToPosition(0));
        if (save) Setting.putHot(result);
    }

    private void loadHomeContent() {
        showMode(ContentMode.HOME);
        clearCategoryState();
        mSectionToken++;
        mPendingSections = 0;
        mSectionTypes.clear();
        mSectionVideos.clear();
        mAdapter.clear();
        mBinding.progressLayout.showProgress();
        mViewModel.homeContent();
    }

    private void loadCategoryContent(com.fongmi.android.tv.bean.Class type) {
        showMode(ContentMode.CATEGORY);
        mSectionToken++;
        mPendingSections = 0;
        mCategoryResult = Result.empty();
        mCategoryItems.clear();
        mCategoryLastAdapter = null;
        mCategoryLoadingMore = false;
        mCategoryScroller.reset();
        if (!mCategoryContentOnlyRefresh) {
            mAdapter.clear();
            mBinding.progressLayout.showProgress();
        }
        mViewModel.categoryContent(getHome().getKey(), type.getTypeId(), "1", true, new HashMap<>(mCategoryExtends));
    }

    private void renderHomeResult(Result result) {
        mHomeResult = result;
        Cache.clear().put(result);
        mSectionTypes.clear();
        mSectionTypes.putAll(mapSectionTypes(result.getTypes()));
        updateNavItems(result.getTypes());
        boolean hasContent = renderHomeContent();
        fetchHomeSections();
        if (!hasContent && !mSectionTypes.isEmpty()) mBinding.progressLayout.showProgress();
    }

    private void fetchHomeSections() {
        mSectionVideos.clear();
        mPendingSections = mSectionTypes.size();
        if (mPendingSections == 0) {
            if (!renderHomeContent()) mBinding.progressLayout.showEmpty();
            return;
        }
        int token = ++mSectionToken;
        for (Map.Entry<HomeSection, com.fongmi.android.tv.bean.Class> entry : mSectionTypes.entrySet()) {
            Task.execute(() -> {
                List<Vod> items = getCategoryItems(entry.getValue().getTypeId());
                App.post(() -> {
                    if (token != mSectionToken || !isHomeMode()) return;
                    mSectionVideos.put(entry.getKey(), items);
                    mPendingSections--;
                    boolean hasContent = renderHomeContent();
                    if (!hasContent && mPendingSections == 0) mBinding.progressLayout.showEmpty();
                });
            });
        }
    }

    private List<Vod> getCategoryItems(String typeId) {
        try {
            Result result = fetchCategoryResult(typeId);
            List<Vod> items = result.getList();
            if (items.isEmpty()) return new ArrayList<>();
            return new ArrayList<>(items.subList(0, Math.min(items.size(), HOME_SECTION_LIMIT)));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Result fetchCategoryResult(String typeId) throws Exception {
        Site site = getHome();
        if (site.getType() == 3) {
            String content = site.recent().spider().categoryContent(typeId, "1", true, new HashMap<>());
            return Result.fromJson(content);
        } else {
            ArrayMap<String, String> params = new ArrayMap<>();
            params.put("ac", site.getType() == 0 ? "videolist" : "detail");
            params.put("t", typeId);
            params.put("pg", "1");
            return Result.fromType(site.getType(), call(site, params));
        }
    }

    private String call(Site site, ArrayMap<String, String> params) throws IOException {
        if (!site.getExt().isEmpty()) params.put("extend", site.getExt());
        Call call = site.getExt().length() <= 1000 ? OkHttp.newCall(site.getApi(), site.getHeader(), params) : OkHttp.newCall(site.getApi(), site.getHeader(), OkHttp.toBody(params));
        try (Response response = call.execute()) {
            return Objects.requireNonNull(response.body()).string();
        }
    }

    private boolean renderHomeContent() {
        mAdapter.clear();
        boolean hasContent = addHistoryRow();
        for (HomeSection section : HomeSection.values()) {
            List<Vod> items = mSectionVideos.get(section);
            if (items == null || items.isEmpty()) continue;
            mAdapter.add(getString(section.getTitleRes()));
            addGrid(items, HOME_ROW_STYLE, getHomeGridSpec(HOME_ROW_STYLE));
            hasContent = true;
        }
        if (!hasContent && mSectionTypes.isEmpty() && !mHomeResult.getList().isEmpty()) {
            mAdapter.add(getString(R.string.home_recommend));
            addGrid(limit(mHomeResult.getList()), HOME_ROW_STYLE, getHomeGridSpec(HOME_ROW_STYLE));
            hasContent = true;
        }
        if (hasContent) showMainContent();
        return hasContent;
    }

    private void renderCategoryContent(Result result) {
        mCategoryResult = result;
        HomeNavItem item = mActiveNavItem;
        if (item == null || !item.isCategory()) return;
        Style style = item != null && item.isCategory() ? result.getStyle(item.getType().getStyle()) : result.getStyle(getHome().getStyle(HOME_ROW_STYLE));
        if (mCategoryLoadingMore) {
            mCategoryScroller.endLoading(result);
            mCategoryLoadingMore = false;
            if (result.getList().isEmpty()) return;
            mCategoryItems.addAll(result.getList());
            appendCategoryItems(result.getList(), style);
            showMainContent();
            return;
        }
        mCategoryItems.clear();
        mCategoryItems.addAll(result.getList());
        mCategoryScroller.reset();
        mCategoryScroller.endLoading(result);
        if (mCategoryContentOnlyRefresh) {
            clearCategoryContentRows();
        } else {
            renderCategoryChrome();
        }
        mCategoryContentOnlyRefresh = false;
        mCategoryLastAdapter = null;
        if (mCategoryItems.isEmpty()) {
            clearCategoryContentRows();
            if (mCategoryFilterVisible || !mCategoryExtends.isEmpty()) showMainContent();
            else mBinding.progressLayout.showEmpty();
            return;
        }
        appendCategoryItems(mCategoryItems, style);
        showMainContent();
    }

    private boolean addHistoryRow() {
        List<HomeHistoryItem> items = getHomeHistoryItems();
        if (items.isEmpty()) {
            mRecentAdapter = null;
            return false;
        }
        mAdapter.add(getString(R.string.home_history));
        mRecentAdapter = new ArrayObjectAdapter(mHistoryPresenter = new HomeHistoryPresenter(this, mHomeRowSpec));
        mRecentAdapter.setItems(items, new BaseDiffCallback<HomeHistoryItem>());
        mAdapter.add(new ListRow(mRecentAdapter));
        return true;
    }

    private List<HomeHistoryItem> getHomeHistoryItems() {
        List<History> histories = History.get();
        if (histories.isEmpty()) return new ArrayList<>();
        List<HomeHistoryItem> items = new ArrayList<>();
        histories.stream().limit(HOME_HISTORY_LIMIT).forEach(item -> items.add(HomeHistoryItem.create(item)));
        if (histories.size() > HOME_HISTORY_LIMIT) items.add(HomeHistoryItem.more());
        return items;
    }

    private List<Vod> limit(List<Vod> items) {
        return new ArrayList<>(items.subList(0, Math.min(items.size(), HOME_SECTION_LIMIT)));
    }

    private void addGrid(List<Vod> items, Style style) {
        addGrid(items, style, null);
    }

    private void addGrid(List<Vod> items, Style style, @Nullable int[] size) {
        for (List<Vod> part : Lists.partition(items, Math.max(1, getColumn(style)))) {
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(size == null ? new VodPresenter(this, style) : new VodPresenter(this, style, size, true));
            adapter.setItems(part, new BaseDiffCallback<Vod>());
            mAdapter.add(new ListRow(adapter));
        }
    }

    private int getColumn(Style style) {
        return Product.getColumn(style);
    }

    @Nullable
    private int[] getHomeGridSpec(Style style) {
        if (style.isList()) return null;
        int column = Math.max(1, getColumn(style));
        int contentPadding = ResUtil.dp2px(16 + 16 + 120 + 6 + 8 + 8);
        int rowPadding = ResUtil.dp2px(6 * 2);
        int rowSpacing = ResUtil.dp2px(12 * Math.max(0, column - 1));
        int available = ResUtil.getScreenWidth() - contentPadding - rowPadding - rowSpacing;
        int width = Math.max(1, available / column);
        int height = (int) (width / style.getRatio());
        return new int[]{width, height};
    }

    private void clearCategoryState() {
        mCategoryResult = Result.empty();
        mCategoryExtends.clear();
        mCategoryItems.clear();
        mCategoryFilters = new ArrayList<>();
        mCategoryFilterVisible = false;
        mCategoryLastAdapter = null;
        mCategoryLoadingMore = false;
        mCategoryContentOnlyRefresh = false;
        if (mCategoryScroller != null) mCategoryScroller.reset();
    }

    private void prepareCategoryState(@NonNull com.fongmi.android.tv.bean.Class type) {
        mCategoryResult = Result.empty();
        mCategoryExtends.clear();
        mCategoryFilters = Cache.copy(type.getTypeId());
        if (mCategoryFilters.isEmpty() && !type.getFilters().isEmpty()) mCategoryFilters = type.getFilters().stream().map(Filter::copy).toList();
        applyCategoryFilters();
        mCategoryFilterVisible = false;
    }

    private void applyCategoryFilters() {
        for (Filter filter : mCategoryFilters) {
            String active = mCategoryExtends.get(filter.getKey());
            for (Value value : filter.getValue()) value.setActivated(active != null && value.equals(Value.create(active)));
        }
    }

    private void addCategoryFilters() {
        applyCategoryFilters();
        for (Filter filter : mCategoryFilters) mAdapter.add(getFilterRow(filter));
    }

    private void renderCategoryChrome() {
        mAdapter.clear();
        if (mActiveNavItem != null) mAdapter.add(mActiveNavItem.getText());
        if (mActiveNavItem != null && mActiveNavItem.isCategory() && mCategoryFilterVisible) addCategoryFilters();
    }

    private void rebuildCategoryContent() {
        if (mActiveNavItem == null || !mActiveNavItem.isCategory()) return;
        renderCategoryChrome();
        mCategoryLastAdapter = null;
        if (mCategoryItems.isEmpty()) {
            if (mCategoryFilterVisible || !mCategoryExtends.isEmpty()) showMainContent();
            else mBinding.progressLayout.showEmpty();
            return;
        }
        Style style = mCategoryResult.getStyle(mActiveNavItem.getType().getStyle());
        appendCategoryItems(mCategoryItems, style);
        showMainContent();
    }

    private int getCategoryChromeSize() {
        int size = mActiveNavItem == null ? 0 : 1;
        if (mActiveNavItem != null && mActiveNavItem.isCategory() && mCategoryFilterVisible) size += mCategoryFilters.size();
        return size;
    }

    private void clearCategoryContentRows() {
        int start = getCategoryChromeSize();
        if (mAdapter.size() > start) mAdapter.removeItems(start, mAdapter.size() - start);
    }

    private void appendCategoryItems(List<Vod> items, Style style) {
        if (items.isEmpty()) return;
        if (style.isList()) {
            mAdapter.addAll(mAdapter.size(), items);
        } else {
            addCategoryGrid(items, style, getHomeGridSpec(style));
        }
    }

    private boolean checkCategoryLastSize(List<Vod> items, Style style, @Nullable int[] size) {
        if (mCategoryLastAdapter == null || items.isEmpty()) return false;
        int count = getColumn(style) - mCategoryLastAdapter.size();
        if (count == 0) return false;
        count = Math.min(count, items.size());
        mCategoryLastAdapter.addAll(mCategoryLastAdapter.size(), new ArrayList<>(items.subList(0, count)));
        addCategoryGrid(new ArrayList<>(items.subList(count, items.size())), style, size);
        return true;
    }

    private void addCategoryGrid(List<Vod> items, Style style, @Nullable int[] size) {
        if (checkCategoryLastSize(items, style, size)) return;
        for (List<Vod> part : Lists.partition(items, Math.max(1, getColumn(style)))) {
            mCategoryLastAdapter = new ArrayObjectAdapter(size == null ? new VodPresenter(this, style) : new VodPresenter(this, style, size, true));
            mCategoryLastAdapter.setItems(part, new BaseDiffCallback<Vod>());
            mAdapter.add(new ListRow(mCategoryLastAdapter));
        }
    }

    private void onCategoryLoadMore(String page) {
        if (!isCategoryMode() || mActiveNavItem == null || !mActiveNavItem.isCategory()) return;
        mCategoryLoadingMore = true;
        mCategoryScroller.setLoading(true);
        mViewModel.categoryContent(getHome().getKey(), mActiveNavItem.getType().getTypeId(), page, true, new HashMap<>(mCategoryExtends));
    }

    private ListRow getFilterRow(Filter filter) {
        FilterPresenter presenter = new FilterPresenter(filter.getKey());
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenter);
        presenter.setOnClickListener((key, item) -> setCategoryFilter(adapter, key, item));
        adapter.setItems(filter.getValue(), null);
        return new ListRow(adapter);
    }

    private void setCategoryFilter(ArrayObjectAdapter adapter, String key, Value item) {
        for (int i = 0; i < adapter.size(); i++) ((Value) adapter.get(i)).setActivated(item);
        adapter.notifyArrayItemRangeChanged(0, adapter.size());
        if (item.isActivated()) mCategoryExtends.put(key, item.getV());
        else mCategoryExtends.remove(key);
        if (mActiveNavItem != null && mActiveNavItem.isCategory()) {
            mCategoryContentOnlyRefresh = true;
            loadCategoryContent(mActiveNavItem.getType());
        }
    }

    private boolean hasCategoryFilters(@Nullable HomeNavItem item) {
        return item != null && item.isCategory() && !mCategoryFilters.isEmpty();
    }

    private boolean toggleCategoryFilters() {
        if (!hasCategoryFilters(mActiveNavItem)) return false;
        mCategoryFilterVisible = !mCategoryFilterVisible;
        rebuildCategoryContent();
        return true;
    }

    private Map<HomeSection, com.fongmi.android.tv.bean.Class> mapSectionTypes(List<com.fongmi.android.tv.bean.Class> types) {
        Map<HomeSection, com.fongmi.android.tv.bean.Class> map = new EnumMap<>(HomeSection.class);
        Set<String> used = new HashSet<>();
        for (HomeSection section : HomeSection.values()) {
            for (com.fongmi.android.tv.bean.Class type : types) {
                if (used.contains(type.getTypeId())) continue;
                if (!section.matches(type.getTypeName())) continue;
                used.add(type.getTypeId());
                map.put(section, type);
                break;
            }
        }
        return map;
    }

    private void updateNavItems(List<com.fongmi.android.tv.bean.Class> types) {
        List<HomeNavItem> items = new ArrayList<>();
        items.add(HomeNavItem.search());
        items.add(HomeNavItem.home());
        Set<String> used = new HashSet<>();
        for (HomeSection section : HomeSection.values()) {
            com.fongmi.android.tv.bean.Class type = mSectionTypes.get(section);
            if (type == null) continue;
            used.add(type.getTypeId());
            items.add(HomeNavItem.category(type));
        }
        for (com.fongmi.android.tv.bean.Class type : types) {
            if (used.contains(type.getTypeId())) continue;
            items.add(HomeNavItem.category(type));
        }
        items.add(HomeNavItem.keep());
        String activeKey = resolveActiveKey(items);
        items.forEach(item -> item.setActivated(item.getKey().equals(activeKey)));
        mNavAdapter.setItems(items, new BaseDiffCallback<HomeNavItem>());
        mActiveNavItem = items.stream().filter(item -> item.getKey().equals(activeKey)).findFirst().orElse(HomeNavItem.home());
        setNavSelection(activeKey);
    }

    private String resolveActiveKey(List<HomeNavItem> items) {
        String fallback = HomeNavItem.home().getKey();
        if (mActiveNavItem == null || !mActiveNavItem.supportsContent()) return fallback;
        return items.stream().anyMatch(item -> item.getKey().equals(mActiveNavItem.getKey())) ? mActiveNavItem.getKey() : fallback;
    }

    private void setNavSelection(String key) {
        int index = indexOfNav(key);
        if (index >= 0) mBinding.nav.setSelectedPosition(index);
    }

    private int indexOfNav(String key) {
        for (int i = 0; i < mNavAdapter.size(); i++) {
            HomeNavItem item = (HomeNavItem) mNavAdapter.get(i);
            if (item.getKey().equals(key)) return i;
        }
        return -1;
    }

    @Nullable
    private HomeNavItem getNavItem(int position) {
        return position >= 0 && position < mNavAdapter.size() ? (HomeNavItem) mNavAdapter.get(position) : null;
    }

    private void activateNav(HomeNavItem item, boolean requestFocus) {
        if (item == null || !item.supportsContent()) return;
        boolean changed = !item.equals(mActiveNavItem);
        if (item.equals(mActiveNavItem) && !requestFocus) return;
        mActiveNavItem = item;
        for (int i = 0; i < mNavAdapter.size(); i++) ((HomeNavItem) mNavAdapter.get(i)).setActivated(i == indexOfNav(item.getKey()));
        mNavAdapter.notifyArrayItemRangeChanged(0, mNavAdapter.size());
        setNavSelection(item.getKey());
        if (item.isHome()) loadHomeContent();
        else if (item.isSearch()) showSearchContent();
        else if (item.isKeep()) showKeepContent();
        else {
            if (changed) prepareCategoryState(item.getType());
            loadCategoryContent(item.getType());
        }
        if (requestFocus) mBinding.nav.requestFocus();
    }

    private void activateHome(boolean requestFocus) {
        HomeNavItem item = getNavItem(indexOfNav(HomeNavItem.home().getKey()));
        if (item != null) activateNav(item, requestFocus);
        else loadHomeContent();
    }

    private boolean isHomeMode() {
        return mMode == ContentMode.HOME;
    }

    private boolean isCategoryMode() {
        return mMode == ContentMode.CATEGORY;
    }

    private boolean isSearchMode() {
        return mMode == ContentMode.SEARCH;
    }

    private boolean isKeepMode() {
        return mMode == ContentMode.KEEP;
    }

    private boolean requestActiveContentFocus() {
        if (isSearchMode()) return mSearchBinding.homeSearchKeyword.requestFocus();
        if (isKeepMode()) return mKeepBinding.homeKeepRecycler.requestFocus();
        return mBinding.recycler.requestFocus();
    }

    private boolean hasSearchRecords() {
        return mRecordAdapter != null && mRecordAdapter.getItemCount() > 0 && mSearchBinding.homeSearchRecordLayout.getVisibility() == View.VISIBLE;
    }

    private boolean requestSearchRecordFocus() {
        if (!hasSearchRecords()) return false;
        RecyclerView.LayoutManager manager = mSearchBinding.homeSearchRecordRecycler.getLayoutManager();
        View first = manager == null ? null : manager.findViewByPosition(0);
        return first != null ? first.requestFocus() : mSearchBinding.homeSearchRecordRecycler.requestFocus();
    }

    private void setHistoryDelete(boolean delete) {
        if (mHistoryPresenter == null) return;
        mHistoryPresenter.setDelete(delete);
        if (mRecentAdapter != null) mRecentAdapter.notifyArrayItemRangeChanged(0, mRecentAdapter.size());
    }

    private void clearHistory() {
        History.delete(VodConfig.getCid());
        setHistoryDelete(false);
        RefreshEvent.history();
    }

    private void setLogo() {
        com.fongmi.android.tv.utils.ImgUtil.logo(mBinding.logo);
    }

    private void updateNetworkIcon() {
        if (mConnectivityManager == null) return;
        Network active = mConnectivityManager.getActiveNetwork();
        NetworkCapabilities capabilities = active == null ? null : mConnectivityManager.getNetworkCapabilities(active);
        boolean connected = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        mBinding.network.setImageResource(connected ? R.drawable.ic_home_network : R.drawable.ic_home_network_off);
        mBinding.network.setContentDescription(getString(connected ? R.string.home_network_online : R.string.home_network_offline));
    }

    private void registerNetworkCallback() {
        if (mConnectivityManager == null || mNetworkRegistered) return;
        if (mNetworkCallback == null) {
            mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    App.post(HomeActivity.this::updateNetworkIcon);
                }

                @Override
                public void onLost(@NonNull Network network) {
                    App.post(HomeActivity.this::updateNetworkIcon);
                }

                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                    App.post(HomeActivity.this::updateNetworkIcon);
                }
            };
        }
        try {
            mConnectivityManager.registerDefaultNetworkCallback(mNetworkCallback);
            mNetworkRegistered = true;
        } catch (Exception ignored) {
        }
    }

    private void unregisterNetworkCallback() {
        if (mConnectivityManager == null || !mNetworkRegistered || mNetworkCallback == null) return;
        try {
            mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
        } catch (Exception ignored) {
        } finally {
            mNetworkRegistered = false;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfigEvent(ConfigEvent event) {
        switch (event.type()) {
            case VOD:
                setTitle();
                setLogo();
                RefreshEvent.history();
                RefreshEvent.home();
                break;
            case BOOT:
                LiveActivity.start(this);
                break;
            default:
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        switch (event.getType()) {
            case HOME:
                setTitle();
                activateHome(false);
                break;
            case HISTORY:
                if (isHomeMode()) renderHomeContent();
                break;
            case KEEP:
                if (isKeepMode()) renderKeepContent();
                break;
            case SIZE:
                if (isHomeMode()) renderHomeContent();
                else if (mActiveNavItem != null && mActiveNavItem.isCategory()) loadCategoryContent(mActiveNavItem.getType());
                else if (isKeepMode()) renderKeepContent();
                break;
            default:
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        switch (event.type()) {
            case SEARCH:
                CollectActivity.start(this, event.text());
                break;
            case PUSH:
                VideoActivity.push(this, event.text());
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCastEvent(CastEvent event) {
        if (VodConfig.get().getConfig().equals(event.config())) {
            VideoActivity.cast(this, event.history().save(VodConfig.getCid()));
        } else {
            VodConfig.load(event.config(), getCallback(event));
        }
    }

    private Callback getCallback(CastEvent event) {
        return new Callback() {
            @Override
            public void success() {
                onCastEvent(event);
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
            }
        };
    }

    @Override
    public void onItemClick(HomeNavItem item) {
        if (item.isCategory() && item.equals(mActiveNavItem) && toggleCategoryFilters()) requestActiveContentFocus();
        else if (item.supportsContent()) requestActiveContentFocus();
    }

    @Override
    public void onItemClick(Vod item) {
        if (item.isAction()) {
            mViewModel.action(getHome().getKey(), item.getAction());
        } else if (item.isFolder()) {
            VodActivity.start(this, getHome().getKey(), Result.folder(item));
        } else if (getHome().isIndex()) {
            CollectActivity.start(this, item.getName());
        } else {
            VideoActivity.start(this, getHome().getKey(), item.getId(), item.getName(), item.getPic());
        }
    }

    @Override
    public boolean onLongClick(Vod item) {
        if (item.isAction() || item.isFolder()) return false;
        CollectActivity.start(this, item.getName());
        return true;
    }

    @Override
    public void onItemClick(HomeHistoryItem item) {
        if (item.isMore()) {
            HistoryActivity.start(this);
        } else {
            History history = item.getHistory();
            VideoActivity.start(this, history.getSiteKey(), history.getVodId(), history.getVodName(), history.getVodPic());
        }
    }

    @Override
    public void onItemDelete(History item) {
        item.delete();
        setHistoryDelete(false);
        RefreshEvent.history();
    }

    @Override
    public boolean onLongClick() {
        if (mHistoryPresenter != null && mHistoryPresenter.isDelete()) clearHistory();
        else setHistoryDelete(true);
        return true;
    }

    @Override
    public void showDialog() {
        SiteDialog.create(this).show();
    }

    @Override
    public void onRefresh() {
        if (mActiveNavItem != null && mActiveNavItem.isCategory()) loadCategoryContent(mActiveNavItem.getType());
        else if (isSearchMode()) loadSearchHot();
        else if (isKeepMode()) renderKeepContent();
        else loadHomeContent();
    }

    @Override
    public void setSite(Site item) {
        VodConfig.get().setHome(item);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (KeyUtil.isMenuKey(event)) showDialog();
        if (handleContentLeft(event)) return true;
        if (KeyUtil.isActionDown(event) && KeyUtil.isRightKey(event) && mBinding.nav.hasFocus()) return requestActiveContentFocus();
        if (KeyUtil.isActionDown(event) && KeyUtil.isDownKey(event) && getCurrentFocus() == mBinding.title) return mBinding.nav.requestFocus();
        if (KeyUtil.isActionDown(event) && KeyUtil.isDownKey(event) && getCurrentFocus() == mBinding.actionSetting) return requestActiveContentFocus();
        if (KeyUtil.isActionDown(event) && KeyUtil.isUpKey(event) && mBinding.nav.hasFocus() && mBinding.nav.getSelectedPosition() == 0) return mBinding.title.requestFocus();
        return super.dispatchKeyEvent(event);
    }

    private boolean handleContentLeft(KeyEvent event) {
        if (!KeyUtil.isActionDown(event) || !KeyUtil.isLeftKey(event)) return false;
        View focus = getCurrentFocus();
        if (isDescendantOf(focus, mBinding.recycler)) {
            HorizontalGridView row = findParent(focus, HorizontalGridView.class);
            if (row != null && row.getSelectedPosition() > 0) return false;
            return mBinding.nav.requestFocus();
        }
        if (isSearchMode() && isDescendantOf(focus, mSearchBinding.getRoot())) {
            if (focus == mSearchBinding.homeSearchKeyword || focus == mSearchBinding.homeSearchMic) return requestSearchRecordFocus() || mBinding.nav.requestFocus();
            if (isDescendantOf(focus, mSearchBinding.homeSearchRecordRecycler)) return mBinding.nav.requestFocus();
            if (isDescendantOf(focus, mSearchBinding.homeSearchKeyboard)) {
                return isFirstColumn(focus, mSearchBinding.homeSearchKeyboard, 7) && (requestSearchRecordFocus() || mBinding.nav.requestFocus());
            }
            if (isDescendantOf(focus, mSearchBinding.homeSearchWordRecycler)) return mSearchBinding.homeSearchKeyword.requestFocus();
        }
        if (isKeepMode() && isDescendantOf(focus, mKeepBinding.homeKeepRecycler)) {
            return isFirstColumn(focus, mKeepBinding.homeKeepRecycler, Product.getColumn()) && mBinding.nav.requestFocus();
        }
        return false;
    }

    private boolean isFirstColumn(@Nullable View focus, @NonNull RecyclerView recycler, int spanCount) {
        RecyclerView.ViewHolder holder = recycler.findContainingViewHolder(focus);
        if (holder == null) return true;
        int position = holder.getBindingAdapterPosition();
        return position <= 0 || position % Math.max(1, spanCount) == 0;
    }

    private boolean isDescendantOf(@Nullable View child, @NonNull View ancestor) {
        return findParent(child, ancestor.getClass(), ancestor) != null;
    }

    @Nullable
    private <T extends View> T findParent(@Nullable View child, @NonNull Class<T> type) {
        return findParent(child, type, null);
    }

    @Nullable
    private <T extends View> T findParent(@Nullable View child, @NonNull Class<T> type, @Nullable View stopAt) {
        View current = child;
        while (current != null) {
            if (current == stopAt && type.isInstance(current)) return type.cast(current);
            if (stopAt != null && current == stopAt) return null;
            if (type.isInstance(current)) return type.cast(current);
            ViewParent parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mClock.start();
        updateNetworkIcon();
        registerNetworkCallback();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mClock.stop();
        unregisterNetworkCallback();
    }

    @Override
    protected void onBackInvoked() {
        if (mBinding.progressLayout.isProgress()) {
            showMainContent();
        } else if (mHistoryPresenter != null && mHistoryPresenter.isDelete()) {
            setHistoryDelete(false);
        } else if (isKeepMode() && mKeepAdapter != null && mKeepAdapter.isDelete()) {
            mKeepAdapter.setDelete(false);
        } else if (!isHomeMode()) {
            activateHome(true);
        } else {
            if (PlaybackService.isRunning()) moveTaskToBack(true);
            else super.onBackInvoked();
        }
    }

    @Override
    protected void onDestroy() {
        CacheManager.get().release();
        unregisterNetworkCallback();
        LiveConfig.get().clear();
        VodConfig.get().clear();
        AppDatabase.backup();
        OkHttp.get().clear();
        Source.get().exit();
        Server.get().stop();
        super.onDestroy();
    }
}
