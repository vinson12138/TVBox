package com.fongmi.android.tv.bean;

import java.util.List;

public class EpisodeGroup {

    public static final int PAGE_SIZE_MAIN = 20;
    public static final int PAGE_SIZE_OTHER = 10;

    private final int groupType;
    private final String groupName;
    private final List<Episode> episodes;
    private boolean activated;

    public EpisodeGroup(int groupType, String groupName, List<Episode> episodes) {
        this.groupType = groupType;
        this.groupName = groupName;
        this.episodes = episodes;
    }

    public int getGroupType() {
        return groupType;
    }

    public String getGroupName() {
        return groupName;
    }

    public List<Episode> getEpisodes() {
        return episodes;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(EpisodeGroup item) {
        this.activated = item == this;
    }

    public int getPageSize() {
        return groupType == Episode.MAIN ? PAGE_SIZE_MAIN : PAGE_SIZE_OTHER;
    }

    public int getPageCount() {
        int size = getPageSize();
        return (episodes.size() + size - 1) / size;
    }
}
