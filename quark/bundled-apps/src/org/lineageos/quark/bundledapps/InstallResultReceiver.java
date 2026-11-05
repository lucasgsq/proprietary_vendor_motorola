package org.lineageos.quark.bundledapps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class InstallResultReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        InstallJob.receiveResult(intent);
    }
}
