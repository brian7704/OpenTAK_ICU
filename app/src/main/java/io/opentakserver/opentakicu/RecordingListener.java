package io.opentakserver.opentakicu;

import android.util.Log;

import com.pedro.library.base.recording.RecordController;

public class RecordingListener implements RecordController.Listener{
    private static final String LOGTAG = "RecordingListener";

    @Override
    public void onStatusChange(RecordController.Status status) {
        Log.d(LOGTAG, status.name());
    }

    @Override
    public void onError(Exception e) {
        RecordController.Listener.super.onError(e);
        Log.d(LOGTAG, "Recording error", e);
    }
}
