LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include ${LOCAL_PATH}/opencv/jni/OpenCV.mk

LOCAL_SRC_FILES  := JumpCVJni.cpp JumpCV.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_LDLIBS     += -llog -ldl

LOCAL_MODULE     := jump-cv


include $(BUILD_SHARED_LIBRARY)
