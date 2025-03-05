package com.logan.util.codec;

import com.logan.util.constants.DataType;

import java.nio.ByteBuffer;

public class FramePacket {
    private final FrameType type;
    private final DataType dataType;
    private final ByteBuffer payload;


    public FramePacket(FrameType type, DataType dataType, ByteBuffer payload) {
        this.type = type;
        this.dataType = dataType;
        this.payload = payload;
    }

    public static FramePacket halfPacket(ByteBuffer halfPacket) {
        return new FramePacket(FrameType.HALF_PACKAGE, DataType.UNKNOWN, halfPacket);
    }

    public static FramePacket fullPacket(DataType dataType, ByteBuffer fullPacket) {
        return new FramePacket(FrameType.FULL_PACKAGE, dataType, fullPacket);
    }

    public FrameType getType() {
        return type;
    }

    public DataType getDataType() {
        return dataType;
    }

    public ByteBuffer getPayload() {
        return payload;
    }

    public enum FrameType {
        /**
         * 整包数据
         */
        FULL_PACKAGE,

        /**
         * 半包数据
         */
        HALF_PACKAGE
    }
}
