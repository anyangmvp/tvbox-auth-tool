package anyang.tvbox.tools;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private CoordinatorLayout coordinatorLayout;
    private CardView webViewContainer;
    private CardView servicePanel;
    private ListView serviceList;
    private FrameLayout btnServiceCenter;
    private LinearLayout btnBack;
    private LinearLayout btnForward;
    private LinearLayout btnRefresh;
    private LinearLayout btnReset;
    private MaterialButton btnAuth;
    private MaterialButton btnMenu;
    private WebView webView;
    private ProgressBar progressBar;
    private TextView tvCookieExpires;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化UI组件
        coordinatorLayout = findViewById(R.id.coordinator_layout);
        webViewContainer = findViewById(R.id.web_view_container);
        servicePanel = findViewById(R.id.service_panel);
        serviceList = findViewById(R.id.service_list);
        btnServiceCenter = findViewById(R.id.btn_service_center);
        btnBack = findViewById(R.id.btn_back);
        btnForward = findViewById(R.id.btn_forward);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnReset = findViewById(R.id.btn_reset);
        btnAuth = findViewById(R.id.btn_auth);
        btnMenu = findViewById(R.id.btn_menu);
        webView = findViewById(R.id.web_view);
        progressBar = findViewById(R.id.progress_bar);
        tvCookieExpires = findViewById(R.id.tv_cookie_expires);

        // 配置WebView设置
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // 设置自定义User-Agent
        webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36");

        webSettings.setDomStorageEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                updateCookieExpiresDisplay(url);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        // 默认加载夸克云盘页面
        webView.loadUrl("https://pan.quark.cn");
        webView.setInitialScale(100);

        // 加载服务列表并显示在服务面板中
        List<Map<String, Map<String, String>>> serviceDataList = loadServicesFromJson();
        List<String> services = new ArrayList<>();
        for (Map<String, Map<String, String>> entry : serviceDataList) {
            services.addAll(entry.keySet());
        }

        ServiceListAdapter adapter = new ServiceListAdapter(this, services);
        serviceList.setAdapter(adapter);

        // 中央大按钮 - 切换服务面板
        btnServiceCenter.setOnClickListener(v -> toggleServicePanel());

        // 菜单按钮 - 同样切换服务面板
        btnMenu.setOnClickListener(v -> toggleServicePanel());

        serviceList.setOnItemClickListener((parent, view, position, id) -> {
            String selectedService = services.get(position);
            String url = "";

            // 根据选中的服务查找对应的URL
            for (Map<String, Map<String, String>> entry : serviceDataList) {
                if (entry.containsKey(selectedService)) {
                    url = entry.get(selectedService).get("url");
                    break;
                }
            }

            webView.loadUrl(url);
            servicePanel.setVisibility(View.GONE);
            webViewContainer.setVisibility(View.VISIBLE);
        });

        // 获取认证信息按钮点击事件
        btnAuth.setOnClickListener(v -> handleGetAuthInfo());

        // 刷新按钮点击事件
        btnRefresh.setOnClickListener(v -> webView.reload());

        // 前进按钮点击事件
        btnForward.setOnClickListener(v -> {
            if (webView.canGoForward()) {
                webView.goForward();
            } else {
                Toast.makeText(this, getString(R.string.toast_cannot_forward), Toast.LENGTH_SHORT).show();
            }
        });

        // 后退按钮点击事件
        btnBack.setOnClickListener(v -> {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                Toast.makeText(this, getString(R.string.toast_cannot_back), Toast.LENGTH_SHORT).show();
            }
        });

        // 重置按钮点击事件
        btnReset.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.toast_confirm_reset))
                    .setMessage(getString(R.string.toast_reset_message))
                    .setPositiveButton("确定", (dialog, which) -> {
                        clearCookiesAndLocalStorage();
                        Toast.makeText(this, getString(R.string.toast_reset_success), Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    /**
     * 切换服务面板显示/隐藏
     */
    private void toggleServicePanel() {
        if (servicePanel.getVisibility() == View.VISIBLE) {
            servicePanel.setVisibility(View.GONE);
            webViewContainer.setVisibility(View.VISIBLE);
        } else {
            servicePanel.setVisibility(View.VISIBLE);
            webViewContainer.setVisibility(View.GONE);
        }
    }

    /**
     * 加载JSON配置文件并解析为服务列表。
     * @return 服务列表，包含服务名称和服务配置信息。
     */
    private List<Map<String, Map<String, String>>> loadServicesFromJson() {
        String jsonConfig = loadJsonFromAssets("services.json");
        return parseJsonConfig(jsonConfig);
    }

    /**
     * 解析JSON配置字符串为服务列表。
     * @param jsonConfig JSON配置字符串。
     * @return 服务列表，包含服务名称和服务配置信息。
     */
    private List<Map<String, Map<String, String>>> parseJsonConfig(String jsonConfig) {
        List<Map<String, Map<String, String>>> serviceList = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonConfig);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject serviceObject = jsonArray.getJSONObject(i);
                Iterator<String> keys = serviceObject.keys();
                while (keys.hasNext()) {
                    String serviceName = keys.next();
                    JSONObject serviceDetails = serviceObject.getJSONObject(serviceName);
                    Map<String, String> detailsMap = new HashMap<>();
                    Iterator<String> detailKeys = serviceDetails.keys();
                    while (detailKeys.hasNext()) {
                        String detailKey = detailKeys.next();
                        detailsMap.put(detailKey, serviceDetails.getString(detailKey));
                    }
                    Map<String, Map<String, String>> serviceMap = new HashMap<>();
                    serviceMap.put(serviceName, detailsMap);
                    serviceList.add(serviceMap);
                }
            }
        } catch (JSONException e) {
            Log.e("MainActivity", "Failed to parse JSON configuration: " + e.getMessage(), e);
        }
        return serviceList;
    }

    /**
     * 从Assets目录加载指定文件的JSON内容。
     * @param fileName 文件名。
     * @return JSON内容字符串。
     */
    private String loadJsonFromAssets(String fileName) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            InputStream inputStream = getAssets().open(fileName);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            bufferedReader.close();
            inputStream.close();
        } catch (IOException e) {
            Log.e("MainActivity", "Failed to load JSON from assets: " + e.getMessage(), e);
        }
        return stringBuilder.toString();
    }

    /**
     * 将文本复制到剪贴板。
     * @param text 要复制的文本。
     */
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("AuthInfo", text);
        clipboard.setPrimaryClip(clip);
    }

    /**
     * 处理获取认证信息的逻辑。
     * 根据当前网页URL匹配配置，执行JS命令或读取Cookie。
     */
    private void handleGetAuthInfo() {
        String currentUrl = webView.getUrl();
        if (currentUrl == null) {
            Toast.makeText(this, getString(R.string.toast_load_webpage), Toast.LENGTH_SHORT).show();
            return;
        }

        String jsonConfig = loadJsonFromAssets("services.json");
        List<Map<String, Map<String, String>>> configList = parseJsonConfig(jsonConfig);

        // 遍历配置列表，匹配当前URL并执行相应操作
        for (Map<String, Map<String, String>> entry : configList) {
            for (Map.Entry<String, Map<String, String>> serviceEntry : entry.entrySet()) {
                String serviceName = serviceEntry.getKey();
                Map<String, String> serviceConfig = serviceEntry.getValue();
                String urlConfig = serviceConfig.get("url");
                String jsCommand = serviceConfig.get("js");
                String cookieKey = serviceConfig.get("cookie");

                if (currentUrl.contains(urlConfig)) {
                    if (jsCommand != null && !jsCommand.isEmpty()) {
                        webView.evaluateJavascript(jsCommand, value -> {
                            if (value != null && !value.isEmpty()) {
                                String formattedValue = formatValue(value);
                                copyToClipboard(formattedValue);
                                Toast.makeText(this, getString(R.string.credentials_copied) + ": " + formattedValue, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, getString(R.string.toast_no_auth_info), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else if (cookieKey != null && !cookieKey.isEmpty()) {
                        handleCookie(currentUrl, cookieKey);
                    }
                    return;
                }
            }
        }
    }

    /**
     * 格式化从JS获取的值。
     * @param value JS返回的原始值。
     * @return 格式化后的值。
     */
    private String formatValue(String value) {
        return value.replace("\\\"", "\"").replaceAll("\"", "");
    }

    /**
     * 处理Cookie逻辑。
     * @param currentUrl 当前网页URL。
     * @param cookieKey 需要匹配的Cookie键。
     */
    private void handleCookie(String currentUrl, String cookieKey) {
        CookieManager cookieManager = CookieManager.getInstance();
        String cookies = cookieManager.getCookie(currentUrl);
        if (cookies != null && !cookies.isEmpty()) {
            String[] pairs = cookies.split(";");
            for (String pair : pairs) {
                String[] keyValue = pair.trim().split("=");
                if (keyValue.length == 2 && keyValue[0].equals(cookieKey)) {
                    String cookieValue = keyValue[1];
                    copyToClipboard(cookieKey + "=" + cookieValue);
                    Toast.makeText(this, getString(R.string.credentials_copied) + ": " + cookieKey + "=" + cookieValue, Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        } else {
            Toast.makeText(this, getString(R.string.toast_no_cookie), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 清除WebView的Cookies和LocalStorage。
     */
    private void clearCookiesAndLocalStorage() {
        String currentUrl = webView.getUrl();
        if (currentUrl != null) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeSessionCookies(value -> {});
            cookieManager.removeAllCookies(value -> {});

            webView.evaluateJavascript("localStorage.clear();", null);

            // 清除后隐藏Cookie过期时间显示
            tvCookieExpires.setVisibility(View.GONE);
        }
    }

    /**
     * 更新Cookie过期时间显示
     * @param url 当前页面URL
     */
    private void updateCookieExpiresDisplay(String url) {
        String jsonConfig = loadJsonFromAssets("services.json");
        List<Map<String, Map<String, String>>> configList = parseJsonConfig(jsonConfig);

        // 查找匹配的配置
        for (Map<String, Map<String, String>> entry : configList) {
            for (Map.Entry<String, Map<String, String>> serviceEntry : entry.entrySet()) {
                String urlConfig = serviceEntry.getValue().get("url");
                String cookieKey = serviceEntry.getValue().get("cookie");

                // 只处理使用cookie字段的服务（不是使用js字段的）
                if (url.contains(urlConfig) && cookieKey != null && !cookieKey.isEmpty()) {
                    checkAndDisplayCookieExpiry(url, cookieKey);
                    return;
                }
            }
        }

        // 如果没有匹配的配置，隐藏显示
        tvCookieExpires.setVisibility(View.GONE);
    }

    /**
     * 检查并显示Cookie过期时间
     * @param url URL
     * @param cookieKey Cookie键名
     */
    private void checkAndDisplayCookieExpiry(String url, String cookieKey) {
        CookieManager cookieManager = CookieManager.getInstance();
        String cookies = cookieManager.getCookie(url);

        if (cookies != null && !cookies.isEmpty()) {
            // 解析所有cookie
            String[] cookiePairs = cookies.split(";");
            String targetCookieValue = null;
            String targetCookieExpires = null;

            // 先找到目标cookie的值
            for (String pair : cookiePairs) {
                String trimmed = pair.trim();
                if (trimmed.startsWith(cookieKey + "=")) {
                    targetCookieValue = trimmed.substring(cookieKey.length() + 1).trim();
                    break;
                }
            }

            // 如果找到了目标cookie，继续查找它的expires属性
            if (targetCookieValue != null) {
                for (String pair : cookiePairs) {
                    String trimmed = pair.trim();
                    if (trimmed.toLowerCase().startsWith("expires=")) {
                        targetCookieExpires = trimmed.substring(8).trim();
                        break;
                    }
                }

                if (targetCookieExpires != null) {
                    // 解析过期时间并显示
                    String expiresTime = parseExpiresTime(targetCookieExpires);
                    tvCookieExpires.setText(expiresTime);
                    tvCookieExpires.setVisibility(View.VISIBLE);
                } else {
                    // 没有expires属性，可能是session cookie或者实际过期时间由服务端控制
                    tvCookieExpires.setText("已登录");
                    tvCookieExpires.setVisibility(View.VISIBLE);
                }
                return;
            }
        }

        // 没有找到目标cookie，隐藏显示
        tvCookieExpires.setVisibility(View.GONE);
    }

    /**
     * 解析Cookie过期时间
     * @param expiresValue expires属性的值
     * @return 格式化后的过期时间字符串
     */
    private String parseExpiresTime(String expiresValue) {
        // 解析RFC 1123格式的日期: "Wed, 09 Oct 2024 00:00:00 GMT"
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
            Date expiresDate = inputFormat.parse(expiresValue);

            if (expiresDate != null) {
                // 计算剩余时间
                long now = System.currentTimeMillis();
                long expiresMillis = expiresDate.getTime();
                long diff = expiresMillis - now;

                if (diff <= 0) {
                    return "已过期";
                }

                long days = diff / (1000 * 60 * 60 * 24);
                long hours = (diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);

                if (days > 0) {
                    return "预计剩" + days + "天" + (hours > 0 ? hours + "时" : "");
                } else if (hours > 0) {
                    long minutes = (diff % (1000 * 60 * 60)) / (1000 * 60);
                    return "预计剩" + hours + "时" + (minutes > 0 ? minutes + "分" : "");
                } else {
                    long minutes = diff / (1000 * 60);
                    return "预计剩" + minutes + "分";
                }
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to parse expires time: " + e.getMessage());
        }
        return "已登录";
    }

    @Override
    public void onBackPressed() {
        if (servicePanel.getVisibility() == View.VISIBLE) {
            servicePanel.setVisibility(View.GONE);
            webViewContainer.setVisibility(View.VISIBLE);
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
