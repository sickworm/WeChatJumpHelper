//
// Created by chope on 2018/2/13.
//

#ifndef WECHATJUMPHELPER_COMMON_H
#define WECHATJUMPHELPER_COMMON_H

#include <android/log.h>

#define IN
#define OUT
#define IN_OUT

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "JumpCV", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,  "JumpCV", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  "JumpCV", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  "JumpCV", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "JumpCV", __VA_ARGS__)

#endif //WECHATJUMPHELPER_COMMON_H
