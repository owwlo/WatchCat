package org.owwlo.watchcat.ui.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.rtsp.RtspMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Util;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.greenrobot.eventbus.EventBus;
import org.owwlo.watchcat.ExoPlayer.BitmapOverlayVideoProcessor;
import org.owwlo.watchcat.ExoPlayer.VideoProcessingGLSurfaceView;
import org.owwlo.watchcat.R;
import org.owwlo.watchcat.utils.EventBus.VideoPlayerStateChangeEvent;
import org.owwlo.watchcat.utils.Toaster;

public final class ExoPlayerActivity extends Activity implements View.OnClickListener {
    public final static String TAG = ExoPlayerActivity.class.getSimpleName();

    public static final String INTENT_EXTRA_URI = "INTENT_EXTRA_URI";

    @Nullable
    private PlayerView playerView;

    @Nullable
    private VideoProcessingGLSurfaceView videoProcessingGLSurfaceView;

    @Nullable
    private SimpleExoPlayer player;

    private FloatingActionButton btnSnapshot = null;
    private FloatingActionButton btnBack = null;

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

        btnBack = findViewById(R.id.exoplayer_exit_button);
        btnSnapshot = findViewById(R.id.snapshot_button);
        btnBack.setOnClickListener(this);
        btnSnapshot.setOnClickListener(this);
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
        EventBus.getDefault().post(new VideoPlayerStateChangeEvent(VideoPlayerStateChangeEvent.State.PLAYING));
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
        EventBus.getDefault().post(new VideoPlayerStateChangeEvent(VideoPlayerStateChangeEvent.State.EXITING));
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

        if ((!intent.hasExtra(INTENT_EXTRA_URI))) {
            Toast.makeText(
                    context, R.string.error_no_stream_selected, Toast.LENGTH_LONG)
                    .show();
            this.finish();
        }

        Uri uri = Uri.parse(intent.getStringExtra(INTENT_EXTRA_URI));


        MediaItem mediaItem = MediaItem.fromUri(uri);
        RtspMediaSource mediaSource = new RtspMediaSource.Factory().setDebugLoggingEnabled(true).setForceUseRtpTcp(false).createMediaSource(mediaItem);

        Toaster.debug.info("Playing from URI:" + mediaItem.playbackProperties.uri);

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(1_000, 10_000, 500, 500).build();

        SimpleExoPlayer player = new ExoPlayer.Builder(getApplicationContext()).setLoadControl(loadControl).buildExoPlayer();
        player.setRepeatMode(Player.REPEAT_MODE_OFF);
        player.setMediaSource(mediaSource);
        player.prepare();
        player.play();

        VideoProcessingGLSurfaceView videoProcessingGLSurfaceView =
                Assertions.checkNotNull(this.videoProcessingGLSurfaceView);
        videoProcessingGLSurfaceView.setVideoComponent(
                Assertions.checkNotNull(player.getVideoComponent()));
        Assertions.checkNotNull(playerView).setPlayer(player);
        playerView.setUseController(false);
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

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.exoplayer_exit_button: {
                finish();
                break;
            }
            case R.id.snapshot_button: {
                videoProcessingGLSurfaceView.takeSnapshot();
                break;
            }
        }
    }
}
