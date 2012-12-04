#include <sys/time.h>
#include <jni.h>

/*
 * Get the wall-clock date/time, in usec.
 */
/*
void Java_cz_cuni_kacz_contextlogger_TimeSource_getTimeOfDay(JNIEnv *env, jobject obj)
{
    struct timeval tv;

    gettimeofday(&tv, NULL);
    //return tv.tv_sec * 1000000LL + tv.tv_usec;
}
*/

jlong
Java_cz_cuni_kacz_contextlogger_TimeSource_getTimeOfDay( JNIEnv* env,
                                                  jobject thiz )
{
    struct timeval tv;

    gettimeofday(&tv, NULL);
    return tv.tv_sec * 1000000LL + tv.tv_usec;
    //return 11;
}
