package com.fongmi.android.tv.utils;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.github.catvod.utils.Shell;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    private static final Pattern EPISODE = Pattern.compile("(?i)(?:ep|第|e|[\\-\\.\\s])\\s?(\\d{1,4})");
    private static final Pattern DATE = Pattern.compile("((?:19|20)\\d{2})[.\\-/]?(0[1-9]|1[0-2])[.\\-/]?([0-2]\\d|3[01])");
    private static final Pattern CLEAN_BRACKETS = Pattern.compile("[\\[（【《(][^\\]）】》)]*[\\]）】》)]");
    private static final Pattern CLEAN_QUALITY = Pattern.compile("(?i)\\s*(?:\\b(?:4[kK]|2160[pP]|1080[pP]|720[pP]|480[pP]|HEVC|H\\.?26[45]|[xX]26[45]|AV1|WEB-DL|REMUX|HDR|蓝光)\\b|国语|粤语|原声|中字)\\s*");
    private static final Pattern CLEAN_EPISODE = Pattern.compile("(?i)第\\s?\\d+\\s?[集话話期]|\\bep\\s?\\d{1,4}\\b|\\be\\s?\\d{1,4}\\b");
    private static final Pattern CLEAN_FORMAT = Pattern.compile("(?i)\\s*\\.?\\b(mkv|avi|mov|ts|rmvb|flv|wmv|mp4)\\b\\s*");
    private static final Pattern CLEAN_RESOLUTION = Pattern.compile("(?i)\\s*\\d{3,4}\\s*[xX×]\\s*\\d{3,4}\\s*");
    private static final Pattern CLEAN_FILESIZE = Pattern.compile("(?i)\\s*\\d+\\.?\\d*\\s*(?:GB|MB|KB)\\s*");
    private static final Pattern CLEAN_TITLE_DECORATORS = Pattern.compile("[《》〈〉「」『』]");
    private static final Pattern CLEAN_SEPARATORS = Pattern.compile("^[\\s\\u00B7：:·\\-—_|/\\\\]+|[\\s\\u00B7：:·\\-—_|/\\\\]+$");

    public static void toggleFullscreen(Activity activity, boolean fullscreen) {
        if (fullscreen) hideSystemUI(activity);
        else showSystemUI(activity);
    }

    public static void hideSystemUI(Activity activity) {
        hideSystemUI(activity.getWindow());
    }

    public static void hideSystemUI(Window window) {
        WindowInsetsControllerCompat insets = WindowCompat.getInsetsController(window, window.getDecorView());
        insets.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        insets.hide(WindowInsetsCompat.Type.systemBars());
    }

    public static void showSystemUI(Activity activity) {
        showSystemUI(activity.getWindow());
    }

    public static void showSystemUI(Window window) {
        WindowCompat.getInsetsController(window, window.getDecorView()).show(WindowInsetsCompat.Type.systemBars());
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    public static void showKeyboard(View view) {
        if (!view.requestFocus()) return;
        InputMethodManager imm = (InputMethodManager) App.get().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) view.postDelayed(() -> imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT), 250);
    }

    public static void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) App.get().getSystemService(Context.INPUT_METHOD_SERVICE);
        IBinder windowToken = view.getWindowToken();
        if (imm == null || windowToken == null) return;
        imm.hideSoftInputFromWindow(windowToken, 0);
    }

    public static float getBrightness(Activity activity) {
        try {
            float value = activity.getWindow().getAttributes().screenBrightness;
            if (WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL >= value && value >= WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF) return value;
            return Settings.System.getFloat(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS) / 255;
        } catch (Exception e) {
            return 0.5f;
        }
    }

    public static CharSequence getClipText() {
        ClipboardManager manager = (ClipboardManager) App.get().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = manager == null ? null : manager.getPrimaryClip();
        if (clipData == null || clipData.getItemCount() == 0) return "";
        return clipData.getItemAt(0).getText();
    }

    public static void copy(String text) {
        try {
            ClipboardManager manager = (ClipboardManager) App.get().getSystemService(Context.CLIPBOARD_SERVICE);
            manager.setPrimaryClip(ClipData.newPlainText("", text));
            Notify.show(R.string.copied);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getNumber(String text) {
        try {
            text = text.replaceAll("\\[.*?\\]|\\(.*?\\)", "");
            Matcher dateMatcher = DATE.matcher(text);
            if (dateMatcher.find()) {
                int y = Integer.parseInt(dateMatcher.group(1));
                int m = Integer.parseInt(dateMatcher.group(2));
                int d = Integer.parseInt(dateMatcher.group(3));
                return y * 10000 + m * 100 + d;
            }
            text = text.replaceAll("\\b(19|20)\\d{2}\\b", "");
            text = text.toLowerCase().replaceAll("2160p|1080p|720p|480p|4k|h26[45]|x26[45]|mp4", "");
            Matcher matcher = EPISODE.matcher(text);
            if (matcher.find()) return Integer.parseInt(matcher.group(1));
            String number = text.replaceAll("\\D+", "");
            return number.isEmpty() ? -1 : Integer.parseInt(number);
        } catch (Exception e) {
            return -1;
        }
    }

    public static String clean(String text) {
        if (!text.contains("<")) return text;
        StringBuilder sb = new StringBuilder();
        text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString().replace("\u00A0", " ").replace("\u3000", " ");
        for (String line : text.split("\\r?\\n")) sb.append(line.trim()).append("\n");
        return substring(sb.toString());
    }

    public static String getAndroidId() {
        try {
            String id = Settings.Secure.getString(App.get().getContentResolver(), Settings.Secure.ANDROID_ID);
            if (TextUtils.isEmpty(id)) throw new NullPointerException();
            return id;
        } catch (Exception e) {
            return "0000000000000000";
        }
    }

    public static String getSerial() {
        return Shell.exec("getprop ro.serialno").replace("\n", "");
    }

    public static String getMac(String name) {
        try {
            StringBuilder sb = new StringBuilder();
            NetworkInterface nif = NetworkInterface.getByName(name);
            if (nif.getHardwareAddress() == null) return "";
            for (byte b : nif.getHardwareAddress()) sb.append(String.format("%02X:", b));
            return substring(sb.toString());
        } catch (Exception e) {
            return "";
        }
    }

    public static String getDeviceName() {
        String model = Build.MODEL;
        String manufacturer = Build.MANUFACTURER;
        return model.startsWith(manufacturer) ? model : manufacturer + " " + model;
    }

    public static String substring(String text) {
        return substring(text, 1);
    }

    public static String substring(String text, int num) {
        if (text != null && text.length() > num) return text.substring(0, text.length() - num);
        return text;
    }

    public static boolean isLeanback() {
        return "leanback".equals(BuildConfig.FLAVOR_mode);
    }

    public static boolean isMobile() {
        return "mobile".equals(BuildConfig.FLAVOR_mode);
    }

    public static String format(StringBuilder builder, Formatter formatter, long timeMs) {
        try {
            return androidx.media3.common.util.Util.getStringForTime(builder, formatter, timeMs);
        } catch (Exception e) {
            return "";
        }
    }

    public static String cleanTitle(String name, String vodName) {
        if (TextUtils.isEmpty(name)) return "";
        String text = CLEAN_BRACKETS.matcher(name).replaceAll(" ");
        text = CLEAN_QUALITY.matcher(text).replaceAll(" ");
        text = CLEAN_EPISODE.matcher(text).replaceAll(" ");
        text = DATE.matcher(text).replaceAll(" ");
        text = text.replaceAll("\\b(19|20)\\d{2}\\b", " ");
        text = CLEAN_FORMAT.matcher(text).replaceAll(" ");
        text = CLEAN_RESOLUTION.matcher(text).replaceAll(" ");
        text = CLEAN_FILESIZE.matcher(text).replaceAll(" ");
        if (!TextUtils.isEmpty(vodName)) {
            String escaped = Pattern.quote(vodName);
            // remove vodName and any surrounding title decorators / separators
            text = text.replaceAll("[《\\[【「〖（(]?\\s*" + escaped + "\\s*[》\\]】」〗）)]?\\s*[-_·—]*\\s*", " ");
            String[] words = vodName.split("[\\s·]+");
            if (words.length > 1) {
                for (String word : words) {
                    if (word.length() >= 2) {
                        String wEscaped = Pattern.quote(word);
                        text = text.replaceAll("[《\\[【「〖（(]?\\s*" + wEscaped + "\\s*[》\\]】」〗）)]?\\s*[-_·—]*\\s*", " ");
                    }
                }
            }
        }
        text = CLEAN_TITLE_DECORATORS.matcher(text).replaceAll(" ");
        text = text.replaceAll("\\s{2,}", " ");
        text = CLEAN_SEPARATORS.matcher(text).replaceAll("");
        return text.trim();
    }

    public static Intent getChooser(Intent intent) {
        List<ComponentName> components = new ArrayList<>();
        for (ResolveInfo resolveInfo : App.get().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)) {
            String pkgName = resolveInfo.activityInfo.packageName;
            if (pkgName.equals(App.get().getPackageName())) {
                components.add(new ComponentName(pkgName, resolveInfo.activityInfo.name));
            }
        }
        return Intent.createChooser(intent, null).putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, components.toArray(new ComponentName[0]));
    }
}
