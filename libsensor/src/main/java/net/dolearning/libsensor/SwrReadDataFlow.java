package net.dolearning.libsensor;

/**
 * 从传感器收到的数据流片段
 */
public class SwrReadDataFlow {
    SwrDataLoggerInfo m_dataLoggerInfo;  //DataLogger Info
    byte[] m_dataPacket;                     //Data Packet
    long m_dataSize;                       //Packet Length

    SwrReadDataFlow(
            SwrDataLoggerInfo dataLoggerInfo,
            byte[] packet,
            long size
    ) {
        m_dataLoggerInfo = dataLoggerInfo;
        m_dataPacket = packet;
        m_dataSize = size;
    }
}