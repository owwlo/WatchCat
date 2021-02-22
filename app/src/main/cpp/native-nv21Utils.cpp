#include <jni.h>
#include <string>
#include <android/log.h>
#include <unistd.h>
#include <asm/fcntl.h>
#include <fcntl.h>
#include <android/native_window_jni.h>
#include <stdio.h>
#include <stdlib.h>

#define TAG "native-yuv-to-buffer-lib"

#define MAX(a, b) (((a) > (b)) ? (a) : (b))
#define MIN(a, b) (((a) < (b)) ? (a) : (b))

extern "C"
JNIEXPORT void JNICALL
Java_org_owwlo_watchcat_utils_NativeHelper_nv21Flip(JNIEnv *env, jobject instance, jbyteArray jdata,
                                                    jint imageWidth,
                                                    jint imageHeight) {
    jbyte *data = (jbyte *) env->GetPrimitiveArrayCritical(jdata, NULL);
    int32_t newBufLen = imageWidth * imageHeight * 3 / 2;
    uint8_t *newBuf = (uint8_t *) malloc(newBufLen);
    int32_t i;
    int32_t count = 0;
    for (i = imageWidth * imageHeight - 1; i >= 0; i--) {
        newBuf[count] = data[i];
        count++;
    }
    for (i = newBufLen - 1; i >= imageWidth
                                 * imageHeight; i -= 2) {
        newBuf[count++] = data[i - 1];
        newBuf[count++] = data[i];
    }
    memcpy(data, newBuf, MIN(env->GetArrayLength(jdata), newBufLen));
    free(newBuf);
    env->ReleasePrimitiveArrayCritical(jdata, data, 0);
}


void cleanup(JNIEnv *env, jbyteArray jbuffer, jbyte *buffer, jbyteArray jdata, jbyte *data) {
    env->ReleasePrimitiveArrayCritical(jbuffer, buffer, 0);
    env->ReleasePrimitiveArrayCritical(jdata, data, 0);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_org_owwlo_watchcat_utils_NativeHelper_nv21ToYuv420(JNIEnv *env, jobject instance,
                                                        jbyteArray jbuffer,
                                                        jbyteArray jdata,
                                                        jboolean isPlanar,
                                                        jint sliceHeight, jint height,
                                                        jint stride, jint width,
                                                        jboolean panesReversed,
                                                        jint size, jint yPadding) {
    jbyte *buffer = (jbyte *) env->GetPrimitiveArrayCritical(jbuffer, NULL);
    jbyte *data = (jbyte *) env->GetPrimitiveArrayCritical(jdata, NULL);
    if (!isPlanar) {
        if (sliceHeight == height && stride == width) {
            // Swaps U and V
            if (!panesReversed) {
                for (int i = size; i < size + size / 2; i += 2) {
                    buffer[0] = data[i + 1];
                    data[i + 1] = data[i];
                    data[i] = buffer[0];
                }
            }
            if (yPadding > 0) {
                memcpy(buffer, data, size);
                memcpy(buffer + size + yPadding, data + size, size / 2);
                cleanup(env, jbuffer, buffer, jdata, data);
                return true;
            }
            cleanup(env, jbuffer, buffer, jdata, data);
            return false;
        }
    } else {
        if (sliceHeight == height && stride == width) {
            // De-interleave U and V
            if (!panesReversed) {
                for (int i = 0; i < size / 4; i += 1) {
                    buffer[i] = data[size + 2 * i + 1];
                    buffer[size / 4 + i] = data[size + 2 * i];
                }
            } else {
                for (int i = 0; i < size / 4; i += 1) {
                    buffer[i] = data[size + 2 * i];
                    buffer[size / 4 + i] = data[size + 2 * i + 1];
                }
            }
            if (yPadding == 0) {
                memcpy(data + size, buffer, size / 2);
            } else {
                memcpy(buffer, data, size);
                memcpy(buffer + size + yPadding, buffer, size / 2);
                cleanup(env, jbuffer, buffer, jdata, data);
                return true;
            }
            cleanup(env, jbuffer, buffer, jdata, data);
            return false;
        }
    }
    cleanup(env, jbuffer, buffer, jdata, data);
    return false;
}
