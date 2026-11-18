package kr.podolang.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * v1.0.6 메신저 대화 번역 — 어느 앱에서든 글자를 길게 눌러 고르면 메뉴에 '포도랑 번역'이 나옵니다.
 * (안드로이드 공식 기능 ACTION_PROCESS_TEXT · 접근성 권한 필요 없음)
 *  · 받은 메시지(고칠 수 없는 글)  → '내 언어'로 번역해 작은 창에 보여 줍니다.
 *  · 내가 쓰는 글(입력칸에서 고른 글) → '상대 언어'로 번역하고, [바꿔 넣기]를 누르면 그 자리 글이 번역문으로 바뀝니다.
 *  · 요금: 텍스트 번역과 같은 한 번 0.1 크레딧 (서버 /api/text). 글은 번역에만 쓰고 저장하지 않습니다.
 *  · 로그인 키와 언어는 포도랑 [번역 → 메신저 번역 → 앱에 연결하기]가 podolang://link 로 넣어 줍니다 (LinkActivity).
 */
public class ProcessTextActivity extends Activity {

    static final String PREF = "podolang_msg";
    private static final String API = "https://podolang.hasin7jk.workers.dev/api/text";
    private static final int C1 = 0xFF0284C7, INK = 0xFF111827, SUB = 0xFF6B7280;
    static final String[][] LANGS = {
            {"KO", "한국어"}, {"TH", "태국어"}, {"EN", "영어"}, {"JA", "일본어"}, {"ZH", "중국어"}, {"VI", "베트남어"},
            {"ES", "스페인어"}, {"ID", "인도네시아어"}, {"DE", "독일어"}, {"FR", "프랑스어"}, {"AR", "아랍어"},
            {"IT", "이탈리아어"}, {"RU", "러시아어"}, {"PT", "포르투갈어"}, {"HI", "힌디어"}, {"NL", "네덜란드어"}};

    private String src = "", out = "", tk = "", target = "KO";
    private boolean readOnly = true, busy = false;
    private TextView resultView, statusView;
    private ProgressBar spin;
    private Button replaceBtn, copyBtn;
    private Spinner langSpin;
    private boolean spinReady = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setFinishOnTouchOutside(true);

        Intent i = getIntent();
        CharSequence cs = i == null ? null : i.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        src = cs == null ? "" : cs.toString().trim();
        readOnly = i == null || i.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true);

        SharedPreferences p = getSharedPreferences(PREF, MODE_PRIVATE);
        tk = p.getString("tk", "");
        if (tk.isEmpty()) tk = getSharedPreferences("podolang_audio", MODE_PRIVATE).getString("tk", "");   // 오디오 번역에서 넣어 둔 키
        // 받은 글은 내 언어로, 내가 쓰는 글은 상대 언어로
        target = readOnly ? p.getString("mine", "KO") : p.getString("peer", "EN");

        buildUi();
        if (src.isEmpty()) { finish(); return; }
        if (tk.isEmpty()) { showNeedLink(); return; }
        translate();
    }

    /* ---------- 화면 (코드로 그립니다) ---------- */
    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(16), dp(18), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(22));
        root.setBackground(bg);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("포도랑 번역", 17, C1, true);
        head.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView arrow = text("→", 15, SUB, true);
        arrow.setPadding(0, 0, dp(6), 0);
        head.addView(arrow);
        langSpin = new Spinner(this);
        String[] names = new String[LANGS.length];
        int sel = 0;
        for (int k = 0; k < LANGS.length; k++) { names[k] = LANGS[k][1]; if (LANGS[k][0].equals(target)) sel = k; }
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        langSpin.setAdapter(ad);
        langSpin.setSelection(sel, false);
        langSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
                if (!spinReady) { spinReady = true; return; }
                String code = LANGS[pos][0];
                if (code.equals(target)) return;
                target = code;
                getSharedPreferences(PREF, MODE_PRIVATE).edit().putString(readOnly ? "mine" : "peer", code).apply();   // 다음에도 이 언어로
                if (!tk.isEmpty()) translate();
            }
            @Override public void onNothingSelected(AdapterView<?> a) { }
        });
        head.addView(langSpin);
        root.addView(head);

        TextView orig = text(src, 13, SUB, false);
        orig.setMaxLines(4);
        orig.setEllipsize(android.text.TextUtils.TruncateAt.END);
        orig.setPadding(0, dp(8), 0, dp(10));
        root.addView(orig);

        ScrollView sv = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bx = new GradientDrawable();
        bx.setColor(0xFFF0F9FF);
        bx.setCornerRadius(dp(16));
        box.setBackground(bx);
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        spin = new ProgressBar(this);
        box.addView(spin, new LinearLayout.LayoutParams(dp(28), dp(28)));
        resultView = text("", 18, INK, true);
        resultView.setTextIsSelectable(true);
        box.addView(resultView);
        sv.addView(box);
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        statusView = text(readOnly ? "받은 글을 내 언어로 번역했어요 · 1번 0.1 크레딧" : "쓴 글을 상대 언어로 번역했어요 · 1번 0.1 크레딧", 12, SUB, false);
        statusView.setPadding(0, dp(8), 0, dp(10));
        root.addView(statusView);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END);
        Button close = button("닫기", false);
        close.setOnClickListener(v -> finish());
        copyBtn = button("복사", false);
        copyBtn.setOnClickListener(v -> copy());
        row.addView(close);
        row.addView(copyBtn);
        if (!readOnly) {   // 입력칸에서 고른 글이면 그 자리를 번역문으로 바꿀 수 있음
            replaceBtn = button("바꿔 넣기", true);
            replaceBtn.setOnClickListener(v -> replace());
            row.addView(replaceBtn);
        }
        root.addView(row);
        setEnabledButtons(false);

        setContentView(root);
        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            w.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.92), LinearLayout.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showNeedLink() {
        spin.setVisibility(View.GONE);
        resultView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        resultView.setText("먼저 포도랑과 연결해 주세요.\n\n포도랑 앱 → [번역] → [메신저 번역] → [앱에 연결하기]를 한 번 누르면, 그다음부터 어느 앱에서든 바로 번역돼요.");
        statusView.setText("");
        if (copyBtn != null) {
            copyBtn.setText("포도랑 열기");
            copyBtn.setEnabled(true);
            copyBtn.setOnClickListener(v -> {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://podolang.kr/")).setPackage(getPackageName())); } catch (Throwable ignored) { }
                finish();
            });
        }
    }

    private void setEnabledButtons(boolean on) {
        if (copyBtn != null) copyBtn.setEnabled(on);
        if (replaceBtn != null) replaceBtn.setEnabled(on);
    }

    /* ---------- 번역 (서버 /api/text) ---------- */
    private void translate() {
        if (busy) return;
        busy = true;
        out = "";
        spin.setVisibility(View.VISIBLE);
        resultView.setText("");
        setEnabledButtons(false);
        final String text = src.length() > 2000 ? src.substring(0, 2000) : src;
        final String to = target, key = tk;
        new Thread(() -> {
            String res = null, err = null;
            HttpURLConnection c = null;
            try {
                JSONObject body = new JSONObject();
                body.put("text", text);
                body.put("sourceLang", "AUTO");
                body.put("targetLang", to);
                body.put("uid", key);
                c = (HttpURLConnection) new URL(API).openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(15000);
                c.setReadTimeout(30000);
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                OutputStream o = c.getOutputStream();
                o.write(body.toString().getBytes("UTF-8"));
                o.close();
                int code = c.getResponseCode();
                InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
                JSONObject d = new JSONObject(read(in));
                if (code >= 400 || d.has("error")) err = d.optString("error", "번역하지 못했어요");
                else res = d.optString("translated", "");
            } catch (Throwable e) {
                err = "인터넷 연결을 확인해 주세요";
            } finally {
                if (c != null) c.disconnect();
            }
            final String r = res, e = err;
            runOnUiThread(() -> done(r, e));
        }).start();
    }

    private void done(String res, String err) {
        busy = false;
        if (isFinishing()) return;
        spin.setVisibility(View.GONE);
        if (err != null) {
            resultView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            resultView.setText(err);
            if (err.contains("로그인")) showNeedLink();
            return;
        }
        resultView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        out = res == null ? "" : res;
        resultView.setText(out);
        setEnabledButtons(!out.isEmpty());
    }

    private void copy() {
        if (out.isEmpty()) return;
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("podolang", out));
            Toast.makeText(this, "복사했어요", Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) { }
    }

    private void replace() {
        if (out.isEmpty()) return;
        setResult(RESULT_OK, new Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, out));
        finish();
    }

    /* ---------- 작은 도우미 ---------- */
    private static String read(InputStream in) throws Exception {
        if (in == null) return "{}";
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        in.close();
        return bo.toString("UTF-8");
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setLineSpacing(0, 1.15f);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    private Button button(String s, boolean primary) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(primary ? Color.WHITE : C1);
        GradientDrawable g = new GradientDrawable();
        g.setColor(primary ? C1 : 0xFFE0F2FE);
        g.setCornerRadius(dp(14));
        b.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(44));
        lp.leftMargin = dp(8);
        b.setLayoutParams(lp);
        b.setPadding(dp(16), 0, dp(16), 0);
        return b;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
