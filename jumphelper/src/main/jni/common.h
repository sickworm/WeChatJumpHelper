//
// Created by chope on 2018/2/13.
//

#ifndef WECHATJUMPHELPER_COMMON_H
#define WECHATJUMPHELPER_COMMON_H

#include <android/log.h>

#define IN
#define OUT
#define IN_OUT

#define DEBUG 1
#define INFO 2
#define WARN 3
#define ERROR 4
#define LOG_LEVEL DEBUG

#if LOG_LEVEL <= DEBUG
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,  "JumpCV", __VA_ARGS__)
#else
#define LOGD(...)
#endif

#if LOG_LEVEL <= INFO
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  "JumpCV", __VA_ARGS__)
#else
#define LOGI(...)
#endif

#if LOG_LEVEL <= WARN
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  "JumpCV", __VA_ARGS__)
#else
#define LOGW(...)
#endif

#if LOG_LEVEL <= ERROR
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "JumpCV", __VA_ARGS__)
#else
#define LOGE(...)
#endif

#endif //WECHATJUMPHELPER_COMMON_H
