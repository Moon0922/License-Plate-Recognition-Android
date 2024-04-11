package com.tcvdev.lpr.model;

public class CARPLATEDATA {

    public static int MULTIRESULT = 10;
    public static int SIZE_OF_carplatedata = 224;

    public class Rect {
        public int left;
        public int top;
        public int right;
        public int bottom;
    }

    public class LICENSE {
        public char[] szLicense;
        public float fTrust;
        public CARPLATEDATA.Rect rtPlate;
        public LICENSE() {
            szLicense = new char[20];
            rtPlate = new CARPLATEDATA.Rect();
        }
    }

    public int nPlate;
    public LICENSE[] pPlate;
    int nProcTime;

    public CARPLATEDATA() {
        nPlate = 0;
        pPlate = new LICENSE[MULTIRESULT];
        for (int i = 0; i < MULTIRESULT; i++)
            pPlate[i] = new LICENSE();
    }

    public static void parseFromByte(byte[] pData, CARPLATEDATA carplatedata) {
        int nCursor = 0;
        carplatedata.nPlate = BaseConvertor.byteArray2Int(pData, nCursor);
        nCursor += 4;

        for (int i = 0; i < carplatedata.nPlate; i++) {
            for (int j = 0; j < 20; j++) {
                carplatedata.pPlate[i].szLicense[j] = (char) (pData[nCursor]);
                nCursor++;
            }
            carplatedata.pPlate[i].fTrust = BaseConvertor.byteArray2Float(pData, nCursor);
            nCursor += 4;
            carplatedata.pPlate[i].rtPlate.left = BaseConvertor.byteArray2Int(pData, nCursor);
            nCursor += 4;
            carplatedata.pPlate[i].rtPlate.top = BaseConvertor.byteArray2Int(pData, nCursor);
            nCursor += 4;
            carplatedata.pPlate[i].rtPlate.right = BaseConvertor.byteArray2Int(pData, nCursor);
            nCursor += 4;
            carplatedata.pPlate[i].rtPlate.bottom = BaseConvertor.byteArray2Int(pData, nCursor);
            nCursor += 4;
        }
        carplatedata.nProcTime = BaseConvertor.byteArray2Int(pData, nCursor);
    }

    public static void getByteInfo(CARPLATEDATA carplatedata, byte[] byteInfo) {
        int nCursor = 0;
        byte[] plateNum = new byte[4];
        BaseConvertor.Int2ByteArray(carplatedata.nPlate, plateNum, 0);
        System.arraycopy(plateNum, 0, byteInfo, nCursor,4); nCursor += 4;
        for (int i = 0; i < MULTIRESULT; i++){
            byte[] strArray = new byte[20];
            String lprStr = new String(carplatedata.pPlate[i].szLicense);
            strArray = lprStr.getBytes();
            System.arraycopy(strArray, 0, byteInfo, nCursor, 20); nCursor += 20;
            byte[] confArray = BaseConvertor.float2ByteArray(carplatedata.pPlate[i].fTrust);
            System.arraycopy(confArray, 0, byteInfo, nCursor, 4); nCursor += 4;

            byte[] rectLeft = new byte[4];
            BaseConvertor.Int2ByteArray(carplatedata.pPlate[i].rtPlate.left, rectLeft, 0);
            System.arraycopy(rectLeft, 0, byteInfo, nCursor,4); nCursor += 4;

            byte[] rectTop = new byte[4];
            BaseConvertor.Int2ByteArray(carplatedata.pPlate[i].rtPlate.top, rectTop, 0);
            System.arraycopy(rectTop, 0, byteInfo, nCursor,4); nCursor += 4;

            byte[] rectRight = new byte[4];
            BaseConvertor.Int2ByteArray(carplatedata.pPlate[i].rtPlate.right, rectRight, 0);
            System.arraycopy(rectRight, 0, byteInfo, nCursor,4); nCursor += 4;

            byte[] rectBottom = new byte[4];
            BaseConvertor.Int2ByteArray(carplatedata.pPlate[i].rtPlate.bottom, rectBottom, 0);
            System.arraycopy(rectBottom, 0, byteInfo, nCursor,4); nCursor += 4;
        }
    }
}
