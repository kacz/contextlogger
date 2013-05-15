LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := timesource
LOCAL_SRC_FILES := timesource.c
LOCAL_ARM_MODE  := arm

include $(BUILD_SHARED_LIBRARY)
