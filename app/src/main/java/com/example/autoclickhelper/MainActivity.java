package com.example.autoclickhelper;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private TextView serviceStatus;
    private View serviceStatusDot;
    private Button btnToggleService;
    private Button btnExecuteTask;
    private TextView taskTime;
    private SharedPreferences prefs;
    private boolean isServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("AutoClickHelper", Context.MODE_PRIVATE);

        serviceStatus = findViewById(R.id.service_status);
        serviceStatusDot = findViewById(R.id.service_status_dot);
        btnToggleService = findViewById(R.id.btn_toggle_service);
        btnExecuteTask = findViewById(R.id.btn_execute_task);
        taskTime = findViewById(R.id.task_time);

        Button btnSetTime = findViewById(R.id.btn_set_time);

        String time = prefs.getString("task_time", "08:00");
        taskTime.setText(time);

        btnToggleService.setOnClickListener(v -> toggleService());
        btnExecuteTask.setOnClickListener(v -> executeTask());
        btnSetTime.setOnClickListener(v -> showTimePicker());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
    }

    private void updateServiceStatus() {
        isServiceRunning = AutoClickerService.isRunning();
        if (isServiceRunning) {
            serviceStatus.setText(R.string.status_running);
            serviceStatusDot.setBackgroundResource(R.drawable.dot_success);
            btnToggleService.setText(R.string.btn_stop_service);
        } else {
            serviceStatus.setText(R.string.status_stopped);
            serviceStatusDot.setBackgroundResource(R.drawable.dot_failed);
            btnToggleService.setText(R.string.btn_start_service);
        }
    }

    private void toggleService() {
        if (isServiceRunning) {
            stopService(new Intent(this, AutoClickerService.class));
            cancelAlarm();
        } else {
            startForegroundService(new Intent(this, AutoClickerService.class));
            scheduleAlarm();
        }
        updateServiceStatus();
    }

    private void executeTask() {
        if (!AutoClickerAccessibilityService.isRunning()) {
            Toast.makeText(this, "请先开启无障碍权限", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, AutoClickerService.class);
        intent.putExtra("action", "execute_task");
        startService(intent);

        Toast.makeText(this, "开始执行任务", Toast.LENGTH_SHORT).show();
    }

    private void showTimePicker() {
        String time = prefs.getString("task_time", "08:00");
        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        TimePickerDialog dialog = new TimePickerDialog(this, (view, h, m) -> {
            String newTime = String.format("%02d:%02d", h, m);
            taskTime.setText(newTime);
            prefs.edit().putString("task_time", newTime).apply();
            if (isServiceRunning) {
                scheduleAlarm();
            }
        }, hour, minute, true);

        dialog.setTitle(R.string.time_picker_title);
        dialog.show();
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleAlarm() {
        String time = prefs.getString("task_time", "08:00");
        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AutoClickerService.class);
        intent.putExtra("action", "execute_task");

        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    private void cancelAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AutoClickerService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }
}