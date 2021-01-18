package org.owwlo.watchcat.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.toolbox.NetworkImageView;

import org.owwlo.watchcat.R;
import org.owwlo.watchcat.model.Camera;
import org.owwlo.watchcat.utils.NetworkImageLoader;

import java.util.ArrayList;
import java.util.List;

public class CameraListAdapter extends RecyclerView.Adapter<CameraListAdapter.MyViewHolder> {
    private final static String TAG = CameraListAdapter.class.getCanonicalName();

    private Context mContext;
    private List<Camera> cameraList;
    private List<OnClickListener> mListeners = new ArrayList<>();

    public interface OnClickListener {
        void onClick(MyViewHolder holder, Camera camera);
    }

    public void registerListener(OnClickListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(OnClickListener listener) {
        mListeners.remove(listener);
    }

    private void notifyListener(MyViewHolder holder, Camera camera) {
        for (OnClickListener listener : mListeners) {
            listener.onClick(holder, camera);
        }
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView description;
        public NetworkImageView preview;

        public MyViewHolder(View view) {
            super(view);
            description = (TextView) view.findViewById(R.id.text_camera_description);
            preview = (NetworkImageView) view.findViewById(R.id.image_camera_preview);

        }
    }

    public CameraListAdapter(Context mContext, List<Camera> albumList) {
        this.mContext = mContext;
        this.cameraList = albumList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.camera_card, parent, false);
        MyViewHolder holder = new MyViewHolder(itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        final Camera camera = cameraList.get(position);
        holder.description.setText(camera.getIp());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyListener(holder, camera);
            }
        });

        NetworkImageLoader.getInstance(CameraListAdapter.this.mContext)
                .setImageFromUrl(holder.preview,
                        camera.getUrls().getCameraPreviewURI(camera.getInfo().getThumbnailTimestamp()));
    }

    @Override
    public int getItemCount() {
        return cameraList.size();
    }
}
