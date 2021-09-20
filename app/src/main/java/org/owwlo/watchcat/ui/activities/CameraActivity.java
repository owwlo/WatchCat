package org.owwlo.watchcat.ui.activities;

import static android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
import static android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;

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
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.material.button.MaterialButton;
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
import org.owwlo.watchcat.utils.SharedPreferences;

import java.util.Hashtable;
import java.util.Map;

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

    private SurfaceView surfaceView = null;
    private FloatingActionButton btnSettings = null;
    private View btnToggleCamera = null;
    private FloatingActionButton btnToggleCameraFab = null;
    private FloatingActionButton btnBack = null;
    private View stopActionOverlayView = null;
    private MaterialButton stopActionBtn = null;
    private SensorManager sensorManager = null;
    private SharedPreferences sharedPreferences = null;

    private int lastOrientation = 0;
    private boolean lastFlip = false;

    private CameraDaemon cameraDaemon = null;
    private ServiceDaemon serviceDaemon = null;
    private ServiceConnection cameraDaemonConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            CameraDaemon.LocalBinder binder = (CameraDaemon.LocalBinder) service;
            cameraDaemon = binder.getService();
            setPreviewEnable(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            setPreviewEnable(false);
            cameraDaemon = null;
        }
    };
    private ServiceConnection serviceDaemonConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            ServiceDaemon.LocalBinder binder = (ServiceDaemon.LocalBinder) service;
            serviceDaemon = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceDaemon = null;
        }
    };

    boolean getFlip() {
        boolean flip = false;
        switch (lastOrientation) {
            case Surface.ROTATION_180:
            case Surface.ROTATION_270:
                flip = true;
                break;
        }
        return flip;
    }

    private void setPreviewEnable(boolean isEnabled) {
        if (isEnabled) {
            cameraDaemon.startPreviewing(surfaceView, getFlip());
        } else {
            cameraDaemon.stopPreviewing();
        }
    }

    private void restartPreviewing() {
        cameraDaemon.startPreviewing(surfaceView, getFlip());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera);
        surfaceView = findViewById(R.id.surface);
        btnSettings = findViewById(R.id.camera_setting_button);
        btnToggleCamera = findViewById(R.id.camera_start_serving_button);
        btnToggleCameraFab = findViewById(R.id.camera_start_serving_button_fab);
        btnBack = findViewById(R.id.camera_exit_button);
        stopActionOverlayView = findViewById(R.id.stop_overlay);
        stopActionBtn = findViewById(R.id.stop_streaming_button);

        btnSettings.setOnClickListener(this);
        btnToggleCamera.setOnClickListener(this);
        btnToggleCameraFab.setOnClickListener(this);
        btnBack.setOnClickListener(this);
        stopActionOverlayView.setOnClickListener(this);
        stopActionBtn.setOnClickListener(this);

        stopActionOverlayView.setVisibility(View.INVISIBLE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

        sharedPreferences = new SharedPreferences(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);

        surfaceView.getHolder().addCallback(this);

        Dexter.withContext(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                ).withListener(new PermissionsListener(this, report -> {
            if (report.areAllPermissionsGranted()) {
                // TODO do I need to start the service explicitly
                bindService(new Intent(this, CameraDaemon.class), cameraDaemonConnection, Context.BIND_AUTO_CREATE);
            } else {
                this.finish();
            }
        })).onSameThread().check();

        bindService(new Intent(this, ServiceDaemon.class), serviceDaemonConnection, Context.BIND_AUTO_CREATE);
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
        unbindService(serviceDaemonConnection);
        unbindService(cameraDaemonConnection);
        sensorManager.unregisterListener(this);
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
        if (cameraDaemon != null && rotation != lastOrientation) {
            Log.d(TAG, "new rotation: " + rotation);
            lastOrientation = rotation;
            if (lastFlip != getFlip()) {
                lastFlip = getFlip();
                restartPreviewing();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public class CameraSettingsDialog {
        AlertDialog dialog;
        Spinner streamingQualitySpinner;

        public CameraSettingsDialog(CameraActivity activity) {
            View view = getLayoutInflater().inflate(R.layout.dialog_camera_mode_settings, null, false);
            dialog = new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_MaterialAlertDialog_Rounded).setTitle(R.string.fab_authorization)
                    .setView(view)
                    .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cameraDaemon.getCameraParams().setSelectedProfile(streamingQualitySpinner.getSelectedItemPosition());
                            dialog.dismiss();
                        }
                    })
                    .create();

            streamingQualitySpinner = view.findViewById(R.id.spinner_streaming_quality);
            String[] items = cameraDaemon.getCameraParams().getSupportedProfilesDesc();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, items);
            streamingQualitySpinner.setAdapter(adapter);
            streamingQualitySpinner.setSelection(cameraDaemon.getCameraParams().getSelectedProfileIdx());
        }

        public Dialog getDialog() {
            return dialog;
        }
    }

    private CameraSettingsDialog cameraSettingsDialog = null;

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
                    .setOnDismissListener(dialog -> {
                        toggleBrightness(true);
                        activity.passcodeDialogs.remove(incomingId);
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
                if (cameraSettingsDialog == null) {
                    cameraSettingsDialog = new CameraSettingsDialog(this);
                }
                cameraSettingsDialog.getDialog().show();
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
            switch (lastOrientation) {
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
        stopActionOverlayView.setVisibility(isEnabled ? View.VISIBLE : View.INVISIBLE);

        if (sharedPreferences.getFirstStreaming() && isEnabled) {
            TapTargetView.showFor(this,
                    TapTarget.forView(findViewById(R.id.stop_streaming_button),
                            "Stop the Streaming",
                            "Whenever you want to stop the streaming. Click here.")
                            .drawShadow(true)
                            .cancelable(false)
                            .tintTarget(true)
                            .transparentTarget(true)
                            .targetRadius(60),
                    new TapTargetView.Listener() {
                        @Override
                        public void onTargetClick(TapTargetView view) {
                            sharedPreferences.setFirstStreaming(false);
                            toggleBrightness(true);
                            super.onTargetClick(view);
                        }
                    });
        } else {
            toggleBrightness(isEnabled);
        }
        if (isEnabled) {
            setPreviewEnable(false);
            cameraDaemon.startStream();

        } else {
            cameraDaemon.stopStream();
            setPreviewEnable(true);
        }
    }
}
