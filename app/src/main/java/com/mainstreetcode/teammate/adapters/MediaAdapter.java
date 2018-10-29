package com.mainstreetcode.teammate.adapters;

import androidx.annotation.NonNull;
import android.view.ViewGroup;

import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.adapters.viewholders.AdViewHolder;
import com.mainstreetcode.teammate.adapters.viewholders.ContentAdViewHolder;
import com.mainstreetcode.teammate.adapters.viewholders.ImageMediaViewHolder;
import com.mainstreetcode.teammate.adapters.viewholders.InstallAdViewHolder;
import com.mainstreetcode.teammate.adapters.viewholders.MediaViewHolder;
import com.mainstreetcode.teammate.adapters.viewholders.VideoMediaViewHolder;
import com.mainstreetcode.teammate.model.Ad;
import com.mainstreetcode.teammate.model.Identifiable;
import com.mainstreetcode.teammate.model.Media;
import com.mainstreetcode.teammate.util.ViewHolderUtil;
import com.tunjid.androidbootstrap.view.recyclerview.InteractiveAdapter;
import com.tunjid.androidbootstrap.view.recyclerview.InteractiveViewHolder;

import java.util.List;

import static com.mainstreetcode.teammate.util.ViewHolderUtil.CONTENT_AD;
import static com.mainstreetcode.teammate.util.ViewHolderUtil.INSTALL_AD;
import static com.mainstreetcode.teammate.util.ViewHolderUtil.MEDIA_IMAGE;
import static com.mainstreetcode.teammate.util.ViewHolderUtil.MEDIA_VIDEO;

/**
 * Adapter for {@link Media}
 */

public class MediaAdapter extends InteractiveAdapter<InteractiveViewHolder, MediaAdapter.MediaAdapterListener> {

    private final List<Identifiable> mediaList;

    public MediaAdapter(List<Identifiable> mediaList, MediaAdapterListener listener) {
        super(listener);
        this.mediaList = mediaList;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public InteractiveViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        return viewType == CONTENT_AD
                ? new ContentAdViewHolder(getItemView(R.layout.viewholder_grid_content_ad, viewGroup), adapterListener)
                : viewType == INSTALL_AD
                ? new InstallAdViewHolder(getItemView(R.layout.viewholder_grid_install_ad, viewGroup), adapterListener)
                : viewType == MEDIA_IMAGE
                ? new ImageMediaViewHolder(getItemView(R.layout.viewholder_image, viewGroup), adapterListener)
                : new VideoMediaViewHolder(getItemView(R.layout.viewholder_video, viewGroup), adapterListener);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(@NonNull InteractiveViewHolder viewHolder, int position) {
        Identifiable item = mediaList.get(position);
        if (item instanceof Media) ((MediaViewHolder) viewHolder).bind((Media) item);
        else if (item instanceof Ad) ((AdViewHolder) viewHolder).bind((Ad) item);
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    @Override
    public int getItemViewType(int position) {
        Identifiable identifiable = mediaList.get(position);
        return identifiable instanceof Media ? (((Media) identifiable).isImage() ? MEDIA_IMAGE : MEDIA_VIDEO) : ((Ad) identifiable).getType();
    }

    @Override
    public long getItemId(int position) {
        return mediaList.get(position).hashCode();
    }

    public interface MediaAdapterListener extends InteractiveAdapter.AdapterListener {
        void onMediaClicked(Media item);

        boolean onMediaLongClicked(Media media);

        boolean isSelected(Media media);

        boolean isFullScreen();
    }
}
