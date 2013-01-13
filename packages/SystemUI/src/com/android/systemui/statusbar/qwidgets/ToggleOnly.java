package com.android.systemui.statusbar.qwidgets;

import android.graphics.Color;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;

public abstract class ToggleOnly extends QwikWidget {

    public static final int OFF = 0;
    public static final int ON = 1;
    public static final int TWEEN = 2;

    protected TextView mWidgetLabel;
    protected ImageView mWidgetIcon;
    protected View mWidgetIndic;

    protected int mLabelId;
    protected int mIconId;
    protected int mIndicId;
    
    protected int mStatus;

    @Override
    protected void updateWidgetView() {
        mWidgetLabel = (TextView) mWidgetView.findViewById(R.id.widget_label);
        mWidgetLabel.setTextColor(Color.WHITE);
        mWidgetLabel.setText(mLabelId);
        mWidgetIcon = (ImageView) mWidgetView.findViewById(R.id.widget_icon);
        mWidgetIcon.setImageResource(mIconId);
        mWidgetIndic = (View) mWidgetView.findViewById(R.id.widget_indic);
        switch (mStatus) {
            case OFF:
                mWidgetIndic.setBackgroundColor(Color.WHITE);
                break;
            case ON:
                mWidgetIndic.setBackgroundColor(Color.parseColor("#33B5E5"));
                break;
            case TWEEN:
                mWidgetIndic.setBackgroundColor(Color.YELLOW);
                break;
        }
    }

    @Override
    protected void setupWidget(View view) {
        super.setupWidget(view);
        mWidgetView.setOnClickListener(mClickListener);
        mWidgetView.setOnLongClickListener(mLongClickListener);
    }
}
