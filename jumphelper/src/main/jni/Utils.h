//
// Created by sickworm on 2018/2/3.
//

#ifndef WECHATJUMPHELPER_UTILS_H_H
#define WECHATJUMPHELPER_UTILS_H_H

#include <jni.h>
#include <android/log.h>

//#define OPENCV_DEBUG
#ifdef OPENCV_DEBUG
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,  "OpenCV", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,  "OpenCV", __VA_ARGS__)
#else
#define LOGD(...)
#define LOGE(...)
#endif



#ifdef __cplusplus
extern "C" {
#endif

void bitmapToMat2(JNIEnv *env, jclass, jobject bitmap, jlong m_addr, jboolean needUnPremultiplyAlpha);
void matToBitmap2(JNIEnv *env, jclass, jlong m_addr, jobject bitmap, jboolean needPremultiplyAlpha);


#ifdef __cplusplus
}
#endif

#endif //WECHATJUMPHELPER_UTILS_H_H