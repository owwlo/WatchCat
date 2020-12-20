package org.owwlo.watchcat.ui.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.rtsp.RtspDefaultClient;
import com.google.android.exoplayer2.source.rtsp.RtspMediaSource;
import com.google.android.exoplayer2.source.rtsp.core.Client;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Util;

import org.owwlo.watchcat.ExoPlayer.BitmapOverlayVideoProcessor;
import org.owwlo.watchcat.ExoPlayer.VideoProcessingGLSurfaceView;
import org.owwlo.watchcat.R;

public final class ExoPlayerActivity extends Activity {
    public final static String TAG = ExoPlayerActivity.class.getSimpleName();

    public static final String INTENT_EXTRA_URI = "INTENT_EXTRA_URI";

    @Nullable
    private PlayerView playerView;
    @Nullable
    private VideoProcessingGLSurfaceView videoProcessingGLSurfaceView;

    @Nullable
    private SimpleExoPlayer player;

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exoplayer);
        playerView = findViewById(R.id.player_view);

        Context context = getApplicationContext();
        VideoProcessingGLSurfaceView videoProcessingGLSurfaceView =
                new VideoProcessingGLSurfaceView(
                        context, false, new BitmapOverlayVideoProcessor(context));
        FrameLayout contentFrame = findViewById(R.id.exo_content_frame);
        contentFrame.addView(videoProcessingGLSurfaceView);
        this.videoProcessingGLSurfaceView = videoProcessingGLSurfaceView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
            if (playerView != null) {
                playerView.onResume();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer();
            if (playerView != null) {
                playerView.onResume();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            if (playerView != null) {
                playerView.onPause();
            }
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            if (playerView != null) {
                playerView.onPause();
            }
            releasePlayer();
        }
    }

    private void initializePlayer() {
        Context context = getApplicationContext();
        Intent intent = getIntent();

        // test
        // intent.putExtra(INTENT_EXTRA_URI, "rtsp://192.168.0.101:57526/");

        if ((!intent.hasExtra(INTENT_EXTRA_URI))) {
            Toast.makeText(
                    context, R.string.error_no_stream_selected, Toast.LENGTH_LONG)
                    .show();
            this.finish();
        }

        Uri uri = Uri.parse(intent.getStringExtra(INTENT_EXTRA_URI));
        MediaSource mediaSource = new RtspMediaSource.Factory(RtspDefaultClient.factory()
                .setFlags(Client.FLAG_ENABLE_RTCP_SUPPORT)
                .setNatMethod(Client.RTSP_NAT_DUMMY))
                .createMediaSource(uri);

        SimpleExoPlayer player = new SimpleExoPlayer.Builder(getApplicationContext()).build();
        player.setRepeatMode(Player.REPEAT_MODE_OFF);
        player.prepare(mediaSource);
        player.setPlayWhenReady(true);
        VideoProcessingGLSurfaceView videoProcessingGLSurfaceView =
                Assertions.checkNotNull(this.videoProcessingGLSurfaceView);
        videoProcessingGLSurfaceView.setVideoComponent(
                Assertions.checkNotNull(player.getVideoComponent()));
        Assertions.checkNotNull(playerView).setPlayer(player);
        player.addAnalyticsListener(new EventLogger(/* trackSelector= */ null));
        this.player = player;
    }

    private void releasePlayer() {
        Assertions.checkNotNull(playerView).setPlayer(null);
        if (player != null) {
            player.release();
            Assertions.checkNotNull(videoProcessingGLSurfaceView).setVideoComponent(null);
            player = null;
        }
    }
}
