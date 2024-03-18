package com.tcvdev.lpr.model;

import java.nio.ByteBuffer;

/**
 * Created by Administrator on 7/4/2016.
 */
public class BaseConvertor {

    public static int byte2Int(byte[] data, int offset)
    {
        int value = 0;
        for (int i = 0; i < 4; i++)
        {
            int shift = i * 8;
            value += (data[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }

    public static void Int2ByteArray(int value, byte[] data, int offset) {
        data[offset + 0] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
        data[offset + 2] = (byte) ((value >> 16) & 0xFF);
        data[offset + 3] = (byte) ((value >> 24) & 0xFF);
//        data[offset + 0] = (byte) ((value & 0xFF000000) >> 24);
//        data[offset + 1] = (byte) ((value & 0x00FF0000) >> 16);
//        data[offset + 2] = (byte) ((value & 0x0000FF00) >> 8);
//        data[offset + 3] = (byte) ((value & 0x000000FF) >> 0);
    }

    public static double Byte2Double(byte[] pData, int offset) {
        byte[] pbyDouble = new byte[8];
        System.arraycopy(pData, offset, pbyDouble, 0, 8);
        return ByteBuffer.wrap(pbyDouble).getDouble();
    }

    public static void Double2ByteArray(double dbValue, byte[] pbyValue, int offset) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(dbValue);
        System.arraycopy(bytes, 0, pbyValue, offset, 8);
    }

    public static float byteArray2Float(byte[] data, int offset)
    {
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        float fl = buffer.getFloat();
        return fl;
    }
    public static int byteArray2Int(byte[] data, int offset)
    {
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        int il = buffer.getInt();
        return il;
    }

    public static byte [] float2ByteArray (float value)
    {
        return ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putFloat(value).array();
    }
}
