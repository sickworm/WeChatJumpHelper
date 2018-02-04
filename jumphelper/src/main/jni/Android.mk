LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include ${LOCAL_PATH}/opencv/jni/OpenCV.mk

LOCAL_SRC_FILES  := JumpCVDetectorJni.cpp JumpCV.cpp Graph.cpp NativeMatJni.cpp Utils.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_LDLIBS     += -llog -ldl -ljnigraphics

LOCAL_MODULE     := jump-cv


include $(BUILD_SHARED_LIBRARY)
