package com.comphenix.protocol.utility;

import java.io.IOException;
import com.google.common.base.Preconditions;

public class HexDumper
{
    private static final char[] HEX_DIGITS;
    private int positionLength;
    private char[] positionSuffix;
    private char[] delimiter;
    private int groupLength;
    private int groupCount;
    private char[] lineDelimiter;
    
    public HexDumper() {
        this.positionLength = 6;
        this.positionSuffix = ": ".toCharArray();
        this.delimiter = " ".toCharArray();
        this.groupLength = 2;
        this.groupCount = 24;
        this.lineDelimiter = "\n".toCharArray();
    }
    
    public static HexDumper defaultDumper() {
        return new HexDumper();
    }
    
    public HexDumper lineDelimiter(final String lineDelimiter) {
        this.lineDelimiter = ((String)Preconditions.checkNotNull((Object)lineDelimiter, (Object)"lineDelimiter cannot be NULL")).toCharArray();
        return this;
    }
    
    public HexDumper positionLength(final int positionLength) {
        if (positionLength < 0) {
            throw new IllegalArgumentException("positionLength cannot be less than zero.");
        }
        if (positionLength > 8) {
            throw new IllegalArgumentException("positionLength cannot be greater than eight.");
        }
        this.positionLength = positionLength;
        return this;
    }
    
    public HexDumper positionSuffix(final String positionSuffix) {
        this.positionSuffix = ((String)Preconditions.checkNotNull((Object)positionSuffix, (Object)"positionSuffix cannot be NULL")).toCharArray();
        return this;
    }
    
    public HexDumper delimiter(final String delimiter) {
        this.delimiter = ((String)Preconditions.checkNotNull((Object)delimiter, (Object)"delimiter cannot be NULL")).toCharArray();
        return this;
    }
    
    public HexDumper groupLength(final int groupLength) {
        if (groupLength < 1) {
            throw new IllegalArgumentException("groupLength cannot be less than one.");
        }
        this.groupLength = groupLength;
        return this;
    }
    
    public HexDumper groupCount(final int groupCount) {
        if (groupCount < 1) {
            throw new IllegalArgumentException("groupCount cannot be less than one.");
        }
        this.groupCount = groupCount;
        return this;
    }
    
    public void appendTo(final Appendable appendable, final byte[] data) throws IOException {
        this.appendTo(appendable, data, 0, data.length);
    }
    
    public void appendTo(final Appendable appendable, final byte[] data, final int start, final int length) throws IOException {
        final StringBuilder output = new StringBuilder();
        this.appendTo(output, data, start, length);
        appendable.append(output.toString());
    }
    
    public void appendTo(final StringBuilder builder, final byte[] data) {
        this.appendTo(builder, data, 0, data.length);
    }
    
    public void appendTo(final StringBuilder builder, final byte[] data, final int start, final int length) {
        int dataIndex = start;
        final int dataEnd = start + length;
        int groupCounter = 0;
        int currentGroupLength = 0;
        int value = 0;
        boolean highNiblet = true;
        while (dataIndex < dataEnd || !highNiblet) {
            if (groupCounter == 0 && currentGroupLength == 0) {
                for (int i = this.positionLength - 1; i >= 0; --i) {
                    builder.append(HexDumper.HEX_DIGITS[dataIndex >>> 4 * i & 0xF]);
                }
                builder.append(this.positionSuffix);
            }
            if (highNiblet) {
                value = (data[dataIndex++] & 0xFF);
                builder.append(HexDumper.HEX_DIGITS[value >>> 4]);
            }
            else {
                builder.append(HexDumper.HEX_DIGITS[value & 0xF]);
            }
            highNiblet = !highNiblet;
            if (++currentGroupLength >= this.groupLength) {
                currentGroupLength = 0;
                if (++groupCounter >= this.groupCount) {
                    builder.append(this.lineDelimiter);
                    groupCounter = 0;
                }
                else {
                    builder.append(this.delimiter);
                }
            }
        }
    }
    
    public int getLineLength(final int byteCount) {
        final int constant = this.positionLength + this.positionSuffix.length + this.lineDelimiter.length;
        final int groups = Math.min(2 * byteCount / this.groupLength, this.groupCount);
        return constant + this.delimiter.length * (groups - 1) + this.groupLength * groups;
    }
    
    static {
        HEX_DIGITS = "0123456789ABCDEF".toCharArray();
    }
}
