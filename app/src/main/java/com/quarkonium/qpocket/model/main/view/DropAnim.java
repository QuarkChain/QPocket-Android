package com.quarkonium.qpocket.model.main.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;

public class DropAnim {

    private static DropAnim dropAnim;

    public static DropAnim getInstance() {
        if (dropAnim == null) {
            dropAnim = new DropAnim();
        }
        return dropAnim;
    }

    public void animateOpen(View v, int mHiddenViewMeasuredHeight) {
        v.setVisibility(View.VISIBLE);
        ValueAnimator animator = createDropAnimator(v, 0,
                mHiddenViewMeasuredHeight);
        animator.start();
    }

    public void animateClose(final View view) {
        int origHeight = view.getHeight();
        ValueAnimator animator = createDropAnimator(view, origHeight, 0);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
            }
        });
        animator.start();


    }

    public ValueAnimator createDropAnimator(final View v, int start, int end) {
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.addUpdateListener((ValueAnimator arg0) -> {
            int value = (int) arg0.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
            layoutParams.height = value;
            v.setLayoutParams(layoutParams);

        });
        return animator;
    }
}
