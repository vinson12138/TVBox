package com.fongmi.android.tv.bean;

import androidx.annotation.Nullable;

import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.impl.Diffable;

import java.util.Objects;

public class HomeHistoryItem implements Diffable<HomeHistoryItem> {

    private final History history;
    private final boolean more;

    public static HomeHistoryItem create(History history) {
        return new HomeHistoryItem(history, false);
    }

    public static HomeHistoryItem more() {
        return new HomeHistoryItem(null, true);
    }

    private HomeHistoryItem(History history, boolean more) {
        this.history = history;
        this.more = more;
    }

    public History getHistory() {
        return history;
    }

    public boolean isMore() {
        return more;
    }

    public String getKey() {
        return isMore() ? "more" : Objects.requireNonNull(history).getKey();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof HomeHistoryItem it)) return false;
        return Objects.equals(getKey(), it.getKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey());
    }

    @Override
    public boolean isSameItem(HomeHistoryItem other) {
        return equals(other);
    }

    @Override
    public boolean isSameContent(HomeHistoryItem other) {
        if (isMore() || other.isMore()) return isMore() == other.isMore();
        return Objects.equals(history.getVodName(), other.history.getVodName())
                && Objects.equals(history.getVodRemarks(), other.history.getVodRemarks())
                && Objects.equals(history.getVodPic(), other.history.getVodPic());
    }
}
