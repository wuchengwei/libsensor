package net.dolearning.libsensor;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import com.randdusing.bluetoothle.BluetoothLePlugin;

/**
 * 提供以下操作传感器的功能
 * 1. 蓝牙连接以后调用 Native 的 updateDevice 接口
 * 2. 调用 Native 的 setCollectInterval 接口以设置数据收集间隔
 * 3. 调用 Native 的 getSensorInfo 接口获取设备信息
 * 4. 调用 Native 的 startSingleCollect 接口获取单次的传感器数据
 * 5. 调用 Native 的 startContinuouslyCollect 接口持续获取传感器数据
 * 6. 调用 Native 的 stopCollect 接口停止获取传感器数据
 */

public class DoCordovaPluginSwrSensor extends CordovaPlugin {
    private static final String LOG_TAG = "CordovaPluginSwrSensor";

    private static final String SWR_BLE_SERVICE_UUID = "00001523-1212-efde-1523-785feabcd123";
    private static final String SWR_UUIDSTR_BLE_TRANS_Write = "00001525-1212-efde-1523-785feabcd123";
    private static final String SWR_UUIDSTR_BLE_TRANS_Read = "00001524-1212-efde-1523-785feabcd123";

    private CallbackContext sendSensorInfoCallbackContext;

    private CallbackContext sendSensorDataCallbackContext;

    private DoSwrSensorCordovaCallbackContext bleSubscribeCallbackContext;

    private final String subscribeSensorDataCallbackId = "swrSensorSubscribe";

    private ArrayList<BleDeviceInfo> bleDeviceInfos;

    private BluetoothLePlugin blePlugin = null;

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        bleDeviceInfos = new ArrayList<BleDeviceInfo>();
        blePlugin = (BluetoothLePlugin)webView.getPluginManager().getPlugin("BluetoothLePlugin");
        bleSubscribeCallbackContext = new DoSwrSensorCordovaCallbackContext(subscribeSensorDataCallbackId, webView, this);
        initCallbackObj();
    }

    @Override
    public void onDestroy() {
        swrReleaseAPI();
    }

    public boolean subscribeBleData(BleDeviceInfo deviceInfo) {
        if (blePlugin == null || deviceInfo == null) return false;
        if (deviceInfo.status != SwrDataLoggerInfo.SWR_dataLoggerStateConnected) return false;

        HashMap<Object, Object> connection = blePlugin.getConnection(deviceInfo.deviceMac);
        if (connection == null) return false;

        int state = Integer.valueOf(connection.get(blePlugin.keyState).toString());
        if (state != BluetoothProfile.STATE_CONNECTED) return false;

        BluetoothGatt bluetoothGatt = (BluetoothGatt) connection.get(blePlugin.keyPeripheral);

        BluetoothGattService service = getBleService(bluetoothGatt, SWR_BLE_SERVICE_UUID, 0);
        if (service == null) return false;

        BluetoothGattCharacteristic characteristic = getBleCharacteristic(service, SWR_UUIDSTR_BLE_TRANS_Read, 0);
        if (characteristic == null) return false;

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(blePlugin.clientConfigurationDescriptorUuid);
        if (descriptor == null) return false;

        boolean result = bluetoothGatt.setCharacteristicNotification(characteristic, true);
        if (!result) return false;

        //Use properties to determine whether notification or indication should be used
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
            result = descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            result = descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        }
        if (!result) return false;

        UUID characteristicUuid = characteristic.getUuid();
        blePlugin.AddSequentialCallbackContext(characteristicUuid, connection, blePlugin.operationSubscribe, bleSubscribeCallbackContext);

        result = bluetoothGatt.writeDescriptor(descriptor);
        if (!result) {
            blePlugin.RemoveCallback(characteristicUuid, connection, blePlugin.operationSubscribe);
            return false;
        }
        return true;
    }

    public void onCordovaCallback(String callbackId, PluginResult pluginResult) {
        LOG.i(LOG_TAG, "onCordovaCallback, callbackId: " + callbackId);
        if (callbackId.equals(subscribeSensorDataCallbackId)) {
            String dataStr = "";
            if (pluginResult.getMessageType() == PluginResult.MESSAGE_TYPE_MULTIPART) {
                PluginResult dataResult = pluginResult.getMultipartMessage(0);
                dataStr = dataResult.getMessage();
            } else {
                dataStr = pluginResult.getMessage();
            }
//            LOG.i(LOG_TAG, "onCordovaCallback, data: " + dataStr);
            JSONObject dataObj = parseJSONString(dataStr);
            if (dataObj == null) return;
            String status = dataObj.optString(blePlugin.keyStatus, "");
            String mac = dataObj.optString(blePlugin.keyAddress, "");
            BleDeviceInfo deviceInfo = findBleDeviceInfoByMacAddress(mac);
            if (deviceInfo == null) return;
//            LOG.i(LOG_TAG, "onCordovaCallback, data.status: " + status);
            if (status.equals(blePlugin.statusSubscribed)) {
                new Timeout(() -> swrUpdateDevice(deviceInfo), 300);
                new Timeout(this::swrGetSensorInfo, 600);
            } else if (status.equals(blePlugin.statusSubscribedResult)) {
                byte[] dataBytes = blePlugin.getPropertyBytes(dataObj, blePlugin.keyValue);
//                LOG.i(LOG_TAG, "swrAppendDeviceData, data: " + Utils.bytesToHex(dataBytes) );
                SwrDataLoggerInfo dataLoggerInfo = new SwrDataLoggerInfo(
                        deviceInfo.deviceName,
                        deviceInfo.deviceMac,
                        SwrDataLoggerInfo.SWR_BLECONNECT,
                        SwrDataLoggerInfo.SWR_dataLoggerStateConnected
                );
                SwrReadDataFlow d = new SwrReadDataFlow(dataLoggerInfo, dataBytes, dataBytes.length);
                swrAppendDeviceData(d);
            }
        }
    }

    private BleDeviceInfo findBleDeviceInfoByMacAddress(String mac) {
        if (mac.isEmpty()) return null;
        for (BleDeviceInfo deviceInfo: bleDeviceInfos) {
            if (deviceInfo.deviceMac.equals(mac)) return deviceInfo;
        }
        return null;
    }

    public JSONObject parseJSONString(String json) {
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            return  null;
        }
    }

    public boolean writeBleData(String macAddress, byte[] value) {
        if (blePlugin == null) return false;
        BleDeviceInfo deviceInfo = findBleDeviceInfoByMacAddress(macAddress);
        if (deviceInfo == null) return false;
        if (deviceInfo.status != SwrDataLoggerInfo.SWR_dataLoggerStateConnected) return false;

        HashMap<Object, Object> connection = blePlugin.getConnection(macAddress);
        if (connection == null) return false;

        int state = Integer.valueOf(connection.get(blePlugin.keyState).toString());
        if (state != BluetoothProfile.STATE_CONNECTED) return false;

        BluetoothGatt bluetoothGatt = (BluetoothGatt) connection.get(blePlugin.keyPeripheral);

        BluetoothGattService service = getBleService(bluetoothGatt, SWR_BLE_SERVICE_UUID, 0);
        if (service == null) return false;

        BluetoothGattCharacteristic characteristic = getBleCharacteristic(service, SWR_UUIDSTR_BLE_TRANS_Write, 0);
        if (characteristic == null) return false;

        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        boolean result = characteristic.setValue(value);
        if (!result) return false;
        result = bluetoothGatt.writeCharacteristic(characteristic);
        LOG.i(LOG_TAG, "writeBleData " + Utils.bytesToHex(value));
        return result;
    }

    private BluetoothGattService getBleService(BluetoothGatt bluetoothGatt, String serviceId, int serviceIndex) {
        UUID uuid = getUUID(serviceId);

        int found = 0;
        List<BluetoothGattService> services = bluetoothGatt.getServices();
        for (BluetoothGattService service : services) {
            if (service.getUuid().equals(uuid) && serviceIndex == found) {
                return service;
            } else if (service.getUuid().equals(uuid) && serviceIndex != found) {
                found++;
            }
        }

        return null;
    }

    private BluetoothGattCharacteristic getBleCharacteristic(BluetoothGattService service, String characteristicId, int characteristicIndex) {
        UUID uuid = getUUID(characteristicId);

        int found = 0;
        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
        for (BluetoothGattCharacteristic characteristic : characteristics) {
            if (characteristic.getUuid().equals(uuid) && characteristicIndex == found) {
                return characteristic;
            } else if (characteristic.getUuid().equals(uuid) && characteristicIndex != found) {
                found++;
            }
        }

        return null;
    }

    private UUID getUUID(String value) {
        if (value == null) {
            return null;
        }

        if (value.length() == 4) {
            value = blePlugin.baseUuidStart + value + blePlugin.baseUuidEnd;
        }

        UUID uuid = null;

        try {
            uuid = UUID.fromString(value);
        } catch (Exception ex) {
            return null;
        }

        return uuid;
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false otherwise.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        LOG.i(LOG_TAG, "execute action " + action + " with args: " + args.toString());
        PluginResult pluginResult;
        JSONObject returnObj;
        JSONObject argObj;
        String deviceName;
        String deviceMac;
        switch (action) {
            case "testJNICall":
                pluginResult = new PluginResult(PluginResult.Status.OK, stringFromJNI());
                callbackContext.sendPluginResult(pluginResult);
                return true;
            case "setSensorInfoCallback":
                returnObj = new JSONObject();
                Utils.addJSONObjectProperty(returnObj, "status", "success");
                pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
                pluginResult.setKeepCallback(true);
                sendSensorInfoCallbackContext = callbackContext;
                return true;
            case "setSensorDataCallback":
                returnObj = new JSONObject();
                Utils.addJSONObjectProperty(returnObj, "status", "success");
                pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
                pluginResult.setKeepCallback(true);
                sendSensorDataCallbackContext = callbackContext;
                return true;
            case "swrUpdateDevice":
                argObj = getArgsObject(args);
                if (argObj == null) {
                    pluginResult = new PluginResult(PluginResult.Status.ERROR, "missing arg device info");
                    callbackContext.sendPluginResult(pluginResult);
                    return true;
                }
                deviceMac = argObj.optString("deviceMac", "");
                if (deviceMac.isEmpty()) {
                    pluginResult = new PluginResult(PluginResult.Status.ERROR, "missing arg deviceMac");
                    callbackContext.sendPluginResult(pluginResult);
                    return true;
                }
                deviceName = argObj.optString("deviceName", "");
                if (deviceName.isEmpty()) {
                    pluginResult = new PluginResult(PluginResult.Status.ERROR, "missing arg deviceName");
                    callbackContext.sendPluginResult(pluginResult);
                    return true;
                }
                String status = argObj.optString("status", "unknown");
                int statusCode = status.equals("connected") ?
                        SwrDataLoggerInfo.SWR_dataLoggerStateConnected :
                        SwrDataLoggerInfo.SWR_dataLoggerStateDisconnected;
                BleDeviceInfo deviceInfo = findBleDeviceInfoByMacAddress(deviceMac);
                if (deviceInfo == null) {
                    deviceInfo = new BleDeviceInfo(deviceName, deviceMac, statusCode);
                    bleDeviceInfos.add(deviceInfo);
                } else {
                    deviceInfo.deviceName = deviceName;
                    deviceInfo.deviceMac = deviceMac;
                    deviceInfo.status = statusCode;
                }
                if (status.equals("connected")) {
                    boolean subscribeResult = subscribeBleData(deviceInfo);
                    if (subscribeResult) {
                        callbackContext.success("success");
                    } else {
                        callbackContext.error("failed to subscribe ble data");
                    }
                } else {
                    BleDeviceInfo finalDeviceInfo = deviceInfo;
                    new Timeout(() -> swrUpdateDevice(finalDeviceInfo), 300);
                    callbackContext.success("success");
                }

                return true;
            case "swrGetSensorInfo":
                swrGetSensorInfo();
                pluginResult = new PluginResult(PluginResult.Status.OK, "success");
                callbackContext.sendPluginResult(pluginResult);
                return true;
            case "swrStartSingleCollect":
                swrStartSingleCollect();
                pluginResult = new PluginResult(PluginResult.Status.OK, "success");
                callbackContext.sendPluginResult(pluginResult);
                return true;
            case "swrStartContinuouslyCollect":
                swrStartContinuouslyCollect();
                pluginResult = new PluginResult(PluginResult.Status.OK, "success");
                callbackContext.sendPluginResult(pluginResult);
                return true;
            case "swrStopCollect":
                swrStopCollect();
                pluginResult = new PluginResult(PluginResult.Status.OK, "success");
                callbackContext.sendPluginResult(pluginResult);
                return true;

            case "swrSetCollectInterval":
                Integer interval = getIntegerArgAtIndex(args, 0);
                if (interval == null) {
                    pluginResult = new PluginResult(PluginResult.Status.ERROR, "missing arg interval");
                    callbackContext.sendPluginResult(pluginResult);
                    return true;
                }
                if (interval >= 20 || interval < 5) {
                    pluginResult = new PluginResult(PluginResult.Status.ERROR, "interval must be between 5 and 19");
                    callbackContext.sendPluginResult(pluginResult);
                    return true;
                }
                swrSetCollectInterval(interval);
                pluginResult = new PluginResult(PluginResult.Status.OK, "success");
                callbackContext.sendPluginResult(pluginResult);
                return true;
            case "swrCalibrateSensor":
                return calibrateSensorAction(args, callbackContext);
            case "swrDemarcateSensor":
                return demarcateSensorAction(args, callbackContext);
        }
        return false;
    }

    private boolean calibrateSensorAction(JSONArray args, CallbackContext callbackContext) {
        PluginResult pluginResult;
        String errMsg = "";
        JSONObject argObj = getArgsObject(args);
        if (argObj == null) {
            pluginResult = new PluginResult(PluginResult.Status.ERROR, "missing arg");
            callbackContext.sendPluginResult(pluginResult);
            return true;
        }
        Integer handleId = getIntergerField(argObj, "handleId");
        long[] adValueArr = getAdValueArrayField(argObj, "adValueArray");
        Double currentValue = getDoubleField(argObj, "currentValue");
        Double calibrateValue = getDoubleField(argObj, "calibrateValue");
        if (handleId == null) {
            errMsg = "missing arg handleId";
        } else if (adValueArr == null) {
            errMsg = "missing/incorrect arg adValueArray";
        } else if (currentValue == null) {
            errMsg = "missing arg currentValue";
        } else if (calibrateValue == null) {
            errMsg = "missing arg calibrateValue";
        }
        if (!errMsg.isEmpty()) {
            pluginResult = new PluginResult(PluginResult.Status.ERROR, errMsg);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        }
        swrCalibrateSensor(new SwrCalibrationInfo(handleId, adValueArr, currentValue, calibrateValue));
        pluginResult = new PluginResult(PluginResult.Status.OK, "success");
        callbackContext.sendPluginResult(pluginResult);
        return true;
    }

    private boolean demarcateSensorAction(JSONArray args, CallbackContext callbackContext) {
        PluginResult pluginResult;
        String errMsg = "";
        JSONObject argObj = getArgsObject(args);
        if (argObj == null) {
            pluginResult = new PluginResult(PluginResult.Status.ERROR, "missing arg");
            callbackContext.sendPluginResult(pluginResult);
            return true;
        }
        Integer handleId = getIntergerField(argObj, "handleId");
        Integer offset = getIntergerField(argObj, "offset");
        Long lowAD = getLongField(argObj, "lowAD");
        Long highAD = getLongField(argObj, "highAD");
        Double lowValue = getDoubleField(argObj, "lowValue");
        Double highValue = getDoubleField(argObj, "highValue");

        if (handleId == null) {
            errMsg = "missing arg handleId";
        } else if (offset == null) {
            errMsg = "missing arg offset";
        } else if (lowAD == null) {
            errMsg = "missing arg lowAD";
        } else if (highAD == null) {
            errMsg = "missing arg highAD";
        } else if (lowValue == null) {
            errMsg = "missing arg lowValue";
        } else if (highValue == null) {
            errMsg = "missing arg highValue";
        }

        if (!errMsg.isEmpty()) {
            pluginResult = new PluginResult(PluginResult.Status.ERROR, errMsg);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        }
        swrDemarcateSensor(new SwrDemarcateInfo(handleId, offset, lowAD, highAD, lowValue, highValue));
        pluginResult = new PluginResult(PluginResult.Status.OK, "success");
        callbackContext.sendPluginResult(pluginResult);
        return true;
    }

    /**
     * 苏威尔 cmdTransferCallback 发送过来的数据直接发送给蓝牙
     */
    public void sendBleData(String macAddress, byte[] data) {
        writeBleData(macAddress, data);
    }

    public void sendSensorInfo(SwrSensorInfo info) {
        LOG.i(LOG_TAG, "sendSensorInfo ");
        if (sendSensorInfoCallbackContext == null) return;
        JSONObject sensorInfo = info.toJSONObject();
        JSONObject returnObj = new JSONObject();
        Utils.addJSONObjectProperty(returnObj, "status", "success");
        Utils.addJSONObjectProperty(returnObj, "data", sensorInfo);
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
        pluginResult.setKeepCallback(true);
        sendSensorInfoCallbackContext.sendPluginResult(pluginResult);
    }

    /**
     * 发送获取传感器数据命令后，传感器通过蓝牙发过来的数据在蓝牙的 subscribe 回调(onCordovaCallback)中调用
     * swrAppendDeviceData 将数据发送给苏威尔库，然后苏威尔库调用本方法发送解析以后的传感器数值
     * @param data
     */
    public void sendSensorData(SwrDataLoggerDataInfo data) {
        LOG.i(LOG_TAG, "sendSensorData ");
        // 每收到一条数据即发送给 js 端
        sendCallbackResult(sendSensorDataCallbackContext, "success", data.toJSONObject(), true);
    }

    private void sendCallbackResult(CallbackContext callbackContext, String status, Object data, boolean keepCallback) {
        JSONObject returnObj = new JSONObject();
        Utils.addJSONObjectProperty(returnObj, "status", status);
        Utils.addJSONObjectProperty(returnObj, "data", data);
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
        pluginResult.setKeepCallback(keepCallback);
        callbackContext.sendPluginResult(pluginResult);
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

    private Integer getIntegerArgAtIndex(JSONArray args, Integer index) {
        try {
            return args.getInt(index);
        } catch (JSONException ex) {
        }
        return null;
    }

    private Long getLongArgAtIndex(JSONArray args, Integer index) {
        try {
            return args.getLong(index);
        } catch (JSONException ex) {}
        return null;
    }


    private Integer getIntergerField(JSONObject obj, String field) {
        try {
            return obj.getInt(field);
        } catch (JSONException ex) {}
        return null;
    }

    private Long getLongField(JSONObject obj, String field) {
        try {
            return obj.getLong(field);
        } catch (JSONException ex) {}
        return null;
    }

    private Double getDoubleField(JSONObject obj, String field) {
        try {
            return obj.getDouble(field);
        } catch (JSONException ex) {}
        return null;
    }

    private long[] getAdValueArrayField(JSONObject obj, String field) {
        JSONArray arrayObj = null;
        try {
            arrayObj = obj.getJSONArray(field);
        } catch (JSONException ex) {}
        if (arrayObj == null) return null;
        long[] adValueArr = {0, 0, 0, 0, 0};
        Long adValue = null;
        for (int i = 0; i < 5; i++) {
            adValue = getLongArgAtIndex(arrayObj, i);
            if (adValue == null) return null;
            adValueArr[i] = adValue;
        }
        return adValueArr;
    }

    public native String stringFromJNI();

    public native String initCallbackObj();

    public native void swrReleaseAPI();

    public native void swrUpdateDevice(BleDeviceInfo deviceInfo);

    public native void swrAppendDeviceData(SwrReadDataFlow data);

    public native void swrSetCollectInterval(int interval);

    public native void swrGetSensorInfo();

    public native void swrStartSingleCollect();

    public native void swrStartContinuouslyCollect();

    public native void swrStopCollect();

    public native void swrCalibrateSensor(SwrCalibrationInfo info);

    public native void swrDemarcateSensor(SwrDemarcateInfo info);
}
