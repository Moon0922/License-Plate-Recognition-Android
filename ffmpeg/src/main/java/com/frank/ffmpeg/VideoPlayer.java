package com.frank.ffmpeg;

import android.util.Log;

/**
 * 视频播放器
 * Created by frank on 2018/2/1
 */
public class VideoPlayer {

    static {
        System.loadLibrary("media-handle");
        nativeClassInit();

    }

    private static native void nativeClassInit();

    public native int loadVideo(String filePath, Object surface, byte[] byteFrame);
    public native int setSurface(Object surface);
    public native void play();
    public native void pause();
    public native void stop();

    public native void setSeekStatus(int seek_status);
    public native void setSeekSec(float fltSec);

    public native int setPlayRate(float playRate);

    public native int filter(String filePath, Object surface, String filterType);
    public native void again();
    public native void release();
    public native void reloadVideo();



    public interface FFMPEGCallback {

//        void onCurTime(int cur_time, int duration);
        void onGrabFrame(int cur_time, int duration);
        void onPlayStatus(int play_status);
    }

    private FFMPEGCallback mFFMPEGCallback;


    public void setFFMPEGCallback(FFMPEGCallback callback) {

        this.mFFMPEGCallback = callback;
    }


    private void onGrabFrame(int cur_time, int duration)
    {

        if (mFFMPEGCallback != null)
            mFFMPEGCallback.onGrabFrame(cur_time, duration);
    }

//    private void onCurTime(int cur_time, int duration)
//    {
//        if (mFFMPEGCallback != null)
//            mFFMPEGCallback.onCurTime(cur_time, duration);
//    }

    private void onPlayStatus(int play_status) {
        if (mFFMPEGCallback != null)
            mFFMPEGCallback.onPlayStatus(play_status);
    }
}
