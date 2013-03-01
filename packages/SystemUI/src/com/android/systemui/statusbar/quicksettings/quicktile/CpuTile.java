package com.android.systemui.statusbar.quicksettings.quicktile;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.LayoutInflater;
import android.net.Uri;
import android.view.View;
import android.os.SystemProperties;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.CmStatusBarView;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;

public class CpuTile extends QuickSettingsTile {

    private static final String TAG = "CpuTile";
    private static final String FREQ_CUR_PREF = "pref_cpu_freq_cur";
    private static final String SCALE_CUR_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";
    private static final String FREQINFO_CUR_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_cur_freq";
    private static String FREQ_CUR_FILE = SCALE_CUR_FILE;
    private static final String GOVERNOR = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";
    private String curCpu;

    public CpuTile(Context context, LayoutInflater inflater,
            QuickSettingsContainerView container, QuickSettingsController qsc) {
        super(context, inflater, container, qsc);

        mTileLayout = R.layout.quick_settings_tile_cpu;

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                queryForCpuInformation();
                flipTile();
            }
        };
        mOnLongClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                CmStatusBarView.runCMSettings("com.cyanogenmod.cmparts.activities.CPUActivity", mContext);
                startCollapseActivity();
                return true;
            }
        };
    }

    @Override
    void onPostCreate() {
        queryForCpuInformation();
        mHandler.removeCallbacks(mResetFlip);
        mHandler.postDelayed(mResetFlip, 5000); //5 second
        super.onPostCreate();
    }

    @Override
    void updateQuickSettings() {
        ImageView iv = (ImageView) mTile.findViewById(R.id.cpu_image);
        TextView tvone = (TextView) mTile.findViewById(R.id.cpuone_textview);
        TextView tvtwo = (TextView) mTile.findViewById(R.id.cputwo_textview);
        tvone.setText(curCpu);
        tvtwo.setText(mLabel);
        iv.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_settings_performance));
    }

    private Runnable mResetCpu = new Runnable() {
        public void run() {
            queryForCpuInformation();
        }
    };

    private Runnable mResetFlip = new Runnable() {
        public void run() {
            flipTile();
            mHandler.removeCallbacks(mResetFlip);
            mHandler.postDelayed(mResetFlip, 5000); //5 second
        }
    };

    private void queryForCpuInformation() {
        if (!fileExists(FREQ_CUR_FILE)) {
            FREQ_CUR_FILE = FREQINFO_CUR_FILE;
        }
        curCpu = toMHz(readOneLine(FREQ_CUR_FILE));
        mLabel = readOneLine(GOVERNOR);
        updateQuickSettings();
        mHandler.removeCallbacks(mResetCpu);
        mHandler.postDelayed(mResetCpu, 1000); //1 second
    }

    private static String readOneLine(String fname) {
        BufferedReader br;
        String line = null;

        try {
            br = new BufferedReader(new FileReader(fname), 512);
            try {
                line = br.readLine();
            } finally {
                br.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "IO Exception when reading /sys/ file", e);
        }
        return line;
    }

    private static boolean fileExists(String filename) {
        return new File(filename).exists();
    }

    private String toMHz(String mhzString) {
        if (mhzString == null)
            return "-";
        return new StringBuilder().append(Integer.valueOf(mhzString) / 1000).append(" MHz").toString();
    }
}
