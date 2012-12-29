/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.systemui.statusbar.policy;

import com.android.systemui.statusbar.policy.TelephonyIcons;
import com.android.systemui.R;

class WimaxIcons {
    static final int sWimaxDisconnectedImg =
            R.drawable.stat_sys_data_wimax_signal_disconnected;

    static final int[][] sWimaxSignalImages = {
            { R.drawable.stat_sys_data_wimax_signal_0,
              R.drawable.stat_sys_data_wimax_signal_1,
              R.drawable.stat_sys_data_wimax_signal_2,
              R.drawable.stat_sys_data_wimax_signal_3 },
            { R.drawable.stat_sys_data_wimax_signal_0_fully,
              R.drawable.stat_sys_data_wimax_signal_1_fully,
              R.drawable.stat_sys_data_wimax_signal_2_fully,
              R.drawable.stat_sys_data_wimax_signal_3_fully }
        };

    static final int sWimaxIdleImg = R.drawable.stat_sys_data_wimax_signal_idle;
}
