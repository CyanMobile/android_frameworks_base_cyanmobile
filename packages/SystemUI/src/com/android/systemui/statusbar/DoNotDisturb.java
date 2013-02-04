/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.systemui.statusbar;

import android.app.StatusBarManager;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.content.ContentResolver;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;

public class DoNotDisturb {
    private Context mContext;
    private StatusBarManager mStatusBar;
    private boolean mDoNotDisturb;

    private Handler mHandler;

    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_NOTIF), false, this);
        }

        @Override public void onChange(boolean selfChange) {
            updateDisableRecord();
        }
    }

    public DoNotDisturb(Context context) {
        mContext = context;
        ContentResolver resolver = mContext.getContentResolver();
        mStatusBar = (StatusBarManager)context.getSystemService(Context.STATUS_BAR_SERVICE);

        mHandler = new Handler();
        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();

        mDoNotDisturb = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NOTIF, 1) != 1);
        updateDisableRecord();
    }

    private void updateDisableRecord() {
        ContentResolver resolver = mContext.getContentResolver();
        mDoNotDisturb = (Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NOTIF, 1) != 1);
        final int disabled = StatusBarManager.DISABLE_NOTIFICATION_ICONS
                | StatusBarManager.DISABLE_NOTIFICATION_TICKER;
        mStatusBar.disable(mDoNotDisturb ? disabled : 0);
    }
}

