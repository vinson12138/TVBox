package com.fongmi.android.tv.bean;

import androidx.annotation.Nullable;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.impl.Diffable;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.Objects;

public class HomeNavItem implements Diffable<HomeNavItem> {

    public enum Kind {SEARCH, HOME, KEEP, CATEGORY}

    private final Kind kind;
    private final int textResId;
    private final com.fongmi.android.tv.bean.Class type;
    private boolean activated;

    public static HomeNavItem search() {
        return new HomeNavItem(Kind.SEARCH, R.string.home_search, null);
    }

    public static HomeNavItem home() {
        return new HomeNavItem(Kind.HOME, R.string.home_nav_home, null);
    }

    public static HomeNavItem keep() {
        return new HomeNavItem(Kind.KEEP, R.string.home_keep, null);
    }

    public static HomeNavItem category(com.fongmi.android.tv.bean.Class type) {
        return new HomeNavItem(Kind.CATEGORY, 0, type);
    }

    private HomeNavItem(Kind kind, int textResId, com.fongmi.android.tv.bean.Class type) {
        this.kind = kind;
        this.textResId = textResId;
        this.type = type;
    }

    public Kind getKind() {
        return kind;
    }

    public com.fongmi.android.tv.bean.Class getType() {
        return type;
    }

    public boolean isSearch() {
        return kind == Kind.SEARCH;
    }

    public boolean isHome() {
        return kind == Kind.HOME;
    }

    public boolean isKeep() {
        return kind == Kind.KEEP;
    }

    public boolean isCategory() {
        return kind == Kind.CATEGORY && type != null;
    }

    public boolean hasFilters() {
        return isCategory() && !type.getFilters().isEmpty();
    }

    public boolean supportsContent() {
        return isSearch() || isHome() || isKeep() || isCategory();
    }

    public String getKey() {
        if (isCategory()) return "category:" + type.getTypeId();
        return kind.name().toLowerCase();
    }

    public String getText() {
        return isCategory() ? type.getTypeName() : ResUtil.getString(textResId);
    }

    public String getTypeId() {
        return isCategory() ? type.getTypeId() : "";
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof HomeNavItem it)) return false;
        return Objects.equals(getKey(), it.getKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey());
    }

    @Override
    public boolean isSameItem(HomeNavItem other) {
        return equals(other);
    }

    @Override
    public boolean isSameContent(HomeNavItem other) {
        return activated == other.activated && getText().equals(other.getText());
    }
}
