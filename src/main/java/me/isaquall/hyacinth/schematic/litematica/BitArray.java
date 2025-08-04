package me.isaquall.hyacinth.schematic.litematica;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;

/*
    From: https://github.com/sakura-ryoko/litematica/blob/1.21.8/src/main/java/fi/dy/masa/litematica/schematic/container/LitematicaBitArray.java
    Does not implement dynamic resizing, because in hyacinth we know the number of unique block states before creating the schematic.
 */
@ApiStatus.Internal
class BitArray {

    private final long[] longArray;
    private final int bitsPerEntry;
    private final long maxEntryValue;
    private final long arraySize;

    public BitArray(int bitsPerEntryIn, long arraySizeIn) {
        Validate.inclusiveBetween(1L, 32L, bitsPerEntryIn);
        this.arraySize = arraySizeIn;
        this.bitsPerEntry = bitsPerEntryIn;
        this.maxEntryValue = (1L << bitsPerEntryIn) - 1L;

        this.longArray = new long[(int) (roundUp(arraySizeIn * bitsPerEntryIn, 64L) / 64L)];
    }

    public static long roundUp(long value, long interval) {
        if (interval == 0L) {
            return 0L;
        } else if (value == 0L) {
            return interval;
        } else {
            if (value < 0L) {
                interval *= -1L;
            }

            long remainder = value % interval;

            return remainder == 0L ? value : value + interval - remainder;
        }
    }

    public void setAt(long index, int value) {
        //Validate.inclusiveBetween(0L, this.arraySize - 1L, index);
        //Validate.inclusiveBetween(0L, this.maxEntryValue, value);
        long startOffset = index * (long) this.bitsPerEntry;
        int startArrIndex = (int) (startOffset >> 6); // startOffset / 64
        int endArrIndex = (int) (((index + 1L) * (long) this.bitsPerEntry - 1L) >> 6);
        int startBitOffset = (int) (startOffset & 0x3F); // startOffset % 64
        this.longArray[startArrIndex] = this.longArray[startArrIndex] & ~(this.maxEntryValue << startBitOffset) | ((long) value & this.maxEntryValue) << startBitOffset;

        if (startArrIndex != endArrIndex) {
            int endOffset = 64 - startBitOffset;
            int j1 = this.bitsPerEntry - endOffset;
            this.longArray[endArrIndex] = this.longArray[endArrIndex] >>> j1 << j1 | ((long) value & this.maxEntryValue) >> endOffset;
        }
    }

    public int getAt(long index) {
        //Validate.inclusiveBetween(0L, this.arraySize - 1L, index);
        long startOffset = index * (long) this.bitsPerEntry;
        int startArrIndex = (int) (startOffset >> 6); // startOffset / 64
        int endArrIndex = (int) (((index + 1L) * (long) this.bitsPerEntry - 1L) >> 6);
        int startBitOffset = (int) (startOffset & 0x3F); // startOffset % 64

        if (startArrIndex == endArrIndex) {
            return (int) (this.longArray[startArrIndex] >>> startBitOffset & this.maxEntryValue);
        } else {
            int endOffset = 64 - startBitOffset;
            return (int) ((this.longArray[startArrIndex] >>> startBitOffset | this.longArray[endArrIndex] << endOffset) & this.maxEntryValue);
        }
    }

    public long[] getBackingLongArray() {
        return this.longArray;
    }

    public long size() {
        return this.arraySize;
    }

}
