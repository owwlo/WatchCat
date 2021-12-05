package org.owwlo.watchcat.ui.activities;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.andrognito.pinlockview.IndicatorDots;
import com.andrognito.pinlockview.PinLockListener;
import com.andrognito.pinlockview.PinLockView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.listener.multi.BaseMultiplePermissionsListener;
import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owwlo.watchcat.R;
import org.owwlo.watchcat.model.AuthResult;
import org.owwlo.watchcat.model.Camera;
import org.owwlo.watchcat.model.CameraInfo;
import org.owwlo.watchcat.model.Viewer;
import org.owwlo.watchcat.model.ViewerDao;
import org.owwlo.watchcat.services.ServiceDaemon;
import org.owwlo.watchcat.ui.AuthorizedClientListAdapter;
import org.owwlo.watchcat.ui.CameraListAdapter;
import org.owwlo.watchcat.ui.EmptyRecyclerView;
import org.owwlo.watchcat.utils.Constants;
import org.owwlo.watchcat.utils.EventBus.OutgoingAuthorizationRequestEvent;
import org.owwlo.watchcat.utils.EventBus.OutgoingAuthorizationResultEvent;
import org.owwlo.watchcat.utils.EventBus.PinInputDoneEvent;
import org.owwlo.watchcat.utils.Toaster;

import java.util.ArrayList;
import java.util.List;

// FIXME AppCompatActivity will throw annoying exceptions on Android 8
public class MainScreenActivity extends Activity implements View.OnClickListener, ServiceDaemon.RemoteCameraEventListener, CameraListAdapter.OnClickListener, SpeedDialView.OnActionSelectedListener {
    private final static String TAG = MainScreenActivity.class.getCanonicalName();

    private View mBtnCameraMode;
    private FloatingActionButton mBtnCameraModeFab;
    private SpeedDialView settingsButton;
    private ServiceDaemon mMainService = null;

    private EmptyRecyclerView mRecyclerView;
    private View mListEmptyPlaceholder;
    private CameraListAdapter mCameraAdapter;
    private List<Camera> mCameraList;
    private AuthorizationManagementDialog authorizationManagementDialog = null;

    private Handler mHandler = new Handler();

    // ServiceDaemon binding begins
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            ServiceDaemon.LocalBinder binder = (ServiceDaemon.LocalBinder) service;
            mMainService = binder.getService();
            mMainService.getCameraManager().registerListener(MainScreenActivity.this);

            // Broadcast I am joining the network
            mMainService.getCameraManager().broadcastMyInfo();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mMainService.getCameraManager().removeListener(MainScreenActivity.this);

            // Broadcast I am leaving the network
            mMainService.getCameraManager().broadcastShuttingDown();

            mMainService = null;
        }
    };
    // ServiceDaemon binding ends

    public class PasscodeInputDialog implements PinLockListener {
        AlertDialog dialog;
        MainScreenActivity activity;
        PinLockView pin;
        TextView description;
        TextView error;
        Camera authorizingCamera;

        public PasscodeInputDialog(MainScreenActivity activity) {
            this.activity = activity;
            View view = getLayoutInflater().inflate(R.layout.dialog_passcode_input, null);
            dialog = new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_MaterialAlertDialog_Rounded)
                    .setView(view)
                    .create();
            pin = view.findViewById(R.id.pin_input_view);
            IndicatorDots dots = view.findViewById(R.id.pin_indicator_dots);
            pin.attachIndicatorDots(dots);
            pin.setPinLockListener(this);

            pin.setPinLength(4);
            pin.setTextColor(ContextCompat.getColor(activity, R.color.white));
            dots.setIndicatorType(IndicatorDots.IndicatorType.FIXED);

            description = view.findViewById(R.id.text_pin_description);
            error = view.findViewById(R.id.text_passcode_error);
            error.setVisibility(View.INVISIBLE);
        }

        public void newInput(final Camera camera) {
            authorizingCamera = camera;
            description.setText(camera.getIp());
            reset(View.INVISIBLE);
            dialog.show();
        }

        public void error(final String errorMsg) {
            error.setText(errorMsg);
            reset(View.VISIBLE);
        }

        public void close() {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }

        @Override
        public void onComplete(String passcode) {
            // TODO this won't fully disable the input
            pin.setEnabled(false);
            EventBus.getDefault().post(new PinInputDoneEvent(authorizingCamera, passcode));
        }

        @Override
        public void onEmpty() {

        }

        public void reset(final int errorVisibility) {
            error.setVisibility(errorVisibility);
            pin.resetPinLockView();
            pin.setEnabled(true);
        }

        @Override
        public void onPinChange(int pinLength, String intermediatePin) {

        }
    }

    PasscodeInputDialog passcodeInputDialog = null;

    @Override
    protected void onDestroy() {
        if (authorizationManagementDialog != null) {
            authorizationManagementDialog.getDialog().dismiss();
        }
        EventBus.getDefault().unregister(this);
        unbindService(mServiceConnection);
        super.onDestroy();
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void onMessageEvent(OutgoingAuthorizationResultEvent event) {
        if (event.getResult() == AuthResult.kRESULT_DENIED) {
            passcodeInputDialog.error(getString(R.string.wrong_passcode_input_message));
        } else if (event.getResult() == AuthResult.kRESULT_NEW_AUTH) {
            passcodeInputDialog.newInput(event.getCamera());
        } else if (event.getResult() == AuthResult.kRESULT_REACHED_MAX_VIEWER) {
            passcodeInputDialog.close();
            Toaster.error(getString(R.string.max_viewers_reached));
        } else if (event.getResult() == AuthResult.kRESULT_GRANTED) {
            passcodeInputDialog.close();

            final Camera camera = event.getCamera();

            // ExoPlayer
            Intent intent = new Intent(this, ExoPlayerActivity.class);
            intent.putExtra(ExoPlayerActivity.INTENT_EXTRA_URI,
                    camera.getUrls().getCameraStreamingURI(mMainService.getAuthManager().getMyself().getId()));
            startActivity(intent);
        }
    }

    int getSpan() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        final float density = getResources().getDisplayMetrics().density;
        final float dpHeight = outMetrics.heightPixels / density;
        final float dpWidth = outMetrics.widthPixels / density;

        Log.d(TAG, "dpHeight: " + dpHeight + ", dpWidth: " + dpWidth);

        float effectiveWidth = Constants.PREVIEW_DEFAULT_WIDTH_IN_DP;
        if (dpHeight < Constants.PREVIEW_DEFAULT_HEIGHT_IN_DP) {
            effectiveWidth = effectiveWidth / Constants.PREVIEW_DEFAULT_HEIGHT_IN_DP * dpHeight;
        }
        final float dpItemWidth = Math.min(dpWidth, effectiveWidth);
        return (int) Math.ceil(dpWidth / dpItemWidth);
    }

    // TODO extract into a new class
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
                    .setTitle(R.string.permission_require_dialog_title)
                    .setMessage(R.string.permission_require_dialog2_text)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Dexter.withContext(this)
                .withPermissions(
                        Manifest.permission.BLUETOOTH
                ).withListener(new PermissionsListener(this, report -> {
            if (report.areAllPermissionsGranted()) {
                bindService(new Intent(this, ServiceDaemon.class), mServiceConnection, Context.BIND_AUTO_CREATE);
            } else {
                this.finish();
            }
        })).onSameThread().check();

        EventBus.getDefault().register(this);

        setContentView(R.layout.activity_main_screen);
        passcodeInputDialog = new PasscodeInputDialog(this);

        mBtnCameraMode = findViewById(R.id.camera_mode_button);
        mBtnCameraModeFab = findViewById(R.id.camera_mode_button_fab);
        mBtnCameraMode.setOnClickListener(this);
        mBtnCameraModeFab.setOnClickListener(this);

        mRecyclerView = findViewById(R.id.recycler_view);
        mListEmptyPlaceholder = findViewById(R.id.list_empty_placeholder);

        mCameraList = new ArrayList<>();
        mCameraAdapter = new CameraListAdapter(this, mCameraList);
        mCameraAdapter.registerListener(this);

        int span = getSpan();

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, span);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addItemDecoration(new GridSpacingItemDecoration(span, dpToPx(10), true));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mCameraAdapter);
        mRecyclerView.setEmptyView(mListEmptyPlaceholder);

        settingsButton = findViewById(R.id.settings_menu);
        settingsButton.addActionItem(getNewMenuFab(R.id.fab_authorization, R.drawable.ic_outline_key, R.string.fab_authorization));
        settingsButton.setOnActionSelectedListener(this);
    }

    @ColorInt
    int getThemeColor(int resid) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(resid, typedValue, true);
        return typedValue.data;
    }

    SpeedDialActionItem getNewMenuFab(@IdRes int id, @DrawableRes int fabImageResource, @StringRes int labelRes) {

        return new SpeedDialActionItem.Builder(id, fabImageResource)
                .setLabel(labelRes)
                .setLabelColor(getThemeColor(R.attr.colorOnPrimarySurface))
                .setLabelBackgroundColor(getThemeColor(R.attr.colorPrimarySurface))
                .setLabelClickable(true)
                .setFabSize(FloatingActionButton.SIZE_MINI)
                .create();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.camera_mode_button_fab:
            case R.id.camera_mode_button: {
                Intent intent = new Intent(this, CameraActivity.class);
                startActivity(intent);
                break;
            }
        }
    }

    @Override
    public void onClick(CameraListAdapter.MyViewHolder holder, Camera camera) {
        EventBus.getDefault().post(new OutgoingAuthorizationRequestEvent(camera));
    }

    @Override
    public void onCameraAdded(String ip, CameraInfo info) {
        if (!info.isEnabled()) return;
        synchronized (mCameraList) {
            mCameraList.add(new Camera(ip, info));
            mHandler.post(() -> mCameraAdapter.notifyItemInserted(mCameraList.size() - 1));
        }
    }

    @Override
    public void onCameraRemoved(String ip) {
        synchronized (mCameraList) {
            int idx = -1;
            for (int i = 0; i < mCameraList.size(); i++) {
                Camera camera = mCameraList.get(i);
                if (camera.getIp().equals(ip)) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                mCameraList.remove(idx);
                final int cIdx = idx;
                mHandler.post(() -> mCameraAdapter.notifyItemRemoved(cIdx));
            }
        }
    }

    @Override
    public void onStateUpdated(String ip, CameraInfo newInfo) {
        if (!newInfo.isEnabled()) {
            onCameraRemoved(ip);
        } else {
            final Camera camera = new Camera(ip, newInfo);
            synchronized (mCameraList) {
                int idx = -1;
                for (int i = 0; i < mCameraList.size(); i++) {
                    if (mCameraList.get(i).getIp().equals(ip)) {
                        idx = i;
                        break;
                    }
                }
                if (idx >= 0) {
                    final Camera oldCamera = mCameraList.get(idx);
                    if (!oldCamera.equals(camera)) {
                        Log.d(TAG, "old: " + oldCamera.toString() + ", new: " + camera.toString());
                        mCameraList.set(idx, camera);
                        final int cIdx = idx;
                        mHandler.post(() -> mCameraAdapter.notifyItemChanged(cIdx));
                    }
                } else {
                    mCameraList.add(camera);
                    mHandler.post(() -> mCameraAdapter.notifyItemInserted(mCameraList.size() - 1));
                }
            }
        }
    }

    public class AuthorizationManagementDialog implements AuthorizedClientListAdapter.OnClickListener {
        AlertDialog dialog;
        private AuthorizedClientListAdapter adapter;
        private List<Viewer> viewerList;
        private ViewerDao viewerDao;

        public AuthorizationManagementDialog(MainScreenActivity activity) {
            View view = getLayoutInflater().inflate(R.layout.dialog_authorization_management, null);
            dialog = new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_MaterialAlertDialog_Rounded).setTitle(R.string.fab_authorization)
                    .setView(view)
                    .setPositiveButton(getString(R.string.done_button_text), (dialog, which) -> dialog.dismiss())
                    .create();

            EmptyRecyclerView itemRecyclerList = view.findViewById(R.id.authorization_recycler_view);

            viewerList = new ArrayList<>();
            adapter = new AuthorizedClientListAdapter(activity, viewerList);
            adapter.registerListener(this);

            itemRecyclerList.setLayoutManager(new GridLayoutManager(activity, 1));
            itemRecyclerList.addItemDecoration(new GridSpacingItemDecoration(1, dpToPx(10), true));
            itemRecyclerList.setItemAnimator(new DefaultItemAnimator());
            itemRecyclerList.setAdapter(adapter);
            itemRecyclerList.setEmptyView(view.findViewById(R.id.placeholder_no_access_granted));

            viewerDao = mMainService.getDatabase().viewerDao();
            AsyncTask<Void, Void, Void> fetchAllViewers = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    viewerList.addAll(viewerDao.loadClientViewers());
                    adapter.notifyDataSetChanged();
                    return null;
                }
            };
            fetchAllViewers.execute();
        }

        public Dialog getDialog() {
            return dialog;
        }

        @Override
        public void onClick(AuthorizedClientListAdapter.ViewHolder holder, Viewer viewer) {
            AsyncTask<Void, Void, Void> revokeAccess = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    mMainService.getAuthManager().revokeAccess(viewer.getId());
                    return null;
                }
            };
            revokeAccess.execute();
            viewerList.remove(viewer);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onActionSelected(SpeedDialActionItem actionItem) {
        final int id = actionItem.getId();
        switch (id) {
            case R.id.fab_authorization: {
                if (authorizationManagementDialog == null) {
                    authorizationManagementDialog = new AuthorizationManagementDialog(this);
                }
                authorizationManagementDialog.getDialog().show();
                break;
            }
        }
        return false /* close the menu */;
    }

    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position
            int column = position % spanCount; // item column

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
                outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }
        }
    }

    private int dpToPx(int dp) {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }
}
