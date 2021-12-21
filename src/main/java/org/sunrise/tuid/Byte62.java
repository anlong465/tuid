package org.sunrise.tuid;

public class Byte62 {
    public final static int MAX2 = 62 * 62;
    public final static int MAX3 = 62 * 62 * 62;
    private final static String STR62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    protected final static byte[] BYTE62 = STR62.getBytes();

    public static void to62(long number, byte[] dest, int offset, int len) {
        int j = offset + len;
        for(int i = 0; i < len; i++) {
            j--;
            int t = (int)(number % 62);
            if (t < 0) t += 62;
            dest[j] = BYTE62[t];
            number = number / 62;
        }
    }

    public static String fromTimemills(long timemills) {
        byte[] bs = new byte[7];
        to62(timemills, bs, 0, 7);
        return new String(bs);
    }

    public static long toTimemills(String ts) {
        return toTimemills(ts.getBytes());
    }

    public static long toTimemills( byte[] bs) {
        if (bs.length != 7) {
            throw new RuntimeException("Invalid timeslice string: " + new String(bs));
        }
        long ts = from62(bs[0]);

        ts = ts * 62 + from62(bs[1]);
        ts = ts * 62 + from62(bs[2]);
        ts = ts * 62 + from62(bs[3]);
        ts = ts * 62 + from62(bs[4]);
        ts = ts * 62 + from62(bs[5]);
        ts = ts * 62 + from62(bs[6]);

        return ts;
    }

    private static int from62(byte b62) {
        if (b62 <= '9') return (int) b62 - (int) '0';
        if (b62 > 'Z') return (int) b62 - (int) 'a' + 36;
        return (int) b62 - (int) 'A' + 10;
    }

}
