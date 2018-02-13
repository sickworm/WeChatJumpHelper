//
// Created by sickworm on 2018/2/13.
//
#include <jni.h>
#include <unistd.h>
#include <cstdio>
#include <cstring>
#include <cstdlib>

#include "sendevent.h"

#define FUN(x) Java_com_sickworm_wechat_jumphelper_DeviceHelper_##x

#define IF_ERROR_RETURN if (err) return err;

void wait_mill(int time_mill) {
    struct timeval delay;
    delay.tv_sec = time_mill / 1000;
    delay.tv_usec = (time_mill % 1000) * 1000;
    select(0, NULL, NULL, NULL, &delay);
}

// 适配 Nexus 5，其他设备可能不一样
int main(int argc, char *argv[]) {
    int err;
    err = open_device("/dev/input/event1");
    IF_ERROR_RETURN;

    if (argc != 2) {
        printf("invalid argument\n");
        return 1;
    }
    int time_mill = atoi(argv[1]);
    if (time_mill <= 0) {
        printf("invalid argument\n");
        return 1;
    }

    err = send_event(3, 57, 62);    // 0x39, ABS_MT_TRACKING_ID，事件列表 ID，正常值为递增，随意赋值没有实际影响
    IF_ERROR_RETURN;
    err = send_event(3, 53, 500);   // 0x35, ABS_MT_POSITION_X，x 坐标，若与上次一致则不发送
    IF_ERROR_RETURN;
    err = send_event(3, 54, 500);   // 0x36, ABS_MT_POSITION_Y，y 坐标，若与上次一致则不发送
    IF_ERROR_RETURN;
    err = send_event(3, 58, 46);    // 0x3a, ABS_MT_PRESSURE，压力值，若与上次一致则不发送
    IF_ERROR_RETURN;
    err = send_event(3, 48, 4);     // 0x30, ABS_MT_TOUCH_MAJOR，接触面积，若与上次一致则不发送
    IF_ERROR_RETURN;
    err = send_event(0, 0, 0);      // 同步此次事件
    IF_ERROR_RETURN;

    wait_mill(time_mill);

    err = send_event(3, 57, -1);    // ABS_MT_TRACKING_ID，此时为上一次按键的结束，值为 0xfffffff
    IF_ERROR_RETURN;
    err = send_event(0, 0, 0);      // 同步此次事件
    IF_ERROR_RETURN;

    return JNI_TRUE;
}
