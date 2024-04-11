package com.lpr.gt;

public class GTLPREngine {

    private long mNativeData;
    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("lpr_gt");
    }
    public GTLPREngine() {
        nativeClassInit();
        nativeConstruct();
    }

    private static native void nativeClassInit();
    private native void nativeConstruct();
    public native long Create(int numThreads);
    public native void Start(long hEngineHandle);
    public native void Restart(long hEngineHandle);
    public native void RequestJob(long hEngineHandle, int p_frameId, long matImage, int[] rectArray, int MinPlateH, int MaxPlateH);
    public native void WaitForFinished(long hEngineHandle);
    public native void Destroy(long hEngineHandle);
    public interface GTLPRCallback {
        void onGTLPRResult(byte[] byteArray);
    }
    private GTLPRCallback mLPRCallback;
    public void setLPRCallback(GTLPRCallback callback) {
        this.mLPRCallback = callback;
    }
    private void onLPRProgress(byte[] byteArray) {
        if (mLPRCallback != null) {
            mLPRCallback.onGTLPRResult(byteArray);
        }
    }
}
