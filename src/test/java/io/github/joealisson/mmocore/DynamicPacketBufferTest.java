package io.github.joealisson.mmocore;

import io.github.joealisson.mmocore.internal.DynamicPacketBuffer;
import io.github.joealisson.mmocore.internal.WritableBuffer;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

public class DynamicPacketBufferTest {

    @Test
    public void testIncrease() {
        ConnectionConfig<?> config = new ConnectionConfig<>(null, null, null);
        config.newBufferGroup(4, 32);
        ResourcePool resourcePool = ResourcePool.initialize(config);
        DynamicPacketBuffer packetBuffer = new DynamicPacketBuffer(ByteBuffer.allocate(32), resourcePool);
        for (int i = 0; i < 16; i++) {
            packetBuffer.writeBytes(new byte[34 + i * 64]);
        }
    }

    @Test
    public void testIntegrity() {
        ConnectionConfig<?> config = new ConnectionConfig<>(null, null, null);
        config.newBufferGroup(4, 32);
        ResourcePool resourcePool = ResourcePool.initialize(config);
        WritableBuffer packetBuffer = WritableBuffer.dynamicOf(ByteBuffer.allocate(32), resourcePool);

        packetBuffer.writeByte((byte) 1);
        packetBuffer.writeShort((short) 2);
        packetBuffer.writeChar('A');
        packetBuffer.writeInt(3);
        packetBuffer.writeFloat(4);
        packetBuffer.writeLong(5);
        packetBuffer.writeDouble(6);

        Assert.assertEquals(1, packetBuffer.readByte(0));
        Assert.assertEquals(2, packetBuffer.readShort(1));
        Assert.assertEquals(3, packetBuffer.readInt(5));

        packetBuffer.writeByte(4, (byte) 5);
        packetBuffer.writeShort(10, (short) 6);
        packetBuffer.writeInt(0,  40);

        Assert.assertEquals(5, packetBuffer.readByte(4));
        Assert.assertEquals(6, packetBuffer.readShort(10));
        Assert.assertEquals(40, packetBuffer.readInt(0));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testNegativeIndex() {
        ConnectionConfig<?> config = new ConnectionConfig<>(null, null, null);
        ResourcePool resourcePool = ResourcePool.initialize(config);
        DynamicPacketBuffer packetBuffer = new DynamicPacketBuffer(ByteBuffer.allocate(32), resourcePool);
        packetBuffer.writeInt(-1, 10);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testOutOfBoundIndex() {
        ConnectionConfig<?> config = new ConnectionConfig<>(null, null, null);
        ResourcePool resourcePool = ResourcePool.initialize(config);
        DynamicPacketBuffer packetBuffer = new DynamicPacketBuffer(ByteBuffer.allocate(2), resourcePool);
        packetBuffer.writeInt(100, 10);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testOutOfBoundReadIndex() {
        ConnectionConfig<?> config = new ConnectionConfig<>(null, null, null);
        ResourcePool resourcePool = ResourcePool.initialize(config);
        DynamicPacketBuffer packetBuffer = new DynamicPacketBuffer(ByteBuffer.allocate(2), resourcePool);
        packetBuffer.readInt(0);
    }

    @Test
    public void testBufferLimitWriting() {
        ConnectionConfig<?> config = new ConnectionConfig<>(null, null, null);
        ResourcePool resourcePool = ResourcePool.initialize(config);
        DynamicPacketBuffer packetBuffer = new DynamicPacketBuffer(ByteBuffer.allocate(2), resourcePool);

        packetBuffer.writeInt(1000000);

        int shortLimit = packetBuffer.limit() - 1;
        packetBuffer.position(shortLimit);
        packetBuffer.writeShort((short) 20000);

        int longLimit = packetBuffer.limit() - 1;
        packetBuffer.position(longLimit);
        packetBuffer.writeLong(10000);

        Assert.assertEquals(1000000, packetBuffer.readInt(0));
        Assert.assertEquals(20000, packetBuffer.readShort(shortLimit));
        Assert.assertEquals(10000, packetBuffer.readInt(longLimit));
    }

    @Test
    public void testBufferLimit() {
        ConnectionConfig<?> config = new ConnectionConfig<>(null, null, null);
        ResourcePool resourcePool = ResourcePool.initialize(config);
        DynamicPacketBuffer packetBuffer = new DynamicPacketBuffer(ByteBuffer.allocate(32), resourcePool);

        packetBuffer.writeBytes(new byte[10]);
        int pos = packetBuffer.position();
        packetBuffer.mark();
        Assert.assertEquals(pos, packetBuffer.limit());

        packetBuffer.limit(64);
        Assert.assertTrue(packetBuffer.capacity() >= 64);
    }


    @Test
    public void testMultipleBufferLimits() {
        ConnectionConfig<?> config = new ConnectionConfig<>(null, null, null);
        ResourcePool resourcePool = ResourcePool.initialize(config);
        DynamicPacketBuffer packetBuffer = new DynamicPacketBuffer(ByteBuffer.allocate(32), resourcePool);

        int initial = 10;
        byte end = 20;

        packetBuffer.writeInt(initial);
        packetBuffer.writeBytes(new byte[27]);
        packetBuffer.writeByte(end);

        packetBuffer.mark();

        Assert.assertEquals(32, packetBuffer.limit());
        Assert.assertEquals(initial, packetBuffer.readInt(0));
        Assert.assertEquals(end, packetBuffer.readByte(31));

        var buffers = packetBuffer.toByteBuffers();
        Assert.assertEquals(1, buffers.length);

        var firstBuffer = buffers[0];

        Assert.assertEquals(initial, firstBuffer.getInt());
        firstBuffer.get(new byte[27]);
        Assert.assertEquals(end, firstBuffer.get());


        packetBuffer.writeInt(initial);
        packetBuffer.writeBytes(new byte[27]);
        packetBuffer.writeByte(end);

        packetBuffer.mark();

        Assert.assertEquals(64, packetBuffer.limit());
        Assert.assertEquals(initial, packetBuffer.readInt(32));
        Assert.assertEquals(end, packetBuffer.readByte(63));

        buffers = packetBuffer.toByteBuffers();
        Assert.assertEquals(2, buffers.length);

        var secondBuffer = buffers[1];
        Assert.assertEquals(initial, secondBuffer.getInt());
        secondBuffer.get(new byte[27]);
        Assert.assertEquals(end, secondBuffer.get());
    }

    @Test
    public void testSplitValue() {
        ConnectionConfig<?> config = new ConnectionConfig<>(null, null, null);
        ResourcePool resourcePool = ResourcePool.initialize(config);


        DynamicPacketBuffer packetBuffer = new DynamicPacketBuffer(ByteBuffer.allocate(1), resourcePool);
        packetBuffer.writeShort((short) 10);
        Assert.assertEquals(10, packetBuffer.readShort(0));

        for (int i = 1; i <= 4; i++) {
            packetBuffer = new DynamicPacketBuffer(ByteBuffer.allocate(i), resourcePool);
            packetBuffer.writeInt(10);
            Assert.assertEquals(10, packetBuffer.readInt(0));
        }

        for (int i = 1; i <= 4; i++) {
            packetBuffer = new DynamicPacketBuffer(ByteBuffer.allocate(i), resourcePool);
            packetBuffer.writeFloat(10.5f);
            Assert.assertEquals(10.5f, packetBuffer.readFloat(0), 0f);
        }

        for (int i = 1; i <= 8; i++) {
            packetBuffer = new DynamicPacketBuffer(ByteBuffer.allocate(i), resourcePool);
            packetBuffer.writeLong(10);
            Assert.assertEquals(10, packetBuffer.readLong(0));
        }

        for (int i = 1; i <= 8; i++) {
            packetBuffer = new DynamicPacketBuffer(ByteBuffer.allocate(i), resourcePool);
            packetBuffer.writeDouble(10.5);
            Assert.assertEquals(10.5, packetBuffer.readDouble(0), 0);
        }

        byte[] data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };
        byte[] read = new byte[data.length];

        for (int i = 1; i < data.length; i++) {
            packetBuffer = new DynamicPacketBuffer(ByteBuffer.allocate(i), resourcePool);
            packetBuffer.writeBytes(data);
            packetBuffer.readBytes(0, read);
            Assert.assertArrayEquals(data, read);
        }


    }


}