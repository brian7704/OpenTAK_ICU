package io.opentakserver.opentakicu;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.opentakserver.opentakicu.contants.Preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ImportSchemaTest {

    private static Map<String, String> params(String... keyValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    @Test
    public void parse_fullLink_acceptsAllSettingsWithTypes() {
        ImportSchema.Result result = ImportSchema.parse(params(
                "protocol", "rtmps", "address", "example.com", "port", "1936",
                "path", "/live/icu/callsign", "username", "callsign", "password", "secret",
                "stream_video", "true", "video_source", "camera2", "resolution", "1280x720",
                "bitrate", "3000", "adaptive_bitrate", "true", "fps", "15", "codec", "H264",
                "record", "false", "text_overlay", "true", "enable_audio", "false", "send_cot", "false"));

        assertTrue(result.rejected.isEmpty());
        assertEquals("rtmps", result.accepted.get(Preferences.STREAM_PROTOCOL));
        assertEquals("1936", result.accepted.get(Preferences.STREAM_PORT));
        assertEquals("live/icu/callsign", result.accepted.get(Preferences.STREAM_PATH));
        assertEquals(Boolean.TRUE, result.accepted.get(Preferences.STREAM_VIDEO));
        assertEquals(Boolean.FALSE, result.accepted.get(Preferences.ENABLE_AUDIO));
        assertEquals(Boolean.FALSE, result.accepted.get(Preferences.ATAK_SEND_COT));
        assertEquals("3000", result.accepted.get(Preferences.VIDEO_BITRATE));
        assertEquals("1280x720", result.accepted.get(Preferences.VIDEO_RESOLUTION).toString());
    }

    @Test
    public void parse_oldSixKeyLink_stillWorks() {
        ImportSchema.Result result = ImportSchema.parse(params(
                "protocol", "rtmps", "address", "example.com", "port", "1936",
                "path", "/live/icu/user", "username", "user", "password", "pass"));

        assertEquals(6, result.accepted.size());
        assertTrue(result.rejected.isEmpty());
    }

    @Test
    public void parse_acceptedValuesFollowSchemaOrder() {
        ImportSchema.Result result = ImportSchema.parse(params("send_cot", "false", "protocol", "rtsp"));
        assertEquals(Arrays.asList(Preferences.STREAM_PROTOCOL, Preferences.ATAK_SEND_COT),
                Arrays.asList(result.accepted.keySet().toArray()));
    }

    @Test
    public void parse_rejectsUnknownKeys() {
        ImportSchema.Result result = ImportSchema.parse(params("protocol", "rtsp", "foo", "bar"));
        assertEquals("bar", result.rejected.get("foo"));
        assertFalse(result.accepted.containsKey("foo"));
    }

    @Test
    public void parse_rejectsInvalidValues() {
        ImportSchema.Result result = ImportSchema.parse(params(
                "protocol", "http", "port", "70000", "fps", "25", "codec", "VP8",
                "enable_audio", "maybe", "bitrate", "abc", "resolution", "hd", "path", "/",
                "address", "", "video_source", "screen"));

        assertTrue(result.accepted.isEmpty());
        assertEquals(10, result.rejected.size());
    }

    @Test
    public void convert_addressMustBeNonEmptyWithoutSpaces() {
        ImportSchema.Entry entry = ImportSchema.entry(Preferences.STREAM_ADDRESS);
        assertEquals("example.com", ImportSchema.convert(entry, " example.com "));
        assertEquals("192.168.1.10", ImportSchema.convert(entry, "192.168.1.10"));
        assertEquals(null, ImportSchema.convert(entry, ""));
        assertEquals(null, ImportSchema.convert(entry, "   "));
        assertEquals(null, ImportSchema.convert(entry, "exa mple.com"));
    }

    @Test
    public void convert_videoSourceExcludesScreen() {
        ImportSchema.Entry entry = ImportSchema.entry(Preferences.VIDEO_SOURCE);
        assertEquals("camera2", ImportSchema.convert(entry, "camera2"));
        assertEquals("usb", ImportSchema.convert(entry, "usb"));
        assertEquals(null, ImportSchema.convert(entry, "screen"));
    }

    @Test
    public void convert_booleanVariants() {
        ImportSchema.Entry entry = ImportSchema.entry(Preferences.TEXT_OVERLAY_TIMEZONE);
        assertEquals(Boolean.TRUE, ImportSchema.convert(entry, "TRUE"));
        assertEquals(Boolean.TRUE, ImportSchema.convert(entry, "1"));
        assertEquals(Boolean.FALSE, ImportSchema.convert(entry, "false"));
        assertEquals(Boolean.FALSE, ImportSchema.convert(entry, "0"));
    }

    @Test
    public void convert_enumIsCaseInsensitiveAndCanonical() {
        assertEquals("rtmps", ImportSchema.convert(ImportSchema.entry(Preferences.STREAM_PROTOCOL), "RTMPS"));
        assertEquals("H264", ImportSchema.convert(ImportSchema.entry(Preferences.VIDEO_CODEC), "h264"));
    }

    @Test
    public void convert_resolutionFormats() {
        ImportSchema.Entry entry = ImportSchema.entry(Preferences.VIDEO_RESOLUTION);
        assertEquals("1280x720", ImportSchema.convert(entry, "1280x720").toString());
        assertEquals("1920x1080", ImportSchema.convert(entry, "1920 x 1080").toString());
        assertEquals("640x480", ImportSchema.convert(entry, "640X480").toString());
    }

    @Test
    public void pickResolutionIndex_exactMatch() {
        List<int[]> supported = Arrays.asList(new int[]{1920, 1080}, new int[]{1280, 720}, new int[]{640, 480});
        assertEquals(1, ImportSchema.pickResolutionIndex(supported, new ImportSchema.Resolution(1280, 720)));
    }

    @Test
    public void pickResolutionIndex_largestThatFits() {
        List<int[]> supported = Arrays.asList(new int[]{1920, 1080}, new int[]{1024, 768}, new int[]{960, 540}, new int[]{640, 480});
        // 1024x768 is taller than 720, so the largest size that fits is 960x540
        assertEquals(2, ImportSchema.pickResolutionIndex(supported, new ImportSchema.Resolution(1280, 720)));
    }

    @Test
    public void pickResolutionIndex_smallestWhenNothingFits() {
        List<int[]> supported = Arrays.asList(new int[]{3840, 2160}, new int[]{1920, 1080});
        assertEquals(1, ImportSchema.pickResolutionIndex(supported, new ImportSchema.Resolution(1280, 720)));
    }

    @Test
    public void pickResolutionIndex_emptyList() {
        assertEquals(-1, ImportSchema.pickResolutionIndex(Collections.<int[]>emptyList(), new ImportSchema.Resolution(1280, 720)));
    }
}
