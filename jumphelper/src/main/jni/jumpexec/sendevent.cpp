//
// Created by sickworm on 2018/2/13.
//

#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <stdio.h>
#include <linux/input.h>

#include "sendevent.h"

static int fd;

int open_device(const char *device_path) {
    int version;
    fd = open(device_path, O_RDWR);
    if (fd < 0) {
        printf("could not open %s, %s\n", device_path, strerror(errno));
        return 1;
    }
    if (ioctl(fd, EVIOCGVERSION, &version)) {
        printf("could not get driver version for %s, %s\n", device_path, strerror(errno));
        return 1;
    }
    return 0;
}

int send_event(__u16 type, __u16 code, __s32 value) {
    ssize_t ret;
    struct input_event event;

    if (fd < 0) {
        printf("device not opened\n");
        return 1;
    }

    memset(&event, 0, sizeof(event));
    event.type = type;
    event.code = code;
    event.value = value;
    ret = write(fd, &event, sizeof(event));
    if (ret < (ssize_t) sizeof(event)) {
        printf("write event failed, %s\n", strerror(errno));
        return -1;
    }
    return 0;
}