package uk.binnight.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Opens Midlothian Council's bin lookup. The person searches as normal, and once the page
 * shows their dates this screen hands them back to Bin Night.
 */
public class CouncilActivity extends Activity {
    static final String EXTRA_RESULT = "result";
    private static final String START_URL = "https://my.midlothian.gov.uk/service/Bin_Collection_Dates";

    // Reads the date fields from the page or its form frame. Returns JSON text, or null.
    private static final String EXTRACT_JS =
            "(function(){"
            + "function read(doc){"
            + " var ids=['dateResidual','dateRecycling','dateCard','dateFood','dateGlass','dateGarden'];"
            + " var out={},n=0;"
            + " ids.forEach(function(id){var e=doc.getElementById(id);"
            + "  if(e&&e.value&&/^\\d{2}\\/\\d{2}\\/\\d{4}$/.test(e.value.trim())){out[id]=e.value.trim();n++;}});"
            + " if(!n)return null;"
            + " var a=doc.getElementById('listAddress');"
            + " var addr=(a&&a.selectedIndex>=0&&a.options[a.selectedIndex])?a.options[a.selectedIndex].text.trim():'';"
            + " return {address:addr,dates:out};}"
            + "var r=read(document);if(r)return JSON.stringify(r);"
            + "var fr=document.querySelectorAll('iframe');"
            + "for(var i=0;i<fr.length;i++){"
            + " try{var d=fr[i].contentDocument;if(!d)throw 0;r=read(d);if(r)return JSON.stringify(r);}"
            + " catch(e){if(fr[i].src)return JSON.stringify({frame:fr[i].src});}}"
            + "return null;})()";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WebView webView;
    private TextView banner;
    private String lastResult;
    private boolean pageLoaded;
    private boolean openedFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        banner = new TextView(this);
        banner.setText("Enter your postcode and choose your address. Bin Night will pick up your dates.");
        banner.setTextColor(Color.WHITE);
        banner.setBackgroundColor(Color.parseColor("#1f6f5c"));
        banner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics());
        banner.setPadding(pad, pad, pad, pad);
        root.addView(banner, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        // Present as the phone's normal browser rather than an embedded view
        settings.setUserAgentString(settings.getUserAgentString().replace("; wv", ""));
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageLoaded = true;
            }
        });
        root.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        webView.loadUrl(START_URL);
        handler.postDelayed(poll, 1500);
    }

    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) return;
            webView.evaluateJavascript(EXTRACT_JS, value -> {
                handle(unquote(value));
                handler.postDelayed(poll, 1500);
            });
        }
    };

    private void handle(String result) {
        if (result == null) { lastResult = null; return; }
        try {
            JSONObject obj = new JSONObject(result);
            if (obj.has("frame")) {
                // The form sits in a frame from another site that this screen can't read into,
                // so open the form on its own before the person starts typing.
                if (!openedFrame && pageLoaded) {
                    openedFrame = true;
                    webView.loadUrl(obj.getString("frame"));
                }
                return;
            }
            // Wait until the dates are the same on two checks in a row, so they have finished loading
            if (result.equals(lastResult)) {
                Intent data = new Intent();
                data.putExtra(EXTRA_RESULT, result);
                setResult(RESULT_OK, data);
                finish();
            } else {
                lastResult = result;
                banner.setText("Found your dates. Saving them…");
            }
        } catch (Exception ignored) {
            lastResult = null;
        }
    }

    private static String unquote(String value) {
        if (value == null || "null".equals(value)) return null;
        try {
            return new JSONArray("[" + value + "]").getString(0);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(poll);
        super.onDestroy();
    }
}
