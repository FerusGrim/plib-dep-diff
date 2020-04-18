package com.comphenix.net.sf.cglib.asm;

public class $ClassWriter extends $ClassVisitor
{
    public static final int COMPUTE_MAXS = 1;
    public static final int COMPUTE_FRAMES = 2;
    static final byte[] a;
    $ClassReader K;
    int b;
    int c;
    final $ByteVector d;
    $Item[] e;
    int f;
    final $Item g;
    final $Item h;
    final $Item i;
    final $Item j;
    $Item[] H;
    private short G;
    private int k;
    private int l;
    String I;
    private int m;
    private int n;
    private int o;
    private int[] p;
    private int q;
    private $ByteVector r;
    private int s;
    private int t;
    private $AnnotationWriter u;
    private $AnnotationWriter v;
    private $AnnotationWriter N;
    private $AnnotationWriter O;
    private $Attribute w;
    private int x;
    private $ByteVector y;
    int z;
    $ByteVector A;
    $FieldWriter B;
    $FieldWriter C;
    $MethodWriter D;
    $MethodWriter E;
    private int F;
    boolean J;
    
    public $ClassWriter(final int n) {
        super(327680);
        this.c = 1;
        this.d = new $ByteVector();
        this.e = new $Item[256];
        this.f = (int)(0.75 * this.e.length);
        this.g = new $Item();
        this.h = new $Item();
        this.i = new $Item();
        this.j = new $Item();
        this.F = (((n & 0x2) != 0x0) ? 0 : (((n & 0x1) != 0x0) ? 2 : 3));
    }
    
    public $ClassWriter(final $ClassReader k, final int n) {
        this(n);
        k.a(this);
        this.K = k;
    }
    
    public final void visit(final int b, final int k, final String i, final String s, final String s2, final String[] array) {
        this.b = b;
        this.k = k;
        this.l = this.newClass(i);
        this.I = i;
        if (s != null) {
            this.m = this.newUTF8(s);
        }
        this.n = ((s2 == null) ? 0 : this.newClass(s2));
        if (array != null && array.length > 0) {
            this.o = array.length;
            this.p = new int[this.o];
            for (int j = 0; j < this.o; ++j) {
                this.p[j] = this.newClass(array[j]);
            }
        }
    }
    
    public final void visitSource(final String s, final String s2) {
        if (s != null) {
            this.q = this.newUTF8(s);
        }
        if (s2 != null) {
            this.r = new $ByteVector().c(s2, 0, Integer.MAX_VALUE);
        }
    }
    
    public final void visitOuterClass(final String s, final String s2, final String s3) {
        this.s = this.newClass(s);
        if (s2 != null && s3 != null) {
            this.t = this.newNameType(s2, s3);
        }
    }
    
    public final $AnnotationVisitor visitAnnotation(final String s, final boolean b) {
        final $ByteVector $ByteVector = new $ByteVector();
        $ByteVector.putShort(this.newUTF8(s)).putShort(0);
        final $AnnotationWriter $AnnotationWriter = new $AnnotationWriter(this, true, $ByteVector, $ByteVector, 2);
        if (b) {
            $AnnotationWriter.g = this.u;
            this.u = $AnnotationWriter;
        }
        else {
            $AnnotationWriter.g = this.v;
            this.v = $AnnotationWriter;
        }
        return $AnnotationWriter;
    }
    
    public final $AnnotationVisitor visitTypeAnnotation(final int n, final $TypePath $TypePath, final String s, final boolean b) {
        final $ByteVector $ByteVector = new $ByteVector();
        $AnnotationWriter.a(n, $TypePath, $ByteVector);
        $ByteVector.putShort(this.newUTF8(s)).putShort(0);
        final $AnnotationWriter $AnnotationWriter = new $AnnotationWriter(this, true, $ByteVector, $ByteVector, $ByteVector.b - 2);
        if (b) {
            $AnnotationWriter.g = this.N;
            this.N = $AnnotationWriter;
        }
        else {
            $AnnotationWriter.g = this.O;
            this.O = $AnnotationWriter;
        }
        return $AnnotationWriter;
    }
    
    public final void visitAttribute(final $Attribute w) {
        w.a = this.w;
        this.w = w;
    }
    
    public final void visitInnerClass(final String s, final String s2, final String s3, final int n) {
        if (this.y == null) {
            this.y = new $ByteVector();
        }
        final $Item a = this.a(s);
        if (a.c == 0) {
            ++this.x;
            this.y.putShort(a.a);
            this.y.putShort((s2 == null) ? 0 : this.newClass(s2));
            this.y.putShort((s3 == null) ? 0 : this.newUTF8(s3));
            this.y.putShort(n);
            a.c = this.x;
        }
    }
    
    public final $FieldVisitor visitField(final int n, final String s, final String s2, final String s3, final Object o) {
        return new $FieldWriter(this, n, s, s2, s3, o);
    }
    
    public final $MethodVisitor visitMethod(final int n, final String s, final String s2, final String s3, final String[] array) {
        return new $MethodWriter(this, n, s, s2, s3, array, this.F);
    }
    
    public final void visitEnd() {
    }
    
    public byte[] toByteArray() {
        if (this.c > 65535) {
            throw new RuntimeException("Class file too large!");
        }
        int n = 24 + 2 * this.o;
        int n2 = 0;
        for ($FieldWriter b = this.B; b != null; b = ($FieldWriter)b.fv) {
            ++n2;
            n += b.a();
        }
        int n3 = 0;
        for ($MethodWriter d = this.D; d != null; d = ($MethodWriter)d.mv) {
            ++n3;
            n += d.a();
        }
        int n4 = 0;
        if (this.A != null) {
            ++n4;
            n += 8 + this.A.b;
            this.newUTF8("BootstrapMethods");
        }
        if (this.m != 0) {
            ++n4;
            n += 8;
            this.newUTF8("Signature");
        }
        if (this.q != 0) {
            ++n4;
            n += 8;
            this.newUTF8("SourceFile");
        }
        if (this.r != null) {
            ++n4;
            n += this.r.b + 6;
            this.newUTF8("SourceDebugExtension");
        }
        if (this.s != 0) {
            ++n4;
            n += 10;
            this.newUTF8("EnclosingMethod");
        }
        if ((this.k & 0x20000) != 0x0) {
            ++n4;
            n += 6;
            this.newUTF8("Deprecated");
        }
        if ((this.k & 0x1000) != 0x0 && ((this.b & 0xFFFF) < 49 || (this.k & 0x40000) != 0x0)) {
            ++n4;
            n += 6;
            this.newUTF8("Synthetic");
        }
        if (this.y != null) {
            ++n4;
            n += 8 + this.y.b;
            this.newUTF8("InnerClasses");
        }
        if (this.u != null) {
            ++n4;
            n += 8 + this.u.a();
            this.newUTF8("RuntimeVisibleAnnotations");
        }
        if (this.v != null) {
            ++n4;
            n += 8 + this.v.a();
            this.newUTF8("RuntimeInvisibleAnnotations");
        }
        if (this.N != null) {
            ++n4;
            n += 8 + this.N.a();
            this.newUTF8("RuntimeVisibleTypeAnnotations");
        }
        if (this.O != null) {
            ++n4;
            n += 8 + this.O.a();
            this.newUTF8("RuntimeInvisibleTypeAnnotations");
        }
        if (this.w != null) {
            n4 += this.w.a();
            n += this.w.a(this, null, 0, -1, -1);
        }
        final $ByteVector $ByteVector = new $ByteVector(n + this.d.b);
        $ByteVector.putInt(-889275714).putInt(this.b);
        $ByteVector.putShort(this.c).putByteArray(this.d.a, 0, this.d.b);
        $ByteVector.putShort(this.k & ~(0x60000 | (this.k & 0x40000) / 64)).putShort(this.l).putShort(this.n);
        $ByteVector.putShort(this.o);
        for (int i = 0; i < this.o; ++i) {
            $ByteVector.putShort(this.p[i]);
        }
        $ByteVector.putShort(n2);
        for ($FieldWriter b2 = this.B; b2 != null; b2 = ($FieldWriter)b2.fv) {
            b2.a($ByteVector);
        }
        $ByteVector.putShort(n3);
        for ($MethodWriter d2 = this.D; d2 != null; d2 = ($MethodWriter)d2.mv) {
            d2.a($ByteVector);
        }
        $ByteVector.putShort(n4);
        if (this.A != null) {
            $ByteVector.putShort(this.newUTF8("BootstrapMethods"));
            $ByteVector.putInt(this.A.b + 2).putShort(this.z);
            $ByteVector.putByteArray(this.A.a, 0, this.A.b);
        }
        if (this.m != 0) {
            $ByteVector.putShort(this.newUTF8("Signature")).putInt(2).putShort(this.m);
        }
        if (this.q != 0) {
            $ByteVector.putShort(this.newUTF8("SourceFile")).putInt(2).putShort(this.q);
        }
        if (this.r != null) {
            final int b3 = this.r.b;
            $ByteVector.putShort(this.newUTF8("SourceDebugExtension")).putInt(b3);
            $ByteVector.putByteArray(this.r.a, 0, b3);
        }
        if (this.s != 0) {
            $ByteVector.putShort(this.newUTF8("EnclosingMethod")).putInt(4);
            $ByteVector.putShort(this.s).putShort(this.t);
        }
        if ((this.k & 0x20000) != 0x0) {
            $ByteVector.putShort(this.newUTF8("Deprecated")).putInt(0);
        }
        if ((this.k & 0x1000) != 0x0 && ((this.b & 0xFFFF) < 49 || (this.k & 0x40000) != 0x0)) {
            $ByteVector.putShort(this.newUTF8("Synthetic")).putInt(0);
        }
        if (this.y != null) {
            $ByteVector.putShort(this.newUTF8("InnerClasses"));
            $ByteVector.putInt(this.y.b + 2).putShort(this.x);
            $ByteVector.putByteArray(this.y.a, 0, this.y.b);
        }
        if (this.u != null) {
            $ByteVector.putShort(this.newUTF8("RuntimeVisibleAnnotations"));
            this.u.a($ByteVector);
        }
        if (this.v != null) {
            $ByteVector.putShort(this.newUTF8("RuntimeInvisibleAnnotations"));
            this.v.a($ByteVector);
        }
        if (this.N != null) {
            $ByteVector.putShort(this.newUTF8("RuntimeVisibleTypeAnnotations"));
            this.N.a($ByteVector);
        }
        if (this.O != null) {
            $ByteVector.putShort(this.newUTF8("RuntimeInvisibleTypeAnnotations"));
            this.O.a($ByteVector);
        }
        if (this.w != null) {
            this.w.a(this, null, 0, -1, -1, $ByteVector);
        }
        if (this.J) {
            this.u = null;
            this.v = null;
            this.w = null;
            this.x = 0;
            this.y = null;
            this.B = null;
            this.C = null;
            this.D = null;
            this.E = null;
            this.F = 1;
            this.J = false;
            new $ClassReader($ByteVector.a).accept(this, 264);
            return this.toByteArray();
        }
        return $ByteVector.a;
    }
    
    $Item a(final Object o) {
        if (o instanceof Integer) {
            return this.a((int)o);
        }
        if (o instanceof Byte) {
            return this.a((int)o);
        }
        if (o instanceof Character) {
            return this.a((char)o);
        }
        if (o instanceof Short) {
            return this.a((int)o);
        }
        if (o instanceof Boolean) {
            return this.a(((boolean)o) ? 1 : 0);
        }
        if (o instanceof Float) {
            return this.a((float)o);
        }
        if (o instanceof Long) {
            return this.a((long)o);
        }
        if (o instanceof Double) {
            return this.a((double)o);
        }
        if (o instanceof String) {
            return this.b((String)o);
        }
        if (o instanceof $Type) {
            final $Type $Type = ($Type)o;
            final int sort = $Type.getSort();
            if (sort == 10) {
                return this.a($Type.getInternalName());
            }
            if (sort == 11) {
                return this.c($Type.getDescriptor());
            }
            return this.a($Type.getDescriptor());
        }
        else {
            if (o instanceof $Handle) {
                final $Handle $Handle = ($Handle)o;
                return this.a($Handle.a, $Handle.b, $Handle.c, $Handle.d, $Handle.e);
            }
            throw new IllegalArgumentException("value " + o);
        }
    }
    
    public int newConst(final Object o) {
        return this.a(o).a;
    }
    
    public int newUTF8(final String s) {
        this.g.a(1, s, null, null);
        $Item a = this.a(this.g);
        if (a == null) {
            this.d.putByte(1).putUTF8(s);
            a = new $Item(this.c++, this.g);
            this.b(a);
        }
        return a.a;
    }
    
    $Item a(final String s) {
        this.h.a(7, s, null, null);
        $Item a = this.a(this.h);
        if (a == null) {
            this.d.b(7, this.newUTF8(s));
            a = new $Item(this.c++, this.h);
            this.b(a);
        }
        return a;
    }
    
    public int newClass(final String s) {
        return this.a(s).a;
    }
    
    $Item c(final String s) {
        this.h.a(16, s, null, null);
        $Item a = this.a(this.h);
        if (a == null) {
            this.d.b(16, this.newUTF8(s));
            a = new $Item(this.c++, this.h);
            this.b(a);
        }
        return a;
    }
    
    public int newMethodType(final String s) {
        return this.c(s).a;
    }
    
    $Item a(final int n, final String s, final String s2, final String s3, final boolean b) {
        this.j.a(20 + n, s, s2, s3);
        $Item a = this.a(this.j);
        if (a == null) {
            if (n <= 4) {
                this.b(15, n, this.newField(s, s2, s3));
            }
            else {
                this.b(15, n, this.newMethod(s, s2, s3, b));
            }
            a = new $Item(this.c++, this.j);
            this.b(a);
        }
        return a;
    }
    
    public int newHandle(final int n, final String s, final String s2, final String s3) {
        return this.newHandle(n, s, s2, s3, n == 9);
    }
    
    public int newHandle(final int n, final String s, final String s2, final String s3, final boolean b) {
        return this.a(n, s, s2, s3, b).a;
    }
    
    $Item a(final String s, final String s2, final $Handle $Handle, final Object... array) {
        $ByteVector a = this.A;
        if (a == null) {
            final $ByteVector a2 = new $ByteVector();
            this.A = a2;
            a = a2;
        }
        final int b = a.b;
        int hashCode = $Handle.hashCode();
        a.putShort(this.newHandle($Handle.a, $Handle.b, $Handle.c, $Handle.d, $Handle.isInterface()));
        final int length = array.length;
        a.putShort(length);
        for (final Object o : array) {
            hashCode ^= o.hashCode();
            a.putShort(this.newConst(o));
        }
        final byte[] a3 = a.a;
        final int n = 2 + length << 1;
        final int n2 = hashCode & Integer.MAX_VALUE;
        $Item $Item = this.e[n2 % this.e.length];
    Label_0163:
        while ($Item != null) {
            if ($Item.b == 33 && $Item.j == n2) {
                final int c = $Item.c;
                for (int j = 0; j < n; ++j) {
                    if (a3[b + j] != a3[c + j]) {
                        $Item = $Item.k;
                        continue Label_0163;
                    }
                }
                break;
            }
            $Item = $Item.k;
        }
        int a4;
        if ($Item != null) {
            a4 = $Item.a;
            a.b = b;
        }
        else {
            a4 = this.z++;
            final $Item $Item2 = new $Item(a4);
            $Item2.a(b, n2);
            this.b($Item2);
        }
        this.i.a(s, s2, a4);
        $Item a5 = this.a(this.i);
        if (a5 == null) {
            this.a(18, a4, this.newNameType(s, s2));
            a5 = new $Item(this.c++, this.i);
            this.b(a5);
        }
        return a5;
    }
    
    public int newInvokeDynamic(final String s, final String s2, final $Handle $Handle, final Object... array) {
        return this.a(s, s2, $Handle, array).a;
    }
    
    $Item a(final String s, final String s2, final String s3) {
        this.i.a(9, s, s2, s3);
        $Item a = this.a(this.i);
        if (a == null) {
            this.a(9, this.newClass(s), this.newNameType(s2, s3));
            a = new $Item(this.c++, this.i);
            this.b(a);
        }
        return a;
    }
    
    public int newField(final String s, final String s2, final String s3) {
        return this.a(s, s2, s3).a;
    }
    
    $Item a(final String s, final String s2, final String s3, final boolean b) {
        final int n = b ? 11 : 10;
        this.i.a(n, s, s2, s3);
        $Item a = this.a(this.i);
        if (a == null) {
            this.a(n, this.newClass(s), this.newNameType(s2, s3));
            a = new $Item(this.c++, this.i);
            this.b(a);
        }
        return a;
    }
    
    public int newMethod(final String s, final String s2, final String s3, final boolean b) {
        return this.a(s, s2, s3, b).a;
    }
    
    $Item a(final int n) {
        this.g.a(n);
        $Item a = this.a(this.g);
        if (a == null) {
            this.d.putByte(3).putInt(n);
            a = new $Item(this.c++, this.g);
            this.b(a);
        }
        return a;
    }
    
    $Item a(final float n) {
        this.g.a(n);
        $Item a = this.a(this.g);
        if (a == null) {
            this.d.putByte(4).putInt(this.g.c);
            a = new $Item(this.c++, this.g);
            this.b(a);
        }
        return a;
    }
    
    $Item a(final long n) {
        this.g.a(n);
        $Item a = this.a(this.g);
        if (a == null) {
            this.d.putByte(5).putLong(n);
            a = new $Item(this.c, this.g);
            this.c += 2;
            this.b(a);
        }
        return a;
    }
    
    $Item a(final double n) {
        this.g.a(n);
        $Item a = this.a(this.g);
        if (a == null) {
            this.d.putByte(6).putLong(this.g.d);
            a = new $Item(this.c, this.g);
            this.c += 2;
            this.b(a);
        }
        return a;
    }
    
    private $Item b(final String s) {
        this.h.a(8, s, null, null);
        $Item a = this.a(this.h);
        if (a == null) {
            this.d.b(8, this.newUTF8(s));
            a = new $Item(this.c++, this.h);
            this.b(a);
        }
        return a;
    }
    
    public int newNameType(final String s, final String s2) {
        return this.a(s, s2).a;
    }
    
    $Item a(final String s, final String s2) {
        this.h.a(12, s, s2, null);
        $Item a = this.a(this.h);
        if (a == null) {
            this.a(12, this.newUTF8(s), this.newUTF8(s2));
            a = new $Item(this.c++, this.h);
            this.b(a);
        }
        return a;
    }
    
    int c(final String s) {
        this.g.a(30, s, null, null);
        $Item $Item = this.a(this.g);
        if ($Item == null) {
            $Item = this.c(this.g);
        }
        return $Item.a;
    }
    
    int a(final String g, final int c) {
        this.g.b = 31;
        this.g.c = c;
        this.g.g = g;
        this.g.j = (Integer.MAX_VALUE & 31 + g.hashCode() + c);
        $Item $Item = this.a(this.g);
        if ($Item == null) {
            $Item = this.c(this.g);
        }
        return $Item.a;
    }
    
    private $Item c(final $Item $Item) {
        ++this.G;
        final $Item $Item2 = new $Item(this.G, this.g);
        this.b($Item2);
        if (this.H == null) {
            this.H = new $Item[16];
        }
        if (this.G == this.H.length) {
            final $Item[] h = new $Item[2 * this.H.length];
            System.arraycopy(this.H, 0, h, 0, this.H.length);
            this.H = h;
        }
        return this.H[this.G] = $Item2;
    }
    
    int a(final int n, final int n2) {
        this.h.b = 32;
        this.h.d = ((long)n | (long)n2 << 32);
        this.h.j = (Integer.MAX_VALUE & 32 + n + n2);
        $Item a = this.a(this.h);
        if (a == null) {
            this.h.c = this.c(this.getCommonSuperClass(this.H[n].g, this.H[n2].g));
            a = new $Item(0, this.h);
            this.b(a);
        }
        return a.c;
    }
    
    protected String getCommonSuperClass(final String s, final String s2) {
        final ClassLoader classLoader = this.getClass().getClassLoader();
        Class<?> clazz;
        Class<?> forName;
        try {
            clazz = Class.forName(s.replace('/', '.'), false, classLoader);
            forName = Class.forName(s2.replace('/', '.'), false, classLoader);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex.toString());
        }
        if (clazz.isAssignableFrom(forName)) {
            return s;
        }
        if (forName.isAssignableFrom(clazz)) {
            return s2;
        }
        if (clazz.isInterface() || forName.isInterface()) {
            return "java/lang/Object";
        }
        do {
            clazz = clazz.getSuperclass();
        } while (!clazz.isAssignableFrom(forName));
        return clazz.getName().replace('.', '/');
    }
    
    private $Item a(final $Item $Item) {
        $Item k;
        for (k = this.e[$Item.j % this.e.length]; k != null && (k.b != $Item.b || !$Item.a(k)); k = k.k) {}
        return k;
    }
    
    private void b(final $Item $Item) {
        if (this.c + this.G > this.f) {
            final int length = this.e.length;
            final int n = length * 2 + 1;
            final $Item[] e = new $Item[n];
            for (int i = length - 1; i >= 0; --i) {
                $Item k;
                for ($Item $Item2 = this.e[i]; $Item2 != null; $Item2 = k) {
                    final int n2 = $Item2.j % e.length;
                    k = $Item2.k;
                    $Item2.k = e[n2];
                    e[n2] = $Item2;
                }
            }
            this.e = e;
            this.f = (int)(n * 0.75);
        }
        final int n3 = $Item.j % this.e.length;
        $Item.k = this.e[n3];
        this.e[n3] = $Item;
    }
    
    private void a(final int n, final int n2, final int n3) {
        this.d.b(n, n2).putShort(n3);
    }
    
    private void b(final int n, final int n2, final int n3) {
        this.d.a(n, n2).putShort(n3);
    }
    
    static {
        _clinit_();
        final byte[] a2 = new byte[220];
        final String s = "AAAAAAAAAAAAAAAABCLMMDDDDDEEEEEEEEEEEEEEEEEEEEAAAAAAAADDDDDEEEEEEEEEEEEEEEEEEEEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAANAAAAAAAAAAAAAAAAAAAAJJJJJJJJJJJJJJJJDOPAAAAAAGGGGGGGHIFBFAAFFAARQJJKKSSSSSSSSSSSSSSSSSS";
        for (int i = 0; i < a2.length; ++i) {
            a2[i] = (byte)(s.charAt(i) - 'A');
        }
        a = a2;
    }
    
    static /* synthetic */ void _clinit_() {
    }
}
