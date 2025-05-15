package net.dolearning.libsensor;

import org.json.JSONObject;

public class SwrSensorInfo {
    public String m_DeviceMac;
    public int m_Channel;
    public int m_HandleID;
    public String m_Name;
    public String m_Unit;
    public double m_MinValue;
    public double m_MaxValue;
    public int m_Decimal;

    public SwrSensorInfo(
            String deviceMac,
            int chanel,
            int handleId,
            String name,
            String unit,
            double minValue,
            double maxValue,
            int decimal
    ) {
        m_DeviceMac = deviceMac;
        m_Channel = chanel;
        m_HandleID = handleId;
        m_Name = name;
        m_Unit = unit;
        m_MinValue = minValue;
        m_MaxValue = maxValue;
        m_Decimal = decimal;
    }

    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        Utils.addJSONObjectProperty(obj, "mac", m_DeviceMac);
        Utils.addJSONObjectProperty(obj, "chanel", m_Channel);
        Utils.addJSONObjectProperty(obj, "handleId", m_HandleID);
        Utils.addJSONObjectProperty(obj, "name", m_Name);
        Utils.addJSONObjectProperty(obj, "unit", m_Unit);
        Utils.addJSONObjectProperty(obj, "minValue", m_MinValue);
        Utils.addJSONObjectProperty(obj, "maxValue", m_MaxValue);
        Utils.addJSONObjectProperty(obj, "decimal", m_Decimal);
        return obj;
    }
}
