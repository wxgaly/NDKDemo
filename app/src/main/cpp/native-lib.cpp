#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring

JNICALL
Java_wxgaly_android_ndkdemo_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "OpenCV";
    return env->NewStringUTF(hello.c_str());
}
