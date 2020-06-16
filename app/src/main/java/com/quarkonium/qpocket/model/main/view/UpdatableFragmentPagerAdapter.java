package com.quarkonium.qpocket.model.main.view;

import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.collection.LongSparseArray;
import androidx.viewpager.widget.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public abstract class UpdatableFragmentPagerAdapter extends PagerAdapter {
    private static final String TAG = "FragmentPagerAdapter";

    @NonNull
    private final FragmentManager mFragmentManager;
    @Nullable
    private FragmentTransaction mCurTransaction = null;
    @Nullable
    private Fragment mCurrentPrimaryItem = null;

    @NonNull
    private final LongSparseArray<Fragment> mFragments = new LongSparseArray<>();
    @NonNull
    private final LongSparseArray<Fragment.SavedState> mSavedStates = new LongSparseArray<>();

    public UpdatableFragmentPagerAdapter(@NonNull FragmentManager fm) {
        mFragmentManager = fm;
    }

    /**
     * Return the Fragment associated with a specified position.
     */
    public abstract Fragment getItem(int position);

    @Override
    public void startUpdate(@NonNull ViewGroup container) {
        if (container.getId() == View.NO_ID) {
            throw new IllegalStateException("ViewPager with adapter " + this
                    + " requires a view id");
        }
    }

    @Override
    @NonNull
    public Object instantiateItem(ViewGroup container, int position) {
        long tag = getItemId(position);
        Fragment fragment = mFragments.get(tag);
        // If we already have this item instantiated, there is nothing
        // to do.  This can happen when we are restoring the entire pager
        // from its saved state, where the fragment manager has already
        // taken care of restoring the fragments we previously had instantiated.
        if (fragment != null) {
            return fragment;
        }

        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }

        fragment = getItem(position);
        // restore state
        final Fragment.SavedState savedState = mSavedStates.get(tag);
        if (savedState != null) {
            fragment.setInitialSavedState(savedState);
        }
        fragment.setMenuVisibility(false);
        fragment.setUserVisibleHint(false);
        mFragments.put(tag, fragment);
        mCurTransaction.add(container.getId(), fragment, "f" + tag);

        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, @NonNull Object object) {
        Fragment fragment = (Fragment) object;
        int currentPosition = getItemPosition(fragment);

        int index = mFragments.indexOfValue(fragment);
        long fragmentKey = -1;
        if (index != -1) {
            fragmentKey = mFragments.keyAt(index);
            mFragments.removeAt(index);
        }

        //item hasn't been removed
        if (fragment.isAdded() && currentPosition != POSITION_NONE) {
            mSavedStates.put(fragmentKey, mFragmentManager.saveFragmentInstanceState(fragment));
        } else {
            mSavedStates.remove(fragmentKey);
        }

        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }

        mCurTransaction.remove(fragment);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, @Nullable Object object) {
        Fragment fragment = (Fragment) object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                mCurrentPrimaryItem.setMenuVisibility(false);
                mCurrentPrimaryItem.setUserVisibleHint(false);
            }
            if (fragment != null) {
                fragment.setMenuVisibility(true);
                fragment.setUserVisibleHint(true);
            }
            mCurrentPrimaryItem = fragment;
        }
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        if (mCurTransaction != null) {
            mCurTransaction.commitNowAllowingStateLoss();
            mCurTransaction = null;
        }
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return ((Fragment) object).getView() == view;
    }

    @Override
    public Parcelable saveState() {
        Bundle state = null;
        if (mSavedStates.size() > 0) {
            // save Fragment states
            state = new Bundle();
            long[] stateIds = new long[mSavedStates.size()];
            for (int i = 0; i < mSavedStates.size(); i++) {
                Fragment.SavedState entry = mSavedStates.valueAt(i);
                stateIds[i] = mSavedStates.keyAt(i);
                state.putParcelable(Long.toString(stateIds[i]), entry);
            }
            state.putLongArray("states", stateIds);
        }
        for (int i = 0; i < mFragments.size(); i++) {
            Fragment f = mFragments.valueAt(i);
            if (f != null && f.isAdded()) {
                if (state == null) {
                    state = new Bundle();
                }
                String key = "f" + mFragments.keyAt(i);
                mFragmentManager.putFragment(state, key, f);
            }
        }
        return state;
    }

    @Override
    public void restoreState(@Nullable Parcelable state, ClassLoader loader) {
        if (state != null) {
            Bundle bundle = (Bundle) state;
            bundle.setClassLoader(loader);
            long[] fss = bundle.getLongArray("states");
            mSavedStates.clear();
            mFragments.clear();
            if (fss != null) {
                for (long fs : fss) {
                    mSavedStates.put(fs, bundle.getParcelable(Long.toString(fs)));
                }
            }
            Iterable<String> keys = bundle.keySet();
            for (String key : keys) {
                if (key.startsWith("f")) {
                    try {
                        Fragment f = mFragmentManager.getFragment(bundle, key);
                        if (f != null) {
                            f.setMenuVisibility(false);
                            mFragments.put(Long.parseLong(key.substring(1)), f);
                        } else {
                            Log.w(TAG, "Bad fragment at key " + key);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Bad fragment at key " + key);
                    }
                }
            }
        }
    }

    /**
     * Return a unique identifier for the item at the given position.
     * <p>
     * <p>The default implementation returns the given position.
     * Subclasses should override this method if the positions of items can change.</p>
     *
     * @param position Position within this adapter
     * @return Unique identifier for the item at position
     */
    public long getItemId(int position) {
        return position;
    }

    public LongSparseArray<Fragment> getFragments() {
        return mFragments;
    }
}
