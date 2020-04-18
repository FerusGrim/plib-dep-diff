package com.comphenix.net.sf.cglib.asm;

import java.io.IOException;
import java.io.InputStream;

public class $ClassReader
{
    public static final int SKIP_CODE = 1;
    public static final int SKIP_DEBUG = 2;
    public static final int SKIP_FRAMES = 4;
    public static final int EXPAND_FRAMES = 8;
    public final byte[] b;
    private final int[] a;
    private final String[] c;
    private final int d;
    public final int header;
    
    public $ClassReader(final byte[] array) {
        this(array, 0, array.length);
    }
    
    public $ClassReader(final byte[] b, final int n, final int n2) {
        this.b = b;
        if (this.readShort(n + 6) > 52) {
            throw new IllegalArgumentException();
        }
        this.a = new int[this.readUnsignedShort(n + 8)];
        final int length = this.a.length;
        this.c = new String[length];
        int d = 0;
        int header = n + 10;
        for (int i = 1; i < length; ++i) {
            this.a[i] = header + 1;
            int n3 = 0;
            switch (b[header]) {
                case 3:
                case 4:
                case 9:
                case 10:
                case 11:
                case 12:
                case 18: {
                    n3 = 5;
                    break;
                }
                case 5:
                case 6: {
                    n3 = 9;
                    ++i;
                    break;
                }
                case 1: {
                    n3 = 3 + this.readUnsignedShort(header + 1);
                    if (n3 > d) {
                        d = n3;
                        break;
                    }
                    break;
                }
                case 15: {
                    n3 = 4;
                    break;
                }
                default: {
                    n3 = 3;
                    break;
                }
            }
            header += n3;
        }
        this.d = d;
        this.header = header;
    }
    
    public int getAccess() {
        return this.readUnsignedShort(this.header);
    }
    
    public String getClassName() {
        return this.readClass(this.header + 2, new char[this.d]);
    }
    
    public String getSuperName() {
        return this.readClass(this.header + 4, new char[this.d]);
    }
    
    public String[] getInterfaces() {
        int n = this.header + 6;
        final int unsignedShort = this.readUnsignedShort(n);
        final String[] array = new String[unsignedShort];
        if (unsignedShort > 0) {
            final char[] array2 = new char[this.d];
            for (int i = 0; i < unsignedShort; ++i) {
                n += 2;
                array[i] = this.readClass(n, array2);
            }
        }
        return array;
    }
    
    void a(final $ClassWriter $ClassWriter) {
        final char[] array = new char[this.d];
        final int length = this.a.length;
        final $Item[] e = new $Item[length];
        for (int i = 1; i < length; ++i) {
            final int n = this.a[i];
            final byte b = this.b[n - 1];
            final $Item $Item = new $Item(i);
            switch (b) {
                case 9:
                case 10:
                case 11: {
                    final int n2 = this.a[this.readUnsignedShort(n + 2)];
                    $Item.a(b, this.readClass(n, array), this.readUTF8(n2, array), this.readUTF8(n2 + 2, array));
                    break;
                }
                case 3: {
                    $Item.a(this.readInt(n));
                    break;
                }
                case 4: {
                    $Item.a(Float.intBitsToFloat(this.readInt(n)));
                    break;
                }
                case 12: {
                    $Item.a(b, this.readUTF8(n, array), this.readUTF8(n + 2, array), null);
                    break;
                }
                case 5: {
                    $Item.a(this.readLong(n));
                    ++i;
                    break;
                }
                case 6: {
                    $Item.a(Double.longBitsToDouble(this.readLong(n)));
                    ++i;
                    break;
                }
                case 1: {
                    String s = this.c[i];
                    if (s == null) {
                        final int n3 = this.a[i];
                        final String[] c = this.c;
                        final int n4 = i;
                        final String a = this.a(n3 + 2, this.readUnsignedShort(n3), array);
                        c[n4] = a;
                        s = a;
                    }
                    $Item.a(b, s, null, null);
                    break;
                }
                case 15: {
                    final int n5 = this.a[this.readUnsignedShort(n + 1)];
                    final int n6 = this.a[this.readUnsignedShort(n5 + 2)];
                    $Item.a(20 + this.readByte(n), this.readClass(n5, array), this.readUTF8(n6, array), this.readUTF8(n6 + 2, array));
                    break;
                }
                case 18: {
                    if ($ClassWriter.A == null) {
                        this.a($ClassWriter, e, array);
                    }
                    final int n7 = this.a[this.readUnsignedShort(n + 2)];
                    $Item.a(this.readUTF8(n7, array), this.readUTF8(n7 + 2, array), this.readUnsignedShort(n));
                    break;
                }
                default: {
                    $Item.a(b, this.readUTF8(n, array), null, null);
                    break;
                }
            }
            final int n8 = $Item.j % e.length;
            $Item.k = e[n8];
            e[n8] = $Item;
        }
        final int n9 = this.a[1] - 1;
        $ClassWriter.d.putByteArray(this.b, n9, this.header - n9);
        $ClassWriter.e = e;
        $ClassWriter.f = (int)(0.75 * length);
        $ClassWriter.c = length;
    }
    
    private void a(final $ClassWriter $ClassWriter, final $Item[] array, final char[] array2) {
        int a = this.a();
        boolean b = false;
        for (int i = this.readUnsignedShort(a); i > 0; --i) {
            if ("BootstrapMethods".equals(this.readUTF8(a + 2, array2))) {
                b = true;
                break;
            }
            a += 6 + this.readInt(a + 4);
        }
        if (!b) {
            return;
        }
        final int unsignedShort = this.readUnsignedShort(a + 8);
        int j = 0;
        int n = a + 10;
        while (j < unsignedShort) {
            final int n2 = n - a - 10;
            int hashCode = this.readConst(this.readUnsignedShort(n), array2).hashCode();
            for (int k = this.readUnsignedShort(n + 2); k > 0; --k) {
                hashCode ^= this.readConst(this.readUnsignedShort(n + 4), array2).hashCode();
                n += 2;
            }
            n += 4;
            final $Item $Item = new $Item(j);
            $Item.a(n2, hashCode & Integer.MAX_VALUE);
            final int n3 = $Item.j % array.length;
            $Item.k = array[n3];
            array[n3] = $Item;
            ++j;
        }
        final int int1 = this.readInt(a + 4);
        final $ByteVector a2 = new $ByteVector(int1 + 62);
        a2.putByteArray(this.b, a + 10, int1 - 2);
        $ClassWriter.z = unsignedShort;
        $ClassWriter.A = a2;
    }
    
    public $ClassReader(final InputStream inputStream) throws IOException {
        this(a(inputStream, false));
    }
    
    public $ClassReader(final String s) throws IOException {
        this(a(ClassLoader.getSystemResourceAsStream(s.replace('.', '/') + ".class"), true));
    }
    
    private static byte[] a(final InputStream inputStream, final boolean b) throws IOException {
        if (inputStream == null) {
            throw new IOException("Class not found");
        }
        try {
            byte[] array = new byte[inputStream.available()];
            int n = 0;
            while (true) {
                final int read = inputStream.read(array, n, array.length - n);
                if (read == -1) {
                    if (n < array.length) {
                        final byte[] array2 = new byte[n];
                        System.arraycopy(array, 0, array2, 0, n);
                        array = array2;
                    }
                    return array;
                }
                n += read;
                if (n != array.length) {
                    continue;
                }
                final int read2 = inputStream.read();
                if (read2 < 0) {
                    return array;
                }
                final byte[] array3 = new byte[array.length + 1000];
                System.arraycopy(array, 0, array3, 0, n);
                array3[n++] = (byte)read2;
                array = array3;
            }
        }
        finally {
            if (b) {
                inputStream.close();
            }
        }
    }
    
    public void accept(final $ClassVisitor $ClassVisitor, final int n) {
        this.accept($ClassVisitor, new $Attribute[0], n);
    }
    
    public void accept(final $ClassVisitor $ClassVisitor, final $Attribute[] a, final int b) {
        int header = this.header;
        final char[] c = new char[this.d];
        final $Context $Context = new $Context();
        $Context.a = a;
        $Context.b = b;
        $Context.c = c;
        int unsignedShort = this.readUnsignedShort(header);
        final String class1 = this.readClass(header + 2, c);
        final String class2 = this.readClass(header + 4, c);
        final String[] array = new String[this.readUnsignedShort(header + 6)];
        header += 8;
        for (int i = 0; i < array.length; ++i) {
            array[i] = this.readClass(header, c);
            header += 2;
        }
        String utf8 = null;
        String utf9 = null;
        String a2 = null;
        String class3 = null;
        String utf10 = null;
        String utf11 = null;
        int n = 0;
        int n2 = 0;
        int n3 = 0;
        int n4 = 0;
        int n5 = 0;
        $Attribute a3 = null;
        int a4 = this.a();
        for (int j = this.readUnsignedShort(a4); j > 0; --j) {
            final String utf12 = this.readUTF8(a4 + 2, c);
            if ("SourceFile".equals(utf12)) {
                utf9 = this.readUTF8(a4 + 8, c);
            }
            else if ("InnerClasses".equals(utf12)) {
                n5 = a4 + 8;
            }
            else if ("EnclosingMethod".equals(utf12)) {
                class3 = this.readClass(a4 + 8, c);
                final int unsignedShort2 = this.readUnsignedShort(a4 + 10);
                if (unsignedShort2 != 0) {
                    utf10 = this.readUTF8(this.a[unsignedShort2], c);
                    utf11 = this.readUTF8(this.a[unsignedShort2] + 2, c);
                }
            }
            else if ("Signature".equals(utf12)) {
                utf8 = this.readUTF8(a4 + 8, c);
            }
            else if ("RuntimeVisibleAnnotations".equals(utf12)) {
                n = a4 + 8;
            }
            else if ("RuntimeVisibleTypeAnnotations".equals(utf12)) {
                n3 = a4 + 8;
            }
            else if ("Deprecated".equals(utf12)) {
                unsignedShort |= 0x20000;
            }
            else if ("Synthetic".equals(utf12)) {
                unsignedShort |= 0x41000;
            }
            else if ("SourceDebugExtension".equals(utf12)) {
                final int int1 = this.readInt(a4 + 4);
                a2 = this.a(a4 + 8, int1, new char[int1]);
            }
            else if ("RuntimeInvisibleAnnotations".equals(utf12)) {
                n2 = a4 + 8;
            }
            else if ("RuntimeInvisibleTypeAnnotations".equals(utf12)) {
                n4 = a4 + 8;
            }
            else if ("BootstrapMethods".equals(utf12)) {
                final int[] d = new int[this.readUnsignedShort(a4 + 8)];
                int k = 0;
                int n6 = a4 + 10;
                while (k < d.length) {
                    d[k] = n6;
                    n6 += 2 + this.readUnsignedShort(n6 + 2) << 1;
                    ++k;
                }
                $Context.d = d;
            }
            else {
                final $Attribute a5 = this.a(a, utf12, a4 + 8, this.readInt(a4 + 4), c, -1, null);
                if (a5 != null) {
                    a5.a = a3;
                    a3 = a5;
                }
            }
            a4 += 6 + this.readInt(a4 + 4);
        }
        $ClassVisitor.visit(this.readInt(this.a[1] - 7), unsignedShort, class1, utf8, class2, array);
        if ((b & 0x2) == 0x0 && (utf9 != null || a2 != null)) {
            $ClassVisitor.visitSource(utf9, a2);
        }
        if (class3 != null) {
            $ClassVisitor.visitOuterClass(class3, utf10, utf11);
        }
        if (n != 0) {
            int l = this.readUnsignedShort(n);
            int a6 = n + 2;
            while (l > 0) {
                a6 = this.a(a6 + 2, c, true, $ClassVisitor.visitAnnotation(this.readUTF8(a6, c), true));
                --l;
            }
        }
        if (n2 != 0) {
            int unsignedShort3 = this.readUnsignedShort(n2);
            int a7 = n2 + 2;
            while (unsignedShort3 > 0) {
                a7 = this.a(a7 + 2, c, true, $ClassVisitor.visitAnnotation(this.readUTF8(a7, c), false));
                --unsignedShort3;
            }
        }
        if (n3 != 0) {
            int unsignedShort4 = this.readUnsignedShort(n3);
            int a8 = n3 + 2;
            while (unsignedShort4 > 0) {
                final int a9 = this.a($Context, a8);
                a8 = this.a(a9 + 2, c, true, $ClassVisitor.visitTypeAnnotation($Context.i, $Context.j, this.readUTF8(a9, c), true));
                --unsignedShort4;
            }
        }
        if (n4 != 0) {
            int unsignedShort5 = this.readUnsignedShort(n4);
            int a10 = n4 + 2;
            while (unsignedShort5 > 0) {
                final int a11 = this.a($Context, a10);
                a10 = this.a(a11 + 2, c, true, $ClassVisitor.visitTypeAnnotation($Context.i, $Context.j, this.readUTF8(a11, c), false));
                --unsignedShort5;
            }
        }
        while (a3 != null) {
            final $Attribute a12 = a3.a;
            a3.a = null;
            $ClassVisitor.visitAttribute(a3);
            a3 = a12;
        }
        if (n5 != 0) {
            int n7 = n5 + 2;
            for (int unsignedShort6 = this.readUnsignedShort(n5); unsignedShort6 > 0; --unsignedShort6) {
                $ClassVisitor.visitInnerClass(this.readClass(n7, c), this.readClass(n7 + 2, c), this.readUTF8(n7 + 4, c), this.readUnsignedShort(n7 + 6));
                n7 += 8;
            }
        }
        int n8 = this.header + 10 + 2 * array.length;
        for (int unsignedShort7 = this.readUnsignedShort(n8 - 2); unsignedShort7 > 0; --unsignedShort7) {
            n8 = this.a($ClassVisitor, $Context, n8);
        }
        n8 += 2;
        for (int unsignedShort8 = this.readUnsignedShort(n8 - 2); unsignedShort8 > 0; --unsignedShort8) {
            n8 = this.b($ClassVisitor, $Context, n8);
        }
        $ClassVisitor.visitEnd();
    }
    
    private int a(final $ClassVisitor $ClassVisitor, final $Context $Context, int n) {
        final char[] c = $Context.c;
        int unsignedShort = this.readUnsignedShort(n);
        final String utf8 = this.readUTF8(n + 2, c);
        final String utf9 = this.readUTF8(n + 4, c);
        n += 6;
        String utf10 = null;
        int n2 = 0;
        int n3 = 0;
        int n4 = 0;
        int n5 = 0;
        Object o = null;
        $Attribute a = null;
        for (int i = this.readUnsignedShort(n); i > 0; --i) {
            final String utf11 = this.readUTF8(n + 2, c);
            if ("ConstantValue".equals(utf11)) {
                final int unsignedShort2 = this.readUnsignedShort(n + 8);
                o = ((unsignedShort2 == 0) ? null : this.readConst(unsignedShort2, c));
            }
            else if ("Signature".equals(utf11)) {
                utf10 = this.readUTF8(n + 8, c);
            }
            else if ("Deprecated".equals(utf11)) {
                unsignedShort |= 0x20000;
            }
            else if ("Synthetic".equals(utf11)) {
                unsignedShort |= 0x41000;
            }
            else if ("RuntimeVisibleAnnotations".equals(utf11)) {
                n2 = n + 8;
            }
            else if ("RuntimeVisibleTypeAnnotations".equals(utf11)) {
                n4 = n + 8;
            }
            else if ("RuntimeInvisibleAnnotations".equals(utf11)) {
                n3 = n + 8;
            }
            else if ("RuntimeInvisibleTypeAnnotations".equals(utf11)) {
                n5 = n + 8;
            }
            else {
                final $Attribute a2 = this.a($Context.a, utf11, n + 8, this.readInt(n + 4), c, -1, null);
                if (a2 != null) {
                    a2.a = a;
                    a = a2;
                }
            }
            n += 6 + this.readInt(n + 4);
        }
        n += 2;
        final $FieldVisitor visitField = $ClassVisitor.visitField(unsignedShort, utf8, utf9, utf10, o);
        if (visitField == null) {
            return n;
        }
        if (n2 != 0) {
            int j = this.readUnsignedShort(n2);
            int a3 = n2 + 2;
            while (j > 0) {
                a3 = this.a(a3 + 2, c, true, visitField.visitAnnotation(this.readUTF8(a3, c), true));
                --j;
            }
        }
        if (n3 != 0) {
            int k = this.readUnsignedShort(n3);
            int a4 = n3 + 2;
            while (k > 0) {
                a4 = this.a(a4 + 2, c, true, visitField.visitAnnotation(this.readUTF8(a4, c), false));
                --k;
            }
        }
        if (n4 != 0) {
            int l = this.readUnsignedShort(n4);
            int a5 = n4 + 2;
            while (l > 0) {
                final int a6 = this.a($Context, a5);
                a5 = this.a(a6 + 2, c, true, visitField.visitTypeAnnotation($Context.i, $Context.j, this.readUTF8(a6, c), true));
                --l;
            }
        }
        if (n5 != 0) {
            int unsignedShort3 = this.readUnsignedShort(n5);
            int a7 = n5 + 2;
            while (unsignedShort3 > 0) {
                final int a8 = this.a($Context, a7);
                a7 = this.a(a8 + 2, c, true, visitField.visitTypeAnnotation($Context.i, $Context.j, this.readUTF8(a8, c), false));
                --unsignedShort3;
            }
        }
        while (a != null) {
            final $Attribute a9 = a.a;
            a.a = null;
            visitField.visitAttribute(a);
            a = a9;
        }
        visitField.visitEnd();
        return n;
    }
    
    private int b(final $ClassVisitor $ClassVisitor, final $Context $Context, int n) {
        final char[] c = $Context.c;
        $Context.e = this.readUnsignedShort(n);
        $Context.f = this.readUTF8(n + 2, c);
        $Context.g = this.readUTF8(n + 4, c);
        n += 6;
        int n2 = 0;
        int n3 = 0;
        String[] array = null;
        String utf8 = null;
        int n4 = 0;
        int n5 = 0;
        int n6 = 0;
        int n7 = 0;
        int n8 = 0;
        int n9 = 0;
        int n10 = 0;
        int n11 = 0;
        final int h = n;
        $Attribute a = null;
        for (int i = this.readUnsignedShort(n); i > 0; --i) {
            final String utf9 = this.readUTF8(n + 2, c);
            if ("Code".equals(utf9)) {
                if (($Context.b & 0x1) == 0x0) {
                    n2 = n + 8;
                }
            }
            else if ("Exceptions".equals(utf9)) {
                array = new String[this.readUnsignedShort(n + 8)];
                n3 = n + 10;
                for (int j = 0; j < array.length; ++j) {
                    array[j] = this.readClass(n3, c);
                    n3 += 2;
                }
            }
            else if ("Signature".equals(utf9)) {
                utf8 = this.readUTF8(n + 8, c);
            }
            else if ("Deprecated".equals(utf9)) {
                $Context.e |= 0x20000;
            }
            else if ("RuntimeVisibleAnnotations".equals(utf9)) {
                n5 = n + 8;
            }
            else if ("RuntimeVisibleTypeAnnotations".equals(utf9)) {
                n7 = n + 8;
            }
            else if ("AnnotationDefault".equals(utf9)) {
                n9 = n + 8;
            }
            else if ("Synthetic".equals(utf9)) {
                $Context.e |= 0x41000;
            }
            else if ("RuntimeInvisibleAnnotations".equals(utf9)) {
                n6 = n + 8;
            }
            else if ("RuntimeInvisibleTypeAnnotations".equals(utf9)) {
                n8 = n + 8;
            }
            else if ("RuntimeVisibleParameterAnnotations".equals(utf9)) {
                n10 = n + 8;
            }
            else if ("RuntimeInvisibleParameterAnnotations".equals(utf9)) {
                n11 = n + 8;
            }
            else if ("MethodParameters".equals(utf9)) {
                n4 = n + 8;
            }
            else {
                final $Attribute a2 = this.a($Context.a, utf9, n + 8, this.readInt(n + 4), c, -1, null);
                if (a2 != null) {
                    a2.a = a;
                    a = a2;
                }
            }
            n += 6 + this.readInt(n + 4);
        }
        n += 2;
        final $MethodVisitor visitMethod = $ClassVisitor.visitMethod($Context.e, $Context.f, $Context.g, utf8, array);
        if (visitMethod == null) {
            return n;
        }
        if (visitMethod instanceof $MethodWriter) {
            final $MethodWriter $MethodWriter = ($MethodWriter)visitMethod;
            if ($MethodWriter.b.K == this && utf8 == $MethodWriter.g) {
                int n12 = 0;
                if (array == null) {
                    n12 = (($MethodWriter.j == 0) ? 1 : 0);
                }
                else if (array.length == $MethodWriter.j) {
                    n12 = 1;
                    for (int k = array.length - 1; k >= 0; --k) {
                        n3 -= 2;
                        if ($MethodWriter.k[k] != this.readUnsignedShort(n3)) {
                            n12 = 0;
                            break;
                        }
                    }
                }
                if (n12 != 0) {
                    $MethodWriter.h = h;
                    $MethodWriter.i = n - h;
                    return n;
                }
            }
        }
        if (n4 != 0) {
            for (int l = this.b[n4] & 0xFF, n13 = n4 + 1; l > 0; --l, n13 += 4) {
                visitMethod.visitParameter(this.readUTF8(n13, c), this.readUnsignedShort(n13 + 2));
            }
        }
        if (n9 != 0) {
            final $AnnotationVisitor visitAnnotationDefault = visitMethod.visitAnnotationDefault();
            this.a(n9, c, null, visitAnnotationDefault);
            if (visitAnnotationDefault != null) {
                visitAnnotationDefault.visitEnd();
            }
        }
        if (n5 != 0) {
            int unsignedShort = this.readUnsignedShort(n5);
            int a3 = n5 + 2;
            while (unsignedShort > 0) {
                a3 = this.a(a3 + 2, c, true, visitMethod.visitAnnotation(this.readUTF8(a3, c), true));
                --unsignedShort;
            }
        }
        if (n6 != 0) {
            int unsignedShort2 = this.readUnsignedShort(n6);
            int a4 = n6 + 2;
            while (unsignedShort2 > 0) {
                a4 = this.a(a4 + 2, c, true, visitMethod.visitAnnotation(this.readUTF8(a4, c), false));
                --unsignedShort2;
            }
        }
        if (n7 != 0) {
            int unsignedShort3 = this.readUnsignedShort(n7);
            int a5 = n7 + 2;
            while (unsignedShort3 > 0) {
                final int a6 = this.a($Context, a5);
                a5 = this.a(a6 + 2, c, true, visitMethod.visitTypeAnnotation($Context.i, $Context.j, this.readUTF8(a6, c), true));
                --unsignedShort3;
            }
        }
        if (n8 != 0) {
            int unsignedShort4 = this.readUnsignedShort(n8);
            int a7 = n8 + 2;
            while (unsignedShort4 > 0) {
                final int a8 = this.a($Context, a7);
                a7 = this.a(a8 + 2, c, true, visitMethod.visitTypeAnnotation($Context.i, $Context.j, this.readUTF8(a8, c), false));
                --unsignedShort4;
            }
        }
        if (n10 != 0) {
            this.b(visitMethod, $Context, n10, true);
        }
        if (n11 != 0) {
            this.b(visitMethod, $Context, n11, false);
        }
        while (a != null) {
            final $Attribute a9 = a.a;
            a.a = null;
            visitMethod.visitAttribute(a);
            a = a9;
        }
        if (n2 != 0) {
            visitMethod.visitCode();
            this.a(visitMethod, $Context, n2);
        }
        visitMethod.visitEnd();
        return n;
    }
    
    private void a(final $MethodVisitor $MethodVisitor, final $Context $Context, int i) {
        final byte[] b = this.b;
        final char[] c = $Context.c;
        final int unsignedShort = this.readUnsignedShort(i);
        final int unsignedShort2 = this.readUnsignedShort(i + 2);
        final int int1 = this.readInt(i + 4);
        i += 8;
        final int n = i;
        final int n2 = i + int1;
        final $Label[] h = new $Label[int1 + 2];
        $Context.h = h;
        final $Label[] array = h;
        this.readLabel(int1 + 1, array);
        while (i < n2) {
            final int n3 = i - n;
            switch ($ClassWriter.a[b[i] & 0xFF]) {
                case 0:
                case 4: {
                    ++i;
                    continue;
                }
                case 9: {
                    this.readLabel(n3 + this.readShort(i + 1), array);
                    i += 3;
                    continue;
                }
                case 18: {
                    this.readLabel(n3 + this.readUnsignedShort(i + 1), array);
                    i += 3;
                    continue;
                }
                case 10: {
                    this.readLabel(n3 + this.readInt(i + 1), array);
                    i += 5;
                    continue;
                }
                case 17: {
                    if ((b[i + 1] & 0xFF) == 0x84) {
                        i += 6;
                        continue;
                    }
                    i += 4;
                    continue;
                }
                case 14: {
                    i = i + 4 - (n3 & 0x3);
                    this.readLabel(n3 + this.readInt(i), array);
                    for (int j = this.readInt(i + 8) - this.readInt(i + 4) + 1; j > 0; --j) {
                        this.readLabel(n3 + this.readInt(i + 12), array);
                        i += 4;
                    }
                    i += 12;
                    continue;
                }
                case 15: {
                    i = i + 4 - (n3 & 0x3);
                    this.readLabel(n3 + this.readInt(i), array);
                    for (int k = this.readInt(i + 4); k > 0; --k) {
                        this.readLabel(n3 + this.readInt(i + 12), array);
                        i += 8;
                    }
                    i += 8;
                    continue;
                }
                case 1:
                case 3:
                case 11: {
                    i += 2;
                    continue;
                }
                case 2:
                case 5:
                case 6:
                case 12:
                case 13: {
                    i += 3;
                    continue;
                }
                case 7:
                case 8: {
                    i += 5;
                    continue;
                }
                default: {
                    i += 4;
                    continue;
                }
            }
        }
        for (int l = this.readUnsignedShort(i); l > 0; --l) {
            $MethodVisitor.visitTryCatchBlock(this.readLabel(this.readUnsignedShort(i + 2), array), this.readLabel(this.readUnsignedShort(i + 4), array), this.readLabel(this.readUnsignedShort(i + 6), array), this.readUTF8(this.a[this.readUnsignedShort(i + 8)], c));
            i += 8;
        }
        i += 2;
        int[] a = null;
        int[] a2 = null;
        int n4 = 0;
        int n5 = 0;
        int n6 = -1;
        int n7 = -1;
        int n8 = 0;
        int n9 = 0;
        boolean b2 = true;
        final boolean b3 = ($Context.b & 0x8) != 0x0;
        int a3 = 0;
        int n10 = 0;
        int n11 = 0;
        $Context $Context2 = null;
        $Attribute a4 = null;
        for (int unsignedShort3 = this.readUnsignedShort(i); unsignedShort3 > 0; --unsignedShort3) {
            final String utf8 = this.readUTF8(i + 2, c);
            if ("LocalVariableTable".equals(utf8)) {
                if (($Context.b & 0x2) == 0x0) {
                    n8 = i + 8;
                    int unsignedShort4 = this.readUnsignedShort(i + 8);
                    int n12 = i;
                    while (unsignedShort4 > 0) {
                        final int unsignedShort5 = this.readUnsignedShort(n12 + 10);
                        if (array[unsignedShort5] == null) {
                            final $Label label = this.readLabel(unsignedShort5, array);
                            label.a |= 0x1;
                        }
                        final int n13 = unsignedShort5 + this.readUnsignedShort(n12 + 12);
                        if (array[n13] == null) {
                            final $Label label2 = this.readLabel(n13, array);
                            label2.a |= 0x1;
                        }
                        n12 += 10;
                        --unsignedShort4;
                    }
                }
            }
            else if ("LocalVariableTypeTable".equals(utf8)) {
                n9 = i + 8;
            }
            else if ("LineNumberTable".equals(utf8)) {
                if (($Context.b & 0x2) == 0x0) {
                    int unsignedShort6 = this.readUnsignedShort(i + 8);
                    int n14 = i;
                    while (unsignedShort6 > 0) {
                        final int unsignedShort7 = this.readUnsignedShort(n14 + 10);
                        if (array[unsignedShort7] == null) {
                            final $Label label3 = this.readLabel(unsignedShort7, array);
                            label3.a |= 0x1;
                        }
                        $Label m;
                        for (m = array[unsignedShort7]; m.b > 0; m = m.k) {
                            if (m.k == null) {
                                m.k = new $Label();
                            }
                        }
                        m.b = this.readUnsignedShort(n14 + 12);
                        n14 += 4;
                        --unsignedShort6;
                    }
                }
            }
            else if ("RuntimeVisibleTypeAnnotations".equals(utf8)) {
                a = this.a($MethodVisitor, $Context, i + 8, true);
                n6 = ((a.length == 0 || this.readByte(a[0]) < 67) ? -1 : this.readUnsignedShort(a[0] + 1));
            }
            else if ("RuntimeInvisibleTypeAnnotations".equals(utf8)) {
                a2 = this.a($MethodVisitor, $Context, i + 8, false);
                n7 = ((a2.length == 0 || this.readByte(a2[0]) < 67) ? -1 : this.readUnsignedShort(a2[0] + 1));
            }
            else if ("StackMapTable".equals(utf8)) {
                if (($Context.b & 0x4) == 0x0) {
                    a3 = i + 10;
                    n10 = this.readInt(i + 4);
                    n11 = this.readUnsignedShort(i + 8);
                }
            }
            else if ("StackMap".equals(utf8)) {
                if (($Context.b & 0x4) == 0x0) {
                    b2 = false;
                    a3 = i + 10;
                    n10 = this.readInt(i + 4);
                    n11 = this.readUnsignedShort(i + 8);
                }
            }
            else {
                for (int n15 = 0; n15 < $Context.a.length; ++n15) {
                    if ($Context.a[n15].type.equals(utf8)) {
                        final $Attribute read = $Context.a[n15].read(this, i + 8, this.readInt(i + 4), c, n - 8, array);
                        if (read != null) {
                            read.a = a4;
                            a4 = read;
                        }
                    }
                }
            }
            i += 6 + this.readInt(i + 4);
        }
        i += 2;
        if (a3 != 0) {
            $Context2 = $Context;
            $Context2.o = -1;
            $Context2.p = 0;
            $Context2.q = 0;
            $Context2.r = 0;
            $Context2.t = 0;
            $Context2.s = new Object[unsignedShort2];
            $Context2.u = new Object[unsignedShort];
            if (b3) {
                this.a($Context);
            }
            for (int n16 = a3; n16 < a3 + n10 - 2; ++n16) {
                if (b[n16] == 8) {
                    final int unsignedShort8 = this.readUnsignedShort(n16 + 1);
                    if (unsignedShort8 >= 0 && unsignedShort8 < int1 && (b[n + unsignedShort8] & 0xFF) == 0xBB) {
                        this.readLabel(unsignedShort8, array);
                    }
                }
            }
        }
        if (($Context.b & 0x100) != 0x0) {
            $MethodVisitor.visitFrame(-1, unsignedShort2, null, 0, null);
        }
        final int n17 = (($Context.b & 0x100) == 0x0) ? -33 : 0;
        i = n;
        while (i < n2) {
            final int n18 = i - n;
            final $Label $Label = array[n18];
            if ($Label != null) {
                $Label $Label2 = $Label.k;
                $Label.k = null;
                $MethodVisitor.visitLabel($Label);
                if (($Context.b & 0x2) == 0x0 && $Label.b > 0) {
                    $MethodVisitor.visitLineNumber($Label.b, $Label);
                    while ($Label2 != null) {
                        $MethodVisitor.visitLineNumber($Label2.b, $Label);
                        $Label2 = $Label2.k;
                    }
                }
            }
            while ($Context2 != null && ($Context2.o == n18 || $Context2.o == -1)) {
                if ($Context2.o != -1) {
                    if (!b2 || b3) {
                        $MethodVisitor.visitFrame(-1, $Context2.q, $Context2.s, $Context2.t, $Context2.u);
                    }
                    else {
                        $MethodVisitor.visitFrame($Context2.p, $Context2.r, $Context2.s, $Context2.t, $Context2.u);
                    }
                }
                if (n11 > 0) {
                    a3 = this.a(a3, b2, b3, $Context2);
                    --n11;
                }
                else {
                    $Context2 = null;
                }
            }
            int n19 = b[i] & 0xFF;
            switch ($ClassWriter.a[n19]) {
                case 0: {
                    $MethodVisitor.visitInsn(n19);
                    ++i;
                    break;
                }
                case 4: {
                    if (n19 > 54) {
                        n19 -= 59;
                        $MethodVisitor.visitVarInsn(54 + (n19 >> 2), n19 & 0x3);
                    }
                    else {
                        n19 -= 26;
                        $MethodVisitor.visitVarInsn(21 + (n19 >> 2), n19 & 0x3);
                    }
                    ++i;
                    break;
                }
                case 9: {
                    $MethodVisitor.visitJumpInsn(n19, array[n18 + this.readShort(i + 1)]);
                    i += 3;
                    break;
                }
                case 10: {
                    $MethodVisitor.visitJumpInsn(n19 + n17, array[n18 + this.readInt(i + 1)]);
                    i += 5;
                    break;
                }
                case 18: {
                    final int n20 = (n19 < 218) ? (n19 - 49) : (n19 - 20);
                    final $Label $Label3 = array[n18 + this.readUnsignedShort(i + 1)];
                    if (n20 == 167 || n20 == 168) {
                        $MethodVisitor.visitJumpInsn(n20 + 33, $Label3);
                    }
                    else {
                        final int n21 = (n20 <= 166) ? ((n20 + 1 ^ 0x1) - 1) : (n20 ^ 0x1);
                        final $Label $Label4 = new $Label();
                        $MethodVisitor.visitJumpInsn(n21, $Label4);
                        $MethodVisitor.visitJumpInsn(200, $Label3);
                        $MethodVisitor.visitLabel($Label4);
                        if (a3 != 0 && ($Context2 == null || $Context2.o != n18 + 3)) {
                            $MethodVisitor.visitFrame(256, 0, null, 0, null);
                        }
                    }
                    i += 3;
                    break;
                }
                case 17: {
                    final int n22 = b[i + 1] & 0xFF;
                    if (n22 == 132) {
                        $MethodVisitor.visitIincInsn(this.readUnsignedShort(i + 2), this.readShort(i + 4));
                        i += 6;
                        break;
                    }
                    $MethodVisitor.visitVarInsn(n22, this.readUnsignedShort(i + 2));
                    i += 4;
                    break;
                }
                case 14: {
                    i = i + 4 - (n18 & 0x3);
                    final int n23 = n18 + this.readInt(i);
                    final int int2 = this.readInt(i + 4);
                    final int int3 = this.readInt(i + 8);
                    final $Label[] array2 = new $Label[int3 - int2 + 1];
                    i += 12;
                    for (int n24 = 0; n24 < array2.length; ++n24) {
                        array2[n24] = array[n18 + this.readInt(i)];
                        i += 4;
                    }
                    $MethodVisitor.visitTableSwitchInsn(int2, int3, array[n23], array2);
                    break;
                }
                case 15: {
                    i = i + 4 - (n18 & 0x3);
                    final int n25 = n18 + this.readInt(i);
                    final int int4 = this.readInt(i + 4);
                    final int[] array3 = new int[int4];
                    final $Label[] array4 = new $Label[int4];
                    i += 8;
                    for (int n26 = 0; n26 < int4; ++n26) {
                        array3[n26] = this.readInt(i);
                        array4[n26] = array[n18 + this.readInt(i + 4)];
                        i += 8;
                    }
                    $MethodVisitor.visitLookupSwitchInsn(array[n25], array3, array4);
                    break;
                }
                case 3: {
                    $MethodVisitor.visitVarInsn(n19, b[i + 1] & 0xFF);
                    i += 2;
                    break;
                }
                case 1: {
                    $MethodVisitor.visitIntInsn(n19, b[i + 1]);
                    i += 2;
                    break;
                }
                case 2: {
                    $MethodVisitor.visitIntInsn(n19, this.readShort(i + 1));
                    i += 3;
                    break;
                }
                case 11: {
                    $MethodVisitor.visitLdcInsn(this.readConst(b[i + 1] & 0xFF, c));
                    i += 2;
                    break;
                }
                case 12: {
                    $MethodVisitor.visitLdcInsn(this.readConst(this.readUnsignedShort(i + 1), c));
                    i += 3;
                    break;
                }
                case 6:
                case 7: {
                    final int n27 = this.a[this.readUnsignedShort(i + 1)];
                    final boolean b4 = b[n27 - 1] == 11;
                    final String class1 = this.readClass(n27, c);
                    final int n28 = this.a[this.readUnsignedShort(n27 + 2)];
                    final String utf9 = this.readUTF8(n28, c);
                    final String utf10 = this.readUTF8(n28 + 2, c);
                    if (n19 < 182) {
                        $MethodVisitor.visitFieldInsn(n19, class1, utf9, utf10);
                    }
                    else {
                        $MethodVisitor.visitMethodInsn(n19, class1, utf9, utf10, b4);
                    }
                    if (n19 == 185) {
                        i += 5;
                        break;
                    }
                    i += 3;
                    break;
                }
                case 8: {
                    final int n29 = this.a[this.readUnsignedShort(i + 1)];
                    int n30 = $Context.d[this.readUnsignedShort(n29)];
                    final $Handle $Handle = ($Handle)this.readConst(this.readUnsignedShort(n30), c);
                    final int unsignedShort9 = this.readUnsignedShort(n30 + 2);
                    final Object[] array5 = new Object[unsignedShort9];
                    n30 += 4;
                    for (int n31 = 0; n31 < unsignedShort9; ++n31) {
                        array5[n31] = this.readConst(this.readUnsignedShort(n30), c);
                        n30 += 2;
                    }
                    final int n32 = this.a[this.readUnsignedShort(n29 + 2)];
                    $MethodVisitor.visitInvokeDynamicInsn(this.readUTF8(n32, c), this.readUTF8(n32 + 2, c), $Handle, array5);
                    i += 5;
                    break;
                }
                case 5: {
                    $MethodVisitor.visitTypeInsn(n19, this.readClass(i + 1, c));
                    i += 3;
                    break;
                }
                case 13: {
                    $MethodVisitor.visitIincInsn(b[i + 1] & 0xFF, b[i + 2]);
                    i += 3;
                    break;
                }
                default: {
                    $MethodVisitor.visitMultiANewArrayInsn(this.readClass(i + 1, c), b[i + 3] & 0xFF);
                    i += 4;
                    break;
                }
            }
            while (a != null && n4 < a.length && n6 <= n18) {
                if (n6 == n18) {
                    final int a5 = this.a($Context, a[n4]);
                    this.a(a5 + 2, c, true, $MethodVisitor.visitInsnAnnotation($Context.i, $Context.j, this.readUTF8(a5, c), true));
                }
                n6 = ((++n4 >= a.length || this.readByte(a[n4]) < 67) ? -1 : this.readUnsignedShort(a[n4] + 1));
            }
            while (a2 != null && n5 < a2.length && n7 <= n18) {
                if (n7 == n18) {
                    final int a6 = this.a($Context, a2[n5]);
                    this.a(a6 + 2, c, true, $MethodVisitor.visitInsnAnnotation($Context.i, $Context.j, this.readUTF8(a6, c), false));
                }
                n7 = ((++n5 >= a2.length || this.readByte(a2[n5]) < 67) ? -1 : this.readUnsignedShort(a2[n5] + 1));
            }
        }
        if (array[int1] != null) {
            $MethodVisitor.visitLabel(array[int1]);
        }
        if (($Context.b & 0x2) == 0x0 && n8 != 0) {
            int[] array6 = null;
            if (n9 != 0) {
                i = n9 + 2;
                array6 = new int[this.readUnsignedShort(n9) * 3];
                for (int length = array6.length; length > 0; array6[--length] = i + 6, array6[--length] = this.readUnsignedShort(i + 8), array6[--length] = this.readUnsignedShort(i), i += 10) {}
            }
            i = n8 + 2;
            for (int unsignedShort10 = this.readUnsignedShort(n8); unsignedShort10 > 0; --unsignedShort10) {
                final int unsignedShort11 = this.readUnsignedShort(i);
                final int unsignedShort12 = this.readUnsignedShort(i + 2);
                final int unsignedShort13 = this.readUnsignedShort(i + 8);
                String utf11 = null;
                if (array6 != null) {
                    for (int n33 = 0; n33 < array6.length; n33 += 3) {
                        if (array6[n33] == unsignedShort11 && array6[n33 + 1] == unsignedShort13) {
                            utf11 = this.readUTF8(array6[n33 + 2], c);
                            break;
                        }
                    }
                }
                $MethodVisitor.visitLocalVariable(this.readUTF8(i + 4, c), this.readUTF8(i + 6, c), utf11, array[unsignedShort11], array[unsignedShort11 + unsignedShort12], unsignedShort13);
                i += 10;
            }
        }
        if (a != null) {
            for (int n34 = 0; n34 < a.length; ++n34) {
                if (this.readByte(a[n34]) >> 1 == 32) {
                    final int a7 = this.a($Context, a[n34]);
                    this.a(a7 + 2, c, true, $MethodVisitor.visitLocalVariableAnnotation($Context.i, $Context.j, $Context.l, $Context.m, $Context.n, this.readUTF8(a7, c), true));
                }
            }
        }
        if (a2 != null) {
            for (int n35 = 0; n35 < a2.length; ++n35) {
                if (this.readByte(a2[n35]) >> 1 == 32) {
                    final int a8 = this.a($Context, a2[n35]);
                    this.a(a8 + 2, c, true, $MethodVisitor.visitLocalVariableAnnotation($Context.i, $Context.j, $Context.l, $Context.m, $Context.n, this.readUTF8(a8, c), false));
                }
            }
        }
        while (a4 != null) {
            final $Attribute a9 = a4.a;
            a4.a = null;
            $MethodVisitor.visitAttribute(a4);
            a4 = a9;
        }
        $MethodVisitor.visitMaxs(unsignedShort, unsignedShort2);
    }
    
    private int[] a(final $MethodVisitor $MethodVisitor, final $Context $Context, int n, final boolean b) {
        final char[] c = $Context.c;
        final int[] array = new int[this.readUnsignedShort(n)];
        n += 2;
        for (int i = 0; i < array.length; ++i) {
            array[i] = n;
            final int int1 = this.readInt(n);
            switch (int1 >>> 24) {
                case 0:
                case 1:
                case 22: {
                    n += 2;
                    break;
                }
                case 19:
                case 20:
                case 21: {
                    ++n;
                    break;
                }
                case 64:
                case 65: {
                    for (int j = this.readUnsignedShort(n + 1); j > 0; --j) {
                        final int unsignedShort = this.readUnsignedShort(n + 3);
                        final int unsignedShort2 = this.readUnsignedShort(n + 5);
                        this.readLabel(unsignedShort, $Context.h);
                        this.readLabel(unsignedShort + unsignedShort2, $Context.h);
                        n += 6;
                    }
                    n += 3;
                    break;
                }
                case 71:
                case 72:
                case 73:
                case 74:
                case 75: {
                    n += 4;
                    break;
                }
                default: {
                    n += 3;
                    break;
                }
            }
            final int byte1 = this.readByte(n);
            if (int1 >>> 24 == 66) {
                final $TypePath $TypePath = (byte1 == 0) ? null : new $TypePath(this.b, n);
                n += 1 + 2 * byte1;
                n = this.a(n + 2, c, true, $MethodVisitor.visitTryCatchAnnotation(int1, $TypePath, this.readUTF8(n, c), b));
            }
            else {
                n = this.a(n + 3 + 2 * byte1, c, true, null);
            }
        }
        return array;
    }
    
    private int a(final $Context $Context, int n) {
        final int int1 = this.readInt(n);
        int i = 0;
        switch (int1 >>> 24) {
            case 0:
            case 1:
            case 22: {
                i = (int1 & 0xFFFF0000);
                n += 2;
                break;
            }
            case 19:
            case 20:
            case 21: {
                i = (int1 & 0xFF000000);
                ++n;
                break;
            }
            case 64:
            case 65: {
                i = (int1 & 0xFF000000);
                final int unsignedShort = this.readUnsignedShort(n + 1);
                $Context.l = new $Label[unsignedShort];
                $Context.m = new $Label[unsignedShort];
                $Context.n = new int[unsignedShort];
                n += 3;
                for (int j = 0; j < unsignedShort; ++j) {
                    final int unsignedShort2 = this.readUnsignedShort(n);
                    final int unsignedShort3 = this.readUnsignedShort(n + 2);
                    $Context.l[j] = this.readLabel(unsignedShort2, $Context.h);
                    $Context.m[j] = this.readLabel(unsignedShort2 + unsignedShort3, $Context.h);
                    $Context.n[j] = this.readUnsignedShort(n + 4);
                    n += 6;
                }
                break;
            }
            case 71:
            case 72:
            case 73:
            case 74:
            case 75: {
                i = (int1 & 0xFF0000FF);
                n += 4;
                break;
            }
            default: {
                i = (int1 & ((int1 >>> 24 < 67) ? -256 : -16777216));
                n += 3;
                break;
            }
        }
        final int byte1 = this.readByte(n);
        $Context.i = i;
        $Context.j = ((byte1 == 0) ? null : new $TypePath(this.b, n));
        return n + 1 + 2 * byte1;
    }
    
    private void b(final $MethodVisitor $MethodVisitor, final $Context $Context, int a, final boolean b) {
        final int n = this.b[a++] & 0xFF;
        int n2;
        int i;
        for (n2 = $Type.getArgumentTypes($Context.g).length - n, i = 0; i < n2; ++i) {
            final $AnnotationVisitor visitParameterAnnotation = $MethodVisitor.visitParameterAnnotation(i, "Ljava/lang/Synthetic;", false);
            if (visitParameterAnnotation != null) {
                visitParameterAnnotation.visitEnd();
            }
        }
        final char[] c = $Context.c;
        while (i < n + n2) {
            int j = this.readUnsignedShort(a);
            a += 2;
            while (j > 0) {
                a = this.a(a + 2, c, true, $MethodVisitor.visitParameterAnnotation(i, this.readUTF8(a, c), b));
                --j;
            }
            ++i;
        }
    }
    
    private int a(int n, final char[] array, final boolean b, final $AnnotationVisitor $AnnotationVisitor) {
        int i = this.readUnsignedShort(n);
        n += 2;
        if (b) {
            while (i > 0) {
                n = this.a(n + 2, array, this.readUTF8(n, array), $AnnotationVisitor);
                --i;
            }
        }
        else {
            while (i > 0) {
                n = this.a(n, array, null, $AnnotationVisitor);
                --i;
            }
        }
        if ($AnnotationVisitor != null) {
            $AnnotationVisitor.visitEnd();
        }
        return n;
    }
    
    private int a(int n, final char[] array, final String s, final $AnnotationVisitor $AnnotationVisitor) {
        if ($AnnotationVisitor != null) {
            Label_1225: {
                switch (this.b[n++] & 0xFF) {
                    case 68:
                    case 70:
                    case 73:
                    case 74: {
                        $AnnotationVisitor.visit(s, this.readConst(this.readUnsignedShort(n), array));
                        n += 2;
                        break;
                    }
                    case 66: {
                        $AnnotationVisitor.visit(s, new Byte((byte)this.readInt(this.a[this.readUnsignedShort(n)])));
                        n += 2;
                        break;
                    }
                    case 90: {
                        $AnnotationVisitor.visit(s, (this.readInt(this.a[this.readUnsignedShort(n)]) == 0) ? Boolean.FALSE : Boolean.TRUE);
                        n += 2;
                        break;
                    }
                    case 83: {
                        $AnnotationVisitor.visit(s, new Short((short)this.readInt(this.a[this.readUnsignedShort(n)])));
                        n += 2;
                        break;
                    }
                    case 67: {
                        $AnnotationVisitor.visit(s, new Character((char)this.readInt(this.a[this.readUnsignedShort(n)])));
                        n += 2;
                        break;
                    }
                    case 115: {
                        $AnnotationVisitor.visit(s, this.readUTF8(n, array));
                        n += 2;
                        break;
                    }
                    case 101: {
                        $AnnotationVisitor.visitEnum(s, this.readUTF8(n, array), this.readUTF8(n + 2, array));
                        n += 4;
                        break;
                    }
                    case 99: {
                        $AnnotationVisitor.visit(s, $Type.getType(this.readUTF8(n, array)));
                        n += 2;
                        break;
                    }
                    case 64: {
                        n = this.a(n + 2, array, true, $AnnotationVisitor.visitAnnotation(s, this.readUTF8(n, array)));
                        break;
                    }
                    case 91: {
                        final int unsignedShort = this.readUnsignedShort(n);
                        n += 2;
                        if (unsignedShort == 0) {
                            return this.a(n - 2, array, false, $AnnotationVisitor.visitArray(s));
                        }
                        switch (this.b[n++] & 0xFF) {
                            case 66: {
                                final byte[] array2 = new byte[unsignedShort];
                                for (int i = 0; i < unsignedShort; ++i) {
                                    array2[i] = (byte)this.readInt(this.a[this.readUnsignedShort(n)]);
                                    n += 3;
                                }
                                $AnnotationVisitor.visit(s, array2);
                                --n;
                                break Label_1225;
                            }
                            case 90: {
                                final boolean[] array3 = new boolean[unsignedShort];
                                for (int j = 0; j < unsignedShort; ++j) {
                                    array3[j] = (this.readInt(this.a[this.readUnsignedShort(n)]) != 0);
                                    n += 3;
                                }
                                $AnnotationVisitor.visit(s, array3);
                                --n;
                                break Label_1225;
                            }
                            case 83: {
                                final short[] array4 = new short[unsignedShort];
                                for (int k = 0; k < unsignedShort; ++k) {
                                    array4[k] = (short)this.readInt(this.a[this.readUnsignedShort(n)]);
                                    n += 3;
                                }
                                $AnnotationVisitor.visit(s, array4);
                                --n;
                                break Label_1225;
                            }
                            case 67: {
                                final char[] array5 = new char[unsignedShort];
                                for (int l = 0; l < unsignedShort; ++l) {
                                    array5[l] = (char)this.readInt(this.a[this.readUnsignedShort(n)]);
                                    n += 3;
                                }
                                $AnnotationVisitor.visit(s, array5);
                                --n;
                                break Label_1225;
                            }
                            case 73: {
                                final int[] array6 = new int[unsignedShort];
                                for (int n2 = 0; n2 < unsignedShort; ++n2) {
                                    array6[n2] = this.readInt(this.a[this.readUnsignedShort(n)]);
                                    n += 3;
                                }
                                $AnnotationVisitor.visit(s, array6);
                                --n;
                                break Label_1225;
                            }
                            case 74: {
                                final long[] array7 = new long[unsignedShort];
                                for (int n3 = 0; n3 < unsignedShort; ++n3) {
                                    array7[n3] = this.readLong(this.a[this.readUnsignedShort(n)]);
                                    n += 3;
                                }
                                $AnnotationVisitor.visit(s, array7);
                                --n;
                                break Label_1225;
                            }
                            case 70: {
                                final float[] array8 = new float[unsignedShort];
                                for (int n4 = 0; n4 < unsignedShort; ++n4) {
                                    array8[n4] = Float.intBitsToFloat(this.readInt(this.a[this.readUnsignedShort(n)]));
                                    n += 3;
                                }
                                $AnnotationVisitor.visit(s, array8);
                                --n;
                                break Label_1225;
                            }
                            case 68: {
                                final double[] array9 = new double[unsignedShort];
                                for (int n5 = 0; n5 < unsignedShort; ++n5) {
                                    array9[n5] = Double.longBitsToDouble(this.readLong(this.a[this.readUnsignedShort(n)]));
                                    n += 3;
                                }
                                $AnnotationVisitor.visit(s, array9);
                                --n;
                                break Label_1225;
                            }
                            default: {
                                n = this.a(n - 3, array, false, $AnnotationVisitor.visitArray(s));
                                break Label_1225;
                            }
                        }
                        break;
                    }
                }
            }
            return n;
        }
        switch (this.b[n] & 0xFF) {
            case 101: {
                return n + 5;
            }
            case 64: {
                return this.a(n + 3, array, true, null);
            }
            case 91: {
                return this.a(n + 1, array, false, null);
            }
            default: {
                return n + 3;
            }
        }
    }
    
    private void a(final $Context $Context) {
        final String g = $Context.g;
        final Object[] s = $Context.s;
        int q = 0;
        if (($Context.e & 0x8) == 0x0) {
            if ("<init>".equals($Context.f)) {
                s[q++] = $Opcodes.UNINITIALIZED_THIS;
            }
            else {
                s[q++] = this.readClass(this.header + 2, $Context.c);
            }
        }
        int n = 1;
        while (true) {
            final int n2 = n;
            switch (g.charAt(n++)) {
                case 'B':
                case 'C':
                case 'I':
                case 'S':
                case 'Z': {
                    s[q++] = $Opcodes.INTEGER;
                    continue;
                }
                case 'F': {
                    s[q++] = $Opcodes.FLOAT;
                    continue;
                }
                case 'J': {
                    s[q++] = $Opcodes.LONG;
                    continue;
                }
                case 'D': {
                    s[q++] = $Opcodes.DOUBLE;
                    continue;
                }
                case '[': {
                    while (g.charAt(n) == '[') {
                        ++n;
                    }
                    if (g.charAt(n) == 'L') {
                        ++n;
                        while (g.charAt(n) != ';') {
                            ++n;
                        }
                    }
                    s[q++] = g.substring(n2, ++n);
                    continue;
                }
                case 'L': {
                    while (g.charAt(n) != ';') {
                        ++n;
                    }
                    s[q++] = g.substring(n2 + 1, n++);
                    continue;
                }
                default: {
                    $Context.q = q;
                }
            }
        }
    }
    
    private int a(int n, final boolean b, final boolean b2, final $Context $Context) {
        final char[] c = $Context.c;
        final $Label[] h = $Context.h;
        int n2;
        if (b) {
            n2 = (this.b[n++] & 0xFF);
        }
        else {
            n2 = 255;
            $Context.o = -1;
        }
        $Context.r = 0;
        int unsignedShort;
        if (n2 < 64) {
            unsignedShort = n2;
            $Context.p = 3;
            $Context.t = 0;
        }
        else if (n2 < 128) {
            unsignedShort = n2 - 64;
            n = this.a($Context.u, 0, n, c, h);
            $Context.p = 4;
            $Context.t = 1;
        }
        else {
            unsignedShort = this.readUnsignedShort(n);
            n += 2;
            if (n2 == 247) {
                n = this.a($Context.u, 0, n, c, h);
                $Context.p = 4;
                $Context.t = 1;
            }
            else if (n2 >= 248 && n2 < 251) {
                $Context.p = 2;
                $Context.r = 251 - n2;
                $Context.q -= $Context.r;
                $Context.t = 0;
            }
            else if (n2 == 251) {
                $Context.p = 3;
                $Context.t = 0;
            }
            else if (n2 < 255) {
                int n3 = b2 ? $Context.q : 0;
                for (int i = n2 - 251; i > 0; --i) {
                    n = this.a($Context.s, n3++, n, c, h);
                }
                $Context.p = 1;
                $Context.r = n2 - 251;
                $Context.q += $Context.r;
                $Context.t = 0;
            }
            else {
                $Context.p = 0;
                int j = this.readUnsignedShort(n);
                n += 2;
                $Context.r = j;
                $Context.q = j;
                int n4 = 0;
                while (j > 0) {
                    n = this.a($Context.s, n4++, n, c, h);
                    --j;
                }
                int k = this.readUnsignedShort(n);
                n += 2;
                $Context.t = k;
                int n5 = 0;
                while (k > 0) {
                    n = this.a($Context.u, n5++, n, c, h);
                    --k;
                }
            }
        }
        this.readLabel($Context.o += unsignedShort + 1, h);
        return n;
    }
    
    private int a(final Object[] array, final int n, int n2, final char[] array2, final $Label[] array3) {
        switch (this.b[n2++] & 0xFF) {
            case 0: {
                array[n] = $Opcodes.TOP;
                break;
            }
            case 1: {
                array[n] = $Opcodes.INTEGER;
                break;
            }
            case 2: {
                array[n] = $Opcodes.FLOAT;
                break;
            }
            case 3: {
                array[n] = $Opcodes.DOUBLE;
                break;
            }
            case 4: {
                array[n] = $Opcodes.LONG;
                break;
            }
            case 5: {
                array[n] = $Opcodes.NULL;
                break;
            }
            case 6: {
                array[n] = $Opcodes.UNINITIALIZED_THIS;
                break;
            }
            case 7: {
                array[n] = this.readClass(n2, array2);
                n2 += 2;
                break;
            }
            default: {
                array[n] = this.readLabel(this.readUnsignedShort(n2), array3);
                n2 += 2;
                break;
            }
        }
        return n2;
    }
    
    protected $Label readLabel(final int n, final $Label[] array) {
        if (array[n] == null) {
            array[n] = new $Label();
        }
        return array[n];
    }
    
    private int a() {
        int n = this.header + 8 + this.readUnsignedShort(this.header + 6) * 2;
        for (int i = this.readUnsignedShort(n); i > 0; --i) {
            for (int j = this.readUnsignedShort(n + 8); j > 0; --j) {
                n += 6 + this.readInt(n + 12);
            }
            n += 8;
        }
        n += 2;
        for (int k = this.readUnsignedShort(n); k > 0; --k) {
            for (int l = this.readUnsignedShort(n + 8); l > 0; --l) {
                n += 6 + this.readInt(n + 12);
            }
            n += 8;
        }
        return n + 2;
    }
    
    private $Attribute a(final $Attribute[] array, final String s, final int n, final int n2, final char[] array2, final int n3, final $Label[] array3) {
        for (int i = 0; i < array.length; ++i) {
            if (array[i].type.equals(s)) {
                return array[i].read(this, n, n2, array2, n3, array3);
            }
        }
        return new $Attribute(s).read(this, n, n2, null, -1, null);
    }
    
    public int getItemCount() {
        return this.a.length;
    }
    
    public int getItem(final int n) {
        return this.a[n];
    }
    
    public int getMaxStringLength() {
        return this.d;
    }
    
    public int readByte(final int n) {
        return this.b[n] & 0xFF;
    }
    
    public int readUnsignedShort(final int n) {
        final byte[] b = this.b;
        return (b[n] & 0xFF) << 8 | (b[n + 1] & 0xFF);
    }
    
    public short readShort(final int n) {
        final byte[] b = this.b;
        return (short)((b[n] & 0xFF) << 8 | (b[n + 1] & 0xFF));
    }
    
    public int readInt(final int n) {
        final byte[] b = this.b;
        return (b[n] & 0xFF) << 24 | (b[n + 1] & 0xFF) << 16 | (b[n + 2] & 0xFF) << 8 | (b[n + 3] & 0xFF);
    }
    
    public long readLong(final int n) {
        return (long)this.readInt(n) << 32 | ((long)this.readInt(n + 4) & 0xFFFFFFFFL);
    }
    
    public String readUTF8(int n, final char[] array) {
        final int unsignedShort = this.readUnsignedShort(n);
        if (n == 0 || unsignedShort == 0) {
            return null;
        }
        final String s = this.c[unsignedShort];
        if (s != null) {
            return s;
        }
        n = this.a[unsignedShort];
        return this.c[unsignedShort] = this.a(n + 2, this.readUnsignedShort(n), array);
    }
    
    private String a(int i, final int n, final char[] array) {
        final int n2 = i + n;
        final byte[] b = this.b;
        int n3 = 0;
        int n4 = 0;
        int n5 = 0;
        while (i < n2) {
            final byte b2 = b[i++];
            switch (n4) {
                case 0: {
                    final int n6 = b2 & 0xFF;
                    if (n6 < 128) {
                        array[n3++] = (char)n6;
                        continue;
                    }
                    if (n6 < 224 && n6 > 191) {
                        n5 = (char)(n6 & 0x1F);
                        n4 = 1;
                        continue;
                    }
                    n5 = (char)(n6 & 0xF);
                    n4 = 2;
                    continue;
                }
                case 1: {
                    array[n3++] = (char)(n5 << 6 | (b2 & 0x3F));
                    n4 = 0;
                    continue;
                }
                case 2: {
                    n5 = (char)(n5 << 6 | (b2 & 0x3F));
                    n4 = 1;
                    continue;
                }
            }
        }
        return new String(array, 0, n3);
    }
    
    public String readClass(final int n, final char[] array) {
        return this.readUTF8(this.a[this.readUnsignedShort(n)], array);
    }
    
    public Object readConst(final int n, final char[] array) {
        final int n2 = this.a[n];
        switch (this.b[n2 - 1]) {
            case 3: {
                return new Integer(this.readInt(n2));
            }
            case 4: {
                return new Float(Float.intBitsToFloat(this.readInt(n2)));
            }
            case 5: {
                return new Long(this.readLong(n2));
            }
            case 6: {
                return new Double(Double.longBitsToDouble(this.readLong(n2)));
            }
            case 7: {
                return $Type.getObjectType(this.readUTF8(n2, array));
            }
            case 8: {
                return this.readUTF8(n2, array);
            }
            case 16: {
                return $Type.getMethodType(this.readUTF8(n2, array));
            }
            default: {
                final int byte1 = this.readByte(n2);
                final int[] a = this.a;
                final int n3 = a[this.readUnsignedShort(n2 + 1)];
                final boolean b = this.b[n3 - 1] == 11;
                final String class1 = this.readClass(n3, array);
                final int n4 = a[this.readUnsignedShort(n3 + 2)];
                return new $Handle(byte1, class1, this.readUTF8(n4, array), this.readUTF8(n4 + 2, array), b);
            }
        }
    }
}
