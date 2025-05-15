package net.dolearning.libsensor;

import android.content.res.AssetManager;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import androidx.webkit.WebViewAssetLoader;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.LOG;
import org.apache.cordova.engine.SystemWebViewClient;
import org.apache.cordova.engine.SystemWebViewEngine;
import java.util.Locale;

public class DoSystemWebViewClient extends SystemWebViewClient {
    private final String TAG = "DoSystemWebViewClient";
    private WebViewAssetLoader doAssetLoader;

    public DoSystemWebViewClient(SystemWebViewEngine engine, CordovaPreferences preferences) {
        super(engine);

        WebViewAssetLoader.Builder assetLoaderBuilder = new WebViewAssetLoader.Builder()
                .setDomain(preferences.getString("hostname", "localhost").toLowerCase(Locale.getDefault()))
                .setHttpAllowed(true);

        assetLoaderBuilder.addPathHandler("/", path -> {
            LOG.i(TAG, "request path handler for path " + path);
            String assetPath = path;
            // if (path.isEmpty()) {
            //     assetPath = "dolearning.html";
            // }
            if (!assetPath.equals("dolearning.html") && !assetPath.startsWith("cordova/")) {
                return null;
            }
            try {
                AssetManager assets = parentEngine.getView().getContext().getAssets();
                String finalPath = "www/" + path;
                java.io.InputStream iStream = assets.open(finalPath, AssetManager.ACCESS_STREAMING);

                String mimeType = "text/html";
                String extension = MimeTypeMap.getFileExtensionFromUrl(path);
                if (extension != null) {
                    if (path.endsWith(".js") || path.endsWith(".mjs")) {
                        mimeType = "application/javascript";
                    } else if (path.endsWith(".wasm")) {
                        mimeType = "application/wasm";
                    } else {
                        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    }
                }
                return new WebResourceResponse(mimeType, null, iStream);
            } catch (Exception e) {
                // e.printStackTrace();
                LOG.e(TAG, e.getMessage());
            }
            return null;
        });

        this.doAssetLoader = assetLoaderBuilder.build();
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return (doAssetLoader != null) ?
                doAssetLoader.shouldInterceptRequest(request.getUrl()) :
                null;
    }
}
