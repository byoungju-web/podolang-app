package kr.podolang.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;

/**
 * v1.0.4 오류 기록 화면 — 앱이 꺼진 이유(오류 내용)를 보여 주고 '복사하기'로 복사할 수 있게 합니다.
 * 별도 프로세스(:crash)에서 열려서, 앱이 꺼져도 이 화면은 뜹니다.
 */
public class CrashActivity extends Activity {

    static final String EXTRA_REPORT = "report";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String report = getIntent() == null ? null : getIntent().getStringExtra(EXTRA_REPORT);
        if (report == null) report = readFile();
        if (report == null) report = "저장된 오류 기록이 없습니다.";
        final String text = report;

        int p = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, dp(28), p, p);

        TextView title = new TextView(this);
        title.setText("포도랑 앱이 꺼졌어요");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTextColor(0xFF111827);
        root.addView(title);

        TextView guide = new TextView(this);
        guide.setText("아래 '복사하기'를 눌러 오류 내용을 복사한 뒤, 채팅에 붙여 넣어 보내 주세요.");
        guide.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        guide.setTextColor(0xFF374151);
        guide.setPadding(0, dp(8), 0, dp(12));
        root.addView(guide);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button copy = new Button(this);
        copy.setText("복사하기");
        copy.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("podolang crash", text));
                    Toast.makeText(CrashActivity.this, "복사했어요", Toast.LENGTH_SHORT).show();
                } catch (Throwable ignored) { }
            }
        });
        Button close = new Button(this);
        close.setText("닫기");
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        row.addView(copy);
        row.addView(close);
        root.addView(row);

        ScrollView sv = new ScrollView(this);
        TextView body = new TextView(this);
        body.setText(text);
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        body.setTextColor(0xFF111827);
        body.setTextIsSelectable(true);
        body.setPadding(0, dp(12), 0, 0);
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private String readFile() {
        try {
            File f = new File(getFilesDir(), App.CRASH_FILE);
            if (!f.exists()) return null;
            byte[] buf = new byte[(int) f.length()];
            FileInputStream in = new FileInputStream(f);
            int n = in.read(buf);
            in.close();
            return n > 0 ? new String(buf, 0, n, "UTF-8") : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
