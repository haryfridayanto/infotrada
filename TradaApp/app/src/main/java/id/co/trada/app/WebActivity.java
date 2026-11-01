package id.co.trada.app;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * TRADA 2027 — pembungkus WebView untuk Web App Apps Script.
 * Memuat deployment /exec (lihat strings.xml: start_url).
 * Login password lokal aplikasi berjalan di dalam WebView (tanpa OAuth Google),
 * sehingga tidak ada masalah redirect akun Google di WebView.
 */
public class WebActivity extends AppCompatActivity {

    private WebView web;
    private ProgressBar progress;
    private SwipeRefreshLayout swipe;
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_CHOOSER_CODE = 1001;
    private long lastBackMs = 0L;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        web = findViewById(R.id.webview);
        progress = findViewById(R.id.progress);
        swipe = findViewById(R.id.swipe);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setAllowFileAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String url = uri.toString();
                String host = uri.getHost() == null ? "" : uri.getHost();
                // Tautan Google (Apps Script, Drive, usercontent) tetap di dalam WebView.
                if (host.endsWith("google.com") || host.endsWith("googleusercontent.com")
                        || host.endsWith("gstatic.com") || host.endsWith("googleapis.com")) {
                    return false;
                }
                // mailto, tel, intent, dan domain lain dibuka di aplikasi eksternal.
                if (url.startsWith("http://") || url.startsWith("https://")
                        || url.startsWith("mailto:") || url.startsWith("tel:")
                        || url.startsWith("whatsapp:") || url.startsWith("intent:")) {
                    openExternal(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                swipe.setRefreshing(false);
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progress.setVisibility(ProgressBar.VISIBLE);
                    progress.setProgress(newProgress);
                } else {
                    progress.setVisibility(ProgressBar.GONE);
                }
            }

            // Dukungan input <input type="file"> (unggah PDF kontrak / foto bukti).
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = filePathCallback;
                try {
                    Intent intent = params.createIntent();
                    startActivityForResult(intent, FILE_CHOOSER_CODE);
                } catch (Exception e) {
                    fileCallback = null;
                    Toast.makeText(WebActivity.this, "Tidak dapat membuka pemilih berkas.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });

        // Unduhan (PDF, ekspor) diteruskan ke pengelola unduhan sistem.
        web.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimetype, long contentLength) {
                try {
                    DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                    req.setMimeType(mimetype);
                    req.allowScanningByMediaScanner();
                    req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "trada_download");
                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.enqueue(req);
                        Toast.makeText(WebActivity.this, "Mengunduh berkas…", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    openExternal(url);
                }
            }
        });

        swipe.setOnRefreshListener(() -> web.reload());

        // Tombol Back: navigasi riwayat WebView; dua kali tekan untuk keluar.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (web.canGoBack()) {
                    web.goBack();
                } else {
                    long now = System.currentTimeMillis();
                    if (now - lastBackMs < 2000) {
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    } else {
                        lastBackMs = now;
                        Toast.makeText(WebActivity.this, R.string.exit_hint, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        if (savedInstanceState == null) {
            loadStart();
        } else {
            web.restoreState(savedInstanceState);
        }
    }

    private void loadStart() {
        if (isOnline()) {
            web.loadUrl(getString(R.string.start_url));
        } else {
            Toast.makeText(this, R.string.offline_message, Toast.LENGTH_LONG).show();
            swipe.setRefreshing(false);
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return true;
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Tidak ada aplikasi untuk membuka tautan ini.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_CODE) {
            if (fileCallback == null) return;
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int n = data.getClipData().getItemCount();
                    results = new Uri[n];
                    for (int i = 0; i < n; i++) results[i] = data.getClipData().getItemAt(i).getUri();
                } else if (data.getData() != null) {
                    results = new Uri[]{data.getData()};
                }
            }
            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }
}
