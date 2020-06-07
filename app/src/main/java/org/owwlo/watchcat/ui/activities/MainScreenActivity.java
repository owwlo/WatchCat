package org.owwlo.watchcat.ui.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.owwlo.watchcat.model.CameraInfo;
import org.owwlo.watchcat.ui.CameraListAdapter;
import org.owwlo.watchcat.utils.Constants;
import org.owwlo.watchcat.R;
import org.owwlo.watchcat.services.ServiceDaemon;
import org.owwlo.watchcat.model.Camera;

import java.util.ArrayList;
import java.util.List;

// FIXME AppCompatActivity will throw annoying exceptions on Android 8
public class MainScreenActivity extends Activity implements View.OnClickListener, ServiceDaemon.RemoteCameraManager.RemoteCameraEventListener, CameraListAdapter.OnClickListener {
    private final static String TAG = MainScreenActivity.class.getCanonicalName();

    private Button mBtnCameraMode;
    private ServiceDaemon mMainService = null;

    private RecyclerView mRecyclerView;
    private CameraListAdapter mCameraAdapter;
    private List<Camera> mCameraList;


    // ServiceDaemon binding begins
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            ServiceDaemon.LocalBinder binder = (ServiceDaemon.LocalBinder) service;
            mMainService = binder.getService();
            mMainService.getCameraManager().registerListener(MainScreenActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mMainService.getCameraManager().removeListener(MainScreenActivity.this);
            mMainService = null;
        }
    };
    // ServiceDaemon binding ends

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        mBtnCameraMode = findViewById(R.id.btn_camera_mode);
        mBtnCameraMode.setOnClickListener(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        mCameraList = new ArrayList<>();
        mCameraAdapter = new CameraListAdapter(this, mCameraList);
        mCameraAdapter.registerListener(this);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 1);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(10), true));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mCameraAdapter);

        bindService(new Intent(this, ServiceDaemon.class), mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_camera_mode: {
                Intent intent = new Intent(this, CameraActivity.class);
                startActivity(intent);
                break;
            }
        }
    }

    @Override
    public void onClick(CameraListAdapter.MyViewHolder holder, Camera camera) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(Constants.INTENT_EXTRA_CAMERA_IP, camera.getIp());
        intent.putExtra(Constants.INTENT_EXTRA_CAMERA_PORT, camera.getPort());
        startActivity(intent);
    }

    @Override
    public void onCameraAdded(String ip, CameraInfo info) {
        Log.d(TAG, "camera added to the list: " + ip);
        mCameraList.add(new Camera(ip, info.getStreamingPort()));
        mCameraAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCameraRmoved(String ip) {
        int idx = -1;
        for (int i = 0; i < mCameraList.size(); i++) {
            Camera camera = mCameraList.get(i);
            if (camera.getIp() == ip) {
                idx = i;
                break;
            }
        }
        if (idx >= 0) {
            mCameraList.remove(idx);
            mCameraAdapter.notifyItemRemoved(idx);
        }
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