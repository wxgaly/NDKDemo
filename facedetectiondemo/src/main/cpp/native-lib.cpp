#include <jni.h>
#include <string>
#include <vector>

#include <android/log.h>


#define LOG_TAG "FaceDetection/DetectionBasedTracker"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))


extern "C"
JNIEXPORT jlong JNICALL
Java_wxgaly_android_facedetectiondemo_DetectionBasedTracker_nativeCreateObject(JNIEnv *env, jobject instance,
                                                                               jstring cascadeName_, jint minFaceSize) {
    const char *cascadeName = env->GetStringUTFChars(cascadeName_, 0);

    // TODO

    env->ReleaseStringUTFChars(cascadeName_, cascadeName);
}extern "C"
JNIEXPORT void JNICALL
Java_wxgaly_android_facedetectiondemo_DetectionBasedTracker_nativeDestroyObject(JNIEnv *env, jobject instance,
                                                                                jlong thiz) {

    // TODO

}extern "C"
JNIEXPORT void JNICALL
Java_wxgaly_android_facedetectiondemo_DetectionBasedTracker_nativeStart(JNIEnv *env, jobject instance, jlong thiz) {

    // TODO

}extern "C"
JNIEXPORT void JNICALL
Java_wxgaly_android_facedetectiondemo_DetectionBasedTracker_nativeStop(JNIEnv *env, jobject instance, jlong thiz) {

    // TODO

}extern "C"
JNIEXPORT void JNICALL
Java_wxgaly_android_facedetectiondemo_DetectionBasedTracker_nativeSetFaceSize(JNIEnv *env, jobject instance, jlong thiz,
                                                                              jint size) {

    // TODO

}extern "C"
JNIEXPORT void JNICALL
Java_wxgaly_android_facedetectiondemo_DetectionBasedTracker_nativeDetect(JNIEnv *env, jobject instance, jlong thiz,
                                                                         jlong inputImage, jlong faces) {

    // TODO

}