package com.mainstreetcode.teammate.model;


import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import com.mainstreetcode.teammate.notifications.FeedItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Flowable;
import io.reactivex.functions.BiFunction;

import static android.support.v7.util.DiffUtil.calculateDiff;
import static com.mainstreetcode.teammate.model.Identifiable.Util.getPoints;
import static com.mainstreetcode.teammate.model.Identifiable.Util.isSameModel;
import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.computation;

public interface Identifiable {

    String getId();

    default boolean areContentsTheSame(Identifiable other) {
        return getId().equals(other.getId());
    }

    default Object getChangePayload(Identifiable other) {
        return null;
    }

    static <T extends Identifiable> Flowable<DiffUtil.DiffResult> diff(Flowable<List<T>> sourceFlowable,
                                                                       Callable<List<T>> sourceSupplier,
                                                                       BiFunction<List<T>, List<T>, List<T>> accumulator) {

        AtomicReference<List<T>> sourceUpdater = new AtomicReference<>();

        return sourceFlowable.concatMapDelayError(fetchedItems -> Flowable.fromCallable(() -> {
                    List<T> stale = sourceSupplier.call();
                    List<T> updated = accumulator.apply(new ArrayList<>(stale), fetchedItems);

                    sourceUpdater.set(updated);

                    return calculateDiff(new IdentifiableDiffCallback(updated, stale));
                })
                        .subscribeOn(computation())
                        .observeOn(mainThread())
                        .doOnNext(diffResult -> {
                            List<T> source = sourceSupplier.call();
                            source.clear();
                            source.addAll(sourceUpdater.get());
                        })
        );
    }

    class IdentifiableDiffCallback extends DiffUtil.Callback {

        private final List<? extends Identifiable> stale;
        private final List<? extends Identifiable> updated;

        IdentifiableDiffCallback(List<? extends Identifiable> updated, List<? extends Identifiable> stale) {
            this.updated = updated;
            this.stale = stale;
        }

        @Override
        public int getOldListSize() {
            return stale.size();
        }

        @Override
        public int getNewListSize() {
            return updated.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return stale.get(oldItemPosition).getId().equals(updated.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return stale.get(oldItemPosition).areContentsTheSame(updated.get(newItemPosition));
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            return stale.get(oldItemPosition).getChangePayload(updated.get(newItemPosition));
        }
    }

    @SuppressWarnings("unchecked")
    Comparator<Identifiable> COMPARATOR = (modelA, modelB) -> {
        int pointsA = getPoints(modelA);
        int pointsB = getPoints(modelB);

        int a, b, modelComparison;
        a = b = modelComparison = Integer.compare(pointsA, pointsB);


        if (!isSameModel(modelA, modelB)) return modelComparison;
        else a += ((Model) modelA).compareTo(modelB);

        return Integer.compare(a, b);
    };


    class Util {
        static int getPoints(Identifiable identifiable) {
            if (identifiable instanceof FeedItem) identifiable = ((FeedItem) identifiable).getModel();
            if (identifiable.getClass().equals(Item.class)) return 25;
            if (identifiable.getClass().equals(Role.class)) return 20;
            if (identifiable.getClass().equals(JoinRequest.class)) return 15;
            if (identifiable.getClass().equals(Event.class)) return 10;
            if (identifiable.getClass().equals(Media.class)) return 5;
            return 0;
        }

        static boolean isSameModel(Identifiable modelA, Identifiable modelB) {
            return modelA instanceof Model
                    && modelB instanceof Model
                    && modelA.getClass().equals(modelB.getClass());
        }
    }
}
