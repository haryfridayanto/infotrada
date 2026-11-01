package id.co.trada.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Gerbang login aplikasi (peluncur).
 *
 * CATATAN KEAMANAN: ini pengaman ringan untuk membatasi akses pengguna awam ke
 * dasbor luring. Kredensial yang ditanam di APK pada dasarnya dapat diekstrak,
 * jadi JANGAN menganggap ini pengamanan kuat. Password TIDAK disimpan sebagai
 * teks; hanya hash SHA-256-nya yang dibandingkan. Transaksi sesungguhnya tetap
 * dilindungi login server pada aplikasi operasional (WebActivity → /exec).
 */
public class LoginActivity extends AppCompatActivity {

    // Username bukan rahasia. Password dibandingkan sebagai hash SHA-256.
    private static final String USERNAME = "infosiaga112";
    private static final String PASSWORD_SHA256 =
            "3be360fd5fc3aa5f6ced00b9bc09b69f5d6afeb4203ccfda581dc01cafaf224b";

    private static final String PREF = "trada_auth";
    private static final String KEY_AUTHED = "authed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Jika sebelumnya memilih "Ingat saya", langsung ke dasbor.
        SharedPreferences sp = getSharedPreferences(PREF, MODE_PRIVATE);
        if (sp.getBoolean(KEY_AUTHED, false)) {
            goDashboard();
            return;
        }

        setContentView(R.layout.activity_login);
        final EditText user = findViewById(R.id.username);
        final EditText pass = findViewById(R.id.password);
        final CheckBox remember = findViewById(R.id.remember);
        final TextView error = findViewById(R.id.error);
        Button btn = findViewById(R.id.loginBtn);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String u = user.getText().toString().trim();
                String p = pass.getText().toString();
                boolean ok = USERNAME.equalsIgnoreCase(u)
                        && constantEquals(sha256(p), PASSWORD_SHA256);
                if (!ok) {
                    error.setText("Username atau password salah.");
                    error.setVisibility(View.VISIBLE);
                    pass.setText("");
                    return;
                }
                if (remember.isChecked()) {
                    getSharedPreferences(PREF, MODE_PRIVATE).edit()
                            .putBoolean(KEY_AUTHED, true).apply();
                }
                goDashboard();
            }
        });
    }

    private void goDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(Character.forDigit((b >> 4) & 0xF, 16))
                               .append(Character.forDigit(b & 0xF, 16));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // Perbandingan waktu-konstan sederhana.
    private static boolean constantEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i);
        return diff == 0;
    }
}
