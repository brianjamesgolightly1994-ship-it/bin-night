package uk.binnight.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

import org.json.JSONObject;

/** Shows the bundled Bin Night page, which works out every date on the phone. */
public class MainActivity extends Activity {
    private static final int LOOKUP = 1;
    private static final String HOST = "appassets.androidplatform.net";

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Serve the page from an https origin so its saved dates persist in localStorage
        final WebViewAssetLoader assets = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webView.addJavascriptInterface(new Bridge(), "Android");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assets.shouldInterceptRequest(request.getUrl());
            }

            // Open outside links (the council's page) in the browser, not inside the app
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                if (HOST.equals(url.getHost())) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, url));
                return true;
            }
        });
        setContentView(webView);
        webView.loadUrl("https://" + HOST + "/assets/index.html");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recount the days whenever the app comes back to the front
        if (webView != null) webView.evaluateJavascript("typeof render === 'function' && render()", null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != LOOKUP || resultCode != RESULT_OK || data == null) return;
        String json = data.getStringExtra(CouncilActivity.EXTRA_RESULT);
        if (json == null) return;
        webView.evaluateJavascript("window.binNightImport(" + JSONObject.quote(json) + ")", null);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    private class Bridge {
        @JavascriptInterface
        public void openCouncilLookup() {
            runOnUiThread(() -> startActivityForResult(new Intent(MainActivity.this, CouncilActivity.class), LOOKUP));
        }
    }
}
