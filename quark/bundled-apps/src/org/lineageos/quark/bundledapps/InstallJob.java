package org.lineageos.quark.bundledapps;

import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

public final class InstallJob extends JobService {
    private static final String TAG = "QuarkBundledApps";
    private static final ConcurrentHashMap<Integer, Result> RESULTS = new ConcurrentHashMap<>();
    private volatile Thread worker;
    private static final class Result {
        final CountDownLatch ready = new CountDownLatch(1);
        volatile int status = PackageInstaller.STATUS_FAILURE;
        volatile String message = "no installation result";
    }

    static void receiveResult(Intent intent) {
        int id = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1);
        Result result = RESULTS.get(id);
        if (result == null) return;
        result.status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE);
        result.message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        result.ready.countDown();
    }

    @Override public boolean onStartJob(JobParameters parameters) {
        worker = new Thread(new Runnable() {
          @Override public void run() {
            try {
                if (checkSelfPermission("android.permission.INSTALL_PACKAGES")
                        != PackageManager.PERMISSION_GRANTED) {
                    throw new SecurityException("INSTALL_PACKAGES permission not granted");
                }
                JSONArray apps = readManifest();
                SharedPreferences prefs = getSharedPreferences("installed", MODE_PRIVATE);
                for (int i = 0; i < apps.length(); i++) {
                    if (Thread.currentThread().isInterrupted()) break;
                    JSONObject app = apps.getJSONObject(i);
                    String pkg = app.getString("package");
                    if (prefs.getBoolean(pkg + ".done", false)) continue;
                    try {
                        PackageInfo installed = getPackageManager().getPackageInfo(pkg, 0);
                        if (installed.getLongVersionCode() >= app.getLong("versionCode")) {
                            prefs.edit().putBoolean(pkg + ".done", true).commit();
                            Log.i(TAG, pkg + ": existing version retained");
                            continue;
                        }
                    } catch (PackageManager.NameNotFoundException absent) {
                        // Install the bundled copy only when absent or older.
                    }
                    int attempts = prefs.getInt(pkg + ".attempts", 0);
                    if (attempts >= 3) {
                        Log.w(TAG, pkg + ": retry limit reached; no further automatic attempts");
                        continue;
                    }
                    if (!prefs.edit().putInt(pkg + ".attempts", attempts + 1).commit())
                        throw new IllegalStateException("Could not save installation attempt");
                    try {
                        install(app);
                        prefs.edit().putBoolean(pkg + ".done", true).commit();
                        Log.i(TAG, pkg + ": installed as a user app");
                    } catch (InterruptedException stopped) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception error) {
                        Log.e(TAG, pkg + ": installation failed; existing app left intact", error);
                    }
                }
            } catch (Exception error) {
                Log.e(TAG, "Bundled app setup failed", error);
            } finally {
                jobFinished(parameters, false);
            }
          }
        }, "QuarkBundledApps");
        worker.start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters parameters) {
        Thread current = worker;
        if (current != null) current.interrupt();
        return false;
    }

    private JSONArray readManifest() throws Exception {
        try (InputStream input = getAssets().open("apps.json");
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] bytes = new byte[4096];
            int count;
            while ((count = input.read(bytes)) != -1) output.write(bytes, 0, count);
            return new JSONArray(output.toString("UTF-8"));
        }
    }

    private InputStream openPayload(JSONObject app) throws Exception {
        JSONArray names = app.getJSONArray("parts");
        ArrayList<InputStream> inputs = new ArrayList<>();
        try {
            for (int i = 0; i < names.length(); i++)
                inputs.add(getAssets().open(names.getString(i)));
            InputStream sequence = new SequenceInputStream(Collections.enumeration(inputs));
            return app.getBoolean("gzip") ? new GZIPInputStream(sequence) : sequence;
        } catch (Exception error) {
            for (InputStream input : inputs) try { input.close(); } catch (Exception ignored) { }
            throw error;
        }
    }

    private void install(JSONObject app) throws Exception {
        String pkg = app.getString("package");
        long expectedSize = app.getLong("size");
        PackageInstaller installer = getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams parameters = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        parameters.setAppPackageName(pkg);
        parameters.setSize(expectedSize);
        parameters.setInstallReason(PackageManager.INSTALL_REASON_DEVICE_SETUP);
        int id = installer.createSession(parameters);
        Result result = new Result();
        RESULTS.put(id, result);
        boolean completed = false;
        try (PackageInstaller.Session session = installer.openSession(id)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long total = 0;
            try (InputStream input = openPayload(app);
                    OutputStream output = session.openWrite("base.apk", 0, expectedSize)) {
                byte[] bytes = new byte[65536];
                int count;
                while ((count = input.read(bytes)) != -1) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                    total += count;
                    if (total > expectedSize) throw new SecurityException("APK larger than expected");
                    digest.update(bytes, 0, count);
                    output.write(bytes, 0, count);
                }
                session.fsync(output);
            }
            StringBuilder hash = new StringBuilder();
            for (byte value : digest.digest()) hash.append(String.format("%02x", value & 255));
            if (total != expectedSize || !hash.toString().equals(app.getString("sha256")))
                throw new SecurityException("Bundled APK checksum mismatch");
            Intent callback = new Intent(this, InstallResultReceiver.class)
                    .setAction(getPackageName() + ".INSTALL_RESULT." + id);
            // targetSdk 30: mutable so PackageInstaller can supply the result extras.
            PendingIntent pending = PendingIntent.getBroadcast(this, id, callback,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            session.commit(pending.getIntentSender());
            if (!result.ready.await(120, TimeUnit.SECONDS))
                throw new IllegalStateException("Timed out waiting for PackageInstaller");
            if (result.status != PackageInstaller.STATUS_SUCCESS)
                throw new IllegalStateException("PackageInstaller status=" + result.status
                        + " " + result.message);
            completed = true;
        } finally {
            RESULTS.remove(id);
            if (!completed) {
                try { installer.abandonSession(id); }
                catch (Exception error) { Log.w(TAG, "Could not abandon session " + id, error); }
            }
        }
    }
}
