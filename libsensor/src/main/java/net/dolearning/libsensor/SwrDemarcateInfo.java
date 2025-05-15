package net.dolearning.libsensor;

public class SwrDemarcateInfo {
    int m_HandleID;           //Sensor Handle ID
    int m_Offset;               //offset AD Value
    long m_LowAD;                //low-end AD Value
    long m_HighAD;               //high-end AD Value
    double m_LowValue;            //low-end Value
    double m_HighValue;           //high-end Value

    SwrDemarcateInfo(
            int handleId,
            int offset,
            long lowAD,
            long highAD,
            double lowValue,
            double highValue
    ) {
        m_HandleID = handleId;
        m_Offset = offset;
        m_LowAD = lowAD;
        m_HighAD = highAD;
        m_LowValue = lowValue;
        m_HighValue = highValue;
    }
}
