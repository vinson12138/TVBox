package com.fongmi.android.tv.bean;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.fongmi.android.tv.impl.Diffable;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.utils.Trans;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Episode implements Parcelable, Diffable<Episode> {

    public static final int MAIN = 0;
    public static final int MOVIE = 1;
    public static final int SPECIAL = 2;
    public static final int BONUS = 3;
    public static final int TRAILER = 4;

    private static final Pattern VK_RESOLUTION = Pattern.compile("(\\d{3,4})[xX×](\\d{3,4})");
    private static final Pattern VK_FILESIZE = Pattern.compile("(?i)(\\d+\\.?\\d*)\\s*(GB|MB)");
    private static final Pattern VK_FORMAT = Pattern.compile("(?i)\\b(mkv|avi|ts|rmvb|flv|wmv)\\b");
    private static final Pattern LEADING_EPISODE_NUM = Pattern.compile("^\\d+\\s*[.·\\-\\s]\\s*");
    private static final Pattern LEADING_SEP = Pattern.compile("^[\\s·:\\-—_|/\\\\]+");
    private static final Pattern CONTENT_TYPE_KEYWORDS = Pattern.compile("(?i)(?:特别篇|特典|\\bova\\b|\\boad\\b|\\bsp\\b|花絮|幕后|彩蛋|预告|先导|\\btrailer\\b|\\bpv\\b)");

    @SerializedName("name")
    private String name;
    @SerializedName("desc")
    private String desc;
    @SerializedName("url")
    private String url;

    private int index;
    private int number;
    private int contentType;
    private int displayIndex = -1;
    private String versionKey;
    private boolean activated;
    private boolean selected;

    public static Episode create(String name, String url) {
        return new Episode(name, "", url).trans();
    }

    public static Episode create(String name, String desc, String url) {
        return new Episode(name, desc, url).trans();
    }

    private Episode(String name, String desc, String url) {
        this.number = Util.getNumber(name);
        this.contentType = parseContentType(name);
        this.versionKey = parseVersionKey(name);
        this.name = name;
        this.desc = desc;
        this.url = url;
    }

    public Episode() {
    }

    private static int parseContentType(String name) {
        if (name == null || name.isEmpty()) return MAIN;
        String lower = name.toLowerCase();
        if (lower.contains("剧场版") || lower.contains("电影版")) return MOVIE;
        if (lower.contains("特别篇") || lower.contains("特典") || lower.contains("ova")
                || lower.contains("oad") || lower.matches(".*\\bsp\\b.*")) return SPECIAL;
        if (lower.contains("花絮") || lower.contains("幕后") || lower.contains("彩蛋")) return BONUS;
        if (lower.contains("预告") || lower.contains("先导") || lower.contains("trailer")
                || lower.matches(".*\\bpv\\b.*")) return TRAILER;
        return MAIN;
    }

    private static String parseVersionKey(String name) {
        if (name == null || name.isEmpty()) return "";
        String upper = name.toUpperCase();
        List<String> tags = new ArrayList<>();
        if (upper.contains("4K") || upper.contains("2160P")) tags.add("4K");
        else if (upper.contains("1080P")) tags.add("1080P");
        else if (upper.contains("720P")) tags.add("720P");
        else if (upper.contains("480P")) tags.add("480P");
        if (upper.contains("AV1")) tags.add("AV1");
        else if (upper.contains("HEVC") || upper.contains("H265") || upper.contains("X265")) tags.add("H265");
        else if (upper.contains("H264") || upper.contains("X264")) tags.add("H264");
        if (name.contains("国语")) tags.add("国语");
        if (name.contains("粤语")) tags.add("粤语");
        if (name.contains("原声")) tags.add("原声");
        if (name.contains("中字")) tags.add("中字");
        if (name.contains("蓝光") || upper.contains("REMUX")) tags.add("蓝光");
        if (upper.contains("HDR")) tags.add("HDR");
        if (upper.contains("WEB-DL")) tags.add("WEB-DL");
        Matcher resMatcher = VK_RESOLUTION.matcher(name);
        if (resMatcher.find()) tags.add(resMatcher.group(1) + "x" + resMatcher.group(2));
        Matcher sizeMatcher = VK_FILESIZE.matcher(name);
        if (sizeMatcher.find()) tags.add(sizeMatcher.group(1) + sizeMatcher.group(2).toUpperCase());
        Matcher fmtMatcher = VK_FORMAT.matcher(name);
        if (fmtMatcher.find()) tags.add(fmtMatcher.group(1).toUpperCase());
        return String.join(",", tags);
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return TextUtils.isEmpty(desc) ? "" : desc;
    }

    public String getUrl() {
        return TextUtils.isEmpty(url) ? "" : url;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setDisplayIndex(int displayIndex) {
        this.displayIndex = displayIndex;
    }

    public int getNumber() {
        return number;
    }

    public int getContentType() {
        return contentType;
    }

    public String getVersionKey() {
        return versionKey == null ? "" : versionKey;
    }

    public String getPrimaryText() {
        return getDesc().concat(getDisplayText());
    }

    public String getDisplayText() {
        if (contentType == MAIN || contentType == MOVIE) {
            if (number > 0) return String.valueOf(number);
            return getName();
        }
        // For SPECIAL/BONUS/TRAILER: prefer explicit episode number (< 10000, not a date),
        // fall back to displayIndex assigned by Flag.getGroups(), then full name.
        int n = (number > 0 && number < 10000) ? number : (displayIndex > 0 ? displayIndex : -1);
        if (n > 0) {
            if (contentType == SPECIAL) return "SP " + n;
            if (contentType == BONUS) return "花絮" + n;
            if (contentType == TRAILER) return "预告" + n;
        }
        return getName();
    }

    public String getCleanSubtitle(String vodName) {
        String text = Util.cleanTitle(name, vodName);
        if (contentType == MAIN && number > 0) {
            Matcher m = LEADING_EPISODE_NUM.matcher(text);
            if (m.find()) {
                int found = Integer.parseInt(m.group().replaceAll("[^\\d]", ""));
                if (found == number) {
                    text = text.substring(m.end());
                    text = LEADING_SEP.matcher(text).replaceFirst("").trim();
                }
            }
        } else if (contentType == SPECIAL || contentType == BONUS || contentType == TRAILER) {
            text = CONTENT_TYPE_KEYWORDS.matcher(text).replaceAll("").trim();
            // Strip residual leading episode number after keyword removal (e.g. "01 幕后" after removing "SP")
            Matcher nm = LEADING_EPISODE_NUM.matcher(text);
            if (nm.find()) text = text.substring(nm.end());
            text = LEADING_SEP.matcher(text).replaceFirst("").trim();
        }
        return text;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
        this.selected = activated;
    }

    public void deactivated() {
        setActivated(false);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getScore(String name, int number) {
        if (getName().equalsIgnoreCase(name)) return 100;
        if (number != -1 && getNumber() == number) return 80;
        if (number == -1 && name.length() >= 2 && getName().toLowerCase().contains(name.toLowerCase())) return 70;
        if (number == -1 && getName().length() >= 2 && name.toLowerCase().contains(getName().toLowerCase())) return 60;
        return 0;
    }

    public boolean matchesName(Episode other) {
        if (other == null) return false;
        return getName().equalsIgnoreCase(other.getName());
    }

    public Episode trans() {
        if (Trans.pass()) return this;
        this.name = Trans.s2t(name);
        this.desc = Trans.s2t(desc);
        return this;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Episode it)) return false;
        return Objects.equals(getName(), it.getName()) && Objects.equals(getUrl(), it.getUrl());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getUrl());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.desc);
        dest.writeString(this.url);
        dest.writeInt(this.number);
        dest.writeInt(this.contentType);
        dest.writeString(this.versionKey);
        dest.writeByte(this.activated ? (byte) 1 : (byte) 0);
        dest.writeByte(this.selected ? (byte) 1 : (byte) 0);
    }

    protected Episode(Parcel in) {
        this.name = in.readString();
        this.desc = in.readString();
        this.url = in.readString();
        this.number = in.readInt();
        this.contentType = in.readInt();
        this.versionKey = in.readString();
        this.activated = in.readByte() != 0;
        this.selected = in.readByte() != 0;
    }

    public record Rule(Episode episode, int score) {

        public boolean find() {
            return score > 0;
        }
    }

    public static final Creator<Episode> CREATOR = new Creator<>() {
        @Override
        public Episode createFromParcel(Parcel source) {
            return new Episode(source);
        }

        @Override
        public Episode[] newArray(int size) {
            return new Episode[size];
        }
    };

    @Override
    public boolean isSameItem(Episode other) {
        return equals(other);
    }

    @Override
    public boolean isSameContent(Episode other) {
        return getUrl().equals(other.getUrl()) && getDesc().equals(other.getDesc());
    }
}
