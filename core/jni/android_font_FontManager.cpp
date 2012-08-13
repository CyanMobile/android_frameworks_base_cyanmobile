/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "jni.h"
#include <android_runtime/AndroidRuntime.h>
#include "SkTypeface.h"
#include "SkFontHost.h"
#include "SkString.h"

namespace android {

/** 
 *  FontManager_getSelectableDefaultFonts()
 *  
 *  Get the selectable fonts information.
 *  
 *  @param  env
 *  @param  obj
 *  @param  language
 *  @return Selectable Font list.
 */
static jobjectArray FontManager_getSelectableDefaultFonts(JNIEnv* env, jobject obj, jstring language) {
    SkString sklanguage;
    SKFONTLIST fonts;

    const char* language8 = env->GetStringUTFChars(language, NULL);
    sklanguage.set(language8);
    SkFontManager::getSelectableDefaultFonts(&fonts, &sklanguage);
    env->ReleaseStringUTFChars(language, language8);

    SkFontManager::SkFontName** list = fonts.begin();
    int count = fonts.count();

    jclass jclassFont = env->FindClass("android/font/FontManager$Font");
    jmethodID init = env->GetMethodID(jclassFont, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V");

    jobjectArray jarray = env->NewObjectArray(count, jclassFont, NULL);

    for (int index = 0; index < count; index++) {
        jstring jname = env->NewStringUTF((const char*)list[index]->name.c_str());
        jstring jdisplayName = env->NewStringUTF((const char*)list[index]->displayName.c_str());

        jobject jfonts = env->NewObject(jclassFont, init, jname, jdisplayName);
        env->SetObjectArrayElement(jarray, index, jfonts);
    }
    fonts.deleteAll();
    list = NULL;

    return jarray;
}

/** 
 *  FontManager_getSelectedDefaultFontName()
 *  
 *  Get the selected font name.
 *  
 *  @param  env
 *  @param  obj
 *  @return Selected font name.
 */
static jstring FontManager_getSelectedDefaultFontName(JNIEnv* env, jobject obj) {
    SkString skname;
    SkFontManager::getSelectedDefaultFontName(&skname);

    return env->NewStringUTF((const char*)skname.c_str());
}

/** 
 *  FontManager_setSelectedDefaultFontName()
 *  
 *  Set the selected font name.
 *  
 *  @param  env
 *  @param  obj
 *  @return true - OK / false - NG
 */
static jboolean FontManager_setSelectedDefaultFontName(JNIEnv* env, jobject obj, jstring name) {
    SkString skname;
    jboolean ret;

    const char* name8 = env->GetStringUTFChars(name, NULL);
    skname.set(name8);
    ret = SkFontManager::setSelectedDefaultFontName(&skname);
    env->ReleaseStringUTFChars(name, name8);

    return ret;
}

/** 
 *  FontManager_reset()
 *  
 *  Reset.
 *  
 *  @param  env
 *  @param  obj
 *  @return true - OK / false - NG
 */
static jboolean FontManager_reset(JNIEnv* env, jobject obj) {
    return SkFontManager::reset();
}

/**
 * JNI registration.
 */
static JNINativeMethod gFontManagerMethods[] = {
    { "nativeGetSelectableDefaultFonts",
      "(Ljava/lang/String;)[Landroid/font/FontManager$Font;",
       (void*)FontManager_getSelectableDefaultFonts },
    { "nativeGetSelectedDefaultFontName",
      "()Ljava/lang/String;",
       (void*)FontManager_getSelectedDefaultFontName },
    { "nativeSetSelectedDefaultFontName",
      "(Ljava/lang/String;)Z",
       (void*)FontManager_setSelectedDefaultFontName },
    { "nativeReset",
      "()Z",
       (void*)FontManager_reset }
};

int register_android_font_FontManager(JNIEnv* env)
{
    return android::AndroidRuntime::registerNativeMethods(env,
                                                          "android/font/FontManager",
                                                          gFontManagerMethods,
                                                          SK_ARRAY_COUNT(gFontManagerMethods));
}

}    // namespace android
