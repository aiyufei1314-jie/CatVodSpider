package com.github.catvod.utils;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class BaseUtil {

    public static final Pattern THUNDER = Pattern.compile("(magnet|thunder|ed2k):.*");
    public static final String CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36";
    public static final List<String> MEDIA = Arrays.asList("mp4", "mkv", "mov", "wav", "wma", "wmv", "flv", "avi", "iso", "mpg", "ts", "mp3", "aac", "flac", "m4a", "ape", "ogg", "rm", "rmvb", "asf", "dts", "dsf", "dff");
    public static final List<String> SUB = Arrays.asList("srt", "ass", "ssa", "vtt");

    public static final String FOLDER = "https://staticsns.cdn.bcebos.com/amis/2024-7/1721806895482/fileicon_dir.png";
    public static final String VIDEO = "https://staticsns.cdn.bcebos.com/amis/2024-7/1721806538516/fileicon_video.png";

    public static final String ALIFOLDER = "https://img.alicdn.com/imgextra/i1/O1CN01rGJZac1Zn37NL70IT_!!6000000003238-2-tps-230-180.png";
    public static final String ALIVIDEO = "https://img.alicdn.com/imgextra/i2/O1CN01YgPBAp1zvunG71HdD_!!6000000006777-2-tps-140-140.png";

    public static String getIcon(boolean folder) {
        return folder ? FOLDER : VIDEO;
    }

    public static boolean isThunder(String url) {
        return THUNDER.matcher(url).find() || isTorrent(url);
    }

    public static boolean isTorrent(String url) {
        return !url.startsWith("magnet") && url.split(";")[0].endsWith(".torrent");
    }

    public static boolean isSub(String text) {
        return SUB.contains(getExt(text).toLowerCase());
    }

    public static boolean isMedia(String text) {
        return MEDIA.contains(getExt(text).toLowerCase());
    }

    public static String getExt(String name) {
        return name.contains(".") ? name.substring(name.lastIndexOf(".") + 1).toLowerCase() : name.toLowerCase();
    }

    public static String getSize(double size) {
        if (size <= 0) return "";
        String[] units = new String[]{"bytes", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static String removeExt(String text) {
        return text.contains(".") ? text.substring(0, text.lastIndexOf(".")) : text;
    }

    public static String substring(String text) {
        return substring(text, 1);
    }

    public static String substring(String text, int num) {
        if (text != null && text.length() > num) {
            return text.substring(0, text.length() - num);
        } else {
            return text;
        }
    }

}