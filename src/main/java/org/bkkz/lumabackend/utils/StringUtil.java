package org.bkkz.lumabackend.utils;

import java.util.Map;
import java.util.regex.Pattern;

public class StringUtil {
    public static final Map<String, Integer> THAI_MONTHS = Map.ofEntries(
            Map.entry("มกราคม", 1), Map.entry("มกรา", 1),
            Map.entry("กุมภาพันธ์", 2), Map.entry("กุมภา", 2),
            Map.entry("มีนาคม", 3), Map.entry("มีนา", 3),
            Map.entry("เมษายน", 4), Map.entry("เมษา", 4),
            Map.entry("พฤษภาคม", 5), Map.entry("พฤษภา", 5),
            Map.entry("มิถุนายน", 6), Map.entry("มิถุนา", 6),
            Map.entry("กรกฎาคม", 7), Map.entry("กรกฎา", 7),
            Map.entry("สิงหาคม", 8), Map.entry("สิงหา", 8),
            Map.entry("กันยายน", 9), Map.entry("กันยา", 9),
            Map.entry("ตุลาคม", 10), Map.entry("ตุลา", 10),
            Map.entry("พฤศจิกายน", 11), Map.entry("พฤศจิกา", 11),
            Map.entry("ธันวาคม", 12), Map.entry("ธันวา", 12)
    );

    public static final Pattern YEAR_PATTERN = Pattern.compile("\\b(20\\d{2})\\b");
    public static final Pattern THAI_YEAR_PATTERN = Pattern.compile("\\b(25\\d{2})\\b");
}
