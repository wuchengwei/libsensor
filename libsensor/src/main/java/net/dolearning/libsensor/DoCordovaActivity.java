package net.dolearning.libsensor;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.apache.cordova.CordovaActivity;
import org.apache.cordova.engine.SystemWebView;
import org.apache.cordova.engine.SystemWebViewEngine;

import java.util.ArrayList;

public class DoCordovaActivity extends CordovaActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);

        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        if (getActionBar() != null) getActionBar().hide();

        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dolearning_sensor);

        super.init();

        SystemWebViewEngine doWebViewEngine = (SystemWebViewEngine) appView.getEngine();
        SystemWebView doWebView = (SystemWebView) appView.getEngine().getView();
        doWebView.setWebViewClient(new DoSystemWebViewClient(doWebViewEngine, preferences));

        String pkgVersion = getVersionName();
        if (pkgVersion.isEmpty()) pkgVersion = "undefined";
        String userAgent = doWebView.getSettings().getUserAgentString() + " DolearningDosensor/" + pkgVersion;
        doWebView.getSettings().setUserAgentString(userAgent);

        loadUrl(launchUrl);

        swrInit();

        requestAppPermissions();
    }

    private String getVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    private void requestAppPermissions() {
        String[] permissions = new String[]{
                "android.permission.INTERNET",
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.BLUETOOTH",
                "android.permission.BLUETOOTH_ADMIN",
                "android.permission.BLUETOOTH_SCAN",
                "android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH_ADVERTISE",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.CAMERA"
        };
        ArrayList<String> nPermissions = new ArrayList<String>();
        for (String permission : permissions) {
            if (!cordovaInterface.hasPermission(permission)) {
                nPermissions.add(permission);
            }
        }
        if (nPermissions.isEmpty()) return;

        String[] sPermissions = new String[nPermissions.size()];
        for (int i = 0; i < nPermissions.size(); i++) sPermissions[i] = nPermissions.get(i);
        requestPermissions(sPermissions, 7001);
    }

    public native void swrInit();

    static {
        System.loadLibrary("sensor");
    }
}
