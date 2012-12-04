LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := timesource
LOCAL_SRC_FILES := timesource.c

include $(BUILD_SHARED_LIBRARY)
