package com.logan.util.codec;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LengthFieldFrameDecoderTest {

    @Test
    void decode() {
        byte[] data = {
                (byte) 0xA1, (byte) 0xA1, (byte) 0xA1,              // 异常数据（半包）
                (byte) 0x55, (byte) 0xAA, (byte) 0x55, (byte) 0xAA, // frame header
                0x00, (byte) 0xA1,                                  // type
                0x00, (byte) 0x0C,                                  // length -> aka12
                0x01, 0x02, 0x03, 0x04,                             // content
                (byte) 0x55, (byte) 0xAA, (byte) 0x55, (byte) 0xAA, // frame header
                0x00, (byte) 0xA1,                                  // type
                0x00, (byte) 0x0A,                                  // length -> aka10
                0x01, 0x02,                                         // content
                0x03, 0x04,                                         // 异常数据（半包）
                (byte) 0x55, (byte) 0xAA, (byte) 0x55, (byte) 0xAA, // frame header
                0x00, (byte) 0xA1,                                  // type
                0x00, (byte) 0xA4,                                  // length
                0x01                                                // 数据长度不足
        };

        LengthFieldFrameDecoder decoder = new LengthFieldFrameDecoder();
        List<FramePacket> decodedPackets = decoder.decode(data);

        // 预期解析出的数据包数量
        assertEquals(5, decodedPackets.size());

        // 校验第一个数据包（半包）
        assertEquals(FramePacket.FrameType.HALF_PACKAGE, decodedPackets.get(0).getType());
        assertArrayEquals(new byte[]{(byte) 0xA1, (byte) 0xA1, (byte) 0xA1},
                extractBytes(decodedPackets.get(0).getPayload()));

        // 校验第二个数据包（完整包）
        assertEquals(FramePacket.FrameType.FULL_PACKAGE, decodedPackets.get(1).getType());
        assertArrayEquals(new byte[]{
                (byte) 0x55, (byte) 0xAA, (byte) 0x55, (byte) 0xAA,
                0x00, (byte) 0xA1, 0x00, (byte) 0x0C, 0x01, 0x02, 0x03, 0x04
        }, extractBytes(decodedPackets.get(1).getPayload()));

        // 校验第三个数据包（完整包）
        assertEquals(FramePacket.FrameType.FULL_PACKAGE, decodedPackets.get(2).getType());
        assertArrayEquals(new byte[]{
                (byte) 0x55, (byte) 0xAA, (byte) 0x55, (byte) 0xAA,
                0x00, (byte) 0xA1, 0x00, (byte) 0x0A, 0x01, 0x02
        }, extractBytes(decodedPackets.get(2).getPayload()));

        // 校验第四个数据包（半包）
        assertEquals(FramePacket.FrameType.HALF_PACKAGE, decodedPackets.get(3).getType());
        assertArrayEquals(new byte[]{0x03, 0x04}, extractBytes(decodedPackets.get(3).getPayload()));

        // 校验第五个数据包（半包）
        assertEquals(FramePacket.FrameType.HALF_PACKAGE, decodedPackets.get(4).getType());
        assertArrayEquals(new byte[]{
                (byte) 0x55, (byte) 0xAA, (byte) 0x55, (byte) 0xAA,
                0x00, (byte) 0xA1, 0x00, (byte) 0xA4, 0x01
        }, extractBytes(decodedPackets.get(4).getPayload()));
    }

    private byte[] extractBytes(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes); // 复制并读取数据，避免影响原 buffer
        return bytes;
    }
}