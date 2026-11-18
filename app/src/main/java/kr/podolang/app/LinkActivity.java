package kr.podolang.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import java.util.Locale;

/**
 * v1.0.6 메신저 번역 연결 — podolang.kr [번역 → 메신저 번역 → 앱에 연결하기] 가
 * podolang://link?tk=..&mine=..&peer=.. 로 엽니다. 로그인 키와 내 언어 · 상대 언어를 폰(이 앱 안)에만 저장하고 바로 닫힙니다.
 */
public class LinkActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Uri u = getIntent() == null ? null : getIntent().getData();
        boolean ok = false;
        if (u != null && "podolang".equals(u.getScheme()) && "link".equals(u.getHost())) {
            String tk = u.getQueryParameter("tk");
            String mine = lang(u.getQueryParameter("mine")), peer = lang(u.getQueryParameter("peer"));
            if (tk != null && tk.matches("pl\\.[A-Za-z0-9_-]{30,80}")) {
                SharedPreferences.Editor e = getSharedPreferences(ProcessTextActivity.PREF, MODE_PRIVATE).edit().putString("tk", tk);
                if (!mine.isEmpty()) e.putString("mine", mine);
                if (!peer.isEmpty()) e.putString("peer", peer);
                e.apply();
                ok = true;
            }
        }
        Toast.makeText(this, ok ? "메신저 번역을 연결했어요. 다른 앱에서 글자를 길게 눌러 '포도랑 번역'을 고르세요."
                                : "연결하지 못했어요. 포도랑에 로그인한 뒤 다시 눌러 주세요.", Toast.LENGTH_LONG).show();
        finish();
    }

    private static String lang(String v) {
        if (v == null) return "";
        String s = v.replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
        for (String[] l : ProcessTextActivity.LANGS) if (l[0].equals(s)) return s;
        return "";
    }
}
