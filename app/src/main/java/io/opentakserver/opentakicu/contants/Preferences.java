package io.opentakserver.opentakicu.contants;

import com.pedro.common.AudioCodec;
import com.pedro.common.VideoCodec;

import java.util.UUID;

public class Preferences {
    public static final String UID = "uid";
    public static final String UID_DEFAULT = "OpenTAK-ICU-" + UUID.randomUUID().toString();

    /* Streaming Preferences */
    public static final String STREAM_VIDEO = "stream_video";
    public static final boolean STREAM_VIDEO_DEFAULT = true;
    public static final String STREAM_PROTOCOL = "protocol";
    public static final String STREAM_PROTOCOL_DEFAULT = "rtsp";
    public static final String STREAM_ADDRESS = "address";
    public static final String STREAM_ADDRESS_DEFAULT = "192.168.1.10";
    public static final String STREAM_PORT = "port";
    public static final String STREAM_PORT_DEFAULT = "8554";
    public static final String STREAM_PATH = "path";
    public static final String STREAM_PATH_DEFAULT = "my_path";
    public static final String STREAM_USERNAME = "username";
    public static final String STREAM_USERNAME_DEFAULT = "";
    public static final String STREAM_PASSWORD = "password";
    public static final String STREAM_PASSWORD_DEFAULT = "";
    public static final String STREAM_USE_TCP = "tcp";
    public static final boolean STREAM_USE_TCP_DEFAULT = false;
    public static final String STREAM_SELF_SIGNED_CERT = "self_signed_cert";
    public static final boolean STREAM_SELF_SIGNED_CERT_DEFAULT = false;
    public static final String STREAM_CERTIFICATE = "certificate";
    public static final String STREAM_CERTIFICATE_DEFAULT = null;
    public static final String STREAM_CERTIFICATE_PASSWORD = "certificate_password";
    public static final String STREAM_CERTIFICATE_PASSWORD_DEFAULT = "atakatak";

    /* Video Preferences */
    public static final String VIDEO_RESOLUTION = "resolution";
    public static final String VIDEO_RESOLUTION_DEFAULT = "0";
    public static final String VIDEO_BITRATE = "bitrate";
    public static final String VIDEO_BITRATE_DEFAULT = "1000";
    public static final String VIDEO_ADAPTIVE_BITRATE = "adaptive_bitrate";
    public static final boolean VIDEO_ADAPTIVE_BITRATE_DEFAULT = true;
    public static final String VIDEO_FPS = "fps";
    public static final String VIDEO_FPS_DEFAULT = "30";
    public static final String VIDEO_CODEC = "codec";
    public static final String VIDEO_CODEC_DEFAULT = VideoCodec.H264.name();
    public static final String RECORD_VIDEO = "record";
    public static final boolean RECORD_VIDEO_DEFAULT = false;
    public static final String USB_WIDTH = "usb_width";
    public static final String USB_WIDTH_DEFAULT = "1920";
    public static final String USB_HEIGHT = "usb_height";
    public static final String USB_HEIGHT_DEFAULT = "1080";
    public static final String VIDEO_SOURCE = "video_source";
    public static final String VIDEO_SOURCE_DEFAULT = "camera2";
    public static final String VIDEO_SOURCE_USB = "usb";
    public static final String VIDEO_SOURCE_SCREEN = "screen";
    public static final String TEXT_OVERLAY = "text_overlay";
    public static final boolean TEXT_OVERLAY_DEFAULT = false;
    public static final String TEXT_OVERLAY_TIMEZONE = "text_overlay_timezone";
    public static final boolean TEXT_OVERLAY_TIMEZONE_DEFAULT = true;
    public static final String CHROMA_KEY_BACKGROUND = "chroma_bg";
    public static final String CHROMA_KEY_BACKGROUND_DEFAULT = null;

    /* Audio Preferences */
    public static final String ENABLE_AUDIO = "enable_audio";
    public static final boolean ENABLE_AUDIO_DEFAULT = true;
    public static final String AUDIO_BITRATE = "audio_bitrate";
    public static final String AUDIO_BITRATE_DEFAULT = "128";
    public static final String AUDIO_SAMPLE_RATE = "samplerate";
    public static final String AUDIO_SAMPLE_RATE_DEFAULT = "44100";
    public static final String AUDIO_CODEC = "audio_codec";
    public static final String AUDIO_CODEC_DEFAULT = AudioCodec.OPUS.name();
    public static final String STEREO_AUDIO = "stereo";
    public static final boolean STEREO_AUDIO_DEFAULT = true;
    public static final String AUDIO_ECHO_CANCEL = "echo_cancel";
    public static final boolean AUDIO_ECHO_CANCEL_DEFAULT = true;
    public static final String AUDIO_NOISE_REDUCTION = "noise_reduction";
    public static final boolean AUDIO_NOISE_REDUCTION_DEFAULT = true;

    /* ATAK Preferences */
    public static final String ATAK_SEND_COT = "send_cot";
    public static final boolean ATAK_SEND_COT_DEFAULT = false;
    public static final String ATAK_SEND_STREAM_DETAILS = "send_stream_details";
    public static final boolean ATAK_SEND_STREAM_DETAILS_DEFAULT = false;
    public static final String ATAK_SERVER_ADDRESS = "atak_address";
    public static final String ATAK_SERVER_ADDRESS_DEFAULT = "192.168.1.10";
    public static final String ATAK_SERVER_PORT = "atak_port";
    public static final String ATAK_SERVER_PORT_DEFAULT = "8088";
    public static final String ATAK_SERVER_AUTHENTICATION = "atak_auth";
    public static final boolean ATAK_SERVER_AUTHENTICATION_DEFAULT = false;
    public static final String ATAK_SERVER_USERNAME = "atak_username";
    public static final String ATAK_SERVER_USERNAME_DEFAULT = null;
    public static final String ATAK_SERVER_PASSWORD = "atak_password";
    public static final String ATAK_SERVER_PASSWORD_DEFAULT = null;
    public static final String ATAK_SERVER_SSL = "atak_ssl";
    public static final boolean ATAK_SERVER_SSL_DEFAULT = false;
    public static final String ATAK_SERVER_SELF_SIGNED_CERT = "atak_ssl_self_signed";
    public static final boolean ATAK_SERVER_SELF_SIGNED_CERT_DEFAULT = false;
    public static final String ATAK_SERVER_SSL_TRUST_STORE = "trust_store_certificate";
    public static final String ATAK_SERVER_SSL_TRUST_STORE_DEFAULT = null;
    public static final String ATAK_SERVER_SSL_TRUST_STORE_PASSWORD = "trust_store_cert_password";
    public static final String ATAK_SERVER_SSL_TRUST_STORE_PASSWORD_DEFAULT = "atakatak";
    public static final String ATAK_SERVER_SSL_CLIENT_CERTIFICATE = "client_certificate";
    public static final String ATAK_SERVER_SSL_CLIENT_CERTIFICATE_DEFAULT = null;
    public static final String ATAK_SERVER_SSL_CLIENT_CERTIFICATE_PASSWORD = "client_cert_password";
    public static final String ATAK_SERVER_SSL_CLIENT_CERTIFICATE_PASSWORD_DEFAULT = "atakatak";
}
