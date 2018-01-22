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

    jclass lineClass = env->FindClass("com/sickworm/wechat/graph/Line");
    jmethodID lineMethod = env->GetMethodID(lineClass, "<init>", "(IIII)V");

    jclass ellipseClass = env->FindClass("com/sickworm/wechat/graph/Ellipse");
    jmethodID ellipseMethod = env->GetMethodID(ellipseClass, "<init>", "(IIIIF)V");

    JumpCV *jumpCV = (JumpCV *)instance;
    std::vector<Graph *> graphs = jumpCV->getGraphs();
    for (int i = 0; i < graphs.size(); i++) {
        Graph *graph = graphs[i];
        if (graph->type == TYPE_POINT) {
            cv::Point *p = (cv::Point *)graph->objecct;
            jobject point = env->NewObject(pointClass, pointMethod, p->x, p->y);
            env->CallBooleanMethod(list, addMethod, point);
        } else if (graph->type == TYPE_LINE) {
            cv::Vec4i *p = (cv::Vec4i *)graph->objecct;
            jobject line = env->NewObject(lineClass, lineMethod, (*p)[0], (*p)[1], (*p)[2], (*p)[3]);
            env->CallBooleanMethod(list, addMethod, line);
        } else if (graph->type == TYPE_ELLIPSE) {
            cv::RotatedRect *p = (cv::RotatedRect *)graph->objecct;
            jobject ellipse = env->NewObject(ellipseClass, ellipseMethod,
                                             (int) p->center.x, (int) p->center.y,
                                             (int) p->size.width, (int) p->size.height,
                                             p->angle);
            env->CallBooleanMethod(list, addMethod, ellipse);
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