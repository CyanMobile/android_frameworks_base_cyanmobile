/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar.navbar;

import java.net.URISyntaxException;
import android.animationing.Animator;
import android.animationing.AnimatorListenerAdapter;
import android.animationing.AnimatorSet;
import android.animationing.ObjectAnimator;
import android.animationing.TimeInterpolator;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ActivityNotFoundException;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Matrix;
import android.graphics.PorterDuff.Mode;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.animation.AccelerateInterpolator;
import android.view.Display;
import android.view.KeyEvent;
import android.view.IWindowManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ImageView;
import android.content.ContentResolver;
import android.content.res.Configuration;
import android.provider.Settings;
import android.database.ContentObserver;
import android.content.BroadcastReceiver;
import android.util.Slog;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.android.systemui.statusbar.StatusBarService;
import com.android.systemui.statusbar.CmStatusBarView;
import com.android.systemui.statusbar.popups.ActionItem;
import com.android.systemui.statusbar.popups.QuickAction;
import com.android.systemui.R;

public class NavigationBarView extends LinearLayout {

    public StatusBarService mServices;

    private final Display mDisplay;
    private static final boolean DEBUG = false;
    private static final String TAG = "NavigationBarView";

    private static final int ID_APPLICATION = 1;
    private static final int ID_DISPLAY = 2;
    private static final int ID_INPUT = 3;
    private static final int ID_UIN = 4;
    private static final int ID_LOCKSCREEN = 5;	
    private static final int ID_PERFORMANCE = 6;
    private static final int ID_POWERSAVER = 7;
    private static final int ID_SOUND = 8;
    private static final int ID_TABLET = 9;
    private static final int ID_WIFI = 10;
    private static final int ID_BLUETOOTH = 11;
    private static final int ID_MOBILENETWORK = 12;
    private static final int ID_TETHERING = 13;
    private static final int ID_APPLICATIONS = 14;	
    private static final int ID_LOCSECURE = 15;
    private static final int ID_SOUNDS = 16;
    private static final int ID_DISPLAYS = 17;
    private static final int ID_CALLSET = 18;
    private static final int ID_STORAGE = 19;
    private static final int ID_PROFILE = 20;
    private static final int ID_PRIVACY = 21;
    private static final int ID_DATETIME = 22;	
    private static final int ID_LANGKEY = 23;
    private static final int ID_VOICEN = 24;
    private static final int ID_ACCESS = 25;
    private static final int ID_DEVELOP = 26;
    private static final int ID_ADWLAUNCHER = 27;
    private static final int ID_BACKILL = 28;
    private static final int ID_SCREENSHOT = 29;
    private static final int ID_POWERMENU = 30;
    private static final int ID_SWITCHAPP = 31;

    private QuickAction quickAction;
    private QuickAction quickActionss;
    private QuickAction quickActionrr;
    private QuickAction quickActionmm;

    private ActionItem appItem;
    private ActionItem dispItem;
    private ActionItem inpItem;
    private ActionItem uisItem;
    private ActionItem lockItem;
    private ActionItem prfmItem;
    private ActionItem pwrsItem;
    private ActionItem sndItem;
    private ActionItem tbltItem;
    private ActionItem wifiItem;
    private ActionItem blueItem;
    private ActionItem mobileItem;
    private ActionItem tetherItem;
    private ActionItem appsItem;
    private ActionItem locksecItem;
    private ActionItem soundsItem;
    private ActionItem displayItem;
    private ActionItem callsItem;
    private ActionItem strgItem;
    private ActionItem prfleItem;
    private ActionItem prvcyItem;
    private ActionItem datetimeItem;
    private ActionItem langkeyItem;
    private ActionItem voicItem;
    private ActionItem accsItem;
    private ActionItem dvlpItem;
    private ActionItem adwItem;
    private ActionItem bckItem;
    private ActionItem sscItem;
    private ActionItem pwrItem;
    private ActionItem swtchItem;

    private static final int SWIPE_MIN_DISTANCE = 150;
    private static final int SWIPE_MAX_OFF_PATH = 100;
    private static final int SWIPE_THRESHOLD_VELOCITY = 100;
    private GestureDetector mGestureDetector;

    private View mNaviBackground;
    private View mNaviAdd;
    private View navButtons;
    private View lowLights;
    private KeyButtonView mHomeButton;
    private KeyButtonView mMenuButton;
    private KeyButtonView mBackButton;
    private KeyButtonView mSearchButton;
    private KeyButtonView mVolUpButton;
    private KeyButtonView mVolDownButton;
    private KeyButtonView mQuickButton;

    private ImageView mHomeOutButton;
    private ImageView mMenuOutButton;
    private ImageView mBackOutButton;
    private ImageView mSearchOutButton;
    private ImageView mQuickOutButton;

    private int mNVColor;
    private int mNext;
    private int mPrevious;
    private boolean mNVShow;
    private boolean mShowNV;
    private boolean mShowVol;
    private int mShowAnimate;
    private int mShowHome;
    private int mShowMenu;
    private int mShowBack;
    private int mShowSearch;
    private int mShowQuicker;
    private boolean mLongPressBackKills;
    private boolean mInputShow;
    private Bitmap mRecentIcon;
    private Bitmap mPowerIcon;
    private Bitmap mHomeIcon;
    private Bitmap mMenuIcon;
    private Bitmap mBackIcon;
    private Bitmap mSearchIcon;
    private Bitmap mQuickIcon;
    private Bitmap mVolUpIcon;
    private Bitmap mVolDownIcon;
    private Bitmap mRecentIconNorm;
    private Bitmap mPowerIconNorm;
    private Bitmap mHomeIconNorm;
    private Bitmap mMenuIconNorm;
    private Bitmap mBackIconNorm;
    private Bitmap mSearchIconNorm;
    private Bitmap mQuickIconNorm;
    private Bitmap mVolUpIconNorm;
    private Bitmap mVolDownIconNorm;
    private Bitmap mRecentIconRot;
    private boolean mVisible = true;
    private boolean mForceRotate = false;
    private boolean mDisableAnimate = false;
    private boolean mDoAnimate = true;
    private Handler mHandler;
    private boolean mAttached;
    private boolean mLowProfile;
    private boolean mNotifnew = false;
    private SettingsObserver mSettingsObserver;
    private Context mContext;
    private int mOverColor;
    private int isLightFirst = 1;

    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.NAVI_BUTTONS), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.NAVI_BUTTONS_ANIMATE), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SHOW_NAVI_BUTTONS), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.NAVI_BUTTON_SHOW_HOME), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.NAVI_BUTTON_SHOW_MENU), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.NAVI_BUTTON_SHOW_BACK), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.NAVI_BUTTON_SHOW_SEARCH), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.NAVI_BUTTON_SHOW_QUICKER), false, this);
            resolver.registerContentObserver(
                    Settings.Secure.getUriFor(Settings.Secure.KILL_APP_LONGPRESS_BACK), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.WATCH_IS_NEXT), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.WATCH_IS_PREVIOUS), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.OVERICON_COLOR), false, this);
            onChange(true);
        }

        @Override
        public void onChange(boolean selfChange) {
            ContentResolver resolver = mContext.getContentResolver();
            int defValuesColor = mContext.getResources().getInteger(com.android.internal.R.color.color_default_cyanmobile);
            mNVShow = (Settings.System.getInt(resolver, Settings.System.SHOW_NAVI_BUTTONS, 1) == 1);
            mShowNV = (Settings.System.getInt(resolver, Settings.System.NAVI_BUTTONS, 1) == 1);
            mShowHome = Settings.System.getInt(resolver, Settings.System.NAVI_BUTTON_SHOW_HOME, 1);
            mShowMenu = Settings.System.getInt(resolver, Settings.System.NAVI_BUTTON_SHOW_MENU, 4);
            mShowBack = Settings.System.getInt(resolver, Settings.System.NAVI_BUTTON_SHOW_BACK, 2);
            mShowSearch = Settings.System.getInt(resolver, Settings.System.NAVI_BUTTON_SHOW_SEARCH, 3);
            mShowQuicker = Settings.System.getInt(resolver, Settings.System.NAVI_BUTTON_SHOW_QUICKER, 4);
            mLongPressBackKills = (Settings.Secure.getInt(resolver, Settings.Secure.KILL_APP_LONGPRESS_BACK, 0) == 1);
            mShowAnimate = Settings.System.getInt(resolver, Settings.System.NAVI_BUTTONS_ANIMATE, 20000);
            mNext = Settings.System.getInt(resolver, Settings.System.WATCH_IS_NEXT, 0);
            mPrevious = Settings.System.getInt(resolver, Settings.System.WATCH_IS_PREVIOUS, 1);
            mOverColor = Settings.System.getInt(resolver, Settings.System.OVERICON_COLOR, defValuesColor);
            updateNaviButtons();
        }
    }

    public NavigationBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mDisplay = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                                       float velocityY) {

                    if (e1==null || e2==null)
                         return false;

                    float dX = e2.getX()-e1.getX();
                    float dY = e1.getY()-e2.getY();
                    if (Math.abs(dY)<SWIPE_MAX_OFF_PATH &&
                        Math.abs(velocityX)>=SWIPE_THRESHOLD_VELOCITY &&
                        Math.abs(dX)>=SWIPE_MIN_DISTANCE ) {
                        if (dX>0) {
                            startExpandActivity();
                        } else {
                            startCollapseActivity();
                        }
                        return true;
                    } else if (Math.abs(dX)<SWIPE_MAX_OFF_PATH &&
                        Math.abs(velocityY)>=SWIPE_THRESHOLD_VELOCITY &&
                        Math.abs(dY)>=SWIPE_MIN_DISTANCE ) {
                        if (dY>0) {
                            mServices.toggleRingPanel();
                        } else {
                            mServices.toggleRingPanel();
                        }
                        return true;
                    }
                    return false;
                }
            });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mHandler = new Handler();

        mNaviAdd = findViewById(R.id.navibarAdd);
        mNaviBackground = findViewById(R.id.navibarBackground);
        navButtons = mNaviBackground.findViewById(R.id.navbuttons);
        lowLights = mNaviBackground.findViewById(R.id.lights_out);
        mNVShow = (Settings.System.getInt(mContext.getContentResolver(), Settings.System.SHOW_NAVI_BUTTONS, 1) == 1);

        if (mNVShow) {
            runIconFirst();
            runQuickActions();

            mHomeButton = (KeyButtonView) mNaviBackground.findViewById(R.id.home);
            mHomeButton.mNavigationBarView = this;
            mMenuButton = (KeyButtonView) mNaviBackground.findViewById(R.id.menu);
            mMenuButton.mNavigationBarView = this;
            mBackButton = (KeyButtonView) mNaviBackground.findViewById(R.id.back);
            mBackButton.mNavigationBarView = this;
            mSearchButton = (KeyButtonView) mNaviBackground.findViewById(R.id.search);
            mSearchButton.mNavigationBarView = this;
            mVolUpButton = (KeyButtonView) mNaviBackground.findViewById(R.id.volup);
            mVolUpButton.mNavigationBarView = this;
            mVolDownButton = (KeyButtonView) mNaviBackground.findViewById(R.id.voldown);
            mVolDownButton.mNavigationBarView = this;
            mQuickButton = (KeyButtonView) mNaviBackground.findViewById(R.id.quicker);
            mQuickButton.mNavigationBarView = this;

            mHomeOutButton = (ImageView) mNaviBackground.findViewById(R.id.home_out);
            mMenuOutButton = (ImageView) mNaviBackground.findViewById(R.id.menu_out);
            mBackOutButton = (ImageView) mNaviBackground.findViewById(R.id.back_out);
            mSearchOutButton = (ImageView) mNaviBackground.findViewById(R.id.search_out);
            mQuickOutButton = (ImageView) mNaviBackground.findViewById(R.id.quicker_out);

            mHomeButton.setOnClickListener(new KeyButtonView.OnClickListener() {
	            @Override
                    public void onClick(View v) {
                      if (mShowHome == 1) {
                        if (DEBUG) Slog.i(TAG, "Home clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_HOME);
                      } else if (mShowHome == 4) {
                        if (DEBUG) Slog.i(TAG, "Menu clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_MENU);
                      } else if (mShowHome == 2) {
                        if (DEBUG) Slog.i(TAG, "Back clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_BACK);
                      } else if (mShowHome == 3) {
                        if (DEBUG) Slog.i(TAG, "Search clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_SEARCH);
                      } else if (mShowHome == 5) {
                        if (DEBUG) Slog.i(TAG, "Quick clicked");
                        if (mShowVol) {
                           mShowVol = false;
                        } else {
                           mShowVol = true;
                        }
                      } else if (mShowHome == 6) {
                        if (Settings.System.getInt(mContext.getContentResolver(),
                                    Settings.System.USE_CUSTOM_HOME_APP_TOGGLE, 0) == 1) {
                           CmStatusBarView.runCustomApp((Settings.System.getString(mContext.getContentResolver(),
                                    Settings.System.USE_CUSTOM_HOME_APP_ACTIVITY)), mContext);
                        }
                      } else if (mShowHome == 7) {
                        if (DEBUG) Slog.i(TAG, "Power clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_POWER);
                      } else if (mShowHome == 8) {
                        if (DEBUG) Slog.i(TAG, "Recent clicked");
                        CmStatusBarView.toggleRecentApps(mContext);
                      }
                      updateNaviButtons();
                    }
                }
            );
            mHomeButton.setOnLongClickListener(new KeyButtonView.OnLongClickListener() {
	           @Override
                   public boolean onLongClick(View v) {
                          if (mShowHome == 1) {
                             CmStatusBarView.toggleRecentApps(mContext);
                             return true;
                          } else if (mShowHome == 4) {
                             quickActionss.show(v);
			     quickActionss.setAnimStyle(QuickAction.ANIM_REFLECT);
                             return true;
                          } else if (mShowHome == 2) {
                             quickActionmm.show(v);
			     quickActionmm.setAnimStyle(QuickAction.ANIM_REFLECT);
                             return true;
                          } else if (mShowHome == 3) {
                             quickActionrr.show(v);
			     quickActionrr.setAnimStyle(QuickAction.ANIM_REFLECT);
                             return true;
                          } else if (mShowHome == 5) {
                             quickAction.show(v);
			     quickAction.setAnimStyle(QuickAction.ANIM_REFLECT);
                             return true;
                          } else if (mShowHome == 7) {
                             CmStatusBarView.simulateKeypress(CmStatusBarView.KEYCODE_VIRTUAL_POWER_LONG);
                             return true;
                          } else {
                             return false;
                          }
                    }
                }
            );

            mMenuButton.setOnClickListener(new KeyButtonView.OnClickListener() {
	            @Override
                    public void onClick(View v) {
                      if (mShowMenu == 1) {
                        if (DEBUG) Slog.i(TAG, "Home clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_HOME);
                      } else if (mShowMenu == 4) {
                        if (DEBUG) Slog.i(TAG, "Menu clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_MENU);
                      } else if (mShowMenu == 2) {
                        if (DEBUG) Slog.i(TAG, "Back clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_BACK);
                      } else if (mShowMenu == 3) {
                        if (DEBUG) Slog.i(TAG, "Search clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_SEARCH);
                      } else if (mShowMenu == 5) {
                        if (DEBUG) Slog.i(TAG, "Quick clicked");
                        if (mShowVol) {
                           mShowVol = false;
                        } else {
                           mShowVol = true;
                        }
                      } else if (mShowMenu == 6) {
                        if (Settings.System.getInt(mContext.getContentResolver(),
                                    Settings.System.USE_CUSTOM_MENU_APP_TOGGLE, 0) == 1) {
                           CmStatusBarView.runCustomApp((Settings.System.getString(mContext.getContentResolver(),
                                    Settings.System.USE_CUSTOM_MENU_APP_ACTIVITY)), mContext);
                        }
                      } else if (mShowMenu == 7) {
                        if (DEBUG) Slog.i(TAG, "Power clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_POWER);
                      } else if (mShowMenu == 8) {
                        if (DEBUG) Slog.i(TAG, "Recent clicked");
                        CmStatusBarView.toggleRecentApps(mContext);
                      }
                      updateNaviButtons();
                    }
                }
            );
            mMenuButton.setOnLongClickListener(new KeyButtonView.OnLongClickListener() {
	           @Override
                   public boolean onLongClick(View v) {
                          if (mShowMenu == 1) {
                             CmStatusBarView.toggleRecentApps(mContext);
                             return true;
                          } else if (mShowMenu == 4) {
                             quickActionss.show(v);
			     quickActionss.setAnimStyle(QuickAction.ANIM_REFLECT);
                             return true;
                          } else if (mShowMenu == 2) {
                             quickActionmm.show(v);
			     quickActionmm.setAnimStyle(QuickAction.ANIM_REFLECT);
                             return true;
                          } else if (mShowMenu == 3) {
                             quickActionrr.show(v);
			     quickActionrr.setAnimStyle(QuickAction.ANIM_REFLECT);
                             return true;
                          } else if (mShowMenu == 5) {
                             quickAction.show(v);
			     quickAction.setAnimStyle(QuickAction.ANIM_REFLECT);
                             return true;
                          } else if (mShowMenu == 7) {
                             CmStatusBarView.simulateKeypress(CmStatusBarView.KEYCODE_VIRTUAL_POWER_LONG);
                             return true;
                          } else {
                             return false;
                          }
                    }
                }
            );

            mBackButton.setOnClickListener(new KeyButtonView.OnClickListener() {
	            @Override
                    public void onClick(View v) {
                      if (mShowBack == 1) {
                        if (DEBUG) Slog.i(TAG, "Home clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_HOME);
                      } else if (mShowBack == 4) {
                        if (DEBUG) Slog.i(TAG, "Menu clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_MENU);
                      } else if (mShowBack == 2) {
                        if (DEBUG) Slog.i(TAG, "Back clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_BACK);
                      } else if (mShowBack == 3) {
                        if (DEBUG) Slog.i(TAG, "Search clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_SEARCH);
                      } else if (mShowBack == 5) {
                        if (DEBUG) Slog.i(TAG, "Quick clicked");
                        if (mShowVol) {
                           mShowVol = false;
                        } else {
                           mShowVol = true;
                        }
                      } else if (mShowBack == 6) {
                        if (Settings.System.getInt(mContext.getContentResolver(),
                                    Settings.System.USE_CUSTOM_BACK_APP_TOGGLE, 0) == 1) {
                           CmStatusBarView.runCustomApp((Settings.System.getString(mContext.getContentResolver(),
                                    Settings.System.USE_CUSTOM_BACK_APP_ACTIVITY)), mContext);
                        }
                      } else if (mShowBack == 7) {
                        if (DEBUG) Slog.i(TAG, "Power clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_POWER);
                      } else if (mShowBack == 8) {
                        if (DEBUG) Slog.i(TAG, "Recent clicked");
                        CmStatusBarView.toggleRecentApps(mContext);
                      }
                      updateNaviButtons();
                    }
                }
            );
            mBackButton.setOnLongClickListener(new KeyButtonView.OnLongClickListener() {
	             @Override
                     public boolean onLongClick(View v) {
                          if (mShowBack == 1) {
                             CmStatusBarView.toggleRecentApps(mContext);
                             return true;
                          } else if (mShowBack == 4) {
                             quickActionss.show(v);
			     quickActionss.setAnimStyle(QuickAction.ANIM_REFLECT);
                             return true;
                          } else if (mShowBack == 2) {
                             quickActionmm.show(v);
			     quickActionmm.setAnimStyle(QuickAction.ANIM_REFLECT);
                             return true;
                          } else if (mShowBack == 3) {
                             quickActionrr.show(v);
			     quickActionrr.setAnimStyle(QuickAction.ANIM_REFLECT);
                             return true;
                          } else if (mShowBack == 5) {
                             quickAction.show(v);
			     quickAction.setAnimStyle(QuickAction.ANIM_REFLECT);
                             return true;
                          } else if (mShowBack == 7) {
                             CmStatusBarView.simulateKeypress(CmStatusBarView.KEYCODE_VIRTUAL_POWER_LONG);
                             return true;
                          } else {
                             return false;
                          }
                      }
                 }
            );

            mSearchButton.setOnClickListener(new KeyButtonView.OnClickListener() {
	            @Override
                    public void onClick(View v) {
                      if (mShowSearch == 1) {
                        if (DEBUG) Slog.i(TAG, "Home clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_HOME);
                      } else if (mShowSearch == 4) {
                        if (DEBUG) Slog.i(TAG, "Menu clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_MENU);
                      } else if (mShowSearch == 2) {
                        if (DEBUG) Slog.i(TAG, "Back clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_BACK);
                      } else if (mShowSearch == 3) {
                        if (DEBUG) Slog.i(TAG, "Search clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_SEARCH);
                      } else if (mShowSearch == 5) {
                        if (DEBUG) Slog.i(TAG, "Quick clicked");
                        if (mShowVol) {
                           mShowVol = false;
                        } else {
                           mShowVol = true;
                        }
                      } else if (mShowSearch == 6) {
                        if (Settings.System.getInt(mContext.getContentResolver(),
                                    Settings.System.USE_CUSTOM_NAVISEARCH_APP_TOGGLE, 0) == 1) {
                           CmStatusBarView.runCustomApp((Settings.System.getString(mContext.getContentResolver(),
                                    Settings.System.USE_CUSTOM_NAVISEARCH_APP_ACTIVITY)), mContext);
                        }
                      } else if (mShowSearch == 7) {
                        if (DEBUG) Slog.i(TAG, "Power clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_POWER);
                      } else if (mShowSearch == 8) {
                        if (DEBUG) Slog.i(TAG, "Recent clicked");
                        CmStatusBarView.toggleRecentApps(mContext);
                      }
                      updateNaviButtons();
                    }
                }
            );
            mSearchButton.setOnLongClickListener(new KeyButtonView.OnLongClickListener() {
	             @Override
                     public boolean onLongClick(View v) {
                          if (mShowSearch == 1) {
                             CmStatusBarView.toggleRecentApps(mContext);
                             return true;
                          } else if (mShowSearch == 4) {
                             quickActionss.show(v);
			     quickActionss.setAnimStyle(QuickAction.ANIM_REFLECT);
                             return true;
                          } else if (mShowSearch == 2) {
                             quickActionmm.show(v);
			     quickActionmm.setAnimStyle(QuickAction.ANIM_REFLECT);
                             return true;
                          } else if (mShowSearch == 3) {
                             quickActionrr.show(v);
			     quickActionrr.setAnimStyle(QuickAction.ANIM_REFLECT);
                             return true;
                          } else if (mShowSearch == 5) {
                             quickAction.show(v);
			     quickAction.setAnimStyle(QuickAction.ANIM_REFLECT);
                             return true;
                          } else if (mShowSearch == 7) {
                             CmStatusBarView.simulateKeypress(CmStatusBarView.KEYCODE_VIRTUAL_POWER_LONG);
                             return true;
                          } else {
                             return false;
                          }
                      }
                 }
            );

            mVolUpButton.setOnClickListener(new KeyButtonView.OnClickListener() {
	            @Override
                    public void onClick(View v) {
                        if (DEBUG) Slog.i(TAG, "VolUp clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_VOLUME_UP);
                        mHandler.postDelayed(mResetVolUp, 80);
                    }
                }
            );

            mVolDownButton.setOnClickListener(new KeyButtonView.OnClickListener() {
	            @Override
                    public void onClick(View v) {
                        if (DEBUG) Slog.i(TAG, "VolDown clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_VOLUME_DOWN);
                        mHandler.postDelayed(mResetVolDown, 80);
                    }
                }
            );

            mQuickButton.setOnClickListener(new KeyButtonView.OnClickListener() {
	           @Override
                   public void onClick(View v) {
                      if (mShowQuicker == 0) {
                        if (DEBUG) Slog.i(TAG, "Home clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_HOME);
                      } else if (mShowQuicker == 3) {
                        if (DEBUG) Slog.i(TAG, "Menu clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_MENU);
                      } else if (mShowQuicker == 1) {
                        if (DEBUG) Slog.i(TAG, "Back clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_BACK);
                      } else if (mShowQuicker == 2) {
                        if (DEBUG) Slog.i(TAG, "Search clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_SEARCH);
                      } else if (mShowQuicker == 4) {
                        if (DEBUG) Slog.i(TAG, "Quick clicked");
                        if (mShowVol) {
                           mShowVol = false;
                        } else {
                           mShowVol = true;
                        }
                      } else if (mShowQuicker == 5) {
                        if (Settings.System.getInt(mContext.getContentResolver(),
                                  Settings.System.USE_CUSTOM_QUICK_APP_TOGGLE, 0) == 1) {
                           CmStatusBarView.runCustomApp((Settings.System.getString(mContext.getContentResolver(),
                                    Settings.System.USE_CUSTOM_QUICK_APP_ACTIVITY)), mContext);
                        }
                      } else if (mShowQuicker == 6) {
                        if (DEBUG) Slog.i(TAG, "Power clicked");
                        CmStatusBarView.simulateKeypress(KeyEvent.KEYCODE_POWER);
                      } else if (mShowQuicker == 7) {
                        if (DEBUG) Slog.i(TAG, "Recent clicked");
                        CmStatusBarView.toggleRecentApps(mContext);
                      }
                      updateNaviButtons();
                   }
                }
            );
            mQuickButton.setOnLongClickListener(new KeyButtonView.OnLongClickListener() {
                   @Override
                   public boolean onLongClick(View v) {
                          if (mShowQuicker == 0) {
                             CmStatusBarView.toggleRecentApps(mContext);
                             return true;
                          } else if (mShowQuicker == 3) {
                             quickActionss.show(v);
			     quickActionss.setAnimStyle(QuickAction.ANIM_REFLECT);
                             return true;
                          } else if (mShowQuicker == 1) {
                             quickActionmm.show(v);
			     quickActionmm.setAnimStyle(QuickAction.ANIM_REFLECT);
                             return true;
                          } else if (mShowQuicker == 2) {
                             quickActionrr.show(v);
			     quickActionrr.setAnimStyle(QuickAction.ANIM_REFLECT);
                             return true;
                          } else if (mShowQuicker == 4) {
                             quickAction.show(v);
			     quickAction.setAnimStyle(QuickAction.ANIM_REFLECT);
                             return true;
                          } else if (mShowQuicker == 6) {
                             CmStatusBarView.simulateKeypress(CmStatusBarView.KEYCODE_VIRTUAL_POWER_LONG);
                             return true;
                          } else {
                             return false;
                          }
                     }
                 }
            );

            // set up settings observer
            mSettingsObserver = new SettingsObserver(mHandler);
        }
    }

    private void runIconFirst() {
        mRecentIconNorm = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_sysbar_recent);
        mPowerIconNorm = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_sysbar_power);
        mHomeIconNorm = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_sysbar_home);
        mMenuIconNorm = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_sysbar_menu);
        mBackIconNorm = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_sysbar_back);
        mSearchIconNorm = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_sysbar_search);
        mQuickIconNorm = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_sysbar_quickna);
        mVolUpIconNorm = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_sysbar_volup);
        mVolDownIconNorm = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_sysbar_voldown);
    }

    private void runQuickActions() {
        quickAction = new QuickAction(mContext, QuickAction.VERTICAL);
        quickActionss = new QuickAction(mContext, QuickAction.VERTICAL);
        quickActionrr = new QuickAction(mContext, QuickAction.VERTICAL);
        quickActionmm = new QuickAction(mContext, QuickAction.VERTICAL);
        appItem = new ActionItem(ID_APPLICATION, "Application");
        dispItem = new ActionItem(ID_DISPLAY, "Display");
        inpItem = new ActionItem(ID_INPUT, "Input");
        uisItem = new ActionItem(ID_UIN, "Interface");
        lockItem = new ActionItem(ID_LOCKSCREEN, "Lockscreen");
        prfmItem = new ActionItem(ID_PERFORMANCE, "Performance");
        pwrsItem = new ActionItem(ID_POWERSAVER, "Power saver");
        sndItem = new ActionItem(ID_SOUND, "Sound");
        tbltItem = new ActionItem(ID_TABLET, "Tablet tweaks");
        wifiItem = new ActionItem(ID_WIFI, "Wifi settings");
        blueItem = new ActionItem(ID_BLUETOOTH, "Bluetooth settings");
        mobileItem = new ActionItem(ID_MOBILENETWORK, "Mobile Networks");
        tetherItem = new ActionItem(ID_TETHERING, "Tether settings");
        appsItem = new ActionItem(ID_APPLICATIONS, "Applications");
        locksecItem = new ActionItem(ID_LOCSECURE, "Location and Security");
        soundsItem = new ActionItem(ID_SOUNDS, "Sound settings");
        displayItem = new ActionItem(ID_DISPLAYS, "Display settings");
        callsItem = new ActionItem(ID_CALLSET, "Call settings");
        strgItem = new ActionItem(ID_STORAGE, "Storage settings");
        prfleItem = new ActionItem(ID_PROFILE, "Profile settings");
        prvcyItem = new ActionItem(ID_PRIVACY, "Privacy settings");
        datetimeItem = new ActionItem(ID_DATETIME, "Date and Time");
        langkeyItem = new ActionItem(ID_LANGKEY, "Language and Keyboard");
        voicItem = new ActionItem(ID_VOICEN, "Voice input n output");
        accsItem = new ActionItem(ID_ACCESS, "Accessibility");
        dvlpItem = new ActionItem(ID_DEVELOP, "Development");
        adwItem = new ActionItem(ID_ADWLAUNCHER, "Launcher settings");
        bckItem = new ActionItem(ID_BACKILL, "KillAll app");
        sscItem = new ActionItem(ID_SCREENSHOT, "Screenshots");
        pwrItem = new ActionItem(ID_POWERMENU, "Power menu");
        swtchItem = new ActionItem(ID_SWITCHAPP, "Switch app");
        quickAction.addActionItem(appItem);
        quickAction.addActionItem(dispItem);
        quickAction.addActionItem(inpItem);
        quickAction.addActionItem(uisItem);
        quickAction.addActionItem(lockItem);
        quickAction.addActionItem(prfmItem);
        quickAction.addActionItem(pwrsItem);
        quickAction.addActionItem(sndItem);
        quickAction.addActionItem(tbltItem);
        quickActionss.addActionItem(wifiItem);
        quickActionss.addActionItem(blueItem);
        quickActionss.addActionItem(mobileItem);
        quickActionss.addActionItem(tetherItem);
        quickActionss.addActionItem(appsItem);
        quickActionss.addActionItem(locksecItem);
        quickActionss.addActionItem(soundsItem);
        quickActionss.addActionItem(displayItem);
        quickActionss.addActionItem(callsItem);
        quickActionrr.addActionItem(strgItem);
        quickActionrr.addActionItem(prfleItem);
        quickActionrr.addActionItem(prvcyItem);
        quickActionrr.addActionItem(datetimeItem);
        quickActionrr.addActionItem(langkeyItem);
        quickActionrr.addActionItem(voicItem);
        quickActionrr.addActionItem(accsItem);
        quickActionrr.addActionItem(dvlpItem);
        quickActionrr.addActionItem(adwItem);
        quickActionmm.addActionItem(bckItem);
        quickActionmm.addActionItem(sscItem);
        quickActionmm.addActionItem(pwrItem);
        quickActionmm.addActionItem(swtchItem);

            quickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {			
                  @Override
                  public void onItemClick(QuickAction source, int pos, int actionId) {				
                       ActionItem actionItem = quickAction.getActionItem(pos);
                       if (actionId == ID_APPLICATION) {
                           CmStatusBarView.runCMSettings("com.cyanogenmod.cmparts.activities.ApplicationActivity", mContext);
                       } else if (actionId == ID_DISPLAY) {
                           CmStatusBarView.runCMSettings("com.cyanogenmod.cmparts.activities.DisplayActivity", mContext);
                       } else if (actionId == ID_INPUT) {
                           CmStatusBarView.runCMSettings("com.cyanogenmod.cmparts.activities.InputActivity", mContext);
                       } else if (actionId == ID_UIN) {
                           CmStatusBarView.runCMSettings("com.cyanogenmod.cmparts.activities.UIActivity", mContext);
                       } else if (actionId == ID_LOCKSCREEN) {
                           CmStatusBarView.runCMSettings("com.cyanogenmod.cmparts.activities.LockscreenActivity", mContext);
                       } else if (actionId == ID_PERFORMANCE) {
                           CmStatusBarView.runCMSettings("com.cyanogenmod.cmparts.activities.PerformanceSettingsActivity", mContext);
                       } else if (actionId == ID_POWERSAVER) {
                           CmStatusBarView.runCMSettings("com.cyanogenmod.cmparts.activities.PowerSaverActivity", mContext);
                       } else if (actionId == ID_SOUND) {
                           CmStatusBarView.runCMSettings("com.cyanogenmod.cmparts.activities.SoundActivity", mContext);
                       } else if (actionId == ID_TABLET) {
                           CmStatusBarView.runCMSettings("com.cyanogenmod.cmparts.activities.TabletTweaksActivity", mContext);
                       }
                   }
             });

             quickAction.setOnDismissListener(new QuickAction.OnDismissListener() {			
                   @Override public void onDismiss() {}
             });

             quickActionss.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {			
                   @Override
                   public void onItemClick(QuickAction source, int pos, int actionId) {				
                        ActionItem actionItem = quickAction.getActionItem(pos);
                        if (actionId == ID_WIFI) {
                            CmStatusBarView.runSettings("com.android.settings.wifi.WifiSettings", mContext);
                        } else if (actionId == ID_BLUETOOTH) {
                            CmStatusBarView.runSettings("com.android.settings.bluetooth.BluetoothSettings", mContext);
                        } else if (actionId == ID_MOBILENETWORK) {
                            CmStatusBarView.runPhoneSettings("com.android.phone.Settings", mContext);
                        } else if (actionId == ID_TETHERING) {
                            CmStatusBarView.runSettings("com.android.settings.TetherSettings", mContext);
                        } else if (actionId == ID_APPLICATIONS) {
                            CmStatusBarView.runSettings("com.android.settings.ApplicationSettings", mContext);
                        } else if (actionId == ID_LOCSECURE) {
                            CmStatusBarView.runSettings("com.android.settings.SecuritySettings", mContext);
                        } else if (actionId == ID_SOUNDS) {
                            CmStatusBarView.runSettings("com.android.settings.SoundSettings", mContext);
                        } else if (actionId == ID_DISPLAYS) {
                            CmStatusBarView.runSettings("com.android.settings.DisplaySettings", mContext);
                        } else if (actionId == ID_CALLSET) {
                            CmStatusBarView.runPhoneSettings("com.android.phone.CallFeaturesSetting", mContext);
                        }
                   }
             });
		
             quickActionss.setOnDismissListener(new QuickAction.OnDismissListener() {			
                   @Override public void onDismiss() {}
             });

             quickActionrr.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {			
                   @Override
                   public void onItemClick(QuickAction source, int pos, int actionId) {				
                         ActionItem actionItem = quickAction.getActionItem(pos);
                         if (actionId == ID_STORAGE) {
                             CmStatusBarView.runSettings("com.android.settings.deviceinfo.Memory", mContext);
                         } else if (actionId == ID_PROFILE) {
                             CmStatusBarView.runSettings("com.android.settings.ProfileList", mContext);
                         } else if (actionId == ID_PRIVACY) {
                             CmStatusBarView.runSettings("com.android.settings.PrivacySettings", mContext);
                         } else if (actionId == ID_DATETIME) {
                             CmStatusBarView.runSettings("com.android.settings.DateTimeSettings", mContext);
                         } else if (actionId == ID_LANGKEY) {
                             CmStatusBarView.runSettings("com.android.settings.LanguageSettings", mContext);
                         } else if (actionId == ID_VOICEN) {
                             CmStatusBarView.runSettings("com.android.settings.VoiceInputOutputSettings", mContext);
                         } else if (actionId == ID_ACCESS) {
                             CmStatusBarView.runSettings("com.android.settings.AccessibilitySettings", mContext);
                         } else if (actionId == ID_DEVELOP) {
                             CmStatusBarView.runSettings("com.android.settings.DevelopmentSettings", mContext);
                         } else if (actionId == ID_ADWLAUNCHER) {
                             CmStatusBarView.runLauncherSettings(mContext);
                         }
                   }
             });

             quickActionrr.setOnDismissListener(new QuickAction.OnDismissListener() {			
                   @Override public void onDismiss() {}
             });

             quickActionmm.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {			
                   @Override
                   public void onItemClick(QuickAction source, int pos, int actionId) {				
                       ActionItem actionItem = quickAction.getActionItem(pos);
                       if (actionId == ID_BACKILL) {
                           if (mLongPressBackKills) {
                               CmStatusBarView.simulateKeypress(CmStatusBarView.KEYCODE_VIRTUAL_BACK_LONG);
                           } else {
                               Toast toast = Toast.makeText(mContext, "Enable Kill app back button option to use this!",Toast.LENGTH_LONG);
                               toast.show();
                           }
                       } else if (actionId == ID_SCREENSHOT) {
                           CmStatusBarView.toggleScreenshot(mContext);
                       } else if (actionId == ID_POWERMENU) {
                           CmStatusBarView.simulateKeypress(CmStatusBarView.KEYCODE_VIRTUAL_POWER_LONG);
                       } else if (actionId == ID_SWITCHAPP) {
                           CmStatusBarView.toggleLastApp(mContext);
                       }
                   }
              });
		
              quickActionmm.setOnDismissListener(new QuickAction.OnDismissListener() {			
                   @Override public void onDismiss() {}
              });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            if (mNVShow) mSettingsObserver.observe();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            if (mNVShow) mContext.getContentResolver().unregisterContentObserver(mSettingsObserver);
            mAttached = false;
        }
    }

    private boolean isEventInButton(final KeyButtonView button, final MotionEvent event) {
        return mNVShow && button != null
            && button.getLeft() <= event.getRawX()
            && button.getRight() >= event.getRawX()
            && button.getTop() <= event.getRawY()
            && button.getBottom() >= event.getRawY();
        }

    public void reorient() {
         final int rot = mDisplay.getRotation();
         mForceRotate = (rot == Surface.ROTATION_90 || rot == Surface.ROTATION_270);

        if (mNVShow) {
            mNaviAdd.setVisibility(View.VISIBLE);
            setLowProfile(mLowProfile, false, true /* force */);
        } else {
            mNaviAdd.setVisibility(View.GONE);
        }

        if (mShowAnimate == 1) {
            mDisableAnimate = true;
            mRecentIcon = mForceRotate ? runIconRot(180, mRecentIconNorm) : mRecentIconNorm;
            mPowerIcon = mForceRotate ? runIconRot(180, mPowerIconNorm) : mPowerIconNorm;
            mHomeIcon = mForceRotate ? runIconRot(180, mHomeIconNorm) : mHomeIconNorm;
            mMenuIcon = mForceRotate ? runIconRot(180, mMenuIconNorm) : mMenuIconNorm;
            mBackIcon = mForceRotate ? runIconRot(180, mBackIconNorm) : mBackIconNorm;
            mSearchIcon = mForceRotate ? runIconRot(180, mSearchIconNorm) : mSearchIconNorm;
            mQuickIcon = mForceRotate ? runIconRot(180, mQuickIconNorm) : mQuickIconNorm;
            mVolUpIcon = mForceRotate ? runIconRot(180, mVolUpIconNorm) : mVolUpIconNorm;
            mVolDownIcon = mForceRotate ? runIconRot(180, mVolDownIconNorm) : mVolDownIconNorm;
        } else if (mShowAnimate == 0) {
            mDisableAnimate = true;
            mRecentIcon = mRecentIconNorm;
            mPowerIcon = mPowerIconNorm;
            mHomeIcon = mHomeIconNorm;
            mMenuIcon = mMenuIconNorm;
            mBackIcon = mBackIconNorm;
            mSearchIcon = mSearchIconNorm;
            mQuickIcon = mQuickIconNorm;
            mVolUpIcon = mVolUpIconNorm;
            mVolDownIcon = mVolDownIconNorm;
        }

        updateNaviButtons();
    }

    private Runnable mResetNormal = new Runnable() {
        @Override
        public void run() {
             mDisableAnimate = false;
             mRecentIcon = mRecentIconNorm;
             mPowerIcon = mPowerIconNorm;
             mHomeIcon = mHomeIconNorm;
             mMenuIcon = mMenuIconNorm;
             mBackIcon = mBackIconNorm;
             mSearchIcon = mSearchIconNorm;
             mQuickIcon = mQuickIconNorm;
             mVolUpIcon = mVolUpIconNorm;
             mVolDownIcon = mVolDownIconNorm;
             updateNaviButtons();
             if (mShowAnimate == 2 && !mDisableAnimate && mDoAnimate) {
                mHandler.removeCallbacks(mResetRotate30);
                 mHandler.postDelayed(mResetRotate30, 1000);
             }
             if (mShowAnimate > 3 && !mDisableAnimate && mDoAnimate) {
                mHandler.removeCallbacks(mResetRotate30);
                 mHandler.postDelayed(mResetRotate30, mShowAnimate);
             }
        }
    };

    private Runnable mResetRotate30 = new Runnable() {
        @Override
        public void run() {
            mRecentIcon = runIconRot(30, mRecentIconNorm);
            mPowerIcon = runIconRot(30, mPowerIconNorm);
            mHomeIcon = runIconRot(30, mHomeIconNorm);
            mMenuIcon = runIconRot(30, mMenuIconNorm);
            mBackIcon = runIconRot(30, mBackIconNorm);
            mSearchIcon = runIconRot(30, mSearchIconNorm);
            mQuickIcon = runIconRot(30, mQuickIconNorm);
            mVolUpIcon = runIconRot(30, mVolUpIconNorm);
            mVolDownIcon = runIconRot(30, mVolDownIconNorm);
            updateNaviButtons();
            if (mShowAnimate == 2) {
                mHandler.removeCallbacks(mResetRotate60);
                mHandler.postDelayed(mResetRotate60, 10);
            } else {
                mHandler.removeCallbacks(mResetRotate60);
                mHandler.postDelayed(mResetRotate60, mShowAnimate);
            }
        }
    };

    private Runnable mResetRotate60 = new Runnable() {
        @Override
        public void run() {
            mRecentIcon = runIconRot(60, mRecentIconNorm);
            mPowerIcon = runIconRot(60, mPowerIconNorm);
            mHomeIcon = runIconRot(60, mHomeIconNorm);
            mMenuIcon = runIconRot(60, mMenuIconNorm);
            mBackIcon = runIconRot(60, mBackIconNorm);
            mSearchIcon = runIconRot(60, mSearchIconNorm);
            mQuickIcon = runIconRot(60, mQuickIconNorm);
            mVolUpIcon = runIconRot(60, mVolUpIconNorm);
            mVolDownIcon = runIconRot(60, mVolDownIconNorm);
            updateNaviButtons();
            if (mShowAnimate == 2) {
                mHandler.removeCallbacks(mResetRotate90);
                mHandler.postDelayed(mResetRotate90, 10);
            } else {
                mHandler.removeCallbacks(mResetRotate90);
                mHandler.postDelayed(mResetRotate90, mShowAnimate);
            }
        }
    };

    private Runnable mResetRotate90 = new Runnable() {
        @Override
        public void run() {
            mRecentIcon = runIconRot(90, mRecentIconNorm);
            mPowerIcon = runIconRot(90, mPowerIconNorm);
            mHomeIcon = runIconRot(90, mHomeIconNorm);
            mMenuIcon = runIconRot(90, mMenuIconNorm);
            mBackIcon = runIconRot(90, mBackIconNorm);
            mSearchIcon = runIconRot(90, mSearchIconNorm);
            mQuickIcon = runIconRot(90, mQuickIconNorm);
            mVolUpIcon = runIconRot(90, mVolUpIconNorm);
            mVolDownIcon = runIconRot(90, mVolDownIconNorm);
            updateNaviButtons();
            if (mShowAnimate == 2) {
                mHandler.removeCallbacks(mResetRotate120);
                mHandler.postDelayed(mResetRotate120, 10);
            } else {
                mHandler.removeCallbacks(mResetRotate120);
                mHandler.postDelayed(mResetRotate120, mShowAnimate);
            }
        }
    };

    private Runnable mResetRotate120 = new Runnable() {
        @Override
        public void run() {
            mRecentIcon = runIconRot(120, mRecentIconNorm);
            mPowerIcon = runIconRot(120, mPowerIconNorm);
            mHomeIcon = runIconRot(120, mHomeIconNorm);
            mMenuIcon = runIconRot(120, mMenuIconNorm);
            mBackIcon = runIconRot(120, mBackIconNorm);
            mSearchIcon = runIconRot(120, mSearchIconNorm);
            mQuickIcon = runIconRot(120, mQuickIconNorm);
            mVolUpIcon = runIconRot(120, mVolUpIconNorm);
            mVolDownIcon = runIconRot(120, mVolDownIconNorm);
            updateNaviButtons();
            if (mShowAnimate == 2) {
                mHandler.removeCallbacks(mResetRotate150);
                mHandler.postDelayed(mResetRotate150, 10);
            } else {
                mHandler.removeCallbacks(mResetRotate150);
                mHandler.postDelayed(mResetRotate150, mShowAnimate);
            }
        }
    };

    private Runnable mResetRotate150 = new Runnable() {
        @Override
        public void run() {
            mRecentIcon = runIconRot(150, mRecentIconNorm);
            mPowerIcon = runIconRot(150, mPowerIconNorm);
            mHomeIcon = runIconRot(150, mHomeIconNorm);
            mMenuIcon = runIconRot(150, mMenuIconNorm);
            mBackIcon = runIconRot(150, mBackIconNorm);
            mSearchIcon = runIconRot(150, mSearchIconNorm);
            mQuickIcon = runIconRot(150, mQuickIconNorm);
            mVolUpIcon = runIconRot(150, mVolUpIconNorm);
            mVolDownIcon = runIconRot(150, mVolDownIconNorm);
            updateNaviButtons();
            if (mShowAnimate == 2) {
                mHandler.removeCallbacks(mResetRotate180);
                mHandler.postDelayed(mResetRotate180, 10);
            } else {
                mHandler.removeCallbacks(mResetRotate180);
                mHandler.postDelayed(mResetRotate180, mShowAnimate);
            }
        }
    };

    private Runnable mResetRotate180 = new Runnable() {
        @Override
        public void run() {
            mRecentIcon = runIconRot(180, mRecentIconNorm);
            mPowerIcon = runIconRot(180, mPowerIconNorm);
            mHomeIcon = runIconRot(180, mHomeIconNorm);
            mMenuIcon = runIconRot(180, mMenuIconNorm);
            mBackIcon = runIconRot(180, mBackIconNorm);
            mSearchIcon = runIconRot(180, mSearchIconNorm);
            mQuickIcon = runIconRot(180, mQuickIconNorm);
            mVolUpIcon = runIconRot(180, mVolUpIconNorm);
            mVolDownIcon = runIconRot(180, mVolDownIconNorm);
            updateNaviButtons();
            if (mShowAnimate == 2) {
                mHandler.removeCallbacks(mResetRotate210);
                mHandler.postDelayed(mResetRotate210, 10);
            } else {
                mHandler.removeCallbacks(mResetRotate210);
                mHandler.postDelayed(mResetRotate210, mShowAnimate);
            }
        }
    };

    private Runnable mResetRotate210 = new Runnable() {
        @Override
        public void run() {
            mRecentIcon = runIconRot(210, mRecentIconNorm);
            mPowerIcon = runIconRot(210, mPowerIconNorm);
            mHomeIcon = runIconRot(210, mHomeIconNorm);
            mMenuIcon = runIconRot(210, mMenuIconNorm);
            mBackIcon = runIconRot(210, mBackIconNorm);
            mSearchIcon = runIconRot(210, mSearchIconNorm);
            mQuickIcon = runIconRot(210, mQuickIconNorm);
            mVolUpIcon = runIconRot(210, mVolUpIconNorm);
            mVolDownIcon = runIconRot(210, mVolDownIconNorm);
            updateNaviButtons();
            if (mShowAnimate == 2) {
                mHandler.removeCallbacks(mResetRotate240);
                mHandler.postDelayed(mResetRotate240, 10);
            } else {
                mHandler.removeCallbacks(mResetRotate240);
                mHandler.postDelayed(mResetRotate240, mShowAnimate);
            }
        }
    };

    private Runnable mResetRotate240 = new Runnable() {
        @Override
        public void run() {
            mRecentIcon = runIconRot(240, mRecentIconNorm);
            mPowerIcon = runIconRot(240, mPowerIconNorm);
            mHomeIcon = runIconRot(240, mHomeIconNorm);
            mMenuIcon = runIconRot(240, mMenuIconNorm);
            mBackIcon = runIconRot(240, mBackIconNorm);
            mSearchIcon = runIconRot(240, mSearchIconNorm);
            mQuickIcon = runIconRot(240, mQuickIconNorm);
            mVolUpIcon = runIconRot(240, mVolUpIconNorm);
            mVolDownIcon = runIconRot(240, mVolDownIconNorm);
            updateNaviButtons();
            if (mShowAnimate == 2) {
                mHandler.removeCallbacks(mResetRotate270);
                mHandler.postDelayed(mResetRotate270, 10);
            } else {
                mHandler.removeCallbacks(mResetRotate270);
                mHandler.postDelayed(mResetRotate270, mShowAnimate);
            }
        }
    };

    private Runnable mResetRotate270 = new Runnable() {
        @Override
        public void run() {
            mRecentIcon = runIconRot(270, mRecentIconNorm);
            mPowerIcon = runIconRot(270, mPowerIconNorm);
            mHomeIcon = runIconRot(270, mHomeIconNorm);
            mMenuIcon = runIconRot(270, mMenuIconNorm);
            mBackIcon = runIconRot(270, mBackIconNorm);
            mSearchIcon = runIconRot(270, mSearchIconNorm);
            mQuickIcon = runIconRot(270, mQuickIconNorm);
            mVolUpIcon = runIconRot(270, mVolUpIconNorm);
            mVolDownIcon = runIconRot(270, mVolDownIconNorm);
            updateNaviButtons();
            if (mShowAnimate == 2) {
                mHandler.removeCallbacks(mResetRotate300);
                mHandler.postDelayed(mResetRotate300, 10);
            } else {
                mHandler.removeCallbacks(mResetRotate300);
                mHandler.postDelayed(mResetRotate300, mShowAnimate);
            }
        }
    };

    private Runnable mResetRotate300 = new Runnable() {
        @Override
        public void run() {
            mRecentIcon = runIconRot(300, mRecentIconNorm);
            mPowerIcon = runIconRot(300, mPowerIconNorm);
            mHomeIcon = runIconRot(300, mHomeIconNorm);
            mMenuIcon = runIconRot(300, mMenuIconNorm);
            mBackIcon = runIconRot(300, mBackIconNorm);
            mSearchIcon = runIconRot(300, mSearchIconNorm);
            mQuickIcon = runIconRot(300, mQuickIconNorm);
            mVolUpIcon = runIconRot(300, mVolUpIconNorm);
            mVolDownIcon = runIconRot(300, mVolDownIconNorm);
            updateNaviButtons();
            if (mShowAnimate == 2) {
                mHandler.removeCallbacks(mResetRotate330);
                mHandler.postDelayed(mResetRotate330, 10);
            } else {
                mHandler.removeCallbacks(mResetRotate330);
                mHandler.postDelayed(mResetRotate330, mShowAnimate);
            }
        }
    };

    private Runnable mResetRotate330 = new Runnable() {
        @Override
        public void run() {
            mRecentIcon = runIconRot(330, mRecentIconNorm);
            mPowerIcon = runIconRot(330, mPowerIconNorm);
            mHomeIcon = runIconRot(330, mHomeIconNorm);
            mMenuIcon = runIconRot(330, mMenuIconNorm);
            mBackIcon = runIconRot(330, mBackIconNorm);
            mSearchIcon = runIconRot(330, mSearchIconNorm);
            mQuickIcon = runIconRot(330, mQuickIconNorm);
            mVolUpIcon = runIconRot(330, mVolUpIconNorm);
            mVolDownIcon = runIconRot(330, mVolDownIconNorm);
            updateNaviButtons();
            if (mShowAnimate == 2) {
                mHandler.removeCallbacks(mResetNormal);
                mHandler.postDelayed(mResetNormal, 10);
            } else {
                mHandler.removeCallbacks(mResetNormal);
                mHandler.postDelayed(mResetNormal, mShowAnimate);
            }
        }
    };

    public void setIMEVisible(boolean visible) {
        mInputShow = visible;
        updateNaviButtons();
    }

    public void setNaviVisible(boolean visible) {
        if (visible == mVisible) return;

        mVisible = visible;
      if (mShowNV) {
        if (visible) {
           mNaviBackground.setVisibility(View.VISIBLE);
           mDoAnimate = true;
        } else {
           mNaviBackground.setVisibility(View.GONE);
           mDoAnimate = false;
        }
        updateNaviButtons();
        setLowProfileAfterHide(false);
      }
    }

    public boolean onTouchEvent(final MotionEvent event){
        if (!mNVShow) return super.onTouchEvent(event);

        if (isEventInButton(mHomeButton, event)) {
            mHomeButton.onTouchEvent(event);
            return true;
        }
        if (isEventInButton(mMenuButton, event)) {
            mMenuButton.onTouchEvent(event);
            return true;
        }
        if (isEventInButton(mBackButton, event)) {
            mBackButton.onTouchEvent(event);
            return true;
        }
        if (isEventInButton(mSearchButton, event)) {
            mSearchButton.onTouchEvent(event);
            return true;
        }
        if (isEventInButton(mVolUpButton, event)) {
            mVolUpButton.onTouchEvent(event);
            return true;
        }
        if (isEventInButton(mVolDownButton, event)) {
            mVolDownButton.onTouchEvent(event);
            return true;
        }
        if (isEventInButton(mQuickButton, event)) {
            mQuickButton.onTouchEvent(event);
            return true;
        }
        if (mNVShow) {
            mGestureDetector.onTouchEvent(event);
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    private void updateNaviButtons() {
        if (!mNVShow) return;

        // toggle visibility of buttons - at first, toggle all visible
        mHomeButton.setVisibility(View.VISIBLE);
        mMenuButton.setVisibility(View.VISIBLE);
        mBackButton.setVisibility(View.VISIBLE);
        mSearchButton.setVisibility(View.VISIBLE);
        mVolUpButton.setVisibility(View.VISIBLE);
        mVolDownButton.setVisibility(View.VISIBLE);
        mQuickButton.setVisibility(View.VISIBLE);

        mHomeOutButton.setVisibility(View.VISIBLE);
        mMenuOutButton.setVisibility(View.VISIBLE);
        mBackOutButton.setVisibility(View.VISIBLE);
        mSearchOutButton.setVisibility(View.VISIBLE);
        mQuickOutButton.setVisibility(View.VISIBLE);

        if (mVisible && mShowNV) {
           mNaviBackground.setVisibility(View.VISIBLE);
        }

        if (!mShowNV) {
           mNaviBackground.setVisibility(View.GONE);
        }

        // now toggle off unneeded stuff
        if (mShowHome == 0) {
            mHomeButton.setVisibility(View.GONE);
            mHomeOutButton.setVisibility(View.GONE);
        } else if (mShowHome == 9) {
            mHomeButton.setVisibility(View.INVISIBLE);
            mHomeOutButton.setVisibility(View.INVISIBLE);
        }

        if (mShowMenu == 0) {
            mMenuButton.setVisibility(View.GONE);
            mMenuOutButton.setVisibility(View.GONE);
        } else if (mShowMenu == 9) {
            mMenuButton.setVisibility(View.INVISIBLE);
            mMenuOutButton.setVisibility(View.INVISIBLE);
        }

        if (mShowBack == 0) {
            mBackButton.setVisibility(View.GONE);
            mBackOutButton.setVisibility(View.GONE);
        } else if (mShowBack == 9) {
            mBackButton.setVisibility(View.INVISIBLE);
            mBackOutButton.setVisibility(View.INVISIBLE);
        }

        if (mShowSearch == 0) {
            mSearchButton.setVisibility(View.GONE);
            mSearchOutButton.setVisibility(View.GONE);
        } else if (mShowSearch == 9) {
            mSearchButton.setVisibility(View.INVISIBLE);
            mSearchOutButton.setVisibility(View.INVISIBLE);
        }

        if (!mShowVol) {
            mVolUpButton.setVisibility(View.GONE);
            mVolDownButton.setVisibility(View.GONE);
        }

        if ((mShowAnimate == 0) || (mShowAnimate == 1)) {
            mDisableAnimate = true;
        }

        if (mShowAnimate >= 2 && mDisableAnimate) {
            mHandler.postDelayed(mResetNormal, 10);
        }

        mHandler.removeCallbacks(mResetHome);
        mHandler.postDelayed(mResetHome, 10);

        mHandler.removeCallbacks(mResetMenu);
        mHandler.postDelayed(mResetMenu, 10);

        mHandler.removeCallbacks(mResetBack);
        mHandler.postDelayed(mResetBack, 10);

        mHandler.removeCallbacks(mResetSearch);
        mHandler.postDelayed(mResetSearch, 10);

        mHandler.removeCallbacks(mResetQuick);
        mHandler.postDelayed(mResetQuick, 10);

        mHandler.removeCallbacks(mResetVolUp);
        mHandler.postDelayed(mResetVolUp, 10);

        mHandler.removeCallbacks(mResetVolDown);
        mHandler.postDelayed(mResetVolDown, 10);
    }

    private Bitmap runCustomIcon(String uri) {
        if (uri != null) {
            try {
                Intent i = Intent.parseUri(uri, 0);
                PackageManager pm = mContext.getPackageManager();
                ActivityInfo ai = i.resolveActivityInfo(pm,PackageManager.GET_ACTIVITIES);
                if (ai != null) {
                    Bitmap iconBmp = ((BitmapDrawable)ai.loadIcon(pm)).getBitmap();
                    Bitmap jogBmp = mHomeIcon;
                    int jogWidth = jogBmp.getWidth();
                    int sqSide = (int) (jogWidth / Math.sqrt(2));
                    return Bitmap.createScaledBitmap(iconBmp, sqSide, sqSide, true);
                }
            } catch (URISyntaxException e) {
                return null;
            }
        }
        return null;
    }

    private Bitmap runIconRot(int Degrs, Bitmap asIcon) {
        int w = asIcon.getWidth();
        int h = asIcon.getHeight();
        Matrix mtx = new Matrix();
        mtx.postRotate(Degrs);
        return Bitmap.createBitmap(asIcon, 0, 0, w, h, mtx, true);
    }

    private Runnable mResetHome = new Runnable() {
        @Override
        public void run() {
            if (mShowHome == 1) {
               mHomeButton.setImageBitmap(mHomeIcon);
            } else if (mShowHome == 2) {
               if (mInputShow) {
                   mHomeButton.setImageBitmap(mVolDownIcon);
               } else {
                   mHomeButton.setImageBitmap(mBackIcon);
               }
            } else if (mShowHome == 3) {
               mHomeButton.setImageBitmap(mSearchIcon);
            } else if (mShowHome == 4) {
               mHomeButton.setImageBitmap(mMenuIcon);
            } else if (mShowHome == 5) {
               mHomeButton.setImageBitmap(mQuickIcon);
            } else if (mShowHome == 6) {
               if (Settings.System.getInt(mContext.getContentResolver(),
                        Settings.System.USE_CUSTOM_HOME_APP_TOGGLE, 0) == 1) {
                    mHomeButton.setImageBitmap(runCustomIcon(Settings.System.getString(mContext.getContentResolver(),
                                    Settings.System.USE_CUSTOM_HOME_APP_ACTIVITY)));
               }
            } else if (mShowHome == 7) {
               mHomeButton.setImageBitmap(mPowerIcon);
            } else if (mShowHome == 8) {
               mHomeButton.setImageBitmap(mRecentIcon);
            } else {
               mHomeButton.setImageBitmap(null);
            }
        }
    };

    public void setNotifNew(boolean notifnew) {
        if (notifnew == mNotifnew) return;

        mNotifnew = notifnew;
        if (!mDoAnimate && notifnew) {
            mHandler.removeCallbacks(mResetLightsOut);
            mHandler.postDelayed(mResetLightsOut, 500);
        } else if (!mDoAnimate && !notifnew) {
            isLightFirst = 0;
            mHandler.removeCallbacks(mResetLightsOut);
            mHandler.postDelayed(mResetLightsOut, 500);
        }
    }

    private Runnable mResetLightsOut = new Runnable() {
        @Override
        public void run() {
            if (isLightFirst == 1) {
                isLightFirst = 2;
                mMenuOutButton.setColorFilter(mOverColor, Mode.SRC_ATOP);
                mHomeOutButton.setColorFilter(null);
                mQuickOutButton.setColorFilter(null);
                mSearchOutButton.setColorFilter(null);
                mBackOutButton.setColorFilter(null);
            } else if (isLightFirst == 2) {
                isLightFirst = 3;
                mMenuOutButton.setColorFilter(null);
                mHomeOutButton.setColorFilter(mOverColor, Mode.SRC_ATOP);
                mQuickOutButton.setColorFilter(null);
                mSearchOutButton.setColorFilter(null);
                mBackOutButton.setColorFilter(null);
            } else if (isLightFirst == 3) {
                isLightFirst = 4;
                mMenuOutButton.setColorFilter(null);
                mHomeOutButton.setColorFilter(null);
                mQuickOutButton.setColorFilter(mOverColor, Mode.SRC_ATOP);
                mSearchOutButton.setColorFilter(null);
                mBackOutButton.setColorFilter(null);
            } else if (isLightFirst == 4) {
                isLightFirst = 5;
                mMenuOutButton.setColorFilter(null);
                mHomeOutButton.setColorFilter(null);
                mQuickOutButton.setColorFilter(null);
                mSearchOutButton.setColorFilter(mOverColor, Mode.SRC_ATOP);
                mBackOutButton.setColorFilter(null);
            } else if (isLightFirst == 5) {
                isLightFirst = 0;
                mMenuOutButton.setColorFilter(null);
                mHomeOutButton.setColorFilter(null);
                mQuickOutButton.setColorFilter(null);
                mSearchOutButton.setColorFilter(null);
                mBackOutButton.setColorFilter(mOverColor, Mode.SRC_ATOP);
            } else if (isLightFirst == 0) {
                isLightFirst = 1;
                mMenuOutButton.setColorFilter(null);
                mHomeOutButton.setColorFilter(null);
                mQuickOutButton.setColorFilter(null);
                mSearchOutButton.setColorFilter(null);
                mBackOutButton.setColorFilter(null);
            }
            if (!mDoAnimate && mNotifnew) {
                mHandler.removeCallbacks(mResetLightsOut);
                mHandler.postDelayed(mResetLightsOut, 500);
            }
        }
    };

    private Runnable mResetBack = new Runnable() {
        @Override
        public void run() {
            if (mShowBack == 1) {
               mBackButton.setImageBitmap(mHomeIcon);
            } else if (mShowBack == 2) {
               if (mInputShow) {
                   mBackButton.setImageBitmap(mVolDownIcon);
               } else {
                   mBackButton.setImageBitmap(mBackIcon);
               }
            } else if (mShowBack == 3) {
               mBackButton.setImageBitmap(mSearchIcon);
            } else if (mShowBack == 4) {
               mBackButton.setImageBitmap(mMenuIcon);
            } else if (mShowBack == 5) {
               mBackButton.setImageBitmap(mQuickIcon);
            } else if (mShowBack == 6) {
               if (Settings.System.getInt(mContext.getContentResolver(),
                        Settings.System.USE_CUSTOM_BACK_APP_TOGGLE, 0) == 1) {
                    mBackButton.setImageBitmap(runCustomIcon(Settings.System.getString(mContext.getContentResolver(),
                                    Settings.System.USE_CUSTOM_BACK_APP_ACTIVITY)));
               }
            } else if (mShowBack == 7) {
               mBackButton.setImageBitmap(mPowerIcon);
            } else if (mShowBack == 8) {
               mBackButton.setImageBitmap(mRecentIcon);
            } else {
               mBackButton.setImageBitmap(null);
            }
        }
    };

    private Runnable mResetSearch = new Runnable() {
        @Override
        public void run() {
            if (mShowSearch == 1) {
               mSearchButton.setImageBitmap(mHomeIcon);
            } else if(mShowSearch == 2) {
               if (mInputShow) {
                   mSearchButton.setImageBitmap(mVolDownIcon);
               } else {
                   mSearchButton.setImageBitmap(mBackIcon);
               }
            } else if (mShowSearch == 3) {
               mSearchButton.setImageBitmap(mSearchIcon);
            } else if (mShowSearch == 4) {
               mSearchButton.setImageBitmap(mMenuIcon);
            } else if (mShowSearch == 5) {
               mSearchButton.setImageBitmap(mQuickIcon);
            } else if (mShowSearch == 6) {
               if (Settings.System.getInt(mContext.getContentResolver(),
                        Settings.System.USE_CUSTOM_NAVISEARCH_APP_TOGGLE, 0) == 1) {
                    mSearchButton.setImageBitmap(runCustomIcon(Settings.System.getString(mContext.getContentResolver(),
                                    Settings.System.USE_CUSTOM_NAVISEARCH_APP_ACTIVITY)));
               }
            } else if (mShowSearch == 7) {
               mSearchButton.setImageBitmap(mPowerIcon);
            } else if (mShowSearch == 8) {
               mSearchButton.setImageBitmap(mRecentIcon);
            } else {
               mSearchButton.setImageBitmap(null);
            }
        }
    };

    private Runnable mResetMenu = new Runnable() {
        @Override
        public void run() {
            if (mShowMenu == 1) {
               mMenuButton.setImageBitmap(mHomeIcon);
            } else if (mShowMenu == 2) {
               if (mInputShow) {
                   mMenuButton.setImageBitmap(mVolDownIcon);
               } else {
                   mMenuButton.setImageBitmap(mBackIcon);
               }
            } else if (mShowMenu == 3) {
               mMenuButton.setImageBitmap(mSearchIcon);
            } else if (mShowMenu == 4) {
               mMenuButton.setImageBitmap(mMenuIcon);
            } else if (mShowMenu == 5) {
               mMenuButton.setImageBitmap(mQuickIcon);
            } else if (mShowMenu == 6) {
               if (Settings.System.getInt(mContext.getContentResolver(),
                        Settings.System.USE_CUSTOM_MENU_APP_TOGGLE, 0) == 1) {
                    mMenuButton.setImageBitmap(runCustomIcon(Settings.System.getString(mContext.getContentResolver(),
                                    Settings.System.USE_CUSTOM_MENU_APP_ACTIVITY)));
               }
            } else if (mShowMenu == 7) {
               mMenuButton.setImageBitmap(mPowerIcon);
            } else if (mShowMenu == 8) {
               mMenuButton.setImageBitmap(mRecentIcon);
            } else {
               mMenuButton.setImageBitmap(null);
            }
        }
    };

    private Runnable mResetQuick = new Runnable() {
        @Override
        public void run() {
            if (mShowQuicker == 0) {
               mQuickButton.setImageBitmap(mHomeIcon);
            } else if(mShowQuicker == 1) {
               if (mInputShow) {
                   mQuickButton.setImageBitmap(mVolDownIcon);
               } else {
                   mQuickButton.setImageBitmap(mBackIcon);
               }
            } else if (mShowQuicker == 2) {
               mQuickButton.setImageBitmap(mSearchIcon);
            } else if (mShowQuicker == 3) {
               mQuickButton.setImageBitmap(mMenuIcon);
            } else if (mShowQuicker == 4) {
               mQuickButton.setImageBitmap(mQuickIcon);
            } else if (mShowQuicker == 5) {
               if (Settings.System.getInt(mContext.getContentResolver(),
                        Settings.System.USE_CUSTOM_QUICK_APP_TOGGLE, 0) == 1) {
                    mQuickButton.setImageBitmap(runCustomIcon(Settings.System.getString(mContext.getContentResolver(),
                                    Settings.System.USE_CUSTOM_QUICK_APP_ACTIVITY)));
               }
            } else if (mShowQuicker == 6) {
               mQuickButton.setImageBitmap(mPowerIcon);
            } else if (mShowQuicker == 7) {
               mQuickButton.setImageBitmap(mRecentIcon);
            } else {
               mQuickButton.setImageBitmap(null);
            }
        }
    };

    private Runnable mResetVolUp = new Runnable() {
        @Override
        public void run() {
            mVolUpButton.setImageBitmap(mVolUpIcon);
        }
    };

    private Runnable mResetVolDown = new Runnable() {
        @Override
        public void run() {
            mVolDownButton.setImageBitmap(mVolDownIcon);
        }
    };

    private void startCollapseActivity() {
      if (mPrevious == 1) {
          mServices.animateCollapse();
      } else if (mPrevious == 0) {
          mServices.animateExpand();
      } else if (mPrevious == 2) {
          mServices.toggleQwikWidgets();
      } else {
         // nothing
      }
    }

    private void startExpandActivity() {
      if (mNext == 0) {
          mServices.animateExpand();
      } else if (mNext == 1) {
          mServices.animateCollapse();
      } else if (mNext == 2) {
          mServices.toggleQwikWidgets();
      } else {
         // nothing
      }
    }

    private View.OnTouchListener mLightsOutListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                setLowProfile(false, true, false);
                isLightFirst = 0;
            }
            return false;
        }
    };

    public void setLowProfile(final boolean lightsOut) {
        setLowProfile(lightsOut, true, false);
    }

    private void setLowProfileAfterHide(final boolean lightsOut) {
        setLowProfile(lightsOut, false, false);
    }

    private Animator mLightViewAnim, mNavViewAnim;

    private Animator setVisibilityWhenDone(
            final Animator a, final View v, final int vis) {
        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                v.setVisibility(vis);
            }
        });
        return a;
    }

    private Animator interpolator(TimeInterpolator ti, Animator a) {
        a.setInterpolator(ti);
        return a;
    }

    private Animator startDelay(int d, Animator a) {
        a.setStartDelay(d);
        return a;
    }

    private Animator start(Animator a) {
        a.start();
        return a;
    }

    private void setLowProfile(final boolean lightsOut, final boolean animate, final boolean force) {
        if (!force && lightsOut == mLowProfile) return;

        mLowProfile = lightsOut;

        if (mNavViewAnim != null) mNavViewAnim.cancel();
        if (mLightViewAnim != null) mLightViewAnim.cancel();

        if (!animate) {
            navButtons.setAlpha(lightsOut ? 0f : 1f);

            lowLights.setAlpha(lightsOut ? 1f : 0f);
            lowLights.setVisibility(lightsOut ? View.VISIBLE : View.GONE);
            if (lightsOut) lowLights.setOnTouchListener(mLightsOutListener);
        } else {
            mNavViewAnim = start(ObjectAnimator.ofFloat(navButtons, "alpha", lightsOut ? 1f : 0f, lightsOut ? 0f : 1f)
                                               .setDuration(lightsOut ? 600 : 200));

            lowLights.setOnTouchListener(mLightsOutListener);
            if (lowLights.getVisibility() == View.GONE) {
                lowLights.setAlpha(0f);
                lowLights.setVisibility(View.VISIBLE);
            }

            mLightViewAnim = start(setVisibilityWhenDone(startDelay(lightsOut ? 500 : 0, 
                             interpolator(new AccelerateInterpolator(2.0f),
                             ObjectAnimator.ofFloat(lowLights, "alpha", lightsOut ? 0f : 1f, lightsOut ? 1f : 0f))
                                           .setDuration(lightsOut ? 1000 : 300)),
                             lowLights, lightsOut ? View.VISIBLE : View.GONE));
        }
        mDoAnimate = lightsOut ? false : true;
        mHandler.removeCallbacks(mResetNormal);
        mHandler.postDelayed(mResetNormal, 1000);
        mHandler.removeCallbacks(mResetLightsOut);
        mHandler.postDelayed(mResetLightsOut, 500);
    }
}
