//
// Created by sickworm on 2018/2/3.
//

#include <jni.h>
#include "JumpCV.h"

#include <opencv2/opencv.hpp>

#include "Utils.h"

#define FUN(x) Java_com_sickworm_wechat_graph_NativeMat_##x

#ifdef __cplusplus
extern "C"
{
#endif

jlong FUN(newMat)(JNIEnv */*env*/, jclass /*type*/) {
    return (jlong) new cv::Mat();
}

void FUN(releaseMat)(JNIEnv */*env*/, jclass /*type*/, jlong mat) {
    delete((cv::Mat *)mat);
}

void FUN(bitmapToMat)(JNIEnv *env, jclass type, jobject bitmap, jlong mat) {
    bitmapToMat2(env, type, bitmap, mat, JNI_FALSE);
}

void FUN(matToBitmap)(JNIEnv *env, jclass type, jlong mat, jobject bitmap) {
    matToBitmap2(env, type, mat, bitmap, JNI_FALSE);
}

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,  "OpenCV", __VA_ARGS__)
void FUN(matROI)(JNIEnv */*env*/, jclass /*type*/,jlong originMat, jlong roiMat,
                 jint x, jint y, jint width, jint height) {
    cv::Mat *oldMat = (cv::Mat *)originMat;
    cv::Mat *newMat = (cv::Mat *)roiMat;
    if (newMat->cols != width || newMat->rows != height || newMat->type() != oldMat->type()) {
        newMat->create(width, height, oldMat->type());
    }
    (*oldMat)(cv::Rect(x, y, width, height)).copyTo(*newMat);
}

jint FUN(width)(JNIEnv */*env*/, jclass /*type*/, jlong mat) {
    return ((cv::Mat *)mat)->cols;
}

jint FUN(height)(JNIEnv */*env*/, jclass /*type*/, jlong mat) {
    return ((cv::Mat *)mat)->rows;
}

#ifdef  __cplusplus
}
#endif