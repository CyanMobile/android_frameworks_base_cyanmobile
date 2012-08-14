#
# CyanogenMod Audio Files
#

LOCAL_PATH:= frameworks/base/data/sounds

# Alarms
PRODUCT_COPY_FILES += \
	$(LOCAL_PATH)/CyanAlarm.ogg:system/media/audio/alarms/CyanAlarm.ogg

# Notifications
PRODUCT_COPY_FILES += \
	$(LOCAL_PATH)/notifications/CyanMail.ogg:system/media/audio/notifications/CyanMail.ogg \
	$(LOCAL_PATH)/notifications/CyanMessage.ogg:system/media/audio/notifications/CyanMessage.ogg \
	$(LOCAL_PATH)/notifications/Rang.ogg:system/media/audio/notifications/Rang.ogg \
	$(LOCAL_PATH)/notifications/Stone.ogg:system/media/audio/notifications/Stone.ogg \
	$(LOCAL_PATH)/notifications/CyanPing.ogg:system/media/audio/notifications/CyanPing.ogg \
	$(LOCAL_PATH)/notifications/Naughty.ogg:system/media/audio/notifications/Naughty.ogg \
	$(LOCAL_PATH)/notifications/Pong.ogg:system/media/audio/notifications/Pong.ogg

# Ringtones
PRODUCT_COPY_FILES += \
	$(LOCAL_PATH)/ringtones/Bongo.ogg:system/media/audio/ringtones/Bongo.ogg \
	$(LOCAL_PATH)/ringtones/Boxbeat.ogg:system/media/audio/ringtones/Boxbeat.ogg \
	$(LOCAL_PATH)/ringtones/Silmarillia.ogg:system/media/audio/ringtones/Silmarillia.ogg \
	$(LOCAL_PATH)/ringtones/Gigolo.ogg:system/media/audio/ringtones/Gigolo.ogg \
	$(LOCAL_PATH)/ringtones/House_of_house.ogg:system/media/audio/ringtones/House_of_house.ogg \
	$(LOCAL_PATH)/ringtones/CyanTone.ogg:system/media/audio/ringtones/CyanTone.ogg
