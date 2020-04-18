package com.comphenix.protocol.wrappers.nbt.io;

import com.comphenix.protocol.wrappers.nbt.NbtList;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import java.io.IOException;
import java.io.DataInput;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import com.comphenix.protocol.wrappers.nbt.NbtWrapper;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;
import java.io.DataOutput;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import com.comphenix.protocol.wrappers.nbt.NbtBase;

public class NbtTextSerializer
{
    public static final NbtTextSerializer DEFAULT;
    private NbtBinarySerializer binarySerializer;
    
    public NbtTextSerializer() {
        this(new NbtBinarySerializer());
    }
    
    public NbtTextSerializer(final NbtBinarySerializer binary) {
        this.binarySerializer = binary;
    }
    
    public NbtBinarySerializer getBinarySerializer() {
        return this.binarySerializer;
    }
    
    public <TType> String serialize(final NbtBase<TType> value) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final DataOutputStream dataOutput = new DataOutputStream(outputStream);
        this.binarySerializer.serialize(value, dataOutput);
        return Base64Coder.encodeLines(outputStream.toByteArray());
    }
    
    public <TType> NbtWrapper<TType> deserialize(final String input) throws IOException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(input));
        return this.binarySerializer.deserialize(new DataInputStream(inputStream));
    }
    
    public NbtCompound deserializeCompound(final String input) throws IOException {
        return (NbtCompound)this.deserialize(input);
    }
    
    public <T> NbtList<T> deserializeList(final String input) throws IOException {
        return (NbtList<T>)(NbtList)this.deserialize(input);
    }
    
    static {
        DEFAULT = new NbtTextSerializer();
    }
}
