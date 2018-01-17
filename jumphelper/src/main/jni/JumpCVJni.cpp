#include <jni.h>
#include "JumpCV.h"

#include <opencv2/opencv.hpp>

#define FUN(x) Java_com_sickworm_wechat_jumphelper_JumpCVDetector_##x

#ifdef __cplusplus
extern "C"
{
#endif

jobject genJavaPoint(JNIEnv *env, cv::Point point) {
    jclass clazz = env->FindClass("com/sickworm/wechat/graph/Point");
    jmethodID methodID = env->GetMethodID(clazz, "<init>", "(II)V");
    return env->NewObject(clazz, methodID, point.x, point.y);
}

jlong FUN(newInstance)(JNIEnv */*env*/, jobject /*thiz*/, jint width, jint height, jfloat density) {
    return (jlong) new JumpCV(width, height, density);
}

jlong FUN(deleteInstance)(JNIEnv */*env*/, jobject /*thiz*/, jlong instance) {
    delete((JumpCV *)instance);
}

jobject FUN(findChess)(JNIEnv* env, jobject /*thiz*/, jlong instance, jlong mat) {
    cv::Point point;
    cv::Mat img = *(cv::Mat *) mat;
    JumpCV *jumpCV = (JumpCV *) instance;
    if(jumpCV->findChess(img, point)) {
        return genJavaPoint(env, point);
    }
    return NULL;
}

jobject FUN(findPlatform)(JNIEnv* env, jobject /*thiz*/, jlong instance, jlong mat) {
    cv::Point point;
    cv::Mat img = *(cv::Mat *)mat;
    JumpCV *jumpCV = (JumpCV *)instance;
    if (jumpCV->findPlatform(img, point)) {
        return genJavaPoint(env, point);
    }
    return NULL;
}

jobject FUN(getDebugGraphs)(JNIEnv *env, jobject /*thiz*/, jlong instance) {

    jclass listClass = env->FindClass("java/util/ArrayList");
    jmethodID listMethod = env->GetMethodID(listClass, "<init>", "()V");
    jobject list = env->NewObject(listClass, listMethod);
    jmethodID addMethod = env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z");

    jclass pointClass = env->FindClass("com/sickworm/wechat/graph/Point");
    jmethodID pointMethod = env->GetMethodID(pointClass, "<init>", "(II)V");
    jobject point = env->NewObject(pointClass, pointMethod, 100, 100);
    env->CallBooleanMethod(list, addMethod, point);

    JumpCV *jumpCV = (JumpCV *)instance;
    std::vector<void *> graphs = jumpCV->getGraphs();
    for (int i = 0; i < graphs.size(); i++) {
        void *graph = graphs[i];
        if (typeid(graph) == typeid(cv::Point)) {

        }
    }
    return list;
}

void FUN(clearDebugGraphs)(JNIEnv */*env*/, jobject /*thiz*/, jlong instance) {
    JumpCV *jumpCV = (JumpCV *)instance;
    jumpCV->clearGraphs();
}

#ifdef  __cplusplus
}
#endif