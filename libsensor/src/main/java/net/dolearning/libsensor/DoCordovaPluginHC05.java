package net.dolearning.libsensor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.widget.Toast;


import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public class DoCordovaPluginHC05 extends CordovaPlugin {
    public static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final UUID BLUETOOTH_SERVER_UUID = UUID.fromString("7A9C3B55-78D0-44A7-A94E-A93E3FE118CE");
    private static final String LOG_TAG = "CordovaPluginHC05";

    private BluetoothAdapter mBluetoothAdapter;

    private ConnectThread mConnectThread;
    private AcceptThread mAcceptThread;
    private ConnectedThread mConnectedThread;
    private String mState = "NONE";

    public static final String STATE_NONE = "NONE";
    public static final String STATE_LISTEN = "LISTEN";
    public static final String STATE_CONNECTING = "CONNECTING";
    public static final String STATE_CONNECTED = "CONNECTED";

    private CallbackContext connectCallbackContext;


    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        initBluetooth();
        LOG.i(LOG_TAG, "CordovaPluginHC05 initialized");
    }

    @Override
    public Boolean shouldAllowNavigation(String url) {
        try {
            URL urlObj = new URL(url);
            String host = urlObj.getHost();
            LOG.i(LOG_TAG, "shouldAllowNavigation for " + host);
            // 阻止在外部浏览器中打开链接
            if (
                host.equals("cas2.edu.sh.cn") ||
                host.endsWith("ai-study.net") ||
                host.equals("dolearning.net") ||
                host.equals("dolearning.org") ||
                host.equals("dopkg.dolearning.org")
            ) {
                return true;
            }
            return super.shouldAllowNavigation(url);
        } catch (MalformedURLException e) {
            return false;
        }
    }

    @Override
    public Object onMessage(String id, Object data) {
        if (id.equals("onPageFinished")) {
            URL urlObj = parseURL((String) data);
            if (urlObj != null) {
                String host = urlObj.getHost();
                if (
                    host.equals("dolearning.net") ||
                    host.equals("dolearning.org") ||
                    host.equals("dopkg.dolearning.org")
                ) {
                    LOG.i(LOG_TAG, "inject https://img-static.dolearning.net/learnware/do-sensor/index.js");
                    String jsUrl = "javascript:var script = document.createElement('script');";
                    jsUrl += "script.src=\"https://img-static.dolearning.net/learnware/do-sensor/index.js\";";
                    jsUrl += "document.getElementsByTagName('head')[0].appendChild(script);";
                    webView.getEngine().loadUrl(jsUrl, false);
                }
            }
        }
        return super.onMessage(id, data);
    }

    private URL parseURL(String urlStr) {
        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private void initBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(cordova.getActivity(), "蓝牙不可用", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        LOG.i(LOG_TAG, "execute action " + action + " with args: " + args.toString());
        PluginResult pluginResult;
        JSONObject returnObj;
        JSONObject argObj;
        String macAddress;
        String writingData;
        switch (action) {
            case "test":
                pluginResult = new PluginResult(PluginResult.Status.OK, "OK");
                callbackContext.sendPluginResult(pluginResult);
                return true;
            case "connect":
                argObj = getArgsObject(args);
                if (argObj == null) {
                    pluginResult = new PluginResult(PluginResult.Status.ERROR, "missing arg device info");
                    callbackContext.sendPluginResult(pluginResult);
                    return true;
                }
                macAddress = argObj.optString("address", "");
                if (macAddress.isEmpty()) {
                    pluginResult = new PluginResult(PluginResult.Status.ERROR, "missing arg device.address");
                    callbackContext.sendPluginResult(pluginResult);
                    return true;
                }
                connectCallbackContext = callbackContext;
                connectDevice(macAddress);
                return true;
            case "write":
                argObj = getArgsObject(args);
                if (argObj == null) {
                    pluginResult = new PluginResult(PluginResult.Status.ERROR, "missing arg info");
                    callbackContext.sendPluginResult(pluginResult);
                    return true;
                }
                writingData = argObj.optString("data", "");
                if (writingData.isEmpty()) {
                    pluginResult = new PluginResult(PluginResult.Status.ERROR, "missing arg info.data");
                    callbackContext.sendPluginResult(pluginResult);
                    return true;
                }
                JSONObject sendDataResult = sendDataToHC05(writingData);
                pluginResult = new PluginResult(
                        sendDataResult.optString("status", "").equals("success") ?
                                PluginResult.Status.OK : PluginResult.Status.ERROR,
                        sendDataResult);
                callbackContext.sendPluginResult(pluginResult);
                return true;
            case "disconnect":
                disconnect();
                pluginResult = new PluginResult(PluginResult.Status.OK, "OK");
                callbackContext.sendPluginResult(pluginResult);
                return true;
            case "listen":
                argObj = getArgsObject(args);
                if (argObj == null) {
                    pluginResult = new PluginResult(PluginResult.Status.ERROR, "missing arg device info");
                    callbackContext.sendPluginResult(pluginResult);
                    return true;
                }
                macAddress = argObj.optString("address", "");
                if (macAddress.isEmpty()) {
                    pluginResult = new PluginResult(PluginResult.Status.ERROR, "missing arg device.address");
                    callbackContext.sendPluginResult(pluginResult);
                    return true;
                }
                listen(macAddress);
                pluginResult = new PluginResult(PluginResult.Status.OK, "OK");
                callbackContext.sendPluginResult(pluginResult);
                return true;
        }
        return false;
    }

    private JSONObject getArgsObject(JSONArray args) {
        if (args.length() >= 1) {
            try {
                return args.getJSONObject(0);
            } catch (JSONException ex) {
            }
        }

        return null;
    }

    private JSONObject sendDataToHC05(String data) {
        JSONObject result = new JSONObject();
        if (!mState.equals(STATE_CONNECTED) || mConnectedThread == null) {
            Utils.addJSONObjectProperty(result, "status", "error");
            Utils.addJSONObjectProperty(result, "message", "not connected");
            return result;
        }
        boolean written = mConnectedThread.write(data.getBytes());
        if (written) {
            Utils.addJSONObjectProperty(result, "status", "success");
        } else {
            Utils.addJSONObjectProperty(result, "status", "fail");
        }
        return result;
    }

    private void listen(String macAddress) {
        stopConnectThread();
        stopConnectedThread();
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }

    private void connectDevice(String macAddress) {
        if (mBluetoothAdapter == null) {
            sendConnectCallback(PluginResult.Status.ERROR, "蓝牙不可用");
            return;
        }
        if (mState.equals(STATE_CONNECTING)) {
            sendConnectCallback(PluginResult.Status.ERROR, "正在连接，请稍候再试");
            return;
        }
        if (mState.equals(STATE_CONNECTED)) {
            sendConnectCallback(PluginResult.Status.ERROR, "已经连接，请勿重复连接");
            return;
        }
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(macAddress);

        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        mState = STATE_CONNECTING;
    }

    private void sendConnectCallback(PluginResult.Status status, String msg) {
        if (connectCallbackContext == null) return;
        PluginResult pluginResult = new PluginResult(status, msg);
        connectCallbackContext.sendPluginResult(pluginResult);
    }

    private void sendConnectCallback(PluginResult.Status status, JSONObject result) {
        if (connectCallbackContext == null) return;
        PluginResult pluginResult = new PluginResult(status, result);
        pluginResult.setKeepCallback(true);
        connectCallbackContext.sendPluginResult(pluginResult);
    }

    private synchronized void onConnected(BluetoothSocket socket, BluetoothDevice device) {
        LOG.i(LOG_TAG, "onConnected " + device.getAddress());

        stopConnectThread();
        stopAcceptThread();
        stopConnectedThread();

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        mState = STATE_CONNECTED;

        JSONObject obj = new JSONObject();
        Utils.addJSONObjectProperty(obj, "status", "connected");
        sendConnectCallback(PluginResult.Status.OK, obj);
    }

    private void onDataReceived(byte[] data, int len) {
        byte[] chars = new byte[len];
        System.arraycopy(data, 0, chars, 0, len);
        JSONObject obj = new JSONObject();
        Utils.addJSONObjectProperty(obj, "status", "data");
        Utils.addJSONObjectProperty(obj, "data", new String(chars, StandardCharsets.UTF_8));
        sendConnectCallback(PluginResult.Status.OK, obj);
    }

    private synchronized void onDisconnected() {
        LOG.i(LOG_TAG, "OnDisconnected ");

        stopConnectThread();
        stopAcceptThread();
        stopConnectedThread();

        mState = STATE_NONE;

        JSONObject obj = new JSONObject();
        Utils.addJSONObjectProperty(obj, "status", "disconnected");
        sendConnectCallback(PluginResult.Status.OK, obj);
    }

    private void disconnect() {
        onDisconnected();
    }

    private void stopConnectThread() {
        if (mConnectThread != null) {
            mConnectThread.interrupt();
            mConnectThread.cancel();
            mConnectThread = null;
        }
    }

    private void stopConnectedThread() {
        if (mConnectedThread != null) {
            mConnectedThread.interrupt();
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    private void stopAcceptThread() {
        if (mAcceptThread != null) {
            mAcceptThread.interrupt();
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket socket = null;
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
            } catch (IOException e) {
                LOG.e(LOG_TAG, "createRfcommSocketToServiceRecord failed");
                LOG.e(LOG_TAG, e.getMessage());
            }
            mmSocket = socket;
        }

        public void run() {
            LOG.i(LOG_TAG, "Begin connect thread");
            if (Thread.interrupted()) {
                LOG.i(LOG_TAG, "Connect thread interrupted");
                return;
            }
            setName("hc05ConnectThread");

            if (mmSocket == null) return;

            // cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException e) {
                LOG.e(LOG_TAG, "connect socket failed");
                LOG.e(LOG_TAG, e.getMessage());
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    LOG.e(LOG_TAG, "close socket failed after connect failed");
                }
                mState = STATE_NONE;
                return;
            }

            synchronized (DoCordovaPluginHC05.this) {
                mConnectThread = null;
            }

            onConnected(mmSocket, mmDevice);
        }

        public void cancel() {
            LOG.i(LOG_TAG, "ConnectThread close socket");
            try {
                if (mmSocket != null) mmSocket.close();
            } catch (IOException e) {
                LOG.e(LOG_TAG, "close connect socket failed");
                LOG.e(LOG_TAG, e.getMessage());
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream inStream = null;
            OutputStream outStream = null;

            try {
                inStream = socket.getInputStream();
                outStream = socket.getOutputStream();
            } catch (Exception e) {
                LOG.e(LOG_TAG, "get input/output stream of socket failed");
                LOG.e(LOG_TAG, e.getMessage());
            }

            mmInStream = inStream;
            mmOutStream = outStream;
        }

        public void run() {
            LOG.i(LOG_TAG, "Begin connected thread");
            if (Thread.interrupted()) {
                LOG.i(LOG_TAG, "Connected thread interrupted");
                return;
            }
            setName("hc05ConnectedThread");

            byte[] buffer = new byte[256];
            int bytes;

            while (true) {
                LOG.i(LOG_TAG, "before read bluetooth socket");
                try {
                    bytes = mmInStream.read(buffer);
                    onDataReceived(buffer, bytes);
                    LOG.i(LOG_TAG, "received bluetooth bytes: " + Utils.bytesToHex(buffer, bytes));
                } catch (Exception e) {
                    LOG.e(LOG_TAG, "read input stream of socket failed");
                    LOG.e(LOG_TAG, e.getMessage());
                    onDisconnected();
                    break;
                }
            }
        }

        public boolean write(byte[] buffer) {
            LOG.i(LOG_TAG, "ConnectedThread write");
            try {
                if (mmOutStream != null) {
                    mmOutStream.write(buffer);
                    return true;
                }
            } catch (Exception e) {
                LOG.e(LOG_TAG, "write output stream of socket failed");
                LOG.e(LOG_TAG, e.getMessage());
            }
            return false;
        }

        public void cancel() {
            LOG.i(LOG_TAG, "ConnectedThread cancel");
            try {
                if (mmInStream != null) mmInStream.close();
                if (mmOutStream != null) mmOutStream.close();
                if (mmSocket != null) mmSocket.close();
            } catch (Exception e) {
                LOG.e(LOG_TAG, "close connected socket failed");
                LOG.e(LOG_TAG, e.getMessage());
            }
        }
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket socket = null;

            // Create a new listening server socket
            try {
                socket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("HC05BluetoothService", BLUETOOTH_SERVER_UUID);
            } catch (IOException e) {
                LOG.e(LOG_TAG, "Socket listen() failed", e);
            }
            mmServerSocket = socket;
        }

        public void run() {
            if (Thread.interrupted()) return;
            setName("hc05AcceptThread");

            BluetoothSocket socket;

            // Listen to the server socket if we're not connected
            while (!mState.equals(STATE_CONNECTED)) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    LOG.e(LOG_TAG, "Socket accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (DoCordovaPluginHC05.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                onConnected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    LOG.e(LOG_TAG, "hc05AcceptThread: Could not close socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            LOG.i(LOG_TAG, "hc05AcceptThread end");
        }

        public void cancel() {
            LOG.i(LOG_TAG, "AcceptThread close socket");
            try {
                if (mmServerSocket != null) mmServerSocket.close();
            } catch (IOException e) {
                LOG.e(LOG_TAG, "Socket close() of server failed", e);
            }
        }
    }
}
