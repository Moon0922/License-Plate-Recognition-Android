//
// Created by frank on 2018/2/1.
//
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libswscale/swscale.h"
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <stdio.h>
#include <unistd.h>
#include <libavutil/imgutils.h>
#include <android/log.h>

#define TAG "MediaPlayer"
#define LOGI(FORMAT,...) __android_log_print(ANDROID_LOG_INFO, TAG, FORMAT, ##__VA_ARGS__);
#define LOGE(FORMAT,...) __android_log_print(ANDROID_LOG_ERROR, TAG, FORMAT, ##__VA_ARGS__);

static jmethodID method_onGrabFrame;
static jmethodID method_onCurTime;
static jmethodID method_onPlayStatus;

float play_rate = 1;
long duration = 0;
int delay = 80;
int play_status = 0;        //0;stop, 1;pause, 2;play 3; reload
int seek_status = 0;        // -2: BACK 30, -1: BACK -5, 1: FORWARD 5, 2: FORWARD 30, 3: CUSTOM
int SEEK_UNIT_10 = 5;
int SEEK_UNIT_30 = 30;
float cur_sec = 0;





AVCodecContext  * pCodecCtx1;

ANativeWindow* nativeWindow1;
ANativeWindow_Buffer windowBuffer1;
long videoWidth;
long videoHeight;


void setJNISurfaceView(JNIEnv *env, jobject surface)
{
    nativeWindow1 = ANativeWindow_fromSurface(env, surface);
    videoWidth = pCodecCtx1->width;
    videoHeight = pCodecCtx1->height;
    ANativeWindow_setBuffersGeometry(nativeWindow1, videoWidth, videoHeight, WINDOW_FORMAT_RGBA_8888);
}

JNIEXPORT void JNICALL Java_com_frank_ffmpeg_VideoPlayer_nativeClassInit(JNIEnv* env, jclass clazz)
{
    method_onGrabFrame = (*env)->GetMethodID(env, clazz, "onGrabFrame", "(II)V");
    method_onPlayStatus = (*env)->GetMethodID(env, clazz, "onPlayStatus", "(I)V");

}

JNIEXPORT void JNICALL Java_com_frank_ffmpeg_VideoPlayer_reloadVideo(JNIEnv* env, jclass clazz)
{
    play_status = 3;
}

JNIEXPORT void JNICALL Java_com_frank_ffmpeg_VideoPlayer_play(JNIEnv* env, jclass clazz)
{
    play_status = 2;
}

JNIEXPORT void JNICALL Java_com_frank_ffmpeg_VideoPlayer_pause(JNIEnv* env, jclass clazz)
{
    play_status = 1;
}

JNIEXPORT void JNICALL Java_com_frank_ffmpeg_VideoPlayer_stop(JNIEnv* env, jclass clazz)
{
    play_status = 0;
}

JNIEXPORT void JNICALL Java_com_frank_ffmpeg_VideoPlayer_setSpeed(JNIEnv* env, jclass clazz, jfloat speed)
{
    play_rate = speed;
}

JNIEXPORT void JNICALL Java_com_frank_ffmpeg_VideoPlayer_setSeekStatus(JNIEnv* env, jclass clazz, jint j_nSeekStatus)
{
    if (play_status == 2)
        seek_status = j_nSeekStatus;
}


JNIEXPORT void JNICALL Java_com_frank_ffmpeg_VideoPlayer_setSeekSec(JNIEnv* env, jclass clazz, jfloat j_fltSeekSec)
{
    if (play_status == 2) {
        seek_status = 3;
        cur_sec = j_fltSeekSec;
    }

}




JNIEXPORT jint JNICALL Java_com_frank_ffmpeg_VideoPlayer_loadVideo
        (JNIEnv * env, jobject objThis, jstring filePath, jobject surface, jbyteArray byteFrame){

    const char * file_name = (*env)->GetStringUTFChars(env, filePath, JNI_FALSE);
    //注册所有组件
    av_register_all();
    //分配上下文
    AVFormatContext * pFormatCtx = avformat_alloc_context();
    //打开视频文件
    if(avformat_open_input(&pFormatCtx, file_name, NULL, NULL)!=0) {
        LOGE("Couldn't open file:%s\n", file_name);
        return -1;
    }
    //检索多媒体流信息
    if(avformat_find_stream_info(pFormatCtx, NULL)<0) {
        LOGE("Couldn't find stream information.");
        return -1;
    }
    //寻找视频流的第一帧
    int videoStream = -1, i;
    for (i = 0; i < pFormatCtx->nb_streams; i++) {
        if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO
           && videoStream < 0) {
            videoStream = i;
        }
    }
    if(videoStream==-1) {
        LOGE("couldn't find a video stream.");
        return -1;
    }

    //获取视频总时长
    if (pFormatCtx->duration != AV_NOPTS_VALUE) {
        duration  = (long) (pFormatCtx->duration / AV_TIME_BASE);
        LOGE("duration=%d", duration);
//        (*env)->CallVoidMethod(env, objThis, method_onCurTime, 0, duration);
    }

    //获取codec上下文指针
    pCodecCtx1 = pFormatCtx->streams[videoStream]->codec;
    //寻找视频流的解码器
    AVCodec * pCodec = avcodec_find_decoder(pCodecCtx1->codec_id);
    if(pCodec==NULL) {
        LOGE("couldn't find Codec.");
        return -1;
    }
    if(avcodec_open2(pCodecCtx1, pCodec, NULL) < 0) {
        LOGE("Couldn't open codec.");
        return -1;
    }

    ///

    setJNISurfaceView(env, surface);

    if(avcodec_open2(pCodecCtx1, pCodec, NULL)<0) {
        LOGE("Couldn't open codec.");
        return -1;
    }
    //申请内存
    AVFrame * pFrame = av_frame_alloc();
    AVFrame * pFrameRGBA = av_frame_alloc();
    if(pFrameRGBA == NULL || pFrame == NULL) {
        LOGE("Couldn't allocate video frame.");
        return -1;
    }
    // buffer中数据用于渲染,且格式为RGBA
    int numBytes=av_image_get_buffer_size(AV_PIX_FMT_RGBA, pCodecCtx1->width, pCodecCtx1->height, 1);

    uint8_t * buffer=(uint8_t *)av_malloc(numBytes*sizeof(uint8_t));
    av_image_fill_arrays(pFrameRGBA->data, pFrameRGBA->linesize, buffer, AV_PIX_FMT_RGBA,
                         pCodecCtx1->width, pCodecCtx1->height, 1);

    // 由于解码出来的帧格式不是RGBA的,在渲染之前需要进行格式转换
    struct SwsContext *sws_ctx = sws_getContext(pCodecCtx1->width,
                             pCodecCtx1->height,
                             pCodecCtx1->pix_fmt,
                             pCodecCtx1->width,
                             pCodecCtx1->height,
                             AV_PIX_FMT_RGBA,
                             SWS_BILINEAR,
                             NULL,
                             NULL,
                             NULL);

    int frameFinished;
    AVPacket packet;

//    delay = av_q2d(pCodecCtx1->time_base);

    while(1) {

        if (seek_status != 0) {

            int seek = 0;
            switch (seek_status) {

                case -2:
                    seek = -SEEK_UNIT_30;
                    break;

                case -1:
                    seek = -SEEK_UNIT_10;
                    break;

                case 1:
                    seek = SEEK_UNIT_10;
                    break;

                case 2:
                    seek = SEEK_UNIT_30;
                    break;

                case 3:
                    seek = 0;
                    break;
            }

            seek_status = 0;

            int64_t frame;
            int64_t seek_to = ((cur_sec + seek) * 1000);
            frame = av_rescale(seek_to, pFormatCtx->streams[videoStream]->time_base.den, pFormatCtx->streams[videoStream]->time_base.num);
////            frame = av_rescale_q(seek_to, AV_TIME_BASE_Q, pFormatCtx->streams[videoStream]->time_base);
            frame /= 1000;
//            cur_sec = (float)((packet.pts) / (double)pFormatCtx->streams[videoStream]->time_base.den);



//            frame = (cur_sec + seek) * (double)pFormatCtx->streams[videoStream]->time_base.den;
//            int64_t timeBase = (int64_t)(pCodecCtx1->time_base.num) * AV_TIME_BASE /( int64_t)(pCodecCtx1->time_base.den);
//            LOGE("Cur sec::%.2f, Seek::%d  DEN::%d" , cur_sec, seek, pFormatCtx->streams[videoStream]->time_base.den);
//            LOGE("FRAME:::%" PRId64, frame);
//            LOGE("timeBase:::%" PRId64, timeBase);
//            int seek_result = avformat_seek_file(pFormatCtx, videoStream, INT64_MIN, frame, INT64_MAX, AVSEEK_FLAG_FRAME);


            int seek_result = av_seek_frame(pFormatCtx, videoStream, frame, AVSEEK_FLAG_FRAME );
            if(seek_result < 0) {
                continue;
            }

            avcodec_flush_buffers(pCodecCtx1);
            usleep((unsigned long) (1000 * delay * play_rate));
        }

        //判断是否为视频流

        if (play_status == 1) {     // Pause

//            int64_t frame;
//
//            frame = av_rescale(4000, pFormatCtx->streams[videoStream]->time_base.den,pFormatCtx->streams[videoStream]->time_base.num);
//            frame/=1000;
//
//            if(avformat_seek_file(pFormatCtx,videoStream,0,frame,frame,AVSEEK_FLAG_FRAME)<0) {
//                return 0;
//            }
//
//            avcodec_flush_buffers(pCodecCtx1);

            usleep((unsigned long) (1000 * delay * play_rate));

            continue;
        }

        if (play_status == 0) {     // Stop
            cur_sec = 0;
            seek_status = 0;
            break;
        }

        if( !( av_read_frame(pFormatCtx, &packet)>=0)) {

            cur_sec = 0;
            seek_status = 0;
            // Notify Stop;
            (*env)->CallVoidMethod(env, objThis, method_onPlayStatus, 0);
            break;
        }

        if(packet.stream_index==videoStream) {
            //对该帧进行解码
            avcodec_decode_video2(pCodecCtx1, pFrame, &frameFinished, &packet);

            if (frameFinished) {
                // lock native window
                ANativeWindow_lock(nativeWindow1, &windowBuffer1, 0);
                // 格式转换
                sws_scale(sws_ctx, (uint8_t const * const *)pFrame->data,
                          pFrame->linesize, 0, pCodecCtx1->height,
                          pFrameRGBA->data, pFrameRGBA->linesize);

                // Currrent Time Callback
//                (*env)->CallVoidMethod(env, objThis, method_onCurTime, (int)((packet.pts) / pFormatCtx->streams[videoStream]->time_base.den), duration);


                // 获取stride
                uint8_t * dst = windowBuffer1.bits;
                int dstStride = windowBuffer1.stride * 4;
                uint8_t * src = pFrameRGBA->data[0];
                int srcStride = pFrameRGBA->linesize[0];
                // 由于window的stride和帧的stride不同,因此需要逐行复制

                int h;
                for (h = 0; h < videoHeight; h++) {
                    memcpy(dst + h * dstStride, src + h * srcStride, (size_t) srcStride);
                }

                jbyte* jFrame = (*env)->GetByteArrayElements(env, byteFrame, 0);
                memcpy(jFrame, dst, videoWidth * videoHeight * 4);

                (*env)->ReleaseByteArrayElements(env, byteFrame, jFrame, 0);
                cur_sec = (float)((packet.pts) / (double)pFormatCtx->streams[videoStream]->time_base.den);
                (*env)->CallVoidMethod(env, objThis, method_onGrabFrame, (int)cur_sec, (int)duration);

                ANativeWindow_unlockAndPost(nativeWindow1);
            }
            //延迟等待
            usleep((unsigned long) (1000 * delay * play_rate));

        }
        av_packet_unref(&packet);
    }
    //释放内存以及关闭文件
    av_free(buffer);
    av_free(pFrameRGBA);
    av_free(pFrame);
    avcodec_close(pCodecCtx1);
    avformat_close_input(&pFormatCtx);
    ANativeWindow_release(nativeWindow1);

    (*env)->ReleaseStringUTFChars(env, filePath, file_name);

    return 0;
}

JNIEXPORT jint JNICALL Java_com_frank_ffmpeg_VideoPlayer_setSurface
        (JNIEnv * env, jclass clazz, jobject surface)
{
    setJNISurfaceView(env, surface);
}




//设置播放速率
JNIEXPORT jint JNICALL Java_com_frank_ffmpeg_VideoPlayer_setPlayRate(JNIEnv * env, jclass clazz, jfloat playRate) {
        play_rate = playRate;
}

//获取视频总时长
JNIEXPORT jint JNICALL Java_com_frank_ffmpeg_VideoPlayer_getDuration(JNIEnv * env, jclass clazz) {
        return duration;
}