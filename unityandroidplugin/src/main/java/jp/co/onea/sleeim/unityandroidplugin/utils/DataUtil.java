package jp.co.onea.sleeim.unityandroidplugin.utils;

/**
 * データ操作関連
 */
public class DataUtil {

    /**
     * byte配列を取り出し
     */
    public static byte[] TakeOut(byte[] _byte, int pos, int cnt) {

        byte[] rByte = new byte[cnt];

        System.arraycopy(_byte, pos, rByte, 0, cnt); //指定位置のバイトをcntバイト分取り出し

        return rByte;
    }

    /**
     * byteデータから指定したbitデータを抽出し、intで返す
     *
     * @param msb 抽出したいビットデータの最上位ビット桁(0オリジン)
     * @param lsb 抽出したいビットデータの最下位ビット桁(0オリジン)
     * @return 値(引数が不適切な場合、-1を返す)
     */
    public static int extractBitInByteToInt(byte data, int msb, int lsb) {
        if (msb < lsb) {
            return -1;
        }

        if ((msb > 7 || lsb > 7)
            || (msb < 0 || lsb < 0)) {
            return -1;
        }

        int dataInt = data & 0xFF;
        int mask = 0;
        for (int i = lsb; i <= msb; i++) {
            mask += (int)Math.pow(2, i);
        }
        return (dataInt & mask) >>> lsb;
    }

    /**
     * byteをintに変換
     * 引数のセット方向に注意
     */
    public static int CovertToInt32(byte _byte1, byte _byte2, byte _byte3, byte _byte4) {
        int rtn = (_byte4 << 24) & 0xff000000 |
                (_byte3 << 16) & 0x00ff0000 |
                (_byte2 << 8) & 0x0000ff00 |
                (_byte1 << 0) & 0x000000ff;
        return rtn;
    }
}
