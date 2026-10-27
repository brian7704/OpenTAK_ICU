package io.opentakserver.opentakicu;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class StreamUrlUtilsTest {

    @Test
    public void normalizePath_removesLeadingSlash() {
        assertEquals("live/icu/user", StreamUrlUtils.normalizePath("/live/icu/user"));
    }

    @Test
    public void normalizePath_removesTrailingSlash() {
        assertEquals("live/icu/user", StreamUrlUtils.normalizePath("live/icu/user/"));
    }

    @Test
    public void normalizePath_collapsesRepeatedSlashes() {
        assertEquals("live/icu/user", StreamUrlUtils.normalizePath("//live//icu///user//"));
    }

    @Test
    public void normalizePath_trimsWhitespace() {
        assertEquals("live/icu/user", StreamUrlUtils.normalizePath("  /live/icu/user  "));
    }

    @Test
    public void normalizePath_keepsValidPath() {
        assertEquals("my_path", StreamUrlUtils.normalizePath("my_path"));
        assertEquals("live/icu/user", StreamUrlUtils.normalizePath("live/icu/user"));
    }

    @Test
    public void normalizePath_handlesEmptyAndNull() {
        assertEquals("", StreamUrlUtils.normalizePath(null));
        assertEquals("", StreamUrlUtils.normalizePath(""));
        assertEquals("", StreamUrlUtils.normalizePath("/"));
    }

    @Test
    public void redactUrl_masksRtmpQueryPassword() {
        assertEquals("rtmps://example.com:1936/live/icu/user?user=user&pass=***",
                StreamUrlUtils.redactUrl("rtmps://example.com:1936/live/icu/user?user=user&pass=secret"));
    }

    @Test
    public void redactUrl_masksSrtStreamIdPassword() {
        assertEquals("srt://example.com:8890?streamid=publish:live/icu/user:user:***",
                StreamUrlUtils.redactUrl("srt://example.com:8890?streamid=publish:live/icu/user:user:secret"));
    }

    @Test
    public void redactUrl_masksUserInfoPassword() {
        assertEquals("rtsps://user:***@example.com:8322/live/icu/user",
                StreamUrlUtils.redactUrl("rtsps://user:secret@example.com:8322/live/icu/user"));
    }

    @Test
    public void redactUrl_leavesUrlWithoutPasswordUnchanged() {
        String url = "rtsp://192.168.1.10:8554/my_path";
        assertEquals(url, StreamUrlUtils.redactUrl(url));
        assertNull(StreamUrlUtils.redactUrl(null));
    }
}
