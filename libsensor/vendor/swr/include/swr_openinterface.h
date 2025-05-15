#ifndef SWR_OPENINTERFACE_H
#define SWR_OPENINTERFACE_H

#include "SWR_OpenInterface_global.h"


extern "C"{
/*******************************调用接口**************************/
//Initialize Library
SWR_OPENINTERFACE_EXPORT void initAPI();
//Release Library
SWR_OPENINTERFACE_EXPORT void releaseAPI();
//Add collector data
SWR_OPENINTERFACE_EXPORT void appendDeviceData(SWR_R_DATA_FLOW_C data);
//Update Collector
SWR_OPENINTERFACE_EXPORT void updateDevice(SWR_DataLoggerInfo_C info);
//Start continuous collection
SWR_OPENINTERFACE_EXPORT void startContinuouslyCollect();
//Stop collecting
SWR_OPENINTERFACE_EXPORT void stopCollect();
//Start single collection
SWR_OPENINTERFACE_EXPORT void startSingleCollect();
//Set collection interval
SWR_OPENINTERFACE_EXPORT void setCollectInterval(SWR_INTERVAL_C interval);
//Obtain sensor information
SWR_OPENINTERFACE_EXPORT void getSensorInfo();
//Demarcate Sensor
SWR_OPENINTERFACE_EXPORT void DemarcateSensor(SWR_DemarcateInfo_C info);
//Calibration Sensor
SWR_OPENINTERFACE_EXPORT void CalibrationSensor(SWR_CalibrationInfo_C info);


//

/*******************************设置回调**************************/
//Register command transmission callback
SWR_OPENINTERFACE_EXPORT void registerCMDTransferCall(CMDTransferCallback call);
//Register sensor update callback
SWR_OPENINTERFACE_EXPORT void registerSensorUpdateCall(SensorInfoCallback call);
//Registration data update callback
SWR_OPENINTERFACE_EXPORT void registerDataUpdateCall(DataLoggerDataCallback call);
//
//SWR_OPENINTERFACE_EXPORT void registerLogCall(LOGCallback call);



}

#endif // SWR_OPENINTERFACE_H
