package id.co.trada.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Layar utama (peluncur) — DASBOR LURING.
 * Memuat assets/dashboard.html secara lokal (file:///android_asset) sehingga
 * tetap tampil tanpa internet. Data berasal dari assets/snapshot.json yang
 * dibundel, dialirkan ke halaman lewat jembatan JavaScript Android.getSnapshot().
 * Tombol "Buka Aplikasi Operasional" meluncurkan WebActivity (Web App live /exec).
 *
 * Pola ini mengikuti aplikasi JERUK: UI + snapshot data dikemas di dalam APK.
 */
public class DashboardActivity extends AppCompatActivity {

    private WebView web;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        // Jembatan: halaman lokal tepercaya boleh memakai antarmuka ini.
        web.addJavascriptInterface(new Bridge(), "Android");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // Tautan http(s) dari dasbor luring dibuka melalui aplikasi operasional / browser.
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    openOperasional();
                    return true;
                }
                return false;
            }
        });

        web.loadUrl("file:///android_asset/dashboard.html");
    }

    private void openOperasional() {
        startActivity(new Intent(this, WebActivity.class));
    }

    /** Antarmuka JS untuk halaman lokal tepercaya (dashboard.html). */
    public class Bridge {
        @JavascriptInterface
        public String getSnapshot() {
            return readAsset("snapshot.json");
        }

        @JavascriptInterface
        public void openOperasional() {
            runOnUiThread(DashboardActivity.this::openOperasional);
        }

        @JavascriptInterface
        public String getStartUrl() {
            return getString(R.string.start_url);
        }

        @JavascriptInterface
        public void logout() {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    getSharedPreferences("trada_auth", MODE_PRIVATE).edit()
                            .putBoolean("authed", false).apply();
                    startActivity(new Intent(DashboardActivity.this, LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                    finish();
                }
            });
        }
    }

    private String readAsset(String name) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getAssets().open(name);
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        } catch (Exception e) {
            return "";
        }
        return sb.toString();
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
