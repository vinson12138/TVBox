package com.fongmi.android.tv.ui.custom;

import android.annotation.SuppressLint;

import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.RowPresenter;

import com.fongmi.android.tv.utils.ResUtil;

public class CustomRowPresenter extends ListRowPresenter {

    private final int spacing;
    private final int strategy;
    private final int paddingHorizontal;
    private final int paddingVertical;

    public CustomRowPresenter(int spacing) {
        this(spacing, FocusHighlight.ZOOM_FACTOR_SMALL);
    }

    @SuppressLint("RestrictedApi")
    public CustomRowPresenter(int spacing, int focusZoomFactor) {
        this(spacing, focusZoomFactor, HorizontalGridView.FOCUS_SCROLL_ITEM);
    }

    public CustomRowPresenter(int spacing, int focusZoomFactor, int strategy) {
        this(spacing, focusZoomFactor, strategy, 12, 8);
    }

    public CustomRowPresenter(int spacing, int focusZoomFactor, int strategy, int paddingHorizontal, int paddingVertical) {
        super(focusZoomFactor);
        this.spacing = spacing;
        this.strategy = strategy;
        this.paddingHorizontal = paddingHorizontal;
        this.paddingVertical = paddingVertical;
        setShadowEnabled(false);
        setSelectEffectEnabled(false);
        setKeepChildForeground(false);
    }

    @Override
    @SuppressLint("RestrictedApi")
    protected void initializeRowViewHolder(RowPresenter.ViewHolder holder) {
        super.initializeRowViewHolder(holder);
        ViewHolder vh = (ViewHolder) holder;
        vh.getGridView().setFocusScrollStrategy(strategy);
        vh.getGridView().setHorizontalSpacing(ResUtil.dp2px(spacing));
        vh.getGridView().setClipChildren(false);
        vh.getGridView().setClipToPadding(false);
        vh.getGridView().setPadding(ResUtil.dp2px(paddingHorizontal), ResUtil.dp2px(paddingVertical), ResUtil.dp2px(paddingHorizontal), ResUtil.dp2px(paddingVertical));
    }
}
