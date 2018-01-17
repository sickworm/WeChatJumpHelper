//
// Created by chope on 2018/1/10.
//

#ifndef WECHATJUMPHELPER_JUMPCV_H
#define WECHATJUMPHELPER_JUMPCV_H

#include <android/log.h>

#include <opencv2/opencv.hpp>

#define IN
#define OUT
#define IN_OUT

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "JumpCV", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,  "JumpCV", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  "JumpCV", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  "JumpCV", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "JumpCV", __VA_ARGS__)


#define DEBUG_DEST 1 << 0
#define DEBUG_ALL_DEST 1 << 1
#define DEBUG_CIRCLE 1 << 2
#define DEBUG_SQUARE 1 << 3
#define DEBUG_WHITE_POINT 1 << 4
#define DEBUG_CONTOUR 1 << 5
#define DEBUG_ALL 0xFFFFFFFF

#define DEBUG_TYPE DEBUG_ALL_DEST

class JumpCV {
private:
    int g_width;
    int g_height;
    float g_density;

    std::vector<void *> g_debugGraphs;

    cv::Mat hsv;
    cv::Mat blu;
    cv::Mat mask;
    cv::Mat binary;

    bool findWhitePoint(cv::Mat img, cv::Point &whitePoint);
    bool findPlatformCircle(cv::Mat img, cv::Point &platformPoint);
    bool findPlatformSquare(cv::Mat img, cv::Point &platformPoint);
public:
    JumpCV(int width, int height, float density);
    ~JumpCV();
    bool findChess(IN cv::Mat img, OUT cv::Point &chessPoint);
    bool findPlatform(IN cv::Mat img, OUT cv::Point &platformPoint);
    std::vector<void *> getGraphs();
    void clearGraphs();
};


#endif //WECHATJUMPHELPER_JUMPCV_H
