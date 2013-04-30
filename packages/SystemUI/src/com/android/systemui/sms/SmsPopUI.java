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

package com.android.systemui.sms;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsMessage;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Intents;
import android.provider.Settings;
import android.util.Slog;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManagerImpl;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.cmcustom.SmsHelper;

public class SmsPopUI extends SystemUI {
    static final String TAG = "SmsPopUI";

    static final boolean DEBUG = false;

    Handler mHandler = new Handler();

    private AlertDialog mSmsDialog;
    
    private View mSmsView;
    private ImageView mContactPicture;
    private TextView mContactName;
    private TextView mSmsBody;
    private TextView mTimeStamp;
    private Button mCallsButton;
    private String callNumber;
    private String callerName;
    private String inboxMessage;
    private Bitmap contactImage;
    private String inboxDate;
    private int smsCount;
    private long messageId;

    private static final String SMS_CHANGED = "android.provider.Telephony.SMS_RECEIVED";

    public void start() {
        // Register for Intent broadcasts for...
        IntentFilter filter = new IntentFilter();
        filter.addAction(SMS_CHANGED);
        mContext.registerReceiver(mIntentReceiver, filter, null, mHandler);
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(SMS_CHANGED)) {
                onSmsDialog(intent);
            } else {
                Slog.w(TAG, "unknown intent: " + intent);
            }
        }
    };

    private void onSmsDialog(Intent intent) {
	if (intent != null) {
            String smsBody;
            SmsMessage[] message = Intents.getMessagesFromIntent(intent);
            SmsMessage sms = message[0];
            int pduCount = message.length;
            if (pduCount == 1) {
               smsBody = sms.getDisplayMessageBody();
            } else {
	       StringBuilder body = new StringBuilder(); // sms content
               for (int i = 0; i < pduCount; i++) {
                   sms = message[i];
                   body.append(sms.getDisplayMessageBody());
               }
               smsBody = body.toString();
            }
            if (Settings.System.getInt(mResolver, Settings.System.USE_POPUP_SMS, 1) == 1) {
               setSmsInfo(mContext, sms.getDisplayOriginatingAddress(), smsBody, System.currentTimeMillis());
            }
        }
    }

    private void setSmsInfo(Context context, String smsNumber, String smsBody, long dateTaken) {
        if (mSmsDialog != null) mSmsDialog.dismiss();

        smsCount = SmsHelper.getUnreadSmsCount(context);
        callNumber = smsNumber;
        callerName = SmsHelper.getName(context, callNumber);
        inboxMessage = smsBody;
        inboxDate = SmsHelper.getDate(context, dateTaken);
        messageId = SmsHelper.getSmsId(context);
        contactImage = SmsHelper.getContactPicture(
                context, callNumber);
        showSmsInfo();
    }

    private void resetSmsInfo() {
        smsCount = 0;
        callNumber = null;
        callerName = null;
        inboxMessage = null;
        inboxDate = null;
        messageId = 0;
        contactImage = null;
    }

    private void showSmsInfo() {
        closeLastSmsView();

        View v = (View) mInflater.inflate(R.layout.smscall_widget, null);

        mContactPicture = (ImageView) v.findViewById(R.id.contactpicture);
        mContactName = (TextView) v.findViewById(R.id.contactname);
        mSmsBody = (TextView) v.findViewById(R.id.smsmessage);
        mTimeStamp = (TextView) v.findViewById(R.id.smstime);
        mCallsButton = (Button) v.findViewById(R.id.calls_button);
        mCallsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               Intent dialIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + callNumber));
               dialIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
               mContext.startActivity(dialIntent);
               if (mSmsDialog != null) {
                   mSmsDialog.dismiss();
               }
            }
        });

        if (contactImage != null) {
            mContactPicture.setImageBitmap(contactImage);
        }
        mContactName.setText(callerName);
        mSmsBody.setText(SmsHelper.replaceWithEmotes(inboxMessage, mContext));
        mTimeStamp.setText(inboxDate);

            AlertDialog.Builder b = new AlertDialog.Builder(mContext);
                b.setCancelable(false);
                b.setView(v);
                b.setPositiveButton("QuickReply",
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                               Intent isms = new Intent(Intent.ACTION_MAIN);
                               isms.setClassName("com.android.mms",
                                          "com.android.mms.ui.QuickReplyBox");
                               isms.putExtra("avatars", contactImage);
                               isms.putExtra("numbers", callNumber);
                               isms.putExtra("names", callerName);
                               isms.putExtra("inmessage", inboxMessage);
                               isms.putExtra("indate", inboxDate);
                               isms.putExtra("id", messageId);
                               isms.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mContext.startActivity(isms);
                            if (mSmsDialog != null) {
                                mSmsDialog.dismiss();
                            }
                        }
                    });
                b.setNegativeButton("Inbox",
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                               Intent ioinbox = new Intent(Intent.ACTION_MAIN);  
                               ioinbox.addCategory(Intent.CATEGORY_DEFAULT);  
                               ioinbox.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  
                               ioinbox.setType("vnd.android-dir/mms-sms");  
                            mContext.startActivity(ioinbox);
                            if (mSmsDialog != null) {
                                mSmsDialog.dismiss();
                            }
                        }
                    });
                b.setNeutralButton("Cancel",
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                             Intent ismsread = new Intent(Intent.ACTION_MAIN);
                             ismsread.setClassName("com.android.mms",
                                     "com.android.mms.ui.QuickReader");
                             ismsread.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                             mContext.startActivity(ismsread);
                             if (mSmsDialog != null) {
                                 mSmsDialog.dismiss();
                             }
                        }
                    });
            AlertDialog d = b.create();
            d.setOnDismissListener(mSmsListener);
            d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            d.show();
            mSmsDialog = d;
    }

    private DialogInterface.OnDismissListener mSmsListener
            = new DialogInterface.OnDismissListener() {
        public void onDismiss(DialogInterface dialog) {
            mSmsDialog = null;
            resetSmsInfo();
        }
    };

    private void closeLastSmsView() {
        if (mSmsView != null) {
            WindowManagerImpl.getDefault().removeView(mSmsView);
            mSmsView = null;
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
    }
}

