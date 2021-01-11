package org.owwlo.watchcat.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.owwlo.watchcat.R;
import org.owwlo.watchcat.model.Viewer;

import java.util.ArrayList;
import java.util.List;

public class AuthorizedClientListAdapter extends RecyclerView.Adapter<AuthorizedClientListAdapter.ViewHolder> {
    private final static String TAG = AuthorizedClientListAdapter.class.getCanonicalName();

    private Context mContext;
    private List<Viewer> viewerList;
    private List<OnClickListener> mListeners = new ArrayList<>();

    public interface OnClickListener {
        void onClick(ViewHolder holder, Viewer viewer);
    }

    public void registerListener(OnClickListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(OnClickListener listener) {
        mListeners.remove(listener);
    }

    private void notifyListener(ViewHolder holder, Viewer viewer) {
        for (OnClickListener listener : mListeners) {
            listener.onClick(holder, viewer);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name;

        public ViewHolder(View view) {
            super(view);
            name = (TextView) view.findViewById(R.id.text_viewer_id);
        }
    }

    public AuthorizedClientListAdapter(Context mContext, List<Viewer> list) {
        this.mContext = mContext;
        this.viewerList = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.viewer_authorization_item, parent, false);
        ViewHolder holder = new ViewHolder(itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Viewer viewer = viewerList.get(position);
        holder.name.setText(viewer.getName());
        holder.itemView.findViewById(R.id.btn_revoke_access).
                setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        notifyListener(holder, viewer);
                    }
                });
    }

    @Override
    public int getItemCount() {
        return viewerList.size();
    }
}
