package org.bkkz.lumabackend.utils;

import java.time.Month;

public enum ThaiMonth {
    JANUARY("01", "มกราคม", Month.JANUARY),
    FEBRUARY("02", "กุมภาพันธ์", Month.FEBRUARY),
    MARCH("03", "มีนาคม", Month.MARCH),
    APRIL("04", "เมษายน", Month.APRIL),
    MAY("05", "พฤษภาคม", Month.MAY),
    JUNE("06", "มิถุนายน", Month.JUNE),
    JULY("07", "กรกฎาคม", Month.JULY),
    AUGUST("08", "สิงหาคม", Month.AUGUST),
    SEPTEMBER("09", "กันยายน", Month.SEPTEMBER),
    OCTOBER("10", "ตุลาคม", Month.OCTOBER),
    NOVEMBER("11", "พฤศจิกายน", Month.NOVEMBER),
    DECEMBER("12", "ธันวาคม", Month.DECEMBER);

    // --- Fields ---
    private final String monthNumber;
    private final String thaiName;
    private final Month monthClass;

    // --- Constructor ---
    ThaiMonth(String monthNumber, String thaiName, Month month) {
        this.monthNumber = monthNumber;
        this.thaiName = thaiName;
        this.monthClass = month;
    }

    // --- Getters ---
    public String getMonthNumber() {
        return monthNumber;
    }

    public String getThaiName() {
        return thaiName;
    }

    public Month getMonthClass() {
        return monthClass;
    }

    // --- Static Utility Method ---
    public static ThaiMonth fromMonthNumber(String monthNumber) {
        for (ThaiMonth month : values()) {
            if (month.getMonthNumber().equals(monthNumber)) {
                return month;
            }
        }
        throw new IllegalArgumentException("ไม่พบเดือนสำหรับหมายเลข: " + monthNumber);
    }

    public static String toThaiName(String monthNumber) {
        ThaiMonth month = fromMonthNumber(monthNumber);
        return month.getThaiName();
    }

    public static Month toMonthClass(String monthNumber) {
        ThaiMonth month = fromMonthNumber(monthNumber);
        return month.getMonthClass();
    }
}
