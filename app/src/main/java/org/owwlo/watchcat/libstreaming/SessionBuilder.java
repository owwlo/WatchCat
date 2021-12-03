/*
 * Copyright (C) 2011-2015 GUIGUI Simon, fyhertz@gmail.com
 *
 * This file is part of libstreaming (https://github.com/fyhertz/libstreaming)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.owwlo.watchcat.libstreaming;

import android.content.Context;
import android.hardware.Camera.CameraInfo;
import android.preference.PreferenceManager;

import org.owwlo.watchcat.libstreaming.audio.AACStream;
import org.owwlo.watchcat.libstreaming.audio.AudioQuality;
import org.owwlo.watchcat.libstreaming.audio.AudioStream;
import org.owwlo.watchcat.libstreaming.gl.SurfaceView;
import org.owwlo.watchcat.libstreaming.video.H264Stream;
import org.owwlo.watchcat.libstreaming.video.VideoStream;

import java.io.IOException;

/**
 * Call {@link #getInstance()} to get access to the SessionBuilder.
 */
public class SessionBuilder {

    public final static String TAG = "SessionBuilder";

    /**
     * Can be used with {@link #setVideoEncoder}.
     */
    public final static int VIDEO_H264 = 1;

    /**
     * Can be used with {@link #setAudioEncoder}.
     */
    public final static int AUDIO_NONE = 0;

    /**
     * Can be used with {@link #setAudioEncoder}.
     */
    public final static int AUDIO_AAC = 5;

    private AudioQuality mAudioQuality = AudioQuality.DEFAULT_AUDIO_QUALITY;
    private Context mContext;
    private int mVideoEncoder = VIDEO_H264;
    private int mAudioEncoder = AUDIO_NONE;
    private int mCamera = CameraInfo.CAMERA_FACING_BACK;
    private int mTimeToLive = 64;
    private boolean mFlip = false;
    private boolean mFlash = false;
    private SurfaceView mSurfaceView = null;
    private String mOrigin = null;
    private String mDestination = null;
    private Session.Callback mCallback = null;
    private String auth = "";

    // Removes the default public constructor
    private SessionBuilder() {
    }

    // The SessionManager implements the singleton pattern
    private static volatile SessionBuilder sInstance = null;

    /**
     * Returns a reference to the {@link SessionBuilder}.
     *
     * @return The reference to the {@link SessionBuilder}
     */
    public final static SessionBuilder getInstance() {
        if (sInstance == null) {
            synchronized (SessionBuilder.class) {
                if (sInstance == null) {
                    SessionBuilder.sInstance = new SessionBuilder();
                }
            }
        }
        return sInstance;
    }

    /**
     * Creates a new {@link Session}.
     *
     * @return The new Session
     * @throws IOException
     */
    public Session build() {
        Session session;

        session = new Session();
        session.setOrigin(mOrigin);
        session.setDestination(mDestination);
        session.setTimeToLive(mTimeToLive);
        session.setCallback(mCallback);
        session.setAuth(auth);

        switch (mAudioEncoder) {
            case AUDIO_AAC:
                AACStream stream = new AACStream();
                session.addAudioTrack(stream);
                if (mContext != null)
                    stream.setPreferences(PreferenceManager.getDefaultSharedPreferences(mContext));
                break;
        }

        switch (mVideoEncoder) {
            case VIDEO_H264:
                H264Stream stream = new H264Stream(mCamera);
                if (mContext != null)
                    stream.setPreferences(PreferenceManager.getDefaultSharedPreferences(mContext));
                session.addVideoTrack(stream);
                break;
        }

        if (session.getVideoTrack() != null) {
            VideoStream video = session.getVideoTrack();
            video.setFlashState(mFlash);
            video.setSurfaceView(mSurfaceView);
            video.setFlipImage(mFlip);
            video.setDestinationPorts(5006);
        }

        if (session.getAudioTrack() != null) {
            AudioStream audio = session.getAudioTrack();
            audio.setAudioQuality(mAudioQuality);
            audio.setDestinationPorts(5004);
        }
        session.configure();
        return session;

    }

    /**
     * Access to the context is needed for the H264Stream class to store some stuff in the SharedPreferences.
     * Note that you should pass the Application context, not the context of an Activity.
     **/
    public SessionBuilder setContext(Context context) {
        mContext = context;
        return this;
    }

    /**
     * Sets the destination of the session.
     */
    public SessionBuilder setDestination(String destination) {
        mDestination = destination;
        return this;
    }

    /**
     * Sets the origin of the session. It appears in the SDP of the session.
     */
    public SessionBuilder setOrigin(String origin) {
        mOrigin = origin;
        return this;
    }

    /**
     * Sets the audio encoder.
     */
    public SessionBuilder setAudioEncoder(int encoder) {
        mAudioEncoder = encoder;
        return this;
    }

    /**
     * Sets the audio quality.
     */
    public SessionBuilder setAudioQuality(AudioQuality quality) {
        mAudioQuality = quality.clone();
        return this;
    }

    /**
     * Sets the default video encoder.
     */
    public SessionBuilder setVideoEncoder(int encoder) {
        mVideoEncoder = encoder;
        return this;
    }

    public SessionBuilder setFlashEnabled(boolean enabled) {
        mFlash = enabled;
        return this;
    }

    public SessionBuilder setCamera(int camera) {
        mCamera = camera;
        return this;
    }

    public SessionBuilder setTimeToLive(int ttl) {
        mTimeToLive = ttl;
        return this;
    }

    /**
     * Sets the SurfaceView required to preview the video stream.
     **/
    public SessionBuilder setSurfaceView(SurfaceView surfaceView) {
        mSurfaceView = surfaceView;
        return this;
    }

    public SessionBuilder setPreviewFlip(boolean flip) {
        mFlip = flip;
        return this;
    }

    public SessionBuilder setCallback(Session.Callback callback) {
        mCallback = callback;
        return this;
    }

    public SessionBuilder setAuth(String auth) {
        this.auth = auth;
        return this;
    }

    /**
     * Returns the context set with {@link #setContext(Context)}
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Returns the id of the {@link android.hardware.Camera} set with {@link #setCamera(int)}.
     */
    public int getCamera() {
        return mCamera;
    }

    /**
     * Returns a new {@link SessionBuilder} with the same configuration.
     */
    public SessionBuilder clone() {
        return new SessionBuilder()
                .setDestination(mDestination)
                .setOrigin(mOrigin)
                .setSurfaceView(mSurfaceView)
                .setPreviewFlip(mFlip)
                .setVideoEncoder(mVideoEncoder)
                .setFlashEnabled(mFlash)
                .setCamera(mCamera)
                .setTimeToLive(mTimeToLive)
                .setAudioEncoder(mAudioEncoder)
                .setAudioQuality(mAudioQuality)
                .setContext(mContext)
                .setCallback(mCallback)
                .setAuth(auth);
    }

}
