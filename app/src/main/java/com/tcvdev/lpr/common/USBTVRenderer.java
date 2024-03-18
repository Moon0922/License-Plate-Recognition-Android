package com.tcvdev.lpr.common;


import android.content.Context;

import android.support.annotation.NonNull;
import android.view.Surface;

import com.arksine.libusbtv.DeviceParams;
import com.arksine.libusbtv.UsbTvFrame;
//import android.support.v8.renderscript.*;
//import com.tcvdev.lpr.ScriptC_ConvertYUYV;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

public class USBTVRenderer {

    private Thread mRenderThread;
    private Surface mRenderSurface;
//    private RenderScript  mRs;
//    private Allocation mInputAllocation = null;
//    private Allocation mOutputAllocation = null;
//    private final ScriptC_ConvertYUYV mConvertKernel;
    private final AtomicBoolean mThreadRunning = new AtomicBoolean(false);

    private byte[] mRenderBuf;
    private byte[] mByteConverted;
    private USBRenderCallback mUSBRenderCallback;

    private BlockingQueue<UsbTvFrame> mFrameQueue;


    public void setUSBRenderCallback(USBRenderCallback callback) {

        this.mUSBRenderCallback = callback;
    }

    private final Runnable mRenderRunnable = new Runnable() {
        @Override
        public void run() {
            UsbTvFrame frame;
            mThreadRunning.set(true);
            Timber.d("Render Thread Started");

            long renderTime;
            long highTime = 0;
            long lowTime = 1000;
            int frameCount = 0;

            while (mThreadRunning.get()) {
                try {
                    frame = mFrameQueue.take();
                } catch (InterruptedException e) {
                    break;
                }

                renderTime = System.currentTimeMillis();

                // Copy frame to input allocaton
                ByteBuffer buf = frame.getFrameBuf();
                buf.get(mRenderBuf);
                frame.returnFrame();  // Return Frame to its Pool so it can be reused

//                if (mRenderBuf.length != mInputAllocation.getBytesSize()) {
//                    Timber.e("Incoming frame buffer size, does not match input allocation size");
//                    mThreadRunning.set(false);
//                    return;
//                }
//
//                mInputAllocation.copyFromUnchecked(mRenderBuf);
//                mConvertKernel.forEach_convertFromYUYV(mInputAllocation);
//                mOutputAllocation.copyTo(mByteConverted);
//                mOutputAllocation.ioSend();  // Send output frame to surface
                if (mUSBRenderCallback != null) {

                    mUSBRenderCallback.onUSBFrameGrab(frame.getWidth(), frame.getHeight());
                }

                /*
                Profile every 120 frames (Every two seconds)
                 */
                renderTime = System.currentTimeMillis() - renderTime;
                highTime = Math.max(renderTime, highTime);
                lowTime = Math.min(renderTime, lowTime);
                frameCount++;
                if (frameCount >= 120) {
                    frameCount = 0;
                    Timber.d("Last 120 Frames - High Render Time: %d ms\nLow Render Time: %d ms",
                            highTime, lowTime);
                    highTime = 0;
                    lowTime = 1000;

                }
            }
        }
    };

    public USBTVRenderer(@NonNull Context context, Surface surface) {
//        mRs = RenderScript.create(context);
//        mConvertKernel = new ScriptC_ConvertYUYV(mRs);
        mRenderSurface = surface;
    }

    public void processFrame(UsbTvFrame frame) {
        if (mThreadRunning.get()) {
            if (!mFrameQueue.offer(frame)) {
                frame.returnFrame();
            }
        } else {
            frame.returnFrame();
        }
    }

    public void setSurface(Surface surface) {
        mRenderSurface = surface;
//        if (mOutputAllocation != null && mRenderSurface != null) {
//            mOutputAllocation.setSurface(mRenderSurface);
//        }
    }

    public void startRenderer(DeviceParams params, byte[] byteUSBFrame) {

        mByteConverted = byteUSBFrame;

        if (!mThreadRunning.get()) {
            initAllocations(params);

            mRenderThread = new Thread(mRenderRunnable, "Render Thread");
            mRenderThread.start();
        }
    }

    public void stopRenderer() {
        if (mThreadRunning.compareAndSet(true, false)) {
            try {
                mRenderThread.join(500);
            } catch (InterruptedException e) {
                Timber.d(e);
            }

            if (mRenderThread.isAlive()) {
                mRenderThread.interrupt();
            }

            UsbTvFrame frame;
            while (!mFrameQueue.isEmpty()) {
                try {
                    frame = mFrameQueue.take();
                } catch (InterruptedException e) {
                    Timber.i(e);
                    break;
                }

                frame.returnFrame();
            }
        }
    }

    public boolean isRunning() {
        return mThreadRunning.get();
    }


    private void initAllocations(DeviceParams params) {
        mFrameQueue = new ArrayBlockingQueue<>(params.getFramePoolSize(), true);
        mRenderBuf = new byte[params.getFrameSizeInBytes()];

//        Element inputElement = Element.U8_4(mRs);
//        Type.Builder outputType = new Type.Builder(mRs, Element.RGBA_8888(mRs));
//        outputType.setX(params.getFrameWidth());
//        outputType.setY(params.getFrameHeight());
//        int inputSize = params.getFrameSizeInBytes() / 4;
//
//        mInputAllocation = Allocation.createSized(mRs, inputElement, inputSize, Allocation.USAGE_SCRIPT);
//        mOutputAllocation = Allocation.createTyped(mRs, outputType.create(),
//                Allocation.USAGE_IO_OUTPUT | Allocation.USAGE_SCRIPT);
//
//        if (mRenderSurface != null) {
//            mOutputAllocation.setSurface(mRenderSurface);
//        }
//        mConvertKernel.set_output(mOutputAllocation);
//        mConvertKernel.set_width(params.getFrameWidth());
    }


    public interface USBRenderCallback {
        void onUSBFrameGrab(int width, int height);
    }
}
