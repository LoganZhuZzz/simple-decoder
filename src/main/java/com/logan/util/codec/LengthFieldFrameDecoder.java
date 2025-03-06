package com.logan.util.codec;

import com.logan.util.constants.DataType;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息解码器：
 * 二进制消息，该消息有一个整数头字段，表示表示消息正文或整个消息的长度。
 * 通过消息长度字段将消息分割为完整的包
 * <pre>
 * 消息示例：
 * +--------------+-----------+--------+--------+------------+
 * | Frame Header | Data Type | Length | Others | Frame Tail |
 * | 0x55AA55AA   | 0x00A1    | 0x000C | ...    | 0x5A5A5A5A |
 * +--------------+-----------+--------+--------+------------+
 * </pre>
 */
public class LengthFieldFrameDecoder {


    private static final byte[] FRAME_HEADER = {(byte) 0x55, (byte) 0xAA, (byte) 0x55, (byte) 0xAA};
    private static final int DEFAULT_LENGTH_FIELD_OFFSET = 6;
    private static final int DEFAULT_LENGTH_FIELD_LENGTH = 2;
    private static final int DEFAULT_TYPE_FIELD_OFFSET = 4;

    private final int lengthFieldOffset;
    private final int lengthFieldLength;
    private final int lengthFiledEndOffset;

    public LengthFieldFrameDecoder() {
        this(DEFAULT_LENGTH_FIELD_OFFSET, DEFAULT_LENGTH_FIELD_LENGTH);
    }

    public LengthFieldFrameDecoder(int lengthFieldOffset, int lengthFieldLength) {
        this.lengthFieldOffset = lengthFieldOffset;
        this.lengthFieldLength = lengthFieldLength;
        this.lengthFiledEndOffset = lengthFieldOffset + lengthFieldLength;
    }

    public List<FramePacket> decode(byte[] in) {
        List<FramePacket> frames = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.wrap(in);

        while (buffer.remaining() > 0) {
            int frameOffset = findFrameHeader(buffer);

            if (frameOffset == -1) { // 没找到
                frames.add(FramePacket.halfPacket(extract(buffer, buffer.position(), buffer.limit())));
                break;
            }

            if (frameOffset > buffer.position()) { // FRAME HEADER之前存在半包数据
                frames.add(FramePacket.halfPacket(extract(buffer, buffer.position(), frameOffset)));
            }

            if (buffer.remaining() < lengthFiledEndOffset) { // 读不到length
                frames.add(FramePacket.halfPacket(extract(buffer, buffer.position(), buffer.limit())));
                break;
            }

            // 读到长度了 但数据不够整包
            long frameLength = getFrameLength(buffer, lengthFieldOffset, lengthFieldLength);
            if (buffer.remaining() < frameLength) {
                frames.add(FramePacket.halfPacket(extract(buffer, buffer.position(), buffer.limit())));
                break;
            }

            // 整包数据
            ByteBuffer full = extract(buffer, frameOffset, (int) (frameOffset + frameLength));
            DataType dataType = getDataType(full);
            frames.add(FramePacket.fullPacket(dataType, full));
        }
        return frames;
    }

    private int findFrameHeader(ByteBuffer buffer) {
        int hitIndex = 0;
        for (int i = buffer.position(); i <= buffer.limit() - FRAME_HEADER.length; i++) {
            if (buffer.get(i) != FRAME_HEADER[hitIndex]) {
                hitIndex = 0;
                continue;
            }
            hitIndex++;
            if (hitIndex == FRAME_HEADER.length) {
                return i + 1 - FRAME_HEADER.length;
            }
        }
        return -1;
    }

    private ByteBuffer extract(ByteBuffer buffer, int start, int end) {
        buffer.position(start);
        ByteBuffer slice = buffer.slice();
        slice.limit(end - start);
        buffer.position(end);
        return slice;
    }

    private long getFrameLength(ByteBuffer buffer, int offset, int lengthFiledLength) {
        int readOffset = buffer.position() + offset;
        long frameLength = switch (lengthFiledLength) {
            case 1 -> buffer.get(readOffset);
            case 2 -> buffer.getShort(readOffset);
            case 4 -> buffer.getInt(readOffset);
            case 8 -> buffer.getLong(readOffset);
            default -> throw new DecoderException(
                    "unsupported lengthFieldLength: " + lengthFieldLength + " (expected: 1, 2, 4, or 8)");
        };

        if (frameLength < lengthFieldOffset + lengthFieldLength) {
            throw new DecoderException("Invalid frame length: " + frameLength + " (expected >= " +
                    (DEFAULT_LENGTH_FIELD_OFFSET + DEFAULT_LENGTH_FIELD_LENGTH) + ")");
        }
        return frameLength;
    }

    private DataType getDataType(ByteBuffer buffer) {
        return DataType.fromCode(buffer.getShort(DEFAULT_TYPE_FIELD_OFFSET));
    }
}
