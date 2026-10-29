package io.opentakserver.opentakicu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.opentakserver.opentakicu.contants.Preferences;

/**
 * Describes which settings an opentakicu://import link may change and validates their values.
 * Kept free of Android classes so it can be unit tested on the JVM.
 */
public final class ImportSchema {

    public enum Type { STRING, HOST, PATH, BOOLEAN, ENUM, INT_RANGE, RESOLUTION }

    public static final class Entry {
        public final String key;
        public final Type type;
        final List<String> allowed;
        final int min;
        final int max;

        private Entry(String key, Type type, List<String> allowed, int min, int max) {
            this.key = key;
            this.type = type;
            this.allowed = allowed;
            this.min = min;
            this.max = max;
        }
    }

    /** Width and height requested by a resolution parameter, e.g. "1280x720". */
    public static final class Resolution {
        public final int width;
        public final int height;

        public Resolution(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() {
            return width + "x" + height;
        }
    }

    /** Outcome of {@link #parse(Map)}. */
    public static final class Result {
        /** Accepted values in schema order: String, Boolean or Resolution. */
        public final Map<String, Object> accepted = new LinkedHashMap<>();
        /** Rejected parameters as key to the raw value. */
        public final Map<String, String> rejected = new LinkedHashMap<>();
    }

    private static final Pattern WHITESPACE = Pattern.compile("\\s");
    private static final Pattern RESOLUTION_PATTERN = Pattern.compile("^\\s*(\\d{2,5})\\s*[xX×]\\s*(\\d{2,5})\\s*$");

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        // Stream settings
        add(new Entry(Preferences.STREAM_VIDEO, Type.BOOLEAN, null, 0, 0));
        add(new Entry(Preferences.STREAM_PROTOCOL, Type.ENUM, Arrays.asList("rtsp", "rtsps", "rtmp", "rtmps", "srt", "udp"), 0, 0));
        add(new Entry(Preferences.STREAM_ADDRESS, Type.HOST, null, 0, 0));
        add(new Entry(Preferences.STREAM_PORT, Type.INT_RANGE, null, 1, 65535));
        add(new Entry(Preferences.STREAM_PATH, Type.PATH, null, 0, 0));
        add(new Entry(Preferences.STREAM_USERNAME, Type.STRING, null, 0, 0));
        add(new Entry(Preferences.STREAM_PASSWORD, Type.STRING, null, 0, 0));
        // Video settings. Screen streaming needs a MediaProjection grant, so it cannot be imported.
        add(new Entry(Preferences.VIDEO_SOURCE, Type.ENUM, Arrays.asList(Preferences.VIDEO_SOURCE_DEFAULT, Preferences.VIDEO_SOURCE_USB), 0, 0));
        add(new Entry(Preferences.VIDEO_RESOLUTION, Type.RESOLUTION, null, 0, 0));
        add(new Entry(Preferences.VIDEO_BITRATE, Type.INT_RANGE, null, 100, 50000));
        add(new Entry(Preferences.VIDEO_ADAPTIVE_BITRATE, Type.BOOLEAN, null, 0, 0));
        add(new Entry(Preferences.VIDEO_FPS, Type.ENUM, Arrays.asList("15", "24", "30", "60"), 0, 0));
        add(new Entry(Preferences.VIDEO_CODEC, Type.ENUM, Arrays.asList("H264", "H265", "AV1"), 0, 0));
        add(new Entry(Preferences.RECORD_VIDEO, Type.BOOLEAN, null, 0, 0));
        add(new Entry(Preferences.TEXT_OVERLAY, Type.BOOLEAN, null, 0, 0));
        add(new Entry(Preferences.TEXT_OVERLAY_TIMEZONE, Type.BOOLEAN, null, 0, 0));
        // Audio settings
        add(new Entry(Preferences.ENABLE_AUDIO, Type.BOOLEAN, null, 0, 0));
        // TAK server settings
        add(new Entry(Preferences.ATAK_SEND_COT, Type.BOOLEAN, null, 0, 0));
    }

    private ImportSchema() {
    }

    private static void add(Entry entry) {
        ENTRIES.put(entry.key, entry);
    }

    /** All keys an import link may contain, in display order. */
    public static List<String> keys() {
        return Collections.unmodifiableList(new ArrayList<>(ENTRIES.keySet()));
    }

    public static Entry entry(String key) {
        return ENTRIES.get(key);
    }

    /**
     * Validates the query parameters of an import link. Unknown keys and invalid values end up in
     * {@link Result#rejected}; they never change a setting.
     */
    public static Result parse(Map<String, String> params) {
        Result result = new Result();
        Map<String, String> known = new LinkedHashMap<>();
        for (Map.Entry<String, String> param : params.entrySet()) {
            if (ENTRIES.containsKey(param.getKey())) {
                known.put(param.getKey(), param.getValue());
            } else {
                result.rejected.put(param.getKey(), param.getValue());
            }
        }
        // Keep accepted values in schema order regardless of the link's parameter order
        for (Entry entry : ENTRIES.values()) {
            if (!known.containsKey(entry.key)) {
                continue;
            }
            String raw = known.get(entry.key);
            Object value = convert(entry, raw);
            if (value != null) {
                result.accepted.put(entry.key, value);
            } else {
                result.rejected.put(entry.key, raw);
            }
        }
        return result;
    }

    /** @return the typed value, or null if the raw value is not valid for this entry */
    static Object convert(Entry entry, String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        switch (entry.type) {
            case STRING:
                return value;
            case HOST:
                // An empty address or one with spaces would break the stream URL
                return value.isEmpty() || WHITESPACE.matcher(value).find() ? null : value;
            case PATH:
                String path = StreamUrlUtils.normalizePath(value);
                return path.isEmpty() ? null : path;
            case BOOLEAN:
                String lower = value.toLowerCase(Locale.ROOT);
                if (lower.equals("true") || lower.equals("1") || lower.equals("yes")) return Boolean.TRUE;
                if (lower.equals("false") || lower.equals("0") || lower.equals("no")) return Boolean.FALSE;
                return null;
            case ENUM:
                for (String allowed : entry.allowed) {
                    if (allowed.equalsIgnoreCase(value)) return allowed;
                }
                return null;
            case INT_RANGE:
                try {
                    int number = Integer.parseInt(value);
                    return number >= entry.min && number <= entry.max ? String.valueOf(number) : null;
                } catch (NumberFormatException e) {
                    return null;
                }
            case RESOLUTION:
                Matcher matcher = RESOLUTION_PATTERN.matcher(value);
                if (!matcher.matches()) return null;
                int width = Integer.parseInt(matcher.group(1));
                int height = Integer.parseInt(matcher.group(2));
                return width > 0 && height > 0 ? new Resolution(width, height) : null;
            default:
                return null;
        }
    }

    /**
     * Picks the camera resolution to use for a requested size.
     * Exact match first, otherwise the largest supported size that fits within the request,
     * otherwise the smallest supported size.
     *
     * @param supported supported sizes as {width, height} pairs, in the camera's order
     * @return index into {@code supported}, or -1 if the list is empty
     */
    public static int pickResolutionIndex(List<int[]> supported, Resolution requested) {
        if (supported == null || supported.isEmpty()) {
            return -1;
        }
        int best = -1;
        long bestArea = -1;
        int smallest = 0;
        long smallestArea = Long.MAX_VALUE;
        for (int i = 0; i < supported.size(); i++) {
            int w = supported.get(i)[0];
            int h = supported.get(i)[1];
            if (w == requested.width && h == requested.height) {
                return i;
            }
            long area = (long) w * h;
            if (w <= requested.width && h <= requested.height && area > bestArea) {
                best = i;
                bestArea = area;
            }
            if (area < smallestArea) {
                smallest = i;
                smallestArea = area;
            }
        }
        return best >= 0 ? best : smallest;
    }
}
