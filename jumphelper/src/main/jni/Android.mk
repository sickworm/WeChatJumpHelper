LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include ${LOCAL_PATH}/opencv/jni/OpenCV.mk
LOCAL_SRC_FILES  := JumpCVDetectorJni.cpp \
                    NativeMatJni.cpp \
                    JumpCV.cpp \
                    Graph.cpp \
                    Utils.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_LDLIBS     += -llog -ldl -ljnigraphics
LOCAL_MODULE     := jump-cv

include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)

LOCAL_SRC_FILES  := jumpexec/jump.cpp \
                    jumpexec/sendevent.cpp

LOCAL_C_INCLUDES += ${LOCAL_PATH}/jumpexec
LOCAL_LDLIBS     += -llog
LOCAL_MODULE     := jump

include $(BUILD_EXECUTABLE)