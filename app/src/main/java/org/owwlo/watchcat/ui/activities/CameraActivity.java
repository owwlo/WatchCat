package org.owwlo.watchcat.ui.activities;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ToggleButton;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.owwlo.watchcat.libstreaming.gl.SurfaceView;

import org.owwlo.watchcat.services.CameraDaemon;
import org.owwlo.watchcat.R;
import org.owwlo.watchcat.utils.Utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class CameraActivity extends Activity implements SurfaceHolder.Callback, View.OnClickListener {
    private final static String TAG = CameraActivity.class.getCanonicalName();

    private SurfaceView mSurfaceView = null;
    private ToggleButton mToogleCamera = null;
    private CameraDaemon mCameraDaemon = null;
    private Button mBack = null;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            CameraDaemon.LocalBinder binder = (CameraDaemon.LocalBinder) service;
            mCameraDaemon = binder.getService();
            setPreviewEnable(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            setPreviewEnable(false);
            mCameraDaemon = null;
        }
    };

    private void setPreviewEnable(boolean isEnabled) {
        if (isEnabled) {
            mCameraDaemon.startPreviewing(mSurfaceView, 180);
        } else {
            mCameraDaemon.stopPreviewing();
        }
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {

                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { // permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mToogleCamera = (ToggleButton) findViewById(R.id.toggle_camera_access);
        mBack = (Button) findViewById(R.id.btn_camera_back_main);

        mToogleCamera.setOnClickListener(this);
        mBack.setOnClickListener(this);

        // TODO do I need to start the service explicitly
        bindService(new Intent(this, CameraDaemon.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    50);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    0);
        }

        isStoragePermissionGranted();
        // mSurfaceView.getHolder().addCallback(this);
    }


    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.toggle_camera_access: {
                toggleCamera(mToogleCamera.isChecked());
                break;
            }
            case R.id.btn_camera_back_main: {
                finish();
                break;
            }
        }
    }

    private void toggleCamera(boolean isEnabled) {
        if (isEnabled) {
            File target = Utils.getPreviewPath();
            Bitmap bmp = mCameraDaemon.getLastPreviewImage();
            BufferedOutputStream bos = null;
            try {
                try {
                    bos = new BufferedOutputStream(new FileOutputStream(target));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                bmp.compress(Bitmap.CompressFormat.JPEG, 75, bos);
            } finally {
                bmp.recycle();
                if (bos != null) {
                    try {
                        bos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            Log.d(TAG, "preview saved: " + target.getAbsolutePath());

            setPreviewEnable(false);
            mCameraDaemon.startStream();

        } else {
            mCameraDaemon.stopStream();
            setPreviewEnable(true);
        }
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = isEnabled ? 0.0f : -1.0f;
        getWindow().setAttributes(layoutParams);
    }
}
