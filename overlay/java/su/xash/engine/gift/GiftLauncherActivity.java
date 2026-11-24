package su.xash.engine.gift;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

/**
 * Splash + gift messages, then starts the Xash3D engine activity.
 * Works on all Android versions, including the newest ones:
 *  - Android 11+  : asks for "All files access" so /sdcard/xash/valve can be read
 *  - Android 6-10 : asks for the normal storage permission
 */
public class GiftLauncherActivity extends Activity {

    private static final String ENGINE_ACTIVITY = "su.xash.engine.XashActivity";
    private static final int REQ_STORAGE = 1001;

    private final File gameRoot = new File(Environment.getExternalStorageDirectory(), "xash");
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean welcomeShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#14101F"));

        TextView title = new TextView(this);
        title.setText("Half-Life");
        title.setTextSize(36f);
        title.setTextColor(Color.parseColor("#FF9A1F"));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView credit = new TextView(this);
        credit.setText("تصميم احمد علاء لبنته الصغننه 💛");
        credit.setTextSize(18f);
        credit.setTextColor(Color.WHITE);
        credit.setGravity(Gravity.CENTER);
        credit.setPadding(32, 32, 32, 0);
        root.addView(credit);

        setContentView(root);

        handler.postDelayed(new Runnable() {
            @Override public void run() { showWelcome(); }
        }, 2500);
    }

    private void showWelcome() {
        if (isFinishing()) return;
        welcomeShown = true;
        SharedPreferences prefs = getSharedPreferences("gift", Context.MODE_PRIVATE);
        boolean first = prefs.getBoolean("first_run", true);
        String msg = first
                ? "اتمني ان المفجا تعجبك وتكون سبب سعادتك يا بنوتي 😝"
                : "حان وقت المغامره يا كتكوته 😂";
        if (first) prefs.edit().putBoolean("first_run", false).apply();

        new AlertDialog.Builder(this)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("يلا", (d, w) -> ensureStorageThenStart())
                .show();
    }

    // ---- storage -------------------------------------------------------

    private boolean hasStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void ensureStorageThenStart() {
        if (hasStorageAccess()) {
            checkGameAndStart();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("صلاحية الملفات")
                .setMessage("محتاجين صلاحية الوصول للملفات عشان نقرا فولدر valve بتاع اللعبة.")
                .setCancelable(false)
                .setPositiveButton("تمام", (d, w) -> requestStorage())
                .show();
    }

    private void requestStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(i, REQ_STORAGE);
            } catch (Exception e) {
                startActivityForResult(
                        new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), REQ_STORAGE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_STORAGE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_STORAGE) afterPermission();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == REQ_STORAGE) afterPermission();
    }

    private void afterPermission() {
        if (hasStorageAccess()) {
            checkGameAndStart();
        } else {
            new AlertDialog.Builder(this)
                    .setMessage("من غير صلاحية الملفات اللعبة مش هتشتغل.")
                    .setPositiveButton("حاول تاني", (d, w) -> ensureStorageThenStart())
                    .setNegativeButton("خروج", (d, w) -> finish())
                    .show();
        }
    }

    // ---- game ----------------------------------------------------------

    private void checkGameAndStart() {
        File valve = new File(gameRoot, "valve");
        if (!valve.isDirectory()) {
            gameRoot.mkdirs();
            new AlertDialog.Builder(this)
                    .setTitle("ملفات اللعبة مش موجودة")
                    .setMessage("حطي فولدر valve جوه:\n" + gameRoot.getAbsolutePath()
                            + "\nوبعدين افتحي التطبيق تاني.")
                    .setCancelable(false)
                    .setPositiveButton("تمام", (d, w) -> finish())
                    .show();
            return;
        }
        launchEngine();
    }

    private void launchEngine() {
        Intent i = new Intent();
        i.setClassName(getPackageName(), ENGINE_ACTIVITY);
        i.putExtra("gamedir", "valve");
        i.putExtra("argv", "-game valve");
        i.putExtra("usecwd", false);
        startActivity(i);
        finish();
    }
}
