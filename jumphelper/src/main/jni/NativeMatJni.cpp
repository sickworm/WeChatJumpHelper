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

jint FUN(width)(JNIEnv */*env*/, jclass /*type*/, jlong mat) {
    return ((cv::Mat *)mat)->cols;
}

jint FUN(height)(JNIEnv */*env*/, jclass /*type*/, jlong mat) {
    return ((cv::Mat *)mat)->rows;
}

#ifdef  __cplusplus
}
#endif