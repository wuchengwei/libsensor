package net.dolearning.libsensor;

import org.json.JSONArray;
import org.json.JSONObject;

public class SwrDataLoggerDataInfo {
    boolean m_IsSingleCollect;     // Is it a single data collection
    int m_HandleID;           // Sensor Handle ID
    double m_Value;             // Sensor Data Value
    boolean m_Upside;              // rising edge flg；The use of photoelectric door sensors
    int[] m_ADValueArray;    // Sensor AD Value List
    long m_Index;              // Sensor Data Index
    double m_GDMTime;           // photogate Sensor Time Value；The use of photoelectric door sensors

    public SwrDataLoggerDataInfo(
            boolean isSingleCollect,
            int handleId,
            double value,
            boolean upside,
            int[] adValueArray,
            long index,
            double gdmTime
    ) {
        m_IsSingleCollect = isSingleCollect;
        m_HandleID = handleId;
        m_Value = value;
        m_Upside = upside;
        m_ADValueArray = adValueArray;
        m_Index = index;
        m_GDMTime = gdmTime;
    }

    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        JSONArray adValueList = new JSONArray();
        for (int j : m_ADValueArray) {
            adValueList.put(j);
        }
        Utils.addJSONObjectProperty(obj, "isSingleCollect", m_IsSingleCollect);
        Utils.addJSONObjectProperty(obj, "handleId", m_HandleID);
        Utils.addJSONObjectProperty(obj, "value", m_Value);
        Utils.addJSONObjectProperty(obj, "upside", m_Upside);
        Utils.addJSONObjectProperty(obj, "adValueArray", adValueList);
        Utils.addJSONObjectProperty(obj, "index", m_Index);
        Utils.addJSONObjectProperty(obj, "gdmTime", m_GDMTime);
        return obj;
    }
}
