#include <jni.h>

#include <string>
#include <stdio.h>

#include <android/log.h>

#include "swr_openinterface.h"

/**
 * 参考资料：JNI调用Java方法签名介绍(https://blog.csdn.net/pengtgimust/article/details/82835258)
 */

jobject swrSensorCordovaPlugin = NULL;
JavaVM* gJvm;
// gClassLoader 和 gFindClassMethod 用来加载java的类（从非JNI_OnLoad的线程）
static jobject gClassLoader;
static jmethodID gFindClassMethod;

JNIEnv* getEnv() {
    JNIEnv* env;
    int status = gJvm->GetEnv((void**)&env, JNI_VERSION_1_6);
    if (status < 0) {
        status = gJvm->AttachCurrentThread(&env, NULL);
        if (status < 0) {
            return nullptr;
        }
    }
    return env;
}

// 调用此方法来加载 java 类，name的形式如 “net.dolearning.libsensor.SwrSensorInfo”
jclass findClass(const char* name) {
    JNIEnv* env = getEnv();
    return static_cast<jclass>(env->CallObjectMethod(gClassLoader, gFindClassMethod, env->NewStringUTF(name)));
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *pjvm, void *reserved) {
    gJvm = pjvm;  // cache the JavaVM pointer
    auto env = getEnv();
    auto randomClass = env->FindClass("net/dolearning/libsensor/SwrSensorInfo");
    jclass classClass = env->GetObjectClass(randomClass);
    auto classLoaderClass = env->FindClass("java/lang/ClassLoader");
    auto getClassLoaderMethod = env->GetMethodID(classClass, "getClassLoader",
                                                 "()Ljava/lang/ClassLoader;");
    gClassLoader = env->NewGlobalRef(env->CallObjectMethod(randomClass, getClassLoaderMethod));
    gFindClassMethod = env->GetMethodID(classLoaderClass, "findClass",
                                        "(Ljava/lang/String;)Ljava/lang/Class;");

    return JNI_VERSION_1_6;
}

char const hex_chars[16] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
void charsToHex(char* str, char* hex, int size) {
    int i = 0;
    for (i = 0; i < size; i++) {
        char byte = str[i];
        hex[i * 2] = hex_chars[(byte & 0xF0) >> 4];
        hex[i * 2 + 1] = hex_chars[(byte & 0x0F) >> 0];
    }
    hex[i * 2] = '\0';
}

void cmdTransferCallback(char deviceMac[20], char *data, uint64 len) {
    // send bluetooth data
    if (swrSensorCordovaPlugin == NULL) return;
    JNIEnv* env = getEnv();
    if (env == nullptr) return;

    int size = 0;
    for(; data[size] != '\0'; size++);

    char* hexData = static_cast<char *>(malloc(size * 2 + 1));
    charsToHex(data, hexData, size);
    __android_log_print(ANDROID_LOG_INFO, "NativeSensorLib", "cmdTransferCallback data %s", hexData);
    __android_log_print(ANDROID_LOG_INFO, "NativeSensorLib", "cmdTransferCallback len %llu, size %d", len, size);
    free(hexData);

    // 获取 Java 对象的类
    jclass clazz = env->GetObjectClass(swrSensorCordovaPlugin);
    jstring jDeviceMac = env->NewStringUTF(deviceMac);
    jbyteArray jBytes = env->NewByteArray(len);
    env->SetByteArrayRegion(jBytes, 0, len, reinterpret_cast<const jbyte *>(data));
    jmethodID methodId = env->GetMethodID(clazz, "sendBleData", "(Ljava/lang/String;[B)V");
    env->CallVoidMethod(swrSensorCordovaPlugin, methodId, jDeviceMac, jBytes);
}

void sensorUpdateCallback(SWR_SensorInfo_C *info, uint64 len) {
    if (swrSensorCordovaPlugin == NULL) return;
    JNIEnv* env = getEnv();
    if (env == nullptr) return;

    __android_log_print(ANDROID_LOG_INFO, "NativeSensorLib", "sensorUpdateCallback");
    __android_log_print(ANDROID_LOG_INFO, "NativeSensorLib", "info->m_DeviceMac: %s", info->m_DeviceMac);

    jclass sensorInfoClass = findClass("net.dolearning.libsensor.SwrSensorInfo");
    __android_log_print(ANDROID_LOG_INFO, "NativeSensorLib", "info->m_Name: %s", info->m_Name);
    jmethodID sensorInfoConstructor = env->GetMethodID(
        sensorInfoClass,
        "<init>",
        "(Ljava/lang/String;IILjava/lang/String;Ljava/lang/String;DDI)V"
    );
    jstring deviceMac = env->NewStringUTF(info->m_DeviceMac);
    jstring deviceName = env->NewStringUTF(info->m_Name);
    jstring deviceUnit = env->NewStringUTF(info->m_Unit);
    jobject sensorInfo = env->NewObject(
        sensorInfoClass,
        sensorInfoConstructor,
        deviceMac,
        info->m_Channel,
        info->m_HandleID,
        deviceName,
        deviceUnit,
        info->m_MinValue,
        info->m_MaxValue,
        info->m_Decimal
    );
    jclass clazz = env->GetObjectClass(swrSensorCordovaPlugin);
    jmethodID methodId = env->GetMethodID(
        clazz,
        "sendSensorInfo",
        "(Lnet/dolearning/libsensor/SwrSensorInfo;)V"
    );
    env->CallVoidMethod(swrSensorCordovaPlugin, methodId, sensorInfo);
}

void dataUpdateCallback(SWR_DataLoggerDataInfo_C *data, uint64 len) {
    if (swrSensorCordovaPlugin == NULL) return;
    JNIEnv* env = getEnv();
    if (env == nullptr) return;

    __android_log_print(ANDROID_LOG_INFO, "NativeSensorLib", "dataUpdateCallback");

    jclass sensorDataClass = findClass("net.dolearning.libsensor.SwrDataLoggerDataInfo");
    jmethodID sensorInfoConstructor = env->GetMethodID(
        sensorDataClass,
        "<init>",
        "(ZIDZ[IJD)V"
    );
    int adValueArrayLength = sizeof(data->m_ADValueArray) / sizeof(*(data->m_ADValueArray));
    jintArray jADValueArray = env->NewIntArray(adValueArrayLength);
    env->SetIntArrayRegion(jADValueArray, 0, adValueArrayLength, data->m_ADValueArray);
    jobject sensorData = env->NewObject(
        sensorDataClass,
        sensorInfoConstructor,
        data->m_IsSingleCollect,
        data->m_HandleID,
        data->m_Value,
        data->m_Upside,
        jADValueArray,
        data->m_Index,
        data->m_GDMTime
    );
    jclass clazz = env->GetObjectClass(swrSensorCordovaPlugin);
    jmethodID methodId = env->GetMethodID(
        clazz,
        "sendSensorData",
        "(Lnet/dolearning/libsensor/SwrDataLoggerDataInfo;)V"
    );
    env->CallVoidMethod(swrSensorCordovaPlugin, methodId, sensorData);
}

extern "C" {

JNIEXPORT void JNICALL
Java_net_dolearning_libsensor_DoCordovaActivity_swrInit(
    JNIEnv* env,
    jobject
) {
//    env->GetJavaVM(&theJvm);
    initAPI();
    registerCMDTransferCall(cmdTransferCallback);
    registerSensorUpdateCall(sensorUpdateCallback);
    registerDataUpdateCall(dataUpdateCallback);
    __android_log_print(ANDROID_LOG_INFO, "NativeSensorLib", "swrInit");
}

JNIEXPORT void JNICALL
Java_net_dolearning_libsensor_DoCordovaPluginSwrSensor_swrReleaseAPI(
        JNIEnv* env,
        jobject
) {
    releaseAPI();
    __android_log_print(ANDROID_LOG_INFO, "NativeSensorLib", "releaseAPI");
}

JNIEXPORT jstring JNICALL
Java_net_dolearning_libsensor_DoCordovaPluginSwrSensor_stringFromJNI(
    JNIEnv *env,
    jobject instance
) {
    std::string hello = "Hello DoCordovaPluginSwrSensor from JNI.";
    return env->NewStringUTF(hello.c_str());
}

JNIEXPORT jstring JNICALL
Java_net_dolearning_libsensor_DoCordovaPluginSwrSensor_initCallbackObj(
    JNIEnv *env,
    jobject instance
) {
    swrSensorCordovaPlugin = env->NewGlobalRef(instance);
    std::string hello = "initCallbackObj";
    __android_log_print(ANDROID_LOG_INFO, "NativeSensorLib", "initCallbackObj");
    return env->NewStringUTF(hello.c_str());
}

JNIEXPORT void JNICALL
Java_net_dolearning_libsensor_DoCordovaPluginSwrSensor_swrGetSensorInfo(
    JNIEnv * env ,
    jobject
) {
    __android_log_print(ANDROID_LOG_INFO, "NativeSensorLib", "getSensorInfo");
    getSensorInfo();
}

JNIEXPORT void JNICALL
Java_net_dolearning_libsensor_DoCordovaPluginSwrSensor_swrStartSingleCollect(
    JNIEnv * env ,
    jobject
) {
    __android_log_print(ANDROID_LOG_INFO, "NativeSensorLib", "startSingleCollect");
    startSingleCollect();
}

JNIEXPORT void JNICALL
Java_net_dolearning_libsensor_DoCordovaPluginSwrSensor_swrStartContinuouslyCollect(
    JNIEnv * env ,
    jobject
) {
    __android_log_print(ANDROID_LOG_INFO, "NativeSensorLib", "startContinuouslyCollect");
    startContinuouslyCollect();
}

JNIEXPORT void JNICALL
Java_net_dolearning_libsensor_DoCordovaPluginSwrSensor_swrStopCollect(
    JNIEnv * env ,
    jobject
) {
    __android_log_print(ANDROID_LOG_INFO, "NativeSensorLib", "stopCollect");
    stopCollect();
}

JNIEXPORT void JNICALL
Java_net_dolearning_libsensor_DoCordovaPluginSwrSensor_swrAppendDeviceData(
    JNIEnv * env,
    jobject,
    jobject swrDataFlow
) {
    SWR_R_DATA_FLOW_C data;
    data.m_dataLoggerInfo.m_ConnectState = SWR_dataLoggerStateConnected;
    data.m_dataLoggerInfo.m_ConnectType = SWR_BLECONNECT;

    jfieldID fId;
    jclass dataFlowClass = env->GetObjectClass(swrDataFlow);

    fId = env->GetFieldID(dataFlowClass, "m_dataLoggerInfo", "Lnet/dolearning/libsensor/SwrDataLoggerInfo;");
    jobject dataLoggerInfo = (jobject) env->GetObjectField(swrDataFlow, fId);
    jclass dataLoggerInfoClass = env->GetObjectClass(dataLoggerInfo);

    fId = env->GetFieldID(dataLoggerInfoClass, "m_Name", "Ljava/lang/String;");
    jstring jDeviceName = (jstring) env->GetObjectField(dataLoggerInfo, fId);
    const char* deviceName = env->GetStringUTFChars(jDeviceName, NULL);
    if (deviceName == NULL) return;

    fId = env->GetFieldID(dataLoggerInfoClass, "m_MACAdress", "Ljava/lang/String;");
    jstring jMacAddress = (jstring) env->GetObjectField(dataLoggerInfo, fId);
    const char* macAddress = env->GetStringUTFChars(jMacAddress, NULL);
    if (macAddress == NULL) {
        env->ReleaseStringUTFChars(jDeviceName, deviceName);
        return;
    }

    fId = env->GetFieldID(dataFlowClass, "m_dataPacket", "[B");
    jbyteArray jDataPacket = (jbyteArray) env->GetObjectField(swrDataFlow, fId);
    char* dataPacket = (char*)env->GetByteArrayElements(jDataPacket, NULL);
    if (dataPacket == NULL) {
        env->ReleaseStringUTFChars(jDeviceName, deviceName);
        env->ReleaseStringUTFChars(jMacAddress, macAddress);
        return;
    }

    fId = env->GetFieldID(dataFlowClass, "m_dataSize", "J");
    jlong jDataSize = (jint) env->GetLongField(swrDataFlow, fId);


    memset(data.m_dataLoggerInfo.m_Name, 0x00, 100);
    memset(data.m_dataLoggerInfo.m_MACAdress, 0x00, 20);
    memcpy(data.m_dataLoggerInfo.m_Name, deviceName, strlen(deviceName));
    memcpy(data.m_dataLoggerInfo.m_MACAdress, macAddress, strlen(macAddress));

    data.m_dataPacket = dataPacket;
    data.m_dataSize = jDataSize;

//    char* hexData = static_cast<char *>(malloc(jDataSize + 1));
//    charsToHex(dataPacket, hexData, jDataSize);
//    __android_log_print(ANDROID_LOG_INFO, "NativeSensorLib", "appendDeviceData data.m_dataPacket: %s", hexData);
//    free(hexData);
//    __android_log_print(ANDROID_LOG_INFO, "NativeSensorLib", "appendDeviceData data.m_dataSize: %li", jDataSize);

    appendDeviceData(data);
    env->ReleaseByteArrayElements(jDataPacket, reinterpret_cast<jbyte *>(dataPacket), JNI_ABORT);
    env->ReleaseStringUTFChars(jDeviceName, deviceName);
    env->ReleaseStringUTFChars(jMacAddress, macAddress);
}

/**
 * 蓝牙设备连接以后，调用该接口以进一步调用 swrOpenInterface 的 updateDevice 接口
 * @param env JVM
 * @param deviceInfo 蓝牙设备信息
 */
JNIEXPORT void JNICALL
Java_net_dolearning_libsensor_DoCordovaPluginSwrSensor_swrUpdateDevice(
    JNIEnv* env,
    jobject,
    jobject deviceInfo
) {
    SWR_DataLoggerInfo_C info;

    info.m_ConnectType = SWR_BLECONNECT;

    // 获取 Java 对象的类
    jclass clazz = env->GetObjectClass(deviceInfo);
    jfieldID fId;

    fId = env->GetFieldID(clazz, "status", "I");
    jint jStatus = env->GetIntField(deviceInfo, fId);
    info.m_ConnectState = static_cast<SWR_CONNECTSTATE_C>(jStatus);

    jstring jDeviceName;
    const char* deviceName;
    // 获取 deviceName 字段的 id
    fId = env->GetFieldID(clazz, "deviceName", "Ljava/lang/String;");
    // 获取 deviceName 字段的值
    jDeviceName = (jstring) env->GetObjectField(deviceInfo, fId);
    // 将 Java 的字符串转换为 Native 的字符串
    deviceName = env->GetStringUTFChars(jDeviceName, NULL);
    if (deviceName == NULL) return;

    jstring jDeviceMac;
    const char* deviceMac;
    // 获取 deviceMac 字段的 id
    fId = env->GetFieldID(clazz, "deviceMac", "Ljava/lang/String;");
    // 获取 deviceMac 字段的值
    jDeviceMac = (jstring) env->GetObjectField(deviceInfo, fId);
    // 将 Java 的字符串转换为 Native 的字符串
    deviceMac = env->GetStringUTFChars(jDeviceMac, NULL);
    if (deviceMac == NULL) {
        env->ReleaseStringUTFChars(jDeviceName, deviceName);
        return;
    }
    memset(info.m_Name,0x00,100);
    memset(info.m_MACAdress,0x00,20);
    memcpy(info.m_Name, deviceName, strlen(deviceName));
    memcpy(info.m_MACAdress, deviceMac, strlen(deviceMac));

//    __android_log_print(ANDROID_LOG_INFO, "NativeSensorLib", "updateDevice m_Name: \"%s\"", info.m_Name);
//    __android_log_print(ANDROID_LOG_INFO, "NativeSensorLib", "updateDevice m_MACAdress: \"%s\"", info.m_MACAdress);
//    __android_log_print(ANDROID_LOG_INFO, "NativeSensorLib", "updateDevice m_ConnectType: %i", info.m_ConnectType);
//    __android_log_print(ANDROID_LOG_INFO, "NativeSensorLib", "updateDevice m_ConnectState: %i", info.m_ConnectState);

    updateDevice(info);
    env->ReleaseStringUTFChars(jDeviceName, deviceName);
    env->ReleaseStringUTFChars(jDeviceMac, deviceMac);
}

JNIEXPORT void JNICALL
Java_net_dolearning_libsensor_DoCordovaPluginSwrSensor_swrSetCollectInterval(
    JNIEnv* env,
    jobject,
    jint interval
) {
    __android_log_print(ANDROID_LOG_INFO, "NativeSensorLib", "setCollectInterval %d", interval);
    setCollectInterval((SWR_INTERVAL_C)interval);
}

JNIEXPORT void JNICALL
Java_net_dolearning_libsensor_DoCordovaPluginSwrSensor_swrCalibrateSensor(
    JNIEnv* env,
    jobject,
    jobject calibrationInfo
) {
    SWR_CalibrationInfo_C info;

    // 获取 Java 对象的类
    jclass clazz = env->GetObjectClass(calibrationInfo);
    jfieldID fId;

    fId = env->GetFieldID(clazz, "m_HandleID", "I");
    info.m_HandleID = (int) env->GetIntField(calibrationInfo, fId);

    fId = env->GetFieldID(clazz, "m_ADValueList", "[J");
    jlongArray jADValueList = static_cast<jlongArray>(env->GetObjectField(calibrationInfo, fId));
    jlong* adValueArray = env->GetLongArrayElements(jADValueList, NULL);
    for (int i = 0; i < 5; i++) info.m_ADValueList[i] =  adValueArray[i];

    fId = env->GetFieldID(clazz, "m_CurrentValue", "D");
    info.m_CurrentValue = (double) env->GetDoubleField(calibrationInfo, fId);

    fId = env->GetFieldID(clazz, "m_CalibrateValue", "D");
    info.m_CalibrateValue = (double) env->GetDoubleField(calibrationInfo, fId);

    CalibrationSensor(info);

    env->ReleaseLongArrayElements(jADValueList, adValueArray, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_net_dolearning_libsensor_DoCordovaPluginSwrSensor_swrDemarcateSensor(
    JNIEnv* env,
    jobject,
    jobject demarcateInfo
) {
    SWR_DemarcateInfo_C info;

    // 获取 Java 对象的类
    jclass clazz = env->GetObjectClass(demarcateInfo);
    jfieldID fId;

    fId = env->GetFieldID(clazz, "m_HandleID", "I");
    info.m_HandleID = (int) env->GetIntField(demarcateInfo, fId);

    fId = env->GetFieldID(clazz, "m_Offset", "I");
    info.m_Offset = (int) env->GetIntField(demarcateInfo, fId);

    fId = env->GetFieldID(clazz, "m_LowAD", "J");
    info.m_LowAD = env->GetLongField(demarcateInfo, fId);

    fId = env->GetFieldID(clazz, "m_HighAD", "J");
    info.m_HighAD = env->GetLongField(demarcateInfo, fId);

    fId = env->GetFieldID(clazz, "m_LowValue", "D");
    info.m_LowValue = env->GetDoubleField(demarcateInfo, fId);

    fId = env->GetFieldID(clazz, "m_HighValue", "D");
    info.m_HighValue = env->GetDoubleField(demarcateInfo, fId);

    DemarcateSensor(info);
}

}


