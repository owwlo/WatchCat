package org.owwlo.watchcat.ui.activities;

import android.Manifest;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.listener.multi.BaseMultiplePermissionsListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owwlo.watchcat.R;
import org.owwlo.watchcat.libstreaming.gl.SurfaceView;
import org.owwlo.watchcat.services.CameraDaemon;
import org.owwlo.watchcat.services.ServiceDaemon;
import org.owwlo.watchcat.utils.EventBus.IncomingAuthorizationCancelEvent;
import org.owwlo.watchcat.utils.EventBus.IncomingAuthorizationRequestEvent;

import java.util.Hashtable;
import java.util.Map;

import static android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
import static android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;

public class CameraActivity extends FragmentActivity implements SurfaceHolder.Callback, View.OnClickListener, SensorEventListener {
    private final static String TAG = CameraActivity.class.getCanonicalName();

    public static class PermissionsListener extends BaseMultiplePermissionsListener {
        public interface CheckCallback {
            void results(MultiplePermissionsReport report);
        }

        private final Context context;
        private final CheckCallback resultsCallback;

        private PermissionsListener(Context context,
                                    CheckCallback allGrantedCallback) {
            this.context = context;
            this.resultsCallback = allGrantedCallback;
        }

        @Override
        public void onPermissionsChecked(MultiplePermissionsReport report) {
            super.onPermissionsChecked(report);
            if (!report.areAllPermissionsGranted()) {
                showDialog();
            }
            resultsCallback.results(report);

        }

        private void showDialog() {
            new AlertDialog.Builder(context)
                    .setTitle("Permissions Require")
                    .setMessage("Camera & Audio: required to record the live stream; Storage: required to store the thumbnail.")
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        }
    }

    private SurfaceView mSurfaceView = null;
    private FloatingActionButton mBtnSettings = null;
    private View mBtnToggleCamera = null;
    private FloatingActionButton mBtnToggleCameraFab = null;
    private FloatingActionButton mBtnBack = null;
    private View mStopActionOverlayView = null;
    private Button mStopActionBtn = null;
    private SensorManager mSensorManager = null;

    private int mLastOrientation = 0;
    private boolean mLastFlip = false;

    private CameraDaemon mCameraDaemon = null;
    private ServiceDaemon mServiceDaemon = null;
    private ServiceConnection mCameraDaemonConnection = new ServiceConnection() {
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
    private ServiceConnection mServiceDaemonConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            ServiceDaemon.LocalBinder binder = (ServiceDaemon.LocalBinder) service;
            mServiceDaemon = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mServiceDaemon = null;
        }
    };

    boolean getFlip() {
        boolean flip = false;
        switch (mLastOrientation) {
            case Surface.ROTATION_180:
            case Surface.ROTATION_270:
                flip = true;
                break;
        }
        return flip;
    }

    private void setPreviewEnable(boolean isEnabled) {
        if (isEnabled) {
            mCameraDaemon.startPreviewing(mSurfaceView, getFlip());
        } else {
            mCameraDaemon.stopPreviewing();
        }
    }

    private void restartPreviewing() {
        mCameraDaemon.startPreviewing(mSurfaceView, getFlip());
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
        mStopActionOverlayView = findViewById(R.id.stop_overlay);
        mStopActionBtn = findViewById(R.id.stop_streaming_button);

        mBtnSettings.setOnClickListener(this);
        mBtnToggleCamera.setOnClickListener(this);
        mBtnToggleCameraFab.setOnClickListener(this);
        mBtnBack.setOnClickListener(this);
        mStopActionOverlayView.setOnClickListener(this);
        mStopActionBtn.setOnClickListener(this);

        mStopActionOverlayView.setVisibility(View.INVISIBLE);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    protected void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);

        mSurfaceView.getHolder().addCallback(this);

        Dexter.withContext(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                ).withListener(new PermissionsListener(this, report -> {
            if (report.areAllPermissionsGranted()) {
                // TODO do I need to start the service explicitly
                bindService(new Intent(this, CameraDaemon.class), mCameraDaemonConnection, Context.BIND_AUTO_CREATE);
            } else {
                this.finish();
            }
        })).onSameThread().check();

        bindService(new Intent(this, ServiceDaemon.class), mServiceDaemonConnection, Context.BIND_AUTO_CREATE);
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.v(TAG, "onConfigurationChanged");
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
        unbindService(mServiceDaemonConnection);
        unbindService(mCameraDaemonConnection);
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.v(TAG, "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "surfaceDestroyed");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        if (mCameraDaemon != null && rotation != mLastOrientation) {
            Log.d(TAG, "new rotation: " + rotation);
            mLastOrientation = rotation;
            if (mLastFlip != getFlip()) {
                mLastFlip = getFlip();
                restartPreviewing();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
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

    public void toggleBrightness(boolean dark) {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = dark ? BRIGHTNESS_OVERRIDE_OFF : BRIGHTNESS_OVERRIDE_NONE;
        getWindow().setAttributes(layoutParams);
    }

    public class PasscodeDialog {
        AlertDialog dialog;
        CameraActivity activity;

        public PasscodeDialog(CameraActivity activity, final String incomingId, final String incomingName, final String passcode) {
            this.activity = activity;
            View view = getLayoutInflater().inflate(R.layout.dialog_passcode, null);
            dialog = new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_MaterialAlertDialog_Rounded)
                    .setView(view)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            toggleBrightness(true);
                            activity.passcodeDialogs.remove(incomingId);
                        }
                    })
                    .create();

            TextView textPasscode = view.findViewById(R.id.text_passcode);
            TextView textFor = view.findViewById(R.id.text_passcode_for);
            textPasscode.setText(passcode);
            textFor.setText(incomingName);
        }

        public void show() {
            toggleBrightness(false);
            dialog.show();
        }

        public AlertDialog getDialog() {
            return dialog;
        }
    }

    private Map<String, PasscodeDialog> passcodeDialogs = new Hashtable<>();

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void onMessageEvent(IncomingAuthorizationRequestEvent event) {
        PasscodeDialog dialog = new PasscodeDialog(this, event.getIncomingId(), event.getName(), event.getPasscode());
        passcodeDialogs.put(event.getIncomingId(), dialog);
        dialog.show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void onMessageEvent(IncomingAuthorizationCancelEvent event) {
        PasscodeDialog dialog = passcodeDialogs.get(event.getIncomingId());
        if (dialog != null) {
            dialog.getDialog().dismiss();
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
            case R.id.stop_streaming_button: {
                toggleCamera(false);
            }
        }
    }

    void setAllowOrientate(boolean isAllow) {
        if (isAllow) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);

        } else {
            int requestedOrientation = 0;
            switch (mLastOrientation) {
                case Surface.ROTATION_0: {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                }
                case Surface.ROTATION_90: {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                }
                case Surface.ROTATION_180: {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                }
                case Surface.ROTATION_270: {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                }
            }
            setRequestedOrientation(requestedOrientation);
        }
    }

    private void toggleCamera(boolean isEnabled) {
        setAllowOrientate(!isEnabled);
        mStopActionOverlayView.setVisibility(isEnabled ? View.VISIBLE : View.INVISIBLE);
        if (isEnabled) {
            setPreviewEnable(false);
            mCameraDaemon.startStream();

        } else {
            mCameraDaemon.stopStream();
            setPreviewEnable(true);
        }
        toggleBrightness(isEnabled);
    }
}
