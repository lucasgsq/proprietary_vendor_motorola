LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_PACKAGE_NAME := QuarkBundledApps
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_SDK_VERSION := 30
LOCAL_MIN_SDK_VERSION := 26
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_ASSET_DIR := $(LOCAL_PATH)/assets
LOCAL_DEX_PREOPT := false
LOCAL_PROGUARD_ENABLED := disabled
include $(BUILD_PACKAGE)
