package com.quarkonium.qpocket.view.recycler;

import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;


/**
 * Created by camera360 on 15-7-31.
 */
public class MyAnimator extends BaseItemAnimator {

    @Override
    protected void animateRemoveImpl(final RecyclerView.ViewHolder holder) {
        ViewCompat.animate(holder.itemView)
                .alpha(0)
                .setDuration(getRemoveDuration())
                .setListener(new DefaultRemoveVpaListener(holder))
                .start();
    }

//    @Override
//    protected void preAnimateRemoveImpl(RecyclerView.ViewHolder holder) {
//        ViewCompat.setTranslationY(holder.itemView, 0);
//    }

    @Override
    protected void preAnimateAddImpl(RecyclerView.ViewHolder holder) {
//        ViewCompat.setTranslationX(holder.itemView, -holder.itemView.getRootView().getWidth()-holder.itemView.getWidth());
        ViewCompat.setAlpha(holder.itemView, 0);
    }

    @Override
    protected void animateAddImpl(final RecyclerView.ViewHolder holder) {
//        ViewCompat.animate(holder.itemView)
//                .translationX(0)
//                .setDuration(getAddDuration())
//                .setListener(new DefaultAddVpaListener(holder)).start();
//        mAddAnimations.add(holder);
        ViewCompat.animate(holder.itemView)
                .alpha(1)
                .setDuration(getAddDuration())
                .setListener(new DefaultAddVpaListener(holder)).start();
    }

    @Override
    public boolean animateRemove(final RecyclerView.ViewHolder holder) {
//        endAnimation(holder);
        dispatchRemoveFinished(holder);
        return true;
    }
}
