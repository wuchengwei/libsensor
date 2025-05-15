package net.dolearning.libsensor;

public class SwrCalibrationInfo {
    int m_HandleID;
    long[] m_ADValueList;
    double m_CurrentValue;
    double m_CalibrateValue;

    SwrCalibrationInfo(
            int handleId,
            long[] adValueList,
            double currentValue,
            double calibrateValue
    ) {
        m_HandleID = handleId;
        m_ADValueList = adValueList;
        m_CurrentValue = currentValue;
        m_CalibrateValue = calibrateValue;
    }
}
