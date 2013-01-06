package com.android.systemui.statusbar.quicksettings;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;

public final class DisplayNextTile implements Animation.AnimationListener {
    private boolean mCurrentView;
    View view1;
    View view2;

    public DisplayNextTile(boolean currentView, View view1, View view2) {
        mCurrentView = currentView;
        this.view1 = view1;
        this.view2 = view2;
    }

    public void onAnimationStart(Animation animation) {
    }

    public void onAnimationEnd(Animation animation) {
        view1.post(new SwapViews(mCurrentView, view1, view2));
    }

    public void onAnimationRepeat(Animation animation) {
    }
}

class SwapViews implements Runnable {
    private boolean mIsFirstView;
    View view1;
    View view2;

    public SwapViews(boolean isFirstView, View view1, View view2) {
         mIsFirstView = isFirstView;
         this.view1 = view1;
         this.view2 = view2;
    }

    public void run() {
         final float centerX = view1.getWidth() / 2.0f;
         final float centerY = view1.getHeight() / 2.0f;
         Tile3dFlipAnimation rotation;
        
         if (mIsFirstView) {
              view1.setVisibility(View.GONE);
              view2.setVisibility(View.VISIBLE);
              view2.requestFocus();
        
             rotation = new Tile3dFlipAnimation(-90, 0, centerX, centerY);
         } else {
              view2.setVisibility(View.GONE);
              view1.setVisibility(View.VISIBLE);
              view1.requestFocus();
        
             rotation = new Tile3dFlipAnimation(90, 0, centerX, centerY);
         }
        
         rotation.setDuration(500);
         rotation.setFillAfter(true);
         rotation.setInterpolator(new DecelerateInterpolator());
    
         if (mIsFirstView) {
             view2.startAnimation(rotation);
         } else {
             view1.startAnimation(rotation);
         }
    }
}
