package kr.podolang.app;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * v1.0.4 오류 기록 — 앱이 갑자기 꺼질 때 오류 내용을 파일(files/crash.txt)에 저장하고
 * 별도 화면(CrashActivity)으로 보여 줍니다. 앱의 다른 기능·결제에는 손대지 않습니다.
 */
public class App extends Application {

    static final String CRASH_FILE = "crash.txt";

    @Override
    public void onCreate() {
        super.onCreate();
        if (isCrashProcess()) return;   // 오류 화면 자체에서는 다시 걸지 않음 (무한 반복 방지)

        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                try {
                    String report = buildReport(App.this, t, e);
                    save(App.this, report);
                    Intent i = new Intent(App.this, CrashActivity.class);
                    i.putExtra(CrashActivity.EXTRA_REPORT, report);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    Process.killProcess(Process.myPid());
                    System.exit(10);
                } catch (Throwable ignored) {
                    if (prev != null) prev.uncaughtException(t, e);
                }
            }
        });
    }

    private boolean isCrashProcess() {
        try {
            if (Build.VERSION.SDK_INT >= 28) return Application.getProcessName().endsWith(":crash");
            byte[] buf = new byte[256];
            java.io.FileInputStream in = new java.io.FileInputStream("/proc/self/cmdline");
            int n = in.read(buf);
            in.close();
            return n > 0 && new String(buf, 0, n).trim().contains(":crash");
        } catch (Throwable ignored) {
            return false;
        }
    }

    static String buildReport(Context c, Thread t, Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String ver = "?";
        try {
            ver = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Throwable ignored) { }
        return "시간: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(new Date())
                + "\n앱: " + ver
                + "\n폰: " + Build.MANUFACTURER + " " + Build.MODEL
                + "\n안드로이드: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")"
                + "\n스레드: " + (t == null ? "?" : t.getName())
                + "\n\n" + sw;
    }

    static void save(Context c, String report) {
        try {
            FileOutputStream out = new FileOutputStream(new File(c.getFilesDir(), CRASH_FILE));
            out.write(report.getBytes("UTF-8"));
            out.close();
        } catch (Throwable ignored) { }
    }
}
