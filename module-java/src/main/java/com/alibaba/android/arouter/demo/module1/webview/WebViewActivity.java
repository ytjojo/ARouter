package com.alibaba.android.arouter.demo.module1.webview;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

@Route(path = "/test/globlewebview", priority = -1, secondaryPathes = {"https://", "http://"})
public class WebViewActivity extends AppCompatActivity {
    @Autowired
    public int abc;

    @Autowired
    public String name;

    @Autowired(alternate = {"mUrl"}, name = "url")
    public String url;

    private WebChromeClient webChromeClient = new WebChromeClient() {
        public void onProgressChanged(WebView param1WebView, int param1Int) {
        }

        public void onReceivedTitle(WebView param1WebView, String param1String) {
            super.onReceivedTitle(param1WebView, param1String);
        }
    };

    private WebViewClient webViewClient = new WebViewClient() {
        public boolean shouldOverrideUrlLoading(WebView param1WebView, String param1String) {
            return super.shouldOverrideUrlLoading(param1WebView, param1String);
        }
    };

    protected void onCreate(@Nullable Bundle paramBundle) {
        super.onCreate(paramBundle);
        ARouter.getInstance().inject(this);
        WebView webView = new WebView((Context) this);
        setContentView((View) webView);
        if (getIntent().getData() != null)
            this.url = getIntent().getData().toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("nam e= ");
        stringBuilder.append(this.name);
        stringBuilder.append(" abc = ");
        stringBuilder.append(this.abc);
        Toast.makeText((Context) this, stringBuilder.toString(), Toast.LENGTH_LONG).show();
        webView.loadUrl(this.url);
        webView.setWebChromeClient(this.webChromeClient);
        webView.setWebViewClient(this.webViewClient);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setBuiltInZoomControls(false);

        webSettings.setAllowContentAccess(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        // 移除setAppCacheEnabled调用，因为该方法在API 28及以上版本中已被移除
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setTextZoom(100);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
    }
}