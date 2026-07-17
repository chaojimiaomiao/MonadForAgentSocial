package com.example.autoclickhelper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ViewFlipper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class GuideActivity extends AppCompatActivity {

    private View accessibilityStatus;
    private View autostartStatus;
    private View batteryStatus;
    private Button btnDone;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        prefs = getSharedPreferences("AutoClickHelper", Context.MODE_PRIVATE);

        if (prefs.getBoolean("guide_completed", false)) {
            goToMain();
            return;
        }

        accessibilityStatus = findViewById(R.id.accessibility_status);
        autostartStatus = findViewById(R.id.autostart_status);
        batteryStatus = findViewById(R.id.battery_status);
        btnDone = findViewById(R.id.btn_done);

        Button btnAccessibility = findViewById(R.id.btn_accessibility);
        Button btnAutostart = findViewById(R.id.btn_autostart);
        Button btnBattery = findViewById(R.id.btn_battery);
        Button btnSkip = findViewById(R.id.btn_skip);

        btnAccessibility.setOnClickListener(v -> openAccessibilitySettings());
        btnAutostart.setOnClickListener(v -> openAutostartSettings());
        btnBattery.setOnClickListener(v -> openBatterySettings());
        btnSkip.setOnClickListener(v -> completeGuide());
        btnDone.setOnClickListener(v -> completeGuide());

        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }

    private void checkPermissions() {
        boolean accessibility = isAccessibilityEnabled();
        boolean autostart = prefs.getBoolean("autostart_enabled", false);
        boolean battery = prefs.getBoolean("battery_enabled", false);

        updateStatus(accessibilityStatus, accessibility);
        updateStatus(autostartStatus, autostart);
        updateStatus(batteryStatus, battery);

        btnDone.setEnabled(accessibility);
    }

    private void updateStatus(View view, boolean enabled) {
        if (enabled) {
            view.setBackgroundResource(R.drawable.dot_success);
        } else {
            view.setBackgroundResource(R.drawable.dot_pending);
        }
    }

    private boolean isAccessibilityEnabled() {
        int accessibilityEnabled = 0;
        final String service = getPackageName() + "/" + AutoClickerAccessibilityService.class.getName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        android.text.TextUtils.SimpleStringSplitter mStringColonSplitter = new android.text.TextUtils.SimpleStringSplitter(
                ':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }

    private void openAutostartSettings() {
        prefs.edit().putBoolean("autostart_enabled", true).apply();

        String manufacturer = Build.MANUFACTURER.toLowerCase();
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (manufacturer.contains("xiaomi")) {
            intent.setComponent(new android.content.ComponentName("com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"));
        } else if (manufacturer.contains("samsung")) {
            intent.setAction("android.intent.action.MAIN");
            intent.setComponent(new android.content.ComponentName("com.samsung.android.sm",
                    "com.samsung.android.sm.app.dashboard.SmartManagerDashBoardActivity"));
        } else if (manufacturer.contains("huawei")) {
            intent.setComponent(new android.content.ComponentName("com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"));
        } else if (manufacturer.contains("oppo")) {
            intent.setComponent(new android.content.ComponentName("com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
        } else if (manufacturer.contains("vivo")) {
            intent.setComponent(new android.content.ComponentName("com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
        }

        try {
            startActivity(intent);
        } catch (Exception e) {
            Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            fallback.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(fallback);
        }
    }

    private void openBatterySettings() {
        prefs.edit().putBoolean("battery_enabled", true).apply();

        String manufacturer = Build.MANUFACTURER.toLowerCase();
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (manufacturer.contains("xiaomi")) {
            intent.setComponent(new android.content.ComponentName("com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"));
        } else if (manufacturer.contains("samsung")) {
            intent.setComponent(new android.content.ComponentName("com.samsung.android.sm",
                    "com.samsung.android.sm.app.dashboard.SmartManagerDashBoardActivity"));
        } else if (manufacturer.contains("huawei")) {
            intent.setComponent(new android.content.ComponentName("com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"));
        } else if (manufacturer.contains("oppo")) {
            intent.setComponent(new android.content.ComponentName("com.coloros.safecenter",
                    "com.coloros.safecenter.permission.floatwindow.FloatWindowListActivity"));
        } else if (manufacturer.contains("vivo")) {
            intent.setComponent(new android.content.ComponentName("com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.PurviewTabActivity"));
        } else if (manufacturer.contains("meizu")) {
            intent.setComponent(new android.content.ComponentName("com.meizu.safe",
                    "com.meizu.safe.permission.SmartBGActivity"));
        } else if (manufacturer.contains("oneplus")) {
            intent.setComponent(new android.content.ComponentName("com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        } else {
            intent.setAction(Settings.ACTION_BATTERY_SAVER_SETTINGS);
        }

        try {
            startActivity(intent);
        } catch (Exception e) {
            Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            fallback.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(fallback);
        }
    }

    private void completeGuide() {
        prefs.edit().putBoolean("guide_completed", true).apply();
        goToMain();
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}