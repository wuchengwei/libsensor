package net.dolearning.libsensor;

public class SwrDataLoggerInfo {
    public static final int SWR_USBCONNECT = 0; // USB
    public static final int SWR_BLECONNECT = 1; // bluetooth
    public static final int SWR_dataLoggerStateDisconnected = 0; // Disconnect the connection
    public static final int SWR_dataLoggerStateConnected = 1; // Connected already
    public String m_Name;
    public String m_MACAdress;
    public int m_ConnectType;
    public int m_ConnectState;

    SwrDataLoggerInfo(
            String name,
            String mac,
            int connectType,
            int connectState
    ) {
        m_Name = name;
        m_MACAdress = mac;
        m_ConnectType = connectType;
        m_ConnectState = connectState;
    }
}
