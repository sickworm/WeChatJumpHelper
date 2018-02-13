//
// Created by sickworm on 2018/2/13.
//

#include <sys/types.h>

#ifndef WECHATJUMPHELPER_SENDEVNET_H_H
#define WECHATJUMPHELPER_SENDEVNET_H_H

int open_device(const char *device_path);
int send_event(__u16 type, __u16 code, __s32 value);

#endif //WECHATJUMPHELPER_SENDEVNET_H_H