package org.sunrise.tuid;

import java.util.UUID;

public class TUID {
    private final static ThreadLocal<TUID> instance = ThreadLocal.withInitial(() -> {return new TUID();});

    public static String next() {
        return instance.get().nextUUSeq();
    }

    private static final long MILLS_PER_SECOND = 1000;
    private static final long MILLS_PER_MINUTE = MILLS_PER_SECOND * 60;

    private final byte[] bytes = new byte[36];
    private long lastMills = 0;
    private int sequence = 0;

    private long baseExpiredTime = 0;

    private TUID() {
        bytes[8] = '-';
        bytes[13] = '-';
        bytes[18] = '-';
        bytes[23] = '-';
        initBase();
    }

    private void initBase() {
        UUID base = UUID.randomUUID();
        baseExpiredTime = System.currentTimeMillis();
        baseExpiredTime += MILLS_PER_MINUTE + (baseExpiredTime % MILLS_PER_SECOND) * MILLS_PER_SECOND;

        byte[] tmp = new byte[11];
        Byte62.to62(base.getMostSignificantBits(), tmp, 0, 11);
        bytes[9] = tmp[0];
        bytes[10] = tmp[1];
        bytes[11] = tmp[2];
        bytes[12] = tmp[3];

        bytes[14] = tmp[4];
        bytes[15] = tmp[5];
        bytes[16] = tmp[6];
        bytes[17] = tmp[7];

        bytes[19] = tmp[8];
        bytes[20] = tmp[9];
        bytes[21] = tmp[10];

        Byte62.to62(base.getLeastSignificantBits(), bytes, 25, 11);

        sequence = 0;
        bytes[7] = Byte62.BYTE62[0];
        bytes[22] = Byte62.BYTE62[0];
        bytes[24] = Byte62.BYTE62[0];

    }

    private String nextUUSeq() {
        long mills = System.currentTimeMillis();

        if (mills > baseExpiredTime) {
            initBase();
            lastMills = mills;
            Byte62.to62(mills, bytes, 0, 7);
        } else {
            if (lastMills == mills) {
                sequence++;
                if (sequence >= Byte62.MAX3) {
                    initBase();
                } else {
                    bytes[7] = Byte62.BYTE62[sequence % 62];
                    int tmp = sequence / 62;

                    bytes[22] = Byte62.BYTE62[tmp % 62];
                    bytes[24] = Byte62.BYTE62[tmp / 62];
                }
            } else {
                lastMills = mills;
                Byte62.to62(mills, bytes, 0, 7);
            }
        }


        return new String(bytes);
    }
}
