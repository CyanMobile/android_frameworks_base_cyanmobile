/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.service.wallpaper;

import com.android.internal.os.HandlerCaller;
import com.android.internal.view.BaseIWindow;
import com.android.internal.view.BaseInputHandler;
import com.android.internal.view.BaseSurfaceHolder;

import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.app.Service;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;
import android.util.LogPrinter;
import android.view.Gravity;
import android.view.IWindowSession;
import android.view.InputChannel;
import android.view.InputDevice;
import android.view.InputHandler;
import android.view.InputQueue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewRoot;
import android.view.WindowManager;
import android.view.WindowManagerImpl;
import android.view.WindowManagerPolicy;

import java.util.ArrayList;

/**
 * A wallpaper service is responsible for showing a live wallpaper behind
 * applications that would like to sit on top of it.  This service object
 * itself does very little -- its only purpose is to generate instances of
 * {@link Engine} as needed.  Implementing a wallpaper thus
 * involves subclassing from this, subclassing an Engine implementation,
 * and implementing {@link #onCreateEngine()} to return a new instance of
 * your engine.
 */
public abstract class WallpaperService extends Service {
    /**
     * The {@link Intent} that must be declared as handled by the service.
     * To be supported, the service must also require the
     * {@link android.Manifest.permission#BIND_WALLPAPER} permission so
     * that other applications can not abuse it.
     */
    @SdkConstant(SdkConstantType.SERVICE_ACTION)
    public static final String SERVICE_INTERFACE =
            "android.service.wallpaper.WallpaperService";

    /**
     * Name under which a WallpaperService component publishes information
     * about itself.  This meta-data must reference an XML resource containing
     * a <code>&lt;{@link android.R.styleable#Wallpaper wallpaper}&gt;</code>
     * tag.
     */
    public static final String SERVICE_META_DATA = "android.service.wallpaper";
    
    static final String TAG = "WallpaperService";
    static final boolean DEBUG = false;
    
    private static final int DO_ATTACH = 10;
    private static final int DO_DETACH = 20;
    private static final int DO_SET_DESIRED_SIZE = 30;
    
    private static final int MSG_UPDATE_SURFACE = 10000;
    private static final int MSG_VISIBILITY_CHANGED = 10010;
    private static final int MSG_WALLPAPER_OFFSETS = 10020;
    private static final int MSG_WALLPAPER_COMMAND = 10025;
    private static final int MSG_WINDOW_RESIZED = 10030;
    private static final int MSG_TOUCH_EVENT = 10040;
    
    private Looper mCallbackLooper;
    private final ArrayList<Engine> mActiveEngines
            = new ArrayList<Engine>();
    
    static final class WallpaperCommand {
        String action;
        int x;
        int y;
        int z;
        Bundle extras;
        boolean sync;
    }
    
    /**
     * The actual implementation of a wallpaper.  A wallpaper service may
     * have multiple instances running (for example as a real wallpaper
     * and as a preview), each of which is represented by its own Engine
     * instance.  You must implement {@link WallpaperService#onCreateEngine()}
     * to return your concrete Engine implementation.
     */
    public class Engine {
        IWallpaperEngineWrapper mIWallpaperEngine;
        
        // Copies from mIWallpaperEngine.
        HandlerCaller mCaller;
        IWallpaperConnection mConnection;
        IBinder mWindowToken;
        
        boolean mInitializing = true;
        boolean mVisible;
        boolean mScreenOn = true;
        boolean mReportedVisible;
        boolean mDestroyed;
        
        // Current window state.
        boolean mCreated;
        boolean mSurfaceCreated;
        boolean mIsCreating;
        boolean mDrawingAllowed;
        int mWidth;
        int mHeight;
        int mFormat;
        int mType;
        int mCurWidth;
        int mCurHeight;
        int mWindowFlags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        int mCurWindowFlags = mWindowFlags;
        final Rect mVisibleInsets = new Rect();
        final Rect mWinFrame = new Rect();
        final Rect mContentInsets = new Rect();
        final Configuration mConfiguration = new Configuration();
        
        final WindowManager.LayoutParams mLayout
                = new WindowManager.LayoutParams();
        IWindowSession mSession;
        InputChannel mInputChannel;

        final Object mLock = new Object();
        boolean mOffsetMessageEnqueued;
        float mPendingXOffset;
        float mPendingYOffset;
        float mPendingXOffsetStep;
        float mPendingYOffsetStep;
        boolean mPendingSync;
        MotionEvent mPendingMove;
        
        final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    mScreenOn = true;
                    reportVisibility();
                } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                    mScreenOn = false;
                    reportVisibility();
                }
            }
        };
        
        final BaseSurfaceHolder mSurfaceHolder = new BaseSurfaceHolder() {
            {
                mRequestedFormat = PixelFormat.RGB_565;
            }

            @Override
            public boolean onAllowLockCanvas() {
                return mDrawingAllowed;
            }

            @Override
            public void onRelayoutContainer() {
                Message msg = mCaller.obtainMessage(MSG_UPDATE_SURFACE);
                mCaller.sendMessage(msg);
            }

            @Override
            public void onUpdateSurface() {
                Message msg = mCaller.obtainMessage(MSG_UPDATE_SURFACE);
                mCaller.sendMessage(msg);
            }

            public boolean isCreating() {
                return mIsCreating;
            }

            @Override
            public void setFixedSize(int width, int height) {
                throw new UnsupportedOperationException(
                        "Wallpapers currently only support sizing from layout");
            }
            
            public void setKeepScreenOn(boolean screenOn) {
                throw new UnsupportedOperationException(
                        "Wallpapers do not support keep screen on");
            }
            
        };
        
        final InputHandler mInputHandler = new BaseInputHandler() {
            @Override
            public void handleMotion(MotionEvent event, Runnable finishedCallback) {
                try {
                    int source = event.getSource();
                    if ((source & InputDevice.SOURCE_CLASS_POINTER) != 0) {
                        dispatchPointer(event);
                    }
                } finally {
                    finishedCallback.run();
                }
            }
        };
        
        final BaseIWindow mWindow = new BaseIWindow() {
            @Override
            public void resized(int w, int h, Rect coveredInsets,
                    Rect visibleInsets, boolean reportDraw, Configuration newConfig) {
                Message msg = mCaller.obtainMessageI(MSG_WINDOW_RESIZED,
                        reportDraw ? 1 : 0);
                mCaller.sendMessage(msg);
            }
            
            @Override
            public void dispatchAppVisibility(boolean visible) {
                // We don't do this in preview mode; we'll let the preview
                // activity tell us when to run.
                if (!mIWallpaperEngine.mIsPreview) {
                    Message msg = mCaller.obtainMessageI(MSG_VISIBILITY_CHANGED,
                            visible ? 1 : 0);
                    mCaller.sendMessage(msg);
                }
            }

            @Override
            public void dispatchWallpaperOffsets(float x, float y, float xStep, float yStep,
                    boolean sync) {
                synchronized (mLock) {
                    if (DEBUG) Log.v(TAG, "Dispatch wallpaper offsets: " + x + ", " + y);
                    mPendingXOffset = x;
                    mPendingYOffset = y;
                    mPendingXOffsetStep = xStep;
                    mPendingYOffsetStep = yStep;
                    if (sync) {
                        mPendingSync = true;
                    }
                    if (!mOffsetMessageEnqueued) {
                        mOffsetMessageEnqueued = true;
                        Message msg = mCaller.obtainMessage(MSG_WALLPAPER_OFFSETS);
                        mCaller.sendMessage(msg);
                    }
                }
            }
            
            public void dispatchWallpaperCommand(String action, int x, int y,
                    int z, Bundle extras, boolean sync) {
                synchronized (mLock) {
                    if (DEBUG) Log.v(TAG, "Dispatch wallpaper command: " + x + ", " + y);
                    WallpaperCommand cmd = new WallpaperCommand();
                    cmd.action = action;
                    cmd.x = x;
                    cmd.y = y;
                    cmd.z = z;
                    cmd.extras = extras;
                    cmd.sync = sync;
                    Message msg = mCaller.obtainMessage(MSG_WALLPAPER_COMMAND);
                    msg.obj = cmd;
                    mCaller.sendMessage(msg);
                }
            }
        };
        
        /**
         * Provides access to the surface in which this wallpaper is drawn.
         */
        public SurfaceHolder getSurfaceHolder() {
            return mSurfaceHolder;
        }
        
        /**
         * Convenience for {@link WallpaperManager#getDesiredMinimumWidth()
         * WallpaperManager.getDesiredMinimumWidth()}, returning the width
         * that the system would like this wallpaper to run in.
         */
        public int getDesiredMinimumWidth() {
            return mIWallpaperEngine.mReqWidth;
        }
        
        /**
         * Convenience for {@link WallpaperManager#getDesiredMinimumHeight()
         * WallpaperManager.getDesiredMinimumHeight()}, returning the height
         * that the system would like this wallpaper to run in.
         */
        public int getDesiredMinimumHeight() {
            return mIWallpaperEngine.mReqHeight;
        }
        
        /**
         * Return whether the wallpaper is currently visible to the user,
         * this is the last value supplied to
         * {@link #onVisibilityChanged(boolean)}.
         */
        public boolean isVisible() {
            return mReportedVisible;
        }
        
        /**
         * Returns true if this engine is running in preview mode -- that is,
         * it is being shown to the user before they select it as the actual
         * wallpaper.
         */
        public boolean isPreview() {
            return mIWallpaperEngine.mIsPreview;
        }
        
        /**
         * Control whether this wallpaper will receive raw touch events
         * from the window manager as the user interacts with the window
         * that is currently displaying the wallpaper.  By default they
         * are turned off.  If enabled, the events will be received in
         * {@link #onTouchEvent(MotionEvent)}.
         */
        public void setTouchEventsEnabled(boolean enabled) {
            mWindowFlags = enabled
                    ? (mWindowFlags&~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    : (mWindowFlags|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            if (mCreated) {
                updateSurface(false, false, false);
            }
        }
        
        /**
         * Called once to initialize the engine.  After returning, the
         * engine's surface will be created by the framework.
         */
        public void onCreate(SurfaceHolder surfaceHolder) {
        }
        
        /**
         * Called right before the engine is going away.  After this the
         * surface will be destroyed and this Engine object is no longer
         * valid.
         */
        public void onDestroy() {
        }
        
        /**
         * Called to inform you of the wallpaper becoming visible or
         * hidden.  <em>It is very important that a wallpaper only use
         * CPU while it is visible.</em>.
         */
        public void onVisibilityChanged(boolean visible) {
        }
        
        /**
         * Called as the user performs touch-screen interaction with the
         * window that is currently showing this wallpaper.  Note that the
         * events you receive here are driven by the actual application the
         * user is interacting with, so if it is slow you will get fewer
         * move events.
         */
        public void onTouchEvent(MotionEvent event) {
        }
        
        /**
         * Called to inform you of the wallpaper's offsets changing
         * within its contain, corresponding to the container's
         * call to {@link WallpaperManager#setWallpaperOffsets(IBinder, float, float)
         * WallpaperManager.setWallpaperOffsets()}.
         */
        public void onOffsetsChanged(float xOffset, float yOffset,
                float xOffsetStep, float yOffsetStep,
                int xPixelOffset, int yPixelOffset) {
        }
        
        /**
         * Process a command that was sent to the wallpaper with
         * {@link WallpaperManager#sendWallpaperCommand}.
         * The default implementation does nothing, and always returns null
         * as the result.
         * 
         * @param action The name of the command to perform.  This tells you
         * what to do and how to interpret the rest of the arguments.
         * @param x Generic integer parameter.
         * @param y Generic integer parameter.
         * @param z Generic integer parameter.
         * @param extras Any additional parameters.
         * @param resultRequested If true, the caller is requesting that
         * a result, appropriate for the command, be returned back.
         * @return If returning a result, create a Bundle and place the
         * result data in to it.  Otherwise return null.
         */
        public Bundle onCommand(String action, int x, int y, int z,
                Bundle extras, boolean resultRequested) {
            return null;
        }
        
        /**
         * Called when an application has changed the desired virtual size of
         * the wallpaper.
         */
        public void onDesiredSizeChanged(int desiredWidth, int desiredHeight) {
        }
        
        /**
         * Convenience for {@link SurfaceHolder.Callback#surfaceChanged
         * SurfaceHolder.Callback.surfaceChanged()}.
         */
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        /**
         * Convenience for {@link SurfaceHolder.Callback2#surfaceRedrawNeeded
         * SurfaceHolder.Callback.surfaceRedrawNeeded()}.
         */
        public void onSurfaceRedrawNeeded(SurfaceHolder holder) {
        }

        /**
         * Convenience for {@link SurfaceHolder.Callback#surfaceCreated
         * SurfaceHolder.Callback.surfaceCreated()}.
         */
        public void onSurfaceCreated(SurfaceHolder holder) {
        }

        /**
         * Convenience for {@link SurfaceHolder.Callback#surfaceDestroyed
         * SurfaceHolder.Callback.surfaceDestroyed()}.
         */
        public void onSurfaceDestroyed(SurfaceHolder holder) {
        }
        
        private void dispatchPointer(MotionEvent event) {
            synchronized (mLock) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    mPendingMove = event;
                } else {
                    mPendingMove = null;
                }
            }

            Message msg = mCaller.obtainMessageO(MSG_TOUCH_EVENT, event);
            mCaller.sendMessage(msg);
        }

        void updateSurface(boolean forceRelayout, boolean forceReport, boolean redrawNeeded) {
            if (mDestroyed) {
                Log.w(TAG, "Ignoring updateSurface: destroyed");
            }
            
            int myWidth = mSurfaceHolder.getRequestedWidth();
            if (myWidth <= 0) myWidth = ViewGroup.LayoutParams.MATCH_PARENT;
            int myHeight = mSurfaceHolder.getRequestedHeight();
            if (myHeight <= 0) myHeight = ViewGroup.LayoutParams.MATCH_PARENT;
            
            final boolean creating = !mCreated;
            final boolean surfaceCreating = !mSurfaceCreated;
            final boolean formatChanged = mFormat != mSurfaceHolder.getRequestedFormat();
            boolean sizeChanged = mWidth != myWidth || mHeight != myHeight;
            final boolean typeChanged = mType != mSurfaceHolder.getRequestedType();
            final boolean flagsChanged = mCurWindowFlags != mWindowFlags;
            if (forceRelayout || creating || surfaceCreating || formatChanged || sizeChanged
                    || typeChanged || flagsChanged || redrawNeeded) {

                if (DEBUG) Log.v(TAG, "Changes: creating=" + creating
                        + " format=" + formatChanged + " size=" + sizeChanged);

                try {
                    mWidth = myWidth;
                    mHeight = myHeight;
                    mFormat = mSurfaceHolder.getRequestedFormat();
                    mType = mSurfaceHolder.getRequestedType();

                    mLayout.x = 0;
                    mLayout.y = 0;
                    mLayout.width = myWidth;
                    mLayout.height = myHeight;
                    
                    mLayout.format = mFormat;
                    
                    mCurWindowFlags = mWindowFlags;
                    mLayout.flags = mWindowFlags
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            ;

                    mLayout.memoryType = mType;
                    mLayout.token = mWindowToken;

                    if (!mCreated) {
                        mLayout.type = mIWallpaperEngine.mWindowType;
                        mLayout.gravity = Gravity.LEFT|Gravity.TOP;
                        mLayout.setTitle(WallpaperService.this.getClass().getName());
                        mLayout.windowAnimations =
                                com.android.internal.R.style.Animation_Wallpaper;
                        mInputChannel = new InputChannel();
                        if (mSession.add(mWindow, mLayout, View.VISIBLE, mContentInsets,
                                mInputChannel) < 0) {
                            Log.w(TAG, "Failed to add window while updating wallpaper surface.");
                            return;
                        }
                        mCreated = true;

                        InputQueue.registerInputChannel(mInputChannel, mInputHandler,
                                Looper.myQueue());
                    }
                    
                    mSurfaceHolder.mSurfaceLock.lock();
                    mDrawingAllowed = true;

                    final int relayoutResult = mSession.relayout(
                        mWindow, mLayout, mWidth, mHeight,
                            View.VISIBLE, false, mWinFrame, mContentInsets,
                            mVisibleInsets, mConfiguration, mSurfaceHolder.mSurface);

                    if (DEBUG) Log.v(TAG, "New surface: " + mSurfaceHolder.mSurface
                            + ", frame=" + mWinFrame);
                    
                    int w = mWinFrame.width();
                    if (mCurWidth != w) {
                        sizeChanged = true;
                        mCurWidth = w;
                    }
                    int h = mWinFrame.height();
                    if (mCurHeight != h) {
                        sizeChanged = true;
                        mCurHeight = h;
                    }
                    
                    mSurfaceHolder.mSurfaceLock.unlock();

                    if (!mSurfaceHolder.mSurface.isValid()) {
                        reportSurfaceDestroyed();
                        if (DEBUG) Log.v(TAG, "Layout: Surface destroyed");
                        return;
                    }
                    
                    try {
                        mSurfaceHolder.ungetCallbacks();

                        if (surfaceCreating) {
                            mIsCreating = true;
                            if (DEBUG) Log.v(TAG, "onSurfaceCreated("
                                    + mSurfaceHolder + "): " + this);
                            onSurfaceCreated(mSurfaceHolder);
                            SurfaceHolder.Callback callbacks[] = mSurfaceHolder.getCallbacks();
                            if (callbacks != null) {
                                for (SurfaceHolder.Callback c : callbacks) {
                                    c.surfaceCreated(mSurfaceHolder);
                                }
                            }
                        }

                        redrawNeeded |= creating
                                || (relayoutResult&WindowManagerImpl.RELAYOUT_FIRST_TIME) != 0;

                        if (forceReport || creating || surfaceCreating
                                || formatChanged || sizeChanged) {
                            if (DEBUG) {
                                RuntimeException e = new RuntimeException();
                                e.fillInStackTrace();
                                Log.w(TAG, "forceReport=" + forceReport + " creating=" + creating
                                        + " formatChanged=" + formatChanged
                                        + " sizeChanged=" + sizeChanged, e);
                            }
                            if (DEBUG) Log.v(TAG, "onSurfaceChanged("
                                    + mSurfaceHolder + ", " + mFormat
                                    + ", " + mCurWidth + ", " + mCurHeight
                                    + "): " + this);
                            onSurfaceChanged(mSurfaceHolder, mFormat,
                                    mCurWidth, mCurHeight);
                            SurfaceHolder.Callback callbacks[] = mSurfaceHolder.getCallbacks();
                            if (callbacks != null) {
                                for (SurfaceHolder.Callback c : callbacks) {
                                    c.surfaceChanged(mSurfaceHolder, mFormat,
                                            mCurWidth, mCurHeight);
                                }
                            }
                        }

                        if (redrawNeeded) {
                            onSurfaceRedrawNeeded(mSurfaceHolder);
                            SurfaceHolder.Callback callbacks[] = mSurfaceHolder.getCallbacks();
                            if (callbacks != null) {
                                for (SurfaceHolder.Callback c : callbacks) {
                                    if (c instanceof SurfaceHolder.Callback2) {
                                        ((SurfaceHolder.Callback2)c).surfaceRedrawNeeded(
                                                mSurfaceHolder);
                                    }
                                }
                            }
                        }

                    } finally {
                        mIsCreating = false;
                        mSurfaceCreated = true;
                        if (redrawNeeded) {
                            mSession.finishDrawing(mWindow);
                        }
                    }
                } catch (RemoteException ex) {
                }
                if (DEBUG) Log.v(
                    TAG, "Layout: x=" + mLayout.x + " y=" + mLayout.y +
                    " w=" + mLayout.width + " h=" + mLayout.height);
            }
        }
        
        void attach(IWallpaperEngineWrapper wrapper) {
            if (DEBUG) Log.v(TAG, "attach: " + this + " wrapper=" + wrapper);
            if (mDestroyed) {
                return;
            }
            
            mIWallpaperEngine = wrapper;
            mCaller = wrapper.mCaller;
            mConnection = wrapper.mConnection;
            mWindowToken = wrapper.mWindowToken;
            mSurfaceHolder.setSizeFromLayout();
            mInitializing = true;
            mSession = ViewRoot.getWindowSession(getMainLooper());
            
            mWindow.setSession(mSession);

            mScreenOn = ((PowerManager)getSystemService(Context.POWER_SERVICE)).isScreenOn();

            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            registerReceiver(mReceiver, filter);
            
            if (DEBUG) Log.v(TAG, "onCreate(): " + this);
            onCreate(mSurfaceHolder);
            
            mInitializing = false;
            updateSurface(false, false, false);
        }
        
        void doDesiredSizeChanged(int desiredWidth, int desiredHeight) {
            if (!mDestroyed) {
                if (DEBUG) Log.v(TAG, "onDesiredSizeChanged("
                        + desiredWidth + "," + desiredHeight + "): " + this);
                mIWallpaperEngine.mReqWidth = desiredWidth;
                mIWallpaperEngine.mReqHeight = desiredHeight;
                onDesiredSizeChanged(desiredWidth, desiredHeight);
                doOffsetsChanged();
            }
        }
        
        void doVisibilityChanged(boolean visible) {
            if (!mDestroyed) {
                mVisible = visible;
                reportVisibility();
            }
        }
        
        void reportVisibility() {
            if (!mDestroyed) {
                boolean visible = mVisible && mScreenOn;
                if (mReportedVisible != visible) {
                    mReportedVisible = visible;
                    if (DEBUG) Log.v(TAG, "onVisibilityChanged(" + visible
                            + "): " + this);
                    if (visible) {
                        // If becoming visible, in preview mode the surface
                        // may have been destroyed so now we need to make
                        // sure it is re-created.
                        updateSurface(false, false, false);
                    }
                    onVisibilityChanged(visible);
                }
            }
        }
        
        void doOffsetsChanged() {
            if (mDestroyed) {
                return;
            }
            
            float xOffset;
            float yOffset;
            float xOffsetStep;
            float yOffsetStep;
            boolean sync;
            synchronized (mLock) {
                xOffset = mPendingXOffset;
                yOffset = mPendingYOffset;
                xOffsetStep = mPendingXOffsetStep;
                yOffsetStep = mPendingYOffsetStep;
                sync = mPendingSync;
                mPendingSync = false;
                mOffsetMessageEnqueued = false;
            }
            
            if (mSurfaceCreated) {
                if (DEBUG) Log.v(TAG, "Offsets change in " + this
                        + ": " + xOffset + "," + yOffset);
                final int availw = mIWallpaperEngine.mReqWidth-mCurWidth;
                final int xPixels = availw > 0 ? -(int)(availw*xOffset+.5f) : 0;
                final int availh = mIWallpaperEngine.mReqHeight-mCurHeight;
                final int yPixels = availh > 0 ? -(int)(availh*yOffset+.5f) : 0;
                onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixels, yPixels);
            }
            
            if (sync) {
                try {
                    if (DEBUG) Log.v(TAG, "Reporting offsets change complete");
                    mSession.wallpaperOffsetsComplete(mWindow.asBinder());
                } catch (RemoteException e) {
                }
            }
        }
        
        void doCommand(WallpaperCommand cmd) {
            Bundle result;
            if (!mDestroyed) {
                result = onCommand(cmd.action, cmd.x, cmd.y, cmd.z,
                        cmd.extras, cmd.sync);
            } else {
                result = null;
            }
            if (cmd.sync) {
                try {
                    if (DEBUG) Log.v(TAG, "Reporting command complete");
                    mSession.wallpaperCommandComplete(mWindow.asBinder(), result);
                } catch (RemoteException e) {
                }
            }
        }
        
        void reportSurfaceDestroyed() {
            if (mSurfaceCreated) {
                mSurfaceCreated = false;
                mSurfaceHolder.ungetCallbacks();
                SurfaceHolder.Callback callbacks[] = mSurfaceHolder.getCallbacks();
                if (callbacks != null) {
                    for (SurfaceHolder.Callback c : callbacks) {
                        c.surfaceDestroyed(mSurfaceHolder);
                    }
                }
                if (DEBUG) Log.v(TAG, "onSurfaceDestroyed("
                        + mSurfaceHolder + "): " + this);
                onSurfaceDestroyed(mSurfaceHolder);
            }
        }
        
        void detach() {
           synchronized (mLock) {
            if (mDestroyed) {
                return;
            }
            
            mDestroyed = true;
            
            if (mVisible) {
                mVisible = false;
                if (DEBUG) Log.v(TAG, "onVisibilityChanged(false): " + this);
                onVisibilityChanged(false);
            }
            
            reportSurfaceDestroyed();
            
            if (DEBUG) Log.v(TAG, "onDestroy(): " + this);
            onDestroy();
            
            unregisterReceiver(mReceiver);
            
            if (mCreated) {
                try {
                    if (DEBUG) Log.v(TAG, "Removing window and destroying surface "
                            + mSurfaceHolder.getSurface() + " of: " + this);
                    
                    if (mInputChannel != null) {
                        InputQueue.unregisterInputChannel(mInputChannel);
                    }
                    
                    mSession.remove(mWindow);
                } catch (RemoteException e) {
                }
                mSurfaceHolder.mSurface.release();
                mCreated = false;
                
                // Dispose the input channel after removing the window so the Window Manager
                // doesn't interpret the input channel being closed as an abnormal termination.
                if (mInputChannel != null) {
                    mInputChannel.dispose();
                    mInputChannel = null;
                }
            }
           }
        }
    }
    
    class IWallpaperEngineWrapper extends IWallpaperEngine.Stub
            implements HandlerCaller.Callback {
        private final HandlerCaller mCaller;

        final IWallpaperConnection mConnection;
        final IBinder mWindowToken;
        final int mWindowType;
        final boolean mIsPreview;
        int mReqWidth;
        int mReqHeight;
        
        Engine mEngine;
        
        IWallpaperEngineWrapper(WallpaperService context,
                IWallpaperConnection conn, IBinder windowToken,
                int windowType, boolean isPreview, int reqWidth, int reqHeight) {
            if (DEBUG && mCallbackLooper != null) {
                mCallbackLooper.setMessageLogging(new LogPrinter(Log.VERBOSE, TAG));
            }
            mCaller = new HandlerCaller(context,
                    mCallbackLooper != null
                            ? mCallbackLooper : context.getMainLooper(),
                    this);
            mConnection = conn;
            mWindowToken = windowToken;
            mWindowType = windowType;
            mIsPreview = isPreview;
            mReqWidth = reqWidth;
            mReqHeight = reqHeight;
            
            Message msg = mCaller.obtainMessage(DO_ATTACH);
            mCaller.sendMessage(msg);
        }
        
        public void setDesiredSize(int width, int height) {
            Message msg = mCaller.obtainMessageII(DO_SET_DESIRED_SIZE, width, height);
            mCaller.sendMessage(msg);
        }
        
        public void setVisibility(boolean visible) {
            Message msg = mCaller.obtainMessageI(MSG_VISIBILITY_CHANGED,
                    visible ? 1 : 0);
            mCaller.sendMessage(msg);
        }

        public void dispatchPointer(MotionEvent event) {
            if (mEngine != null) {
                mEngine.dispatchPointer(event);
            }
        }
        
        public void destroy() {
            Message msg = mCaller.obtainMessage(DO_DETACH);
            mCaller.sendMessage(msg);
        }

        public void executeMessage(Message message) {
            switch (message.what) {
                case DO_ATTACH: {
                    try {
                        mConnection.attachEngine(this);
                    } catch (RemoteException e) {
                        Log.w(TAG, "Wallpaper host disappeared", e);
                        return;
                    }
                    Engine engine = onCreateEngine();
                    mEngine = engine;
                    synchronized (mActiveEngines) {
                        mActiveEngines.add(engine);
                    }
                    engine.attach(this);
                    return;
                }
                case DO_DETACH: {
                    mEngine.detach();
                    synchronized (mActiveEngines) {
                        mActiveEngines.remove(mEngine);
                    }
                    return;
                }
                case DO_SET_DESIRED_SIZE: {
                    mEngine.doDesiredSizeChanged(message.arg1, message.arg2);
                    return;
                }
                case MSG_UPDATE_SURFACE:
                    mEngine.updateSurface(true, false, false);
                    break;
                case MSG_VISIBILITY_CHANGED:
                    if (DEBUG) Log.v(TAG, "Visibility change in " + mEngine
                            + ": " + message.arg1);
                    mEngine.doVisibilityChanged(message.arg1 != 0);
                    break;
                case MSG_WALLPAPER_OFFSETS: {
                    mEngine.doOffsetsChanged();
                } break;
                case MSG_WALLPAPER_COMMAND: {
                    WallpaperCommand cmd = (WallpaperCommand)message.obj;
                    mEngine.doCommand(cmd);
                } break;
                case MSG_WINDOW_RESIZED: {
                    final boolean reportDraw = message.arg1 != 0;
                    mEngine.updateSurface(true, false, reportDraw);
                    mEngine.doOffsetsChanged();
                } break;
                case MSG_TOUCH_EVENT: {
                    boolean skip = false;
                    MotionEvent ev = (MotionEvent)message.obj;
                    if (ev.getAction() == MotionEvent.ACTION_MOVE) {
                        synchronized (mEngine.mLock) {
                            if (mEngine.mPendingMove == ev) {
                                mEngine.mPendingMove = null;
                            } else {
                                // this is not the motion event we are looking for....
                                skip = true;
                            }
                        }
                    }
                    if (!skip) {
                        if (DEBUG) Log.v(TAG, "Delivering touch event: " + ev);
                        mEngine.onTouchEvent(ev);
                    }
                    ev.recycle();
                } break;
                default :
                    Log.w(TAG, "Unknown message type " + message.what);
            }
        }
    }

    /**
     * Implements the internal {@link IWallpaperService} interface to convert
     * incoming calls to it back to calls on an {@link WallpaperService}.
     */
    class IWallpaperServiceWrapper extends IWallpaperService.Stub {
        private final WallpaperService mTarget;

        public IWallpaperServiceWrapper(WallpaperService context) {
            mTarget = context;
        }

        public void attach(IWallpaperConnection conn, IBinder windowToken,
                int windowType, boolean isPreview, int reqWidth, int reqHeight) {
            new IWallpaperEngineWrapper(mTarget, conn, windowToken,
                    windowType, isPreview, reqWidth, reqHeight);
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        synchronized (mActiveEngines) {
           for (int i=0; i<mActiveEngines.size(); i++) {
               mActiveEngines.get(i).detach();
           }
           mActiveEngines.clear();
        }
    }

    /**
     * Implement to return the implementation of the internal accessibility
     * service interface.  Subclasses should not override.
     */
    @Override
    public final IBinder onBind(Intent intent) {
        return new IWallpaperServiceWrapper(this);
    }
    
    /**
     * This allows subclasses to change the thread that most callbacks
     * occur on.  Currently hidden because it is mostly needed for the
     * image wallpaper (which runs in the system process and doesn't want
     * to get stuck running on that seriously in use main thread).  Not
     * exposed right now because the semantics of this are not totally
     * well defined and some callbacks can still happen on the main thread).
     * @hide
     */
    public void setCallbackLooper(Looper looper) {
        mCallbackLooper = looper;
    }
    
    /**
     * Must be implemented to return a new instance of the wallpaper's engine.
     * Note that multiple instances may be active at the same time, such as
     * when the wallpaper is currently set as the active wallpaper and the user
     * is in the wallpaper picker viewing a preview of it as well.
     */
    public abstract Engine onCreateEngine();
}
