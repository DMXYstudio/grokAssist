package io.github.dmxystudio.grokassist;

/*
 * Forked from gptAssist (GPLv3). This file keeps GPL header requirements.
 * Modifications for grok.com wrapper by dmxy.
 */

import static android.webkit.WebView.HitTestResult.IMAGE_TYPE;
import static android.webkit.WebView.HitTestResult.SRC_ANCHOR_TYPE;
import static android.webkit.WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.webkit.URLUtilCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private WebView chatWebView = null;
    private ImageButton restrictedButton = null;
    private WebSettings chatWebSettings = null;
    private CookieManager chatCookieManager = null;
    private final Context context = this;
    private SwipeTouchListener swipeTouchListener;

    private static final String TAG = "grokAssist";
    private String urlToLoad = "https://grok.com/";

    // 建议首次启动为不受限，登录完成后用户可切换到受限模式
    private static boolean restricted = false;

    private static final ArrayList<String> allowedDomains = new ArrayList<>();

    private ValueCallback<Uri[]> mUploadMessage;
    private static final int FILE_CHOOSER_REQUEST_CODE = 1;

    @Override
    protected void onPause() {
        if (chatCookieManager != null) chatCookieManager.flush();
        swipeTouchListener = null;
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (restricted) restrictedButton.setImageDrawable(getDrawable(R.drawable.restricted));
        else restrictedButton.setImageDrawable(getDrawable(R.drawable.unrestricted));

        restrictedButton.setOnClickListener(v -> {
            restricted = !restricted;
            if (restricted) {
                restrictedButton.setImageDrawable(getDrawable(R.drawable.restricted));
                Toast.makeText(context, R.string.urls_restricted, Toast.LENGTH_SHORT).show();
                chatWebSettings.setUserAgentString(modUserAgent());
            } else {
                restrictedButton.setImageDrawable(getDrawable(R.drawable.unrestricted));
                Toast.makeText(context, R.string.all_urls, Toast.LENGTH_SHORT).show();
                chatWebSettings.setUserAgentString(modUserAgent());
            }
            chatWebView.reload();
        });

        swipeTouchListener = new SwipeTouchListener(context) {
            public void onSwipeBottom() {
                if (!chatWebView.canScrollVertically(0)) {
                    restrictedButton.setVisibility(View.VISIBLE);
                }
            }
            public void onSwipeTop() {
                restrictedButton.setVisibility(View.GONE);
            }
        };
        chatWebView.setOnTouchListener(swipeTouchListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 默认不受限，便于登录
        restricted = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setTheme(android.R.style.Theme_DeviceDefault_DayNight);
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatWebView = findViewById(R.id.chatWebView);
        registerForContextMenu(chatWebView);
        restrictedButton = findViewById(R.id.restricted);

        // Cookie
        chatCookieManager = CookieManager.getInstance();
        chatCookieManager.setAcceptCookie(true);
        // 为了兼容第三方登录与 Cloudflare 检测，允许第三方 Cookie
        chatCookieManager.setAcceptThirdPartyCookies(chatWebView, true);

        // 域名白名单
        initURLs();

        chatWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                if (consoleMessage.message().contains("NotAllowedError: Write permission denied.")) {
                    Toast.makeText(context, R.string.error_copy, Toast.LENGTH_LONG).show();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
                    }
                }
                if (mUploadMessage != null) {
                    mUploadMessage.onReceiveValue(null);
                    mUploadMessage = null;
                }
                mUploadMessage = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
                return true;
            }

            @Override
            public void onPermissionRequest(final android.webkit.PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (request.getResources().length > 0 && request.getResources()[0].equals(android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            request.grant(request.getResources());
                        } else {
                            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 123);
                        }
                    } else {
                        request.deny();
                    }
                } else {
                    request.grant(request.getResources());
                }
            }
        });

        chatWebView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(final WebView view, WebResourceRequest request) {
                if (!restricted) return null;
                if ("about:blank".equals(request.getUrl().toString())) return null;

                if (!request.getUrl().toString().startsWith("https://")) {
                    Log.d(TAG, "[shouldInterceptRequest][NON-HTTPS] Blocked access to " + request.getUrl());
                    return new WebResourceResponse("text/javascript", "UTF-8", null);
                }

                boolean allowed = false;
                String host = request.getUrl().getHost();
                if (host != null) {
                    for (String url : allowedDomains) {
                        if (host.endsWith(url)) { allowed = true; break; }
                    }
                }

                if (!allowed) {
                    Log.d(TAG, "[shouldInterceptRequest][NOT ON ALLOWLIST] Blocked access to " + host);

                    // 替换头像占位
                    if (request.getUrl().toString().contains("gravatar.com/avatar/")) {
                        AssetManager assetManager = getAssets();
                        try {
                            InputStream inputStream = assetManager.open("avatar.png");
                            return new WebResourceResponse("image/png", "UTF-8", inputStream);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return new WebResourceResponse("text/javascript", "UTF-8", null);
                }
                return null;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!restricted) return false;
                if ("about:blank".equals(request.getUrl().toString())) return false;

                if (!request.getUrl().toString().startsWith("https://")) {
                    Log.d(TAG, "[shouldOverrideUrlLoading][NON-HTTPS] Blocked access to " + request.getUrl());
                    return true;
                }

                boolean allowed = false;
                String host = request.getUrl().getHost();
                if (host != null) {
                    for (String url : allowedDomains) {
                        if (host.endsWith(url)) { allowed = true; break; }
                    }
                }

                if (!allowed) {
                    Log.d(TAG, "[shouldOverrideUrlLoading][NOT ON ALLOWLIST] Blocked access to " + host);
                    return true;
                }
                return false;
            }
        });

        chatWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            Uri source = Uri.parse(url);
            Log.d(TAG, "DownloadManager: " + url);
            DownloadManager.Request request = new DownloadManager.Request(source);
            request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
            request.addRequestHeader("Accept", "text/html,application/xhtml+xml,*/*");
            request.addRequestHeader("Accept-Language", "en-US,en;q=0.7");
            request.addRequestHeader("Referer", url);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            String filename = URLUtilCompat.getFilenameFromContentDisposition(contentDisposition);
            if (filename == null) filename = URLUtilCompat.guessFileName(url, contentDisposition, mimetype);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            Toast.makeText(this, getString(R.string.download) + "\n" + filename, Toast.LENGTH_SHORT).show();
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) dm.enqueue(request);
        });

        chatWebSettings = chatWebView.getSettings();
        chatWebSettings.setJavaScriptEnabled(true);
        chatWebSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        chatWebSettings.setDomStorageEnabled(true);

        chatWebSettings.setAllowContentAccess(false);
        chatWebSettings.setAllowFileAccess(false);
        chatWebSettings.setBuiltInZoomControls(false);
        chatWebSettings.setDatabaseEnabled(false);
        chatWebSettings.setDisplayZoomControls(false);
        chatWebSettings.setSaveFormData(false);
        chatWebSettings.setGeolocationEnabled(false);

        chatWebView.loadUrl(urlToLoad);
        // 如需星标提示，可恢复下一行并替换为你的仓库 URL
        // if (GithubStar.shouldShowStarDialog(this)) GithubStar.starDialog(this, "https://github.com/dmxy/grokassist");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (chatWebView.canGoBack() && !"about:blank".equals(chatWebView.getUrl())) {
                    chatWebView.goBack();
                } else {
                    finish();
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void resetChat() {
        chatWebView.clearFormData();
        chatWebView.clearHistory();
        chatWebView.clearMatches();
        chatWebView.clearSslPreferences();
        chatCookieManager.removeSessionCookie();
        chatCookieManager.removeAllCookie();
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        WebStorage.getInstance().deleteAllData();
        chatWebView.loadUrl(urlToLoad);
    }

    private static void initURLs() {
        // xAI / Grok
        allowedDomains.add("grok.com");
        allowedDomains.add("x.ai");
        allowedDomains.add("accounts.x.ai");
        allowedDomains.add("api.x.ai");
        allowedDomains.add("console.x.ai");

        // X / Twitter 登录
        allowedDomains.add("x.com");
        allowedDomains.add("twitter.com");
        allowedDomains.add("t.co");
        allowedDomains.add("abs.twimg.com");

        // Google / Apple / Microsoft 登录（如需）
        allowedDomains.add("google.com");
        allowedDomains.add("accounts.google.com");
        allowedDomains.add("gstatic.com");
        allowedDomains.add("apple.com");
        allowedDomains.add("appleid.apple.com");
        allowedDomains.add("microsoftonline.com");

        // Cloudflare（托管与 Turnstile）
        allowedDomains.add("cloudflare.com");
        allowedDomains.add("cloudflareinsights.com");
        allowedDomains.add("challenges.cloudflare.com");

        // 观察到的附加域（可按需移除）
        allowedDomains.add("featureassets.org");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (mUploadMessage == null) return;
            Uri[] result = null;
            if (resultCode == Activity.RESULT_OK && intent != null) {
                String dataString = intent.getDataString();
                if (dataString != null) result = new Uri[]{Uri.parse(dataString)};
            }
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        WebView.HitTestResult result = chatWebView.getHitTestResult();
        String url = "";
        if (result.getExtra() != null) {
            if (result.getType() == IMAGE_TYPE) {
                url = result.getExtra();
                Uri source = Uri.parse(url);
                DownloadManager.Request request = new DownloadManager.Request(source);
                request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                request.addRequestHeader("Accept", "text/html,application/xhtml+xml,*/*");
                request.addRequestHeader("Accept-Language", "en-US,en;q=0.7");
                request.addRequestHeader("Referer", url);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                String filename = URLUtil.guessFileName(url, null, "image/jpeg");
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                Toast.makeText(this, getString(R.string.download) + "\n" + filename, Toast.LENGTH_SHORT).show();
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (dm != null) dm.enqueue(request);
            } else if (result.getType() == SRC_IMAGE_ANCHOR_TYPE || result.getType() == SRC_ANCHOR_TYPE) {
                if (result.getType() == SRC_IMAGE_ANCHOR_TYPE) {
                    HandlerThread handlerThread = new HandlerThread("HandlerThread");
                    handlerThread.start();
                    Handler backgroundHandler = new Handler(handlerThread.getLooper());
                    Message msg = backgroundHandler.obtainMessage();
                    chatWebView.requestFocusNodeHref(msg);
                    url = (String) msg.getData().get("url");
                    Toast.makeText(this, "SRC_IMAGE: " + url, Toast.LENGTH_SHORT).show();
                } else if (result.getType() == SRC_ANCHOR_TYPE) {
                    url = result.getExtra();
                    Toast.makeText(this, "SRC_ANCHOR: " + url, Toast.LENGTH_SHORT).show();
                }
                String host = Uri.parse(url).getHost();
                if (host != null) {
                    boolean allowed = false;
                    for (String domain : allowedDomains) {
                        if (host.endsWith(domain)) { allowed = true; break; }
                    }
                    if (!allowed) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText(getString(R.string.app_name), url);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, getString(R.string.url_copied), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    public String modUserAgent() {
        String newPrefix = "Mozilla/5.0 (X11; Linux " + System.getProperty("os.arch") + ")";
        String newUserAgent = WebSettings.getDefaultUserAgent(context);
        String prefix = newUserAgent.substring(0, newUserAgent.indexOf(")") + 1);
        try {
            newUserAgent = newUserAgent.replace(prefix, newPrefix);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newUserAgent;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 123) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Microphone permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Microphone permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Granted
            } else {
                Toast.makeText(context, "Storage permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}