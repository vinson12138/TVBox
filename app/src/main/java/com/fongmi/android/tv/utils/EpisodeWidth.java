package com.fongmi.android.tv.utils;

import android.graphics.Paint;

import com.fongmi.android.tv.bean.Episode;

import java.util.List;

public class EpisodeWidth {

    public static boolean isPrimaryOnly(List<Episode> items, String vodName) {
        if (items == null || items.isEmpty()) return false;
        for (Episode item : items) {
            if (!item.getCleanSubtitle(vodName).isEmpty()) return false;
            if (!item.getVersionKey().isEmpty()) return false;
        }
        return true;
    }

    public static int measure(List<Episode> items, String vodName, int primarySp, int titleSp, int tagSp, int baseExtraDp, int titleBonusDp, int tagBonusDp) {
        if (isPrimaryOnly(items, vodName)) return measurePrimaryOnly(items, primarySp, baseExtraDp);
        int width = ResUtil.dp2px(80);
        for (Episode item : items) width = Math.max(width, measure(item, vodName, primarySp, titleSp, tagSp, baseExtraDp, titleBonusDp, tagBonusDp));
        return width;
    }

    public static int measure(Episode item, String vodName, int primarySp, int titleSp, int tagSp, int baseExtraDp, int titleBonusDp, int tagBonusDp) {
        Paint primaryPaint = new Paint();
        primaryPaint.setTextSize(ResUtil.sp2px(primarySp));
        primaryPaint.setFakeBoldText(true);
        Paint titlePaint = new Paint();
        titlePaint.setTextSize(ResUtil.sp2px(titleSp));
        Paint tagPaint = new Paint();
        tagPaint.setTextSize(ResUtil.sp2px(tagSp));

        String primary = item.getPrimaryText();
        String subtitle = item.getCleanSubtitle(vodName);
        String tag = item.getVersionKey();

        int contentWidth = (int) Math.ceil(primaryPaint.measureText(primary));
        if (!subtitle.isEmpty()) contentWidth = Math.max(contentWidth, (int) Math.ceil(titlePaint.measureText(subtitle)));

        int width = contentWidth + ResUtil.dp2px(baseExtraDp);
        if (!tag.isEmpty()) width += (int) Math.ceil(tagPaint.measureText(tag)) + ResUtil.dp2px(tagBonusDp);
        if (!subtitle.isEmpty()) width += ResUtil.dp2px(titleBonusDp);
        return width;
    }

    private static int measurePrimaryOnly(List<Episode> items, int primarySp, int baseExtraDp) {
        Paint primaryPaint = new Paint();
        primaryPaint.setTextSize(ResUtil.sp2px(primarySp));
        primaryPaint.setFakeBoldText(true);
        int maxDigits = 1;
        for (Episode item : items) maxDigits = Math.max(maxDigits, item.getPrimaryText().length());
        String sample = "8".repeat(maxDigits);
        return (int) Math.ceil(primaryPaint.measureText(sample)) + ResUtil.dp2px(baseExtraDp);
    }
}
