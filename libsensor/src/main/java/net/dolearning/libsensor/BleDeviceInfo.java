package net.dolearning.libsensor;

public class BleDeviceInfo {
    public String deviceName;
    public String deviceMac;
    /**
     * 设备连接状态
     * SwrDataLoggerInfo.SWR_dataLoggerStateConnected
     * SwrDataLoggerInfo.SWR_dataLoggerStateDisconnected
     */
    public int status;

    public BleDeviceInfo(String deviceName, String deviceMac, int statusCode) {
        this.deviceName = deviceName;
        this.deviceMac = deviceMac;
        this.status = statusCode;
    }
}
