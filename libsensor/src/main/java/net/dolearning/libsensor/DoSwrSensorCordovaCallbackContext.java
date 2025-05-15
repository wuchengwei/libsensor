package net.dolearning.libsensor;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;

public class DoSwrSensorCordovaCallbackContext extends CallbackContext {
    private final String LOG_TAG = "DoSwrSensorCordovaCallbackContext";


    private DoCordovaPluginSwrSensor swrSensorPlugin;

    public DoSwrSensorCordovaCallbackContext(String callbackId, CordovaWebView webView, DoCordovaPluginSwrSensor swrSensorPlugin) {
        super(callbackId, webView);
        this.swrSensorPlugin = swrSensorPlugin;
    }

    @Override
    public void sendPluginResult(PluginResult pluginResult) {
        this.swrSensorPlugin.onCordovaCallback(getCallbackId(), pluginResult);
    }
}