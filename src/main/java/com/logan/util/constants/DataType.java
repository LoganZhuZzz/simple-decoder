package com.logan.util.constants;

import java.util.Arrays;

/**
 * 端站的原始数据类型，详见《甚低频/低频（VLF/LF）全闪定位仪功能规格需求书》
 */
public enum DataType {
    UNKNOWN((short) 0x1000, "未知数据"),
    STATUS_DATA((short) 0x00A0, "状态数据"),
    ELECTRIC_TIME_DOMAIN((short) 0x00A1, "电场时域波形数据"),
    MAGNETIC_TIME_DOMAIN((short) 0x00A2, "磁场时域波形数据"),
    ELECTRIC_FREQUENCY_DOMAIN((short) 0x00A3, "电场频域波形数据"),
    MAGNETIC_FREQUENCY_DOMAIN((short) 0x00A4, "磁场频域波形数据"),
    TIME_DOMAIN_FEATURE((short) 0x00B1, "时域波形特征量数据");
    private final short code;
    private final String description;

    DataType(short code, String description) {
        this.code = code;
        this.description = description;
    }

    public short getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DataType fromCode(short code) {
        return Arrays.stream(DataType.values()).filter(item -> item.getCode() == code).findAny().orElse(DataType.UNKNOWN);
    }
}
