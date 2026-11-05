package org.lineageos.quark.bundledapps;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.UserManager;

public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
        UserManager users = context.getSystemService(UserManager.class);
        if (users == null || !users.isSystemUser() || !users.isUserUnlocked()) return;
        context.getSystemService(JobScheduler.class).schedule(
                new JobInfo.Builder(170840, new ComponentName(context, InstallJob.class))
                        .setMinimumLatency(15000).build());
    }
}
