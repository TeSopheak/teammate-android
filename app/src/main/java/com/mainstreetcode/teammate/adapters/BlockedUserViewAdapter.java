package com.mainstreetcode.teammate.adapters;

import android.view.ViewGroup;

import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.adapters.viewholders.input.BaseItemViewHolder;
import com.mainstreetcode.teammate.adapters.viewholders.input.InputViewHolder;
import com.mainstreetcode.teammate.adapters.viewholders.input.TextInputStyle;
import com.mainstreetcode.teammate.model.BlockedUser;
import com.mainstreetcode.teammate.model.Identifiable;
import com.mainstreetcode.teammate.model.Item;
import com.tunjid.androidbootstrap.view.recyclerview.InteractiveAdapter;

import java.util.List;

import androidx.annotation.NonNull;

/**
 * Adapter for {@link BlockedUser}
 */

public class BlockedUserViewAdapter extends InteractiveAdapter<BaseItemViewHolder, InteractiveAdapter.AdapterListener> {

    private final List<Identifiable> items;
    private final Chooser chooser;

    public BlockedUserViewAdapter(List<Identifiable> items) {
        super(new AdapterListener() {});
        this.items = items;
        chooser = new Chooser();
    }

    @NonNull
    @Override
    public BaseItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        return new InputViewHolder(getItemView(R.layout.viewholder_simple_input, viewGroup));
    }

    @Override
    public void onBindViewHolder(@NonNull BaseItemViewHolder baseItemViewHolder, int i) {
        Identifiable item = items.get(i);
        if ((item instanceof Item)) baseItemViewHolder.bind(chooser.get((Item) item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return Item.INPUT;
    }

    private static class Chooser extends TextInputStyle.InputChooser {
        @Override public TextInputStyle apply(Item input) {
            return new TextInputStyle(Item.NO_CLICK, Item.NO_CLICK, Item.FALSE, Item.ALL_INPUT_VALID, Item.NO_ICON);
        }
    }
}
