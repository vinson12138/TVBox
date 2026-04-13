package com.fongmi.android.tv.bean;

import java.util.ArrayList;
import java.util.List;

public class EpisodePage {

    private final String title;
    private final int startIndex;
    private final int endIndex;
    private final int anchorIndex;

    public EpisodePage(String title, int startIndex, int endIndex, int anchorIndex) {
        this.title = title;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.anchorIndex = anchorIndex;
    }

    public static List<EpisodePage> create(List<Episode> episodes, int pageSize, boolean reverse) {
        List<EpisodePage> pages = new ArrayList<>();
        if (episodes == null || episodes.isEmpty()) return pages;
        int size = Math.max(pageSize, 1);
        if (!reverse) {
            for (int start = 0; start < episodes.size(); start += size) {
                addPage(pages, episodes, start, Math.min(start + size - 1, episodes.size() - 1));
            }
            return pages;
        }
        int remain = episodes.size() % size;
        int firstPageSize = remain == 0 ? size : remain;
        addPage(pages, episodes, 0, Math.min(firstPageSize - 1, episodes.size() - 1));
        for (int start = firstPageSize; start < episodes.size(); start += size) {
            addPage(pages, episodes, start, Math.min(start + size - 1, episodes.size() - 1));
        }
        return pages;
    }

    private static void addPage(List<EpisodePage> pages, List<Episode> episodes, int start, int end) {
        String first = episodes.get(start).getPrimaryText();
        String last = episodes.get(end).getPrimaryText();
        pages.add(new EpisodePage(first + "~" + last, start, end, start));
    }

    public String getTitle() {
        return title;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public int getAnchorIndex() {
        return anchorIndex;
    }
}
