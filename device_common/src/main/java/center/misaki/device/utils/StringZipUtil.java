package center.misaki.device.utils;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

/**
 * @author: liuwenzhao
 * @descrition: 字符串压缩，解压工具
 */
public class StringZipUtil {
    /**
     * // 解压
     * @param encdata
     * @return
     */
    public static String decompressData(String encdata) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            InflaterOutputStream zos = new InflaterOutputStream(bos);
            zos.write(convertFromBase64(encdata));
            zos.close();
            return bos.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * // 压缩
     * @param data
     * @return
     */
    public static String compressData(String data) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DeflaterOutputStream zos = new DeflaterOutputStream(bos);
            zos.write(data.getBytes());
            zos.close();
            return convertToBase64(bos.toByteArray());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static byte[] convertFromBase64(String encdata) {
        byte[] compressed = null;
        try {
//            compressed = new sun.misc.BASE64Decoder().decodeBuffer(encdata);
            compressed = new BASE64Decoder().decodeBuffer(encdata);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return compressed;
    }

    private static String convertToBase64(byte[] byteArray) {
        return new BASE64Encoder().encode(byteArray);
    }

}
