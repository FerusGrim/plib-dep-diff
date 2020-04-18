package com.comphenix.net.sf.cglib.asm;

public class $Attribute
{
    public final String type;
    byte[] b;
    $Attribute a;
    
    protected $Attribute(final String type) {
        this.type = type;
    }
    
    public boolean isUnknown() {
        return true;
    }
    
    public boolean isCodeAttribute() {
        return false;
    }
    
    protected $Label[] getLabels() {
        return null;
    }
    
    protected $Attribute read(final $ClassReader $ClassReader, final int n, final int n2, final char[] array, final int n3, final $Label[] array2) {
        final $Attribute $Attribute = new $Attribute(this.type);
        $Attribute.b = new byte[n2];
        System.arraycopy($ClassReader.b, n, $Attribute.b, 0, n2);
        return $Attribute;
    }
    
    protected $ByteVector write(final $ClassWriter $ClassWriter, final byte[] array, final int n, final int n2, final int n3) {
        final $ByteVector $ByteVector = new $ByteVector();
        $ByteVector.a = this.b;
        $ByteVector.b = this.b.length;
        return $ByteVector;
    }
    
    final int a() {
        int n = 0;
        for ($Attribute a = this; a != null; a = a.a) {
            ++n;
        }
        return n;
    }
    
    final int a(final $ClassWriter $ClassWriter, final byte[] array, final int n, final int n2, final int n3) {
        $Attribute a = this;
        int n4 = 0;
        while (a != null) {
            $ClassWriter.newUTF8(a.type);
            n4 += a.write($ClassWriter, array, n, n2, n3).b + 6;
            a = a.a;
        }
        return n4;
    }
    
    final void a(final $ClassWriter $ClassWriter, final byte[] array, final int n, final int n2, final int n3, final $ByteVector $ByteVector) {
        for ($Attribute a = this; a != null; a = a.a) {
            final $ByteVector write = a.write($ClassWriter, array, n, n2, n3);
            $ByteVector.putShort($ClassWriter.newUTF8(a.type)).putInt(write.b);
            $ByteVector.putByteArray(write.a, 0, write.b);
        }
    }
}
