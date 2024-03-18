package com.lpr.ua;

import android.graphics.Rect;

public class UALPREngineInterface {
    private long m_hEngineHandle = 0;
    private UALPREngine m_instance = new UALPREngine();

    private static final int NUM_THREADS = 3;

    public void CreateEngine() {
        m_hEngineHandle = m_instance.Create(NUM_THREADS);
    }


    public void StartEngine() {

        if (m_hEngineHandle != 0) {
            m_instance.Start(m_hEngineHandle);
        }
    }

    public void RestartEngine() {

        if (m_hEngineHandle != 0)
            m_instance.Restart(m_hEngineHandle);
    }

    public void WaitForFinished() {

        if (m_hEngineHandle != 0)
            m_instance.WaitForFinished(m_hEngineHandle);
    }

    public int Process(int frameId, long matImage, Rect rect) {

        int plateNum = -1;
        int minPlateH = 15;
        int maxPlateH = 100;

        if (m_hEngineHandle != 0) {

            int rectArray[] = new int[4];
            rectArray[0] = rect.left;
            rectArray[2] = rect.right;
            rectArray[1] = rect.top;
            rectArray[3] = rect.bottom;

            m_instance.RequestJob(m_hEngineHandle, frameId, matImage, rectArray, minPlateH, maxPlateH);

        }

        return plateNum;
    }

    public void DestroyEngine() {

        if (m_hEngineHandle != 0)
            m_instance.Destroy(m_hEngineHandle);
    }


    public void SetLPRCallback(UALPREngine.UALPRCallback callback) {

        m_instance.setLPRCallback(callback);
    }
}
