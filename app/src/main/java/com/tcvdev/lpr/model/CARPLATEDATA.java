package com.tcvdev.lpr.model;

public class CARPLATEDATA {

    public static int MULTIRESULT = 10;
    public static int SIZE_OF_LPRResultData = 224;

    public class Rect{

        public int left;
        public int top;
        public int right;
        public int bottom;
    }

    public class LICENSE
    {
        public int	nLetNum;
        public char szLicense[];
        public float pfDist;
        public float nTrust;
        public Rect rtPlate;
        public Rect blob[];
        public char	Type;
        public char	bBkColor;
        public char	bCarColor;
        public char	bCarColDp;
        public LICENSE(){
            szLicense = new char[20];
            blob = new Rect[20];
        }
    }
    public int		nPlate;
    public LICENSE	pPlate[];
    int		nProcTime;
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

            carplatedata.pPlate[i].nLetNum = BaseConvertor.byteArray2Int(pData, nCursor);
            nCursor += 4;
            for (int j = 0 ; j < 20; j++) {
                carplatedata.pPlate[i].szLicense[j] = (char)(pData[nCursor]);
                nCursor++;
            }
            carplatedata.pPlate[i].pfDist = BaseConvertor.byteArray2Float(pData, nCursor);
            nCursor += 4;
            carplatedata.pPlate[i].nTrust = BaseConvertor.byteArray2Float(pData, nCursor);
            nCursor += 4;
            carplatedata.pPlate[i].rtPlate.left = BaseConvertor.byteArray2Int(pData, nCursor);
            nCursor += 4;
            carplatedata.pPlate[i].rtPlate.top = BaseConvertor.byteArray2Int(pData, nCursor);
            nCursor += 4;
            carplatedata.pPlate[i].rtPlate.right = BaseConvertor.byteArray2Int(pData, nCursor);
            nCursor += 4;
            carplatedata.pPlate[i].rtPlate.bottom = BaseConvertor.byteArray2Int(pData, nCursor);
            nCursor += 4;

            for (int j = 0 ; j < 20; j++) {
                carplatedata.pPlate[i].blob[j].left = BaseConvertor.byteArray2Int(pData, nCursor);
                nCursor += 4;
                carplatedata.pPlate[i].blob[j].top = BaseConvertor.byteArray2Int(pData, nCursor);
                nCursor += 4;
                carplatedata.pPlate[i].blob[j].right = BaseConvertor.byteArray2Int(pData, nCursor);
                nCursor += 4;
                carplatedata.pPlate[i].blob[j].bottom = BaseConvertor.byteArray2Int(pData, nCursor);
                nCursor += 4;
            }
            carplatedata.pPlate[i].Type = (char)(pData[nCursor]);
            nCursor++;
            carplatedata.pPlate[i].bBkColor = (char)(pData[nCursor]);
            nCursor++;
            carplatedata.pPlate[i].bCarColor = (char)(pData[nCursor]);
            nCursor++;
            carplatedata.pPlate[i].bCarColDp = (char)(pData[nCursor]);
            nCursor++;
        }
        carplatedata.nProcTime = BaseConvertor.byteArray2Int(pData, nCursor);
        nCursor += 4;
    }

    public static void getByteInfo(CARPLATEDATA carplatedata, byte[] byteInfo) {

//        int nCursor = 0;
//
//        byte[] plateNum = new byte[4];
//        BaseConvertor.Int2ByteArray(carplatedata.nPlateNum, plateNum, 0);
//        System.arraycopy(plateNum, 0, byteInfo, nCursor,4); nCursor += 4;

        for (int i = 0; i < carplatedata.nPlate; i++) {

            //Rect;
//            byte[] rectLeft = new byte[4];
//            BaseConvertor.Int2ByteArray(carplatedata.plateData[i].lprRect.left, rectLeft, 0);
//            System.arraycopy(rectLeft, 0, byteInfo, nCursor,4); nCursor += 4;
//
//            byte[] rectTop = new byte[4];
//            BaseConvertor.Int2ByteArray(carplatedata.plateData[i].lprRect.top, rectTop, 0);
//            System.arraycopy(rectTop, 0, byteInfo, nCursor,4); nCursor += 4;
//
//            byte[] rectRight = new byte[4];
//            BaseConvertor.Int2ByteArray(carplatedata.plateData[i].lprRect.right, rectRight, 0);
//            System.arraycopy(rectRight, 0, byteInfo, nCursor,4); nCursor += 4;
//
//            byte[] rectBottom = new byte[4];
//            BaseConvertor.Int2ByteArray(carplatedata.plateData[i].lprRect.bottom, rectBottom, 0);
//            System.arraycopy(rectBottom, 0, byteInfo, nCursor,4); nCursor += 4;
//
//            //Str
//            byte[] strArray = new byte[20];
//            String lprStr = new String(carplatedata.plateData[i].lprStr);
//            strArray = lprStr.getBytes();
//            System.arraycopy(strArray, 0, byteInfo, nCursor, 20); nCursor += 20;
//
//            //Conf;
//            byte[] confArray = BaseConvertor.float2ByteArray(carplatedata.plateData[i].conf);
//            System.arraycopy(confArray, 0, byteInfo, nCursor, 4); nCursor += 4;
//
//            //isPassed;
//            byte[] passedArray = new byte[4];
//            BaseConvertor.Int2ByteArray(carplatedata.plateData[i].isPassed, passedArray, 0);
//            System.arraycopy(passedArray, 0, byteInfo, nCursor,4); nCursor += 4;
        }
    }
}
