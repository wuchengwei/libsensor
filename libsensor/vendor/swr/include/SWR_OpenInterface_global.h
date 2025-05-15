#ifndef SWR_OPENINTERFACE_GLOBAL_H
#define SWR_OPENINTERFACE_GLOBAL_H
;
#pragma pack(push,1)
#if defined(_MSC_VER) || defined(WIN64) || defined(_WIN64) || defined(__WIN64__) || defined(WIN32) || defined(_WIN32) || defined(__WIN32__) || defined(__NT__)
#  define Q_DECL_EXPORT __declspec(dllexport)
#  define Q_DECL_IMPORT __declspec(dllimport)
#else
#  define Q_DECL_EXPORT     __attribute__((visibility("default")))
#  define Q_DECL_IMPORT     __attribute__((visibility("default")))
#endif

#if defined(SWR_OPENINTERFACE_LIBRARY)
#  define SWR_OPENINTERFACE_EXPORT Q_DECL_EXPORT
#else
#  define SWR_OPENINTERFACE_EXPORT Q_DECL_IMPORT
#endif

#include <cstdio>
#include <stdbool.h>
#include <stdio.h>
typedef signed char int8;         /* 8 bit signed */
typedef unsigned char uint8;      /* 8 bit unsigned */
typedef short int16;              /* 16 bit signed */
typedef unsigned short uint16;    /* 16 bit unsigned */
typedef long long int64;
typedef int int32;
typedef unsigned long long uint64;
typedef unsigned int uint32;
typedef unsigned char uchar;

//Collection interval
typedef enum{
    SWR_5MS = 2,
    SWR_10MS,
    SWR_20MS,
    SWR_50MS,
    SWR_100MS,
    SWR_200MS,
    SWR_500MS,
    SWR_1S,
    SWR_2S,
    SWR_5S,
    SWR_10S,
    SWR_20S,
    SWR_1MIN,
    SWR_2MIN,
    SWR_5MIN,
    SWR_10MIN,
    SWR_20MIN,
    SWR_1HOUR,
    SWR_1MS,
}SWR_INTERVAL_C;

//Collector connection type
typedef enum{
    SWR_USBCONNECT = 0,//USB
    SWR_BLECONNECT,    //bluetooth
}SWR_CONNECTTYPE_C;

//Collector connection status
typedef enum{
    SWR_dataLoggerStateDisconnected = 0,    //Disconnect the connection
    SWR_dataLoggerStateConnected,           //Connected already
}SWR_CONNECTSTATE_C;

//DataLogger Info
typedef struct {
    char m_Name[100];             //device name
    char m_MACAdress[20];        //device MAC
    SWR_CONNECTTYPE_C m_ConnectType;      //Device connection type
    SWR_CONNECTSTATE_C m_ConnectState;      //Connection status
}SWR_DataLoggerInfo_C;

//DataLogger Data Info
typedef struct{
    bool m_IsSingleCollect;     // Is it a single data collection
    int32 m_HandleID;           //Sensor Handle ID
    double m_Value;             //Sensor Data Value
    bool m_Upside;              //rising edge flg；The use of photoelectric door sensors
    int32 m_ADValueArray[5];    //Sensor AD Value List
    int64 m_Index;              //Sensor Data Index
    double m_GDMTime;           //photogate Sensor Time Value；The use of photoelectric door sensors
}SWR_DataLoggerDataInfo_C;

typedef struct{
    char m_DeviceMac[20];       //device MACAddress
    int32 m_Channel;            //sensor channel
    int32 m_HandleID;           //Sensor Handle ID
    char m_Name[100];           //Sensor Name
    char m_Unit[100];           //Sensor Unit
    double m_MinValue;          //Minimum value of sensor
    double m_MaxValue;          //Maximum sensor value
    int32 m_Decimal;            //Sensor data display decimal places
}SWR_SensorInfo_C;

typedef struct{
    SWR_DataLoggerInfo_C m_dataLoggerInfo;  //DataLogger Info
    char* m_dataPacket;                     //Data Packet
    int64 m_dataSize;                       //Packet Length
} SWR_R_DATA_FLOW_C;

//标定信息
typedef struct{
    int32 m_HandleID;           //Sensor Handle ID
    int32 m_Offset;               //offset AD Value
    int64 m_LowAD;                //low-end AD Value
    int64 m_HighAD;               //high-end AD Value
    double m_LowValue;            //low-end Value
    double m_HighValue;           //high-end Value
}SWR_DemarcateInfo_C;

//校准信息
typedef struct {
    int32 m_HandleID;       //Sensor Handle ID
    int64 m_ADValueList[5];   //AD Value List
    double m_CurrentValue;    //current Value
    double m_CalibrateValue;  //calibrate Value
}SWR_CalibrationInfo_C;


/**********************************
* @brief    Sensor status callback
* @param    rsInfoMap:Sensor Info List；lenth：Sensor Info list length；
* @return   void
************************************/
typedef void(*SensorInfoCallback)(SWR_SensorInfo_C *rsInfoList,uint64 lenth);
/**********************************
* @brief    Data callback
* @param    dataInfoMap:Data List；len：Data list length；
* @return   void
************************************/
typedef void(*DataLoggerDataCallback)(SWR_DataLoggerDataInfo_C *dataInfoList,uint64 lenth);
/**********************************
* @brief    CMD Transfer Callback
* @param    deviceMac:device Macaddress ；data:Data to be sent;lenth:Data length；
* @return   void
************************************/
typedef void(*CMDTransferCallback)(char deviceMac[20],char *data,uint64 lenth);
//
//typedef void(*LOGCallback)(const char *data,uint64 len);

#pragma pack (pop)


#endif // SWR_OPENINTERFACE_GLOBAL_H
