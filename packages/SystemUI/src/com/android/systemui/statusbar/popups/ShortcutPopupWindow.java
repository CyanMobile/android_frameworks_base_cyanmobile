/*
 * Copyright (C) 2012 Crossbones Software
 * Copyright (C) 2009 The Android Open Source Project - Some code in this file referenced from AOSP
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

package com.android.systemui.statusbar.popups;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.content.IntentFilter;
import android.content.ActivityNotFoundException;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.content.res.Resources;
import java.net.URISyntaxException;

import com.android.systemui.statusbar.CmStatusBarView;
import com.android.systemui.R;

public class ShortcutPopupWindow extends QuickSettings {

    public ShortcutPopupWindow(View anchor) {
        super(anchor);
    }

    private final String TAG = "ShortcutPopupWindow";
    private ImageButton mApp1;
    private ImageButton mApp2;
    private ImageButton mApp3;
    private ImageButton mApp4;
    private ImageButton mApp5;
    private ImageButton mApp6;
    private ImageButton mApp7;
    private ImageButton mApp8;
    private ImageButton mApp9;
    private ImageButton mApp10;
    private ImageButton mApp11;
    private ImageButton mApp12;
    private ImageButton mApp13;
    private ImageButton mApp14;
    private ImageButton mApp15;
    private ImageButton mApp16;
    private ViewGroup root;

    @Override
    protected void onCreate() {
        // inflate layout
        LayoutInflater inflater =
                (LayoutInflater) this.anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final Intent intentStatusBar = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

        final ContentResolver cr = this.anchor.getContext().getContentResolver();

        root = (ViewGroup) inflater.inflate(R.layout.shortcutbar, null);
        mApp1 = (ImageButton)root.findViewById(R.id.tv_app1);
        mApp2 = (ImageButton)root.findViewById(R.id.tv_app2);
        mApp3 = (ImageButton)root.findViewById(R.id.tv_app3);
        mApp4 = (ImageButton)root.findViewById(R.id.tv_app4);
        mApp5 = (ImageButton)root.findViewById(R.id.tv_app5);
        mApp6 = (ImageButton)root.findViewById(R.id.tv_app6);
        mApp7 = (ImageButton)root.findViewById(R.id.tv_app7);
        mApp8 = (ImageButton)root.findViewById(R.id.tv_app8);
        mApp9 = (ImageButton)root.findViewById(R.id.tv_app9);
        mApp10 = (ImageButton)root.findViewById(R.id.tv_app10);
        mApp11 = (ImageButton)root.findViewById(R.id.tv_app11);
        mApp12 = (ImageButton)root.findViewById(R.id.tv_app12);
        mApp13 = (ImageButton)root.findViewById(R.id.tv_app13);
        mApp14 = (ImageButton)root.findViewById(R.id.tv_app14);
        mApp15 = (ImageButton)root.findViewById(R.id.tv_app15);
        mApp16 = (ImageButton)root.findViewById(R.id.tv_app16);

        Bitmap mCustomIcon1 = runCustomIcon(Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT1_ACTIVITY));
        if (mCustomIcon1 != null) {
              mApp1.setImageBitmap(mCustomIcon1);
              mApp1.setBackgroundResource(R.drawable.ic_sysbar_nihil);
              mApp1.setOnClickListener(
                   new ImageButton.OnClickListener() {
                      public void onClick(View v) {
                        CmStatusBarView.runCustomApp((Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT1_ACTIVITY)), v.getContext());
                        dismiss();
                        v.getContext().sendBroadcast(intentStatusBar);
                      }
                   }
              );
        }

        Bitmap mCustomIcon2 = runCustomIcon(Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT2_ACTIVITY));
        if (mCustomIcon2 != null) {
              mApp2.setImageBitmap(mCustomIcon2);
              mApp2.setBackgroundResource(R.drawable.ic_sysbar_nihil);
              mApp2.setOnClickListener(
                   new ImageButton.OnClickListener() {
                      public void onClick(View v) {
                        CmStatusBarView.runCustomApp((Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT2_ACTIVITY)), v.getContext());
                        dismiss();
                        v.getContext().sendBroadcast(intentStatusBar);
                      }
                   }
              );
        }

        Bitmap mCustomIcon3 = runCustomIcon(Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT3_ACTIVITY));
        if (mCustomIcon3 != null) {
              mApp3.setImageBitmap(mCustomIcon3);
              mApp3.setBackgroundResource(R.drawable.ic_sysbar_nihil);
              mApp3.setOnClickListener(
                   new ImageButton.OnClickListener() {
                      public void onClick(View v) {
                        CmStatusBarView.runCustomApp((Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT3_ACTIVITY)), v.getContext());
                        dismiss();
                        v.getContext().sendBroadcast(intentStatusBar);
                      }
                   }
              );
        }

        Bitmap mCustomIcon4 = runCustomIcon(Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT4_ACTIVITY));
        if (mCustomIcon4 != null) {
              mApp4.setImageBitmap(mCustomIcon4);
              mApp4.setBackgroundResource(R.drawable.ic_sysbar_nihil);
              mApp4.setOnClickListener(
                   new ImageButton.OnClickListener() {
                      public void onClick(View v) {
                        CmStatusBarView.runCustomApp((Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT4_ACTIVITY)), v.getContext());
                        dismiss();
                        v.getContext().sendBroadcast(intentStatusBar);
                      }
                   }
              );
        }

        Bitmap mCustomIcon5 = runCustomIcon(Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT5_ACTIVITY));
        if (mCustomIcon5 != null) {
              mApp5.setImageBitmap(mCustomIcon5);
              mApp5.setBackgroundResource(R.drawable.ic_sysbar_nihil);
              mApp5.setOnClickListener(
                   new ImageButton.OnClickListener() {
                      public void onClick(View v) {
                        CmStatusBarView.runCustomApp((Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT5_ACTIVITY)), v.getContext());
                        dismiss();
                        v.getContext().sendBroadcast(intentStatusBar);
                      }
                   }
              );
        }

        Bitmap mCustomIcon6 = runCustomIcon(Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT6_ACTIVITY));
        if (mCustomIcon6 != null) {
              mApp6.setImageBitmap(mCustomIcon6);
              mApp6.setBackgroundResource(R.drawable.ic_sysbar_nihil);
              mApp6.setOnClickListener(
                   new ImageButton.OnClickListener() {
                      public void onClick(View v) {
                        CmStatusBarView.runCustomApp((Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT6_ACTIVITY)), v.getContext());
                        dismiss();
                        v.getContext().sendBroadcast(intentStatusBar);
                      }
                   }
              );
        }

        Bitmap mCustomIcon7 = runCustomIcon(Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT7_ACTIVITY));
        if (mCustomIcon7 != null) {
              mApp7.setImageBitmap(mCustomIcon7);
              mApp7.setBackgroundResource(R.drawable.ic_sysbar_nihil);
              mApp7.setOnClickListener(
                   new ImageButton.OnClickListener() {
                      public void onClick(View v) {
                        CmStatusBarView.runCustomApp((Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT7_ACTIVITY)), v.getContext());
                        dismiss();
                        v.getContext().sendBroadcast(intentStatusBar);
                      }
                   }
              );
        }

        Bitmap mCustomIcon8 = runCustomIcon(Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT8_ACTIVITY));
        if (mCustomIcon8 != null) {
              mApp8.setImageBitmap(mCustomIcon8);
              mApp8.setBackgroundResource(R.drawable.ic_sysbar_nihil);
              mApp8.setOnClickListener(
                   new ImageButton.OnClickListener() {
                      public void onClick(View v) {
                        CmStatusBarView.runCustomApp((Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT8_ACTIVITY)), v.getContext());
                        dismiss();
                        v.getContext().sendBroadcast(intentStatusBar);
                      }
                   }
              );
        }

        Bitmap mCustomIcon9 = runCustomIcon(Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT9_ACTIVITY));
        if (mCustomIcon9 != null) {
              mApp9.setImageBitmap(mCustomIcon9);
              mApp9.setBackgroundResource(R.drawable.ic_sysbar_nihil);
              mApp9.setOnClickListener(
                   new ImageButton.OnClickListener() {
                      public void onClick(View v) {
                        CmStatusBarView.runCustomApp((Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT9_ACTIVITY)), v.getContext());
                        dismiss();
                        v.getContext().sendBroadcast(intentStatusBar);
                      }
                   }
              );
        }

        Bitmap mCustomIcon10 = runCustomIcon(Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT10_ACTIVITY));
        if (mCustomIcon10 != null) {
              mApp10.setImageBitmap(mCustomIcon10);
              mApp10.setBackgroundResource(R.drawable.ic_sysbar_nihil);
              mApp10.setOnClickListener(
                   new ImageButton.OnClickListener() {
                      public void onClick(View v) {
                        CmStatusBarView.runCustomApp((Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT10_ACTIVITY)), v.getContext());
                        dismiss();
                        v.getContext().sendBroadcast(intentStatusBar);
                      }
                   }
              );
        }

        Bitmap mCustomIcon11 = runCustomIcon(Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT11_ACTIVITY));
        if (mCustomIcon11 != null) {
              mApp11.setImageBitmap(mCustomIcon11);
              mApp11.setBackgroundResource(R.drawable.ic_sysbar_nihil);
              mApp11.setOnClickListener(
                   new ImageButton.OnClickListener() {
                      public void onClick(View v) {
                        CmStatusBarView.runCustomApp((Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT11_ACTIVITY)), v.getContext());
                        dismiss();
                        v.getContext().sendBroadcast(intentStatusBar);
                      }
                   }
              );
        }

        Bitmap mCustomIcon12 = runCustomIcon(Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT12_ACTIVITY));
        if (mCustomIcon12 != null) {
              mApp12.setImageBitmap(mCustomIcon12);
              mApp12.setBackgroundResource(R.drawable.ic_sysbar_nihil);
              mApp12.setOnClickListener(
                   new ImageButton.OnClickListener() {
                      public void onClick(View v) {
                        CmStatusBarView.runCustomApp((Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT12_ACTIVITY)), v.getContext());
                        dismiss();
                        v.getContext().sendBroadcast(intentStatusBar);
                      }
                   }
              );
        }

        Bitmap mCustomIcon13 = runCustomIcon(Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT13_ACTIVITY));
        if (mCustomIcon13 != null) {
              mApp13.setImageBitmap(mCustomIcon13);
              mApp13.setBackgroundResource(R.drawable.ic_sysbar_nihil);
              mApp13.setOnClickListener(
                   new ImageButton.OnClickListener() {
                      public void onClick(View v) {
                        CmStatusBarView.runCustomApp((Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT13_ACTIVITY)), v.getContext());
                        dismiss();
                        v.getContext().sendBroadcast(intentStatusBar);
                      }
                   }
              );
        }

        Bitmap mCustomIcon14 = runCustomIcon(Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT14_ACTIVITY));
        if (mCustomIcon14 != null) {
              mApp14.setImageBitmap(mCustomIcon14);
              mApp14.setBackgroundResource(R.drawable.ic_sysbar_nihil);
              mApp14.setOnClickListener(
                   new ImageButton.OnClickListener() {
                      public void onClick(View v) {
                        CmStatusBarView.runCustomApp((Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT14_ACTIVITY)), v.getContext());
                        dismiss();
                        v.getContext().sendBroadcast(intentStatusBar);
                      }
                   }
              );
        }

        Bitmap mCustomIcon15 = runCustomIcon(Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT15_ACTIVITY));
        if (mCustomIcon15 != null) {
              mApp15.setImageBitmap(mCustomIcon15);
              mApp15.setBackgroundResource(R.drawable.ic_sysbar_nihil);
              mApp15.setOnClickListener(
                   new ImageButton.OnClickListener() {
                      public void onClick(View v) {
                        CmStatusBarView.runCustomApp((Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT15_ACTIVITY)), v.getContext());
                        dismiss();
                        v.getContext().sendBroadcast(intentStatusBar);
                      }
                   }
              );
        }

        Bitmap mCustomIcon16 = runCustomIcon(Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT16_ACTIVITY));
        if (mCustomIcon16 != null) {
              mApp16.setImageBitmap(mCustomIcon16);
              mApp16.setBackgroundResource(R.drawable.ic_sysbar_nihil);
              mApp16.setOnClickListener(
                   new ImageButton.OnClickListener() {
                      public void onClick(View v) {
                        CmStatusBarView.runCustomApp((Settings.System.getString(cr,
                                    Settings.System.USE_CUSTOM_SHORTCUT16_ACTIVITY)), v.getContext());
                        dismiss();
                        v.getContext().sendBroadcast(intentStatusBar);
                      }
                   }
              );
        }
        // set the inflated view as what we want to display
        this.setContentView(root);
    }

    private Bitmap runCustomIcon(String uri) {
        if (uri != null) {
            try {
                Intent i = Intent.parseUri(uri, 0);
                PackageManager pm = this.anchor.getContext().getPackageManager();
                ActivityInfo ai = i.resolveActivityInfo(pm,PackageManager.GET_ACTIVITIES);
                if (ai != null) {
                        Bitmap iconBmp = ((BitmapDrawable)ai.loadIcon(pm)).getBitmap();
                        Bitmap jogBmp = BitmapFactory.decodeResource(this.anchor.getContext().getResources(), R.drawable.ic_sysbar_apps);
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
}
