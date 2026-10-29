package io.opentakserver.opentakicu;

import java.util.regex.Pattern;

/**
 * Helpers for building and logging stream URLs.
 */
public final class StreamUrlUtils {

    private static final Pattern MULTIPLE_SLASHES = Pattern.compile("/{2,}");
    private static final Pattern QUERY_PASSWORD = Pattern.compile("([?&]pass=)[^&]*");
    private static final Pattern USER_INFO_PASSWORD = Pattern.compile("(://[^/:@]*:)[^/@]*@");
    private static final Pattern SRT_STREAMID_PASSWORD = Pattern.compile("(streamid=publish:[^:&]*:[^:&]*:)[^&]*");

    private StreamUrlUtils() {
    }

    /**
     * Normalizes a stream path so it can be appended to "protocol://host:port/".
     * Trims whitespace, collapses repeated slashes and removes leading and trailing slashes,
     * e.g. "/live/icu/user" and "live//icu/user/" both become "live/icu/user".
     * A leading slash would otherwise produce "//" in the URL, which RootEncoder 2.6+
     * splits incorrectly for RTMP (see brian7704/OpenTAK_ICU#22, pedroSG94/RootEncoder#2224).
     *
     * @return the normalized path, or an empty string for null input
     */
    public static String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = MULTIPLE_SLASHES.matcher(path.trim()).replaceAll("/");
        int start = 0;
        int end = normalized.length();
        while (start < end && normalized.charAt(start) == '/') {
            start++;
        }
        while (end > start && normalized.charAt(end - 1) == '/') {
            end--;
        }
        return normalized.substring(start, end);
    }

    /**
     * Masks passwords in a stream URL so it can be written to logcat.
     * Covers the MediaMTX RTMP query ("?user=x&amp;pass=y"), SRT stream ids
     * ("streamid=publish:path:user:pass") and user info ("user:pass@host").
     *
     * @return the URL with every password replaced by "***", or null for null input
     */
    public static String redactUrl(String url) {
        if (url == null) {
            return null;
        }
        String redacted = QUERY_PASSWORD.matcher(url).replaceAll("$1***");
        redacted = SRT_STREAMID_PASSWORD.matcher(redacted).replaceAll("$1***");
        redacted = USER_INFO_PASSWORD.matcher(redacted).replaceAll("$1***@");
        return redacted;
    }
}
