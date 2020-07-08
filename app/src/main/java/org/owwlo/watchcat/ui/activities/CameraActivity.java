package org.owwlo.watchcat.ui.activities;

import android.Manifest;
import android.app.Dialog;
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
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.owwlo.watchcat.R;
import org.owwlo.watchcat.libstreaming.gl.SurfaceView;
import org.owwlo.watchcat.services.CameraDaemon;
import org.owwlo.watchcat.utils.Utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class CameraActivity extends FragmentActivity implements SurfaceHolder.Callback, View.OnClickListener {
    private final static String TAG = CameraActivity.class.getCanonicalName();

    private SurfaceView mSurfaceView = null;
    private CameraDaemon mCameraDaemon = null;
    private FloatingActionButton mBtnSettings = null;
    private View mBtnToggleCamera = null;
    private FloatingActionButton mBtnToggleCameraFab = null;
    private FloatingActionButton mBtnBack = null;

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
        mSurfaceView = findViewById(R.id.surface);
        mBtnSettings = findViewById(R.id.camera_setting_button);
        mBtnToggleCamera = findViewById(R.id.camera_start_serving_button);
        mBtnToggleCameraFab = findViewById(R.id.camera_start_serving_button_fab);
        mBtnBack = findViewById(R.id.camera_exit_button);

        mBtnSettings.setOnClickListener(this);
        mBtnToggleCamera.setOnClickListener(this);
        mBtnToggleCameraFab.setOnClickListener(this);
        mBtnBack.setOnClickListener(this);

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

    public static class CameraSettingsDialogFragment extends DialogFragment implements View.OnClickListener {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = requireActivity().getLayoutInflater();
            View inner = inflater.inflate(R.layout.camera_settings_dialog, null);

            Button apply = inner.findViewById(R.id.settings_done_button);
            Button cancel = inner.findViewById(R.id.settings_cancel_button);
            apply.setOnClickListener(CameraSettingsDialogFragment.this);
            cancel.setOnClickListener(CameraSettingsDialogFragment.this);

            return builder.setView(inner).create();
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            switch (id) {
                case R.id.settings_done_button: {
                    CameraSettingsDialogFragment.this.dismiss();
                }
                case R.id.settings_cancel_button: {
                    CameraSettingsDialogFragment.this.dismiss();
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.camera_start_serving_button_fab:
            case R.id.camera_start_serving_button: {
                toggleCamera(true);
                break;
            }
            case R.id.camera_exit_button: {
                toggleCamera(false);
                finish();
                break;
            }
            case R.id.camera_setting_button: {
                DialogFragment newFragment = new CameraSettingsDialogFragment();
                newFragment.show(getSupportFragmentManager(), null);
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
