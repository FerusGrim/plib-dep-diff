package com.comphenix.net.sf.cglib.asm;

class $MethodWriter extends $MethodVisitor
{
    final $ClassWriter b;
    private int c;
    private final int d;
    private final int e;
    private final String f;
    String g;
    int h;
    int i;
    int j;
    int[] k;
    private $ByteVector l;
    private $AnnotationWriter m;
    private $AnnotationWriter n;
    private $AnnotationWriter U;
    private $AnnotationWriter V;
    private $AnnotationWriter[] o;
    private $AnnotationWriter[] p;
    private int R;
    private $Attribute q;
    private $ByteVector r;
    private int s;
    private int t;
    private int T;
    private int u;
    private $ByteVector v;
    private int w;
    private int[] x;
    private int[] z;
    private int A;
    private $Handler B;
    private $Handler C;
    private int Z;
    private $ByteVector $;
    private int D;
    private $ByteVector E;
    private int F;
    private $ByteVector G;
    private int H;
    private $ByteVector I;
    private int Y;
    private $AnnotationWriter W;
    private $AnnotationWriter X;
    private $Attribute J;
    private int K;
    private final int L;
    private $Label M;
    private $Label N;
    private $Label O;
    private int P;
    private int Q;
    
    $MethodWriter(final $ClassWriter b, final int c, final String s, final String f, final String g, final String[] array, final int l) {
        super(327680);
        this.r = new $ByteVector();
        if (b.D == null) {
            b.D = this;
        }
        else {
            b.E.mv = this;
        }
        b.E = this;
        this.b = b;
        this.c = c;
        if ("<init>".equals(s)) {
            this.c |= 0x80000;
        }
        this.d = b.newUTF8(s);
        this.e = b.newUTF8(f);
        this.f = f;
        this.g = g;
        if (array != null && array.length > 0) {
            this.j = array.length;
            this.k = new int[this.j];
            for (int i = 0; i < this.j; ++i) {
                this.k[i] = b.newClass(array[i]);
            }
        }
        if ((this.L = l) != 3) {
            int n = $Type.getArgumentsAndReturnSizes(this.f) >> 2;
            if ((c & 0x8) != 0x0) {
                --n;
            }
            this.t = n;
            this.T = n;
            this.M = new $Label();
            final $Label m = this.M;
            m.a |= 0x8;
            this.visitLabel(this.M);
        }
    }
    
    public void visitParameter(final String s, final int n) {
        if (this.$ == null) {
            this.$ = new $ByteVector();
        }
        ++this.Z;
        this.$.putShort((s == null) ? 0 : this.b.newUTF8(s)).putShort(n);
    }
    
    public $AnnotationVisitor visitAnnotationDefault() {
        this.l = new $ByteVector();
        return new $AnnotationWriter(this.b, false, this.l, null, 0);
    }
    
    public $AnnotationVisitor visitAnnotation(final String s, final boolean b) {
        final $ByteVector $ByteVector = new $ByteVector();
        $ByteVector.putShort(this.b.newUTF8(s)).putShort(0);
        final $AnnotationWriter $AnnotationWriter = new $AnnotationWriter(this.b, true, $ByteVector, $ByteVector, 2);
        if (b) {
            $AnnotationWriter.g = this.m;
            this.m = $AnnotationWriter;
        }
        else {
            $AnnotationWriter.g = this.n;
            this.n = $AnnotationWriter;
        }
        return $AnnotationWriter;
    }
    
    public $AnnotationVisitor visitTypeAnnotation(final int n, final $TypePath $TypePath, final String s, final boolean b) {
        final $ByteVector $ByteVector = new $ByteVector();
        $AnnotationWriter.a(n, $TypePath, $ByteVector);
        $ByteVector.putShort(this.b.newUTF8(s)).putShort(0);
        final $AnnotationWriter $AnnotationWriter = new $AnnotationWriter(this.b, true, $ByteVector, $ByteVector, $ByteVector.b - 2);
        if (b) {
            $AnnotationWriter.g = this.U;
            this.U = $AnnotationWriter;
        }
        else {
            $AnnotationWriter.g = this.V;
            this.V = $AnnotationWriter;
        }
        return $AnnotationWriter;
    }
    
    public $AnnotationVisitor visitParameterAnnotation(final int n, final String s, final boolean b) {
        final $ByteVector $ByteVector = new $ByteVector();
        if ("Ljava/lang/Synthetic;".equals(s)) {
            this.R = Math.max(this.R, n + 1);
            return new $AnnotationWriter(this.b, false, $ByteVector, null, 0);
        }
        $ByteVector.putShort(this.b.newUTF8(s)).putShort(0);
        final $AnnotationWriter $AnnotationWriter = new $AnnotationWriter(this.b, true, $ByteVector, $ByteVector, 2);
        if (b) {
            if (this.o == null) {
                this.o = new $AnnotationWriter[$Type.getArgumentTypes(this.f).length];
            }
            $AnnotationWriter.g = this.o[n];
            this.o[n] = $AnnotationWriter;
        }
        else {
            if (this.p == null) {
                this.p = new $AnnotationWriter[$Type.getArgumentTypes(this.f).length];
            }
            $AnnotationWriter.g = this.p[n];
            this.p[n] = $AnnotationWriter;
        }
        return $AnnotationWriter;
    }
    
    public void visitAttribute(final $Attribute $Attribute) {
        if ($Attribute.isCodeAttribute()) {
            $Attribute.a = this.J;
            this.J = $Attribute;
        }
        else {
            $Attribute.a = this.q;
            this.q = $Attribute;
        }
    }
    
    public void visitCode() {
    }
    
    public void visitFrame(final int n, final int n2, final Object[] array, final int n3, final Object[] array2) {
        if (this.L == 0) {
            return;
        }
        if (this.L == 1) {
            if (this.O.h == null) {
                this.O.h = new $CurrentFrame();
                this.O.h.b = this.O;
                this.O.h.a(this.b, this.c, $Type.getArgumentTypes(this.f), n2);
                this.f();
            }
            else {
                if (n == -1) {
                    this.O.h.a(this.b, n2, array, n3, array2);
                }
                this.b(this.O.h);
            }
        }
        else if (n == -1) {
            if (this.x == null) {
                this.f();
            }
            this.T = n2;
            int a = this.a(this.r.b, n2, n3);
            for (int i = 0; i < n2; ++i) {
                if (array[i] instanceof String) {
                    this.z[a++] = (0x1700000 | this.b.c((String)array[i]));
                }
                else if (array[i] instanceof Integer) {
                    this.z[a++] = (int)array[i];
                }
                else {
                    this.z[a++] = (0x1800000 | this.b.a("", (($Label)array[i]).c));
                }
            }
            for (int j = 0; j < n3; ++j) {
                if (array2[j] instanceof String) {
                    this.z[a++] = (0x1700000 | this.b.c((String)array2[j]));
                }
                else if (array2[j] instanceof Integer) {
                    this.z[a++] = (int)array2[j];
                }
                else {
                    this.z[a++] = (0x1800000 | this.b.a("", (($Label)array2[j]).c));
                }
            }
            this.b();
        }
        else {
            int b;
            if (this.v == null) {
                this.v = new $ByteVector();
                b = this.r.b;
            }
            else {
                b = this.r.b - this.w - 1;
                if (b < 0) {
                    if (n == 3) {
                        return;
                    }
                    throw new IllegalStateException();
                }
            }
            switch (n) {
                case 0: {
                    this.T = n2;
                    this.v.putByte(255).putShort(b).putShort(n2);
                    for (int k = 0; k < n2; ++k) {
                        this.a(array[k]);
                    }
                    this.v.putShort(n3);
                    for (int l = 0; l < n3; ++l) {
                        this.a(array2[l]);
                    }
                    break;
                }
                case 1: {
                    this.T += n2;
                    this.v.putByte(251 + n2).putShort(b);
                    for (int n4 = 0; n4 < n2; ++n4) {
                        this.a(array[n4]);
                    }
                    break;
                }
                case 2: {
                    this.T -= n2;
                    this.v.putByte(251 - n2).putShort(b);
                    break;
                }
                case 3: {
                    if (b < 64) {
                        this.v.putByte(b);
                        break;
                    }
                    this.v.putByte(251).putShort(b);
                    break;
                }
                case 4: {
                    if (b < 64) {
                        this.v.putByte(64 + b);
                    }
                    else {
                        this.v.putByte(247).putShort(b);
                    }
                    this.a(array2[0]);
                    break;
                }
            }
            this.w = this.r.b;
            ++this.u;
        }
        this.s = Math.max(this.s, n3);
        this.t = Math.max(this.t, this.T);
    }
    
    public void visitInsn(final int n) {
        this.Y = this.r.b;
        this.r.putByte(n);
        if (this.O != null) {
            if (this.L == 0 || this.L == 1) {
                this.O.h.a(n, 0, null, null);
            }
            else {
                final int n2 = this.P + $Frame.a[n];
                if (n2 > this.Q) {
                    this.Q = n2;
                }
                this.P = n2;
            }
            if ((n >= 172 && n <= 177) || n == 191) {
                this.e();
            }
        }
    }
    
    public void visitIntInsn(final int n, final int n2) {
        this.Y = this.r.b;
        if (this.O != null) {
            if (this.L == 0 || this.L == 1) {
                this.O.h.a(n, n2, null, null);
            }
            else if (n != 188) {
                final int n3 = this.P + 1;
                if (n3 > this.Q) {
                    this.Q = n3;
                }
                this.P = n3;
            }
        }
        if (n == 17) {
            this.r.b(n, n2);
        }
        else {
            this.r.a(n, n2);
        }
    }
    
    public void visitVarInsn(final int n, final int n2) {
        this.Y = this.r.b;
        if (this.O != null) {
            if (this.L == 0 || this.L == 1) {
                this.O.h.a(n, n2, null, null);
            }
            else if (n == 169) {
                final $Label o = this.O;
                o.a |= 0x100;
                this.O.f = this.P;
                this.e();
            }
            else {
                final int n3 = this.P + $Frame.a[n];
                if (n3 > this.Q) {
                    this.Q = n3;
                }
                this.P = n3;
            }
        }
        if (this.L != 3) {
            int t;
            if (n == 22 || n == 24 || n == 55 || n == 57) {
                t = n2 + 2;
            }
            else {
                t = n2 + 1;
            }
            if (t > this.t) {
                this.t = t;
            }
        }
        if (n2 < 4 && n != 169) {
            int n4;
            if (n < 54) {
                n4 = 26 + (n - 21 << 2) + n2;
            }
            else {
                n4 = 59 + (n - 54 << 2) + n2;
            }
            this.r.putByte(n4);
        }
        else if (n2 >= 256) {
            this.r.putByte(196).b(n, n2);
        }
        else {
            this.r.a(n, n2);
        }
        if (n >= 54 && this.L == 0 && this.A > 0) {
            this.visitLabel(new $Label());
        }
    }
    
    public void visitTypeInsn(final int n, final String s) {
        this.Y = this.r.b;
        final $Item a = this.b.a(s);
        if (this.O != null) {
            if (this.L == 0 || this.L == 1) {
                this.O.h.a(n, this.r.b, this.b, a);
            }
            else if (n == 187) {
                final int n2 = this.P + 1;
                if (n2 > this.Q) {
                    this.Q = n2;
                }
                this.P = n2;
            }
        }
        this.r.b(n, a.a);
    }
    
    public void visitFieldInsn(final int n, final String s, final String s2, final String s3) {
        this.Y = this.r.b;
        final $Item a = this.b.a(s, s2, s3);
        if (this.O != null) {
            if (this.L == 0 || this.L == 1) {
                this.O.h.a(n, 0, this.b, a);
            }
            else {
                final char char1 = s3.charAt(0);
                int n2 = 0;
                switch (n) {
                    case 178: {
                        n2 = this.P + ((char1 == 'D' || char1 == 'J') ? 2 : 1);
                        break;
                    }
                    case 179: {
                        n2 = this.P + ((char1 == 'D' || char1 == 'J') ? -2 : -1);
                        break;
                    }
                    case 180: {
                        n2 = this.P + ((char1 == 'D' || char1 == 'J') ? 1 : 0);
                        break;
                    }
                    default: {
                        n2 = this.P + ((char1 == 'D' || char1 == 'J') ? -3 : -2);
                        break;
                    }
                }
                if (n2 > this.Q) {
                    this.Q = n2;
                }
                this.P = n2;
            }
        }
        this.r.b(n, a.a);
    }
    
    public void visitMethodInsn(final int n, final String s, final String s2, final String s3, final boolean b) {
        this.Y = this.r.b;
        final $Item a = this.b.a(s, s2, s3, b);
        int n2 = a.c;
        if (this.O != null) {
            if (this.L == 0 || this.L == 1) {
                this.O.h.a(n, 0, this.b, a);
            }
            else {
                if (n2 == 0) {
                    n2 = $Type.getArgumentsAndReturnSizes(s3);
                    a.c = n2;
                }
                int n3;
                if (n == 184) {
                    n3 = this.P - (n2 >> 2) + (n2 & 0x3) + 1;
                }
                else {
                    n3 = this.P - (n2 >> 2) + (n2 & 0x3);
                }
                if (n3 > this.Q) {
                    this.Q = n3;
                }
                this.P = n3;
            }
        }
        if (n == 185) {
            if (n2 == 0) {
                n2 = $Type.getArgumentsAndReturnSizes(s3);
                a.c = n2;
            }
            this.r.b(185, a.a).a(n2 >> 2, 0);
        }
        else {
            this.r.b(n, a.a);
        }
    }
    
    public void visitInvokeDynamicInsn(final String s, final String s2, final $Handle $Handle, final Object... array) {
        this.Y = this.r.b;
        final $Item a = this.b.a(s, s2, $Handle, array);
        int c = a.c;
        if (this.O != null) {
            if (this.L == 0 || this.L == 1) {
                this.O.h.a(186, 0, this.b, a);
            }
            else {
                if (c == 0) {
                    c = $Type.getArgumentsAndReturnSizes(s2);
                    a.c = c;
                }
                final int n = this.P - (c >> 2) + (c & 0x3) + 1;
                if (n > this.Q) {
                    this.Q = n;
                }
                this.P = n;
            }
        }
        this.r.b(186, a.a);
        this.r.putShort(0);
    }
    
    public void visitJumpInsn(int n, final $Label $Label) {
        final boolean b = n >= 200;
        n = (b ? (n - 33) : n);
        this.Y = this.r.b;
        $Label $Label2 = null;
        if (this.O != null) {
            if (this.L == 0) {
                this.O.h.a(n, 0, null, null);
                final $Label a = $Label.a();
                a.a |= 0x10;
                this.a(0, $Label);
                if (n != 167) {
                    $Label2 = new $Label();
                }
            }
            else if (this.L == 1) {
                this.O.h.a(n, 0, null, null);
            }
            else if (n == 168) {
                if (($Label.a & 0x200) == 0x0) {
                    $Label.a |= 0x200;
                    ++this.K;
                }
                final $Label o = this.O;
                o.a |= 0x80;
                this.a(this.P + 1, $Label);
                $Label2 = new $Label();
            }
            else {
                this.a(this.P += $Frame.a[n], $Label);
            }
        }
        if (($Label.a & 0x2) != 0x0 && $Label.c - this.r.b < -32768) {
            if (n == 167) {
                this.r.putByte(200);
            }
            else if (n == 168) {
                this.r.putByte(201);
            }
            else {
                if ($Label2 != null) {
                    final $Label $Label3 = $Label2;
                    $Label3.a |= 0x10;
                }
                this.r.putByte((n <= 166) ? ((n + 1 ^ 0x1) - 1) : (n ^ 0x1));
                this.r.putShort(8);
                this.r.putByte(200);
            }
            $Label.a(this, this.r, this.r.b - 1, true);
        }
        else if (b) {
            this.r.putByte(n + 33);
            $Label.a(this, this.r, this.r.b - 1, true);
        }
        else {
            this.r.putByte(n);
            $Label.a(this, this.r, this.r.b - 1, false);
        }
        if (this.O != null) {
            if ($Label2 != null) {
                this.visitLabel($Label2);
            }
            if (n == 167) {
                this.e();
            }
        }
    }
    
    public void visitLabel(final $Label n) {
        final $ClassWriter b = this.b;
        b.J |= n.a(this, this.r.b, this.r.a);
        if ((n.a & 0x1) != 0x0) {
            return;
        }
        if (this.L == 0) {
            if (this.O != null) {
                if (n.c == this.O.c) {
                    final $Label o = this.O;
                    o.a |= (n.a & 0x10);
                    n.h = this.O.h;
                    return;
                }
                this.a(0, n);
            }
            this.O = n;
            if (n.h == null) {
                n.h = new $Frame();
                n.h.b = n;
            }
            if (this.N != null) {
                if (n.c == this.N.c) {
                    final $Label n2 = this.N;
                    n2.a |= (n.a & 0x10);
                    n.h = this.N.h;
                    this.O = this.N;
                    return;
                }
                this.N.i = n;
            }
            this.N = n;
        }
        else if (this.L == 1) {
            if (this.O == null) {
                this.O = n;
            }
            else {
                this.O.h.b = n;
            }
        }
        else if (this.L == 2) {
            if (this.O != null) {
                this.O.g = this.Q;
                this.a(this.P, n);
            }
            this.O = n;
            this.P = 0;
            this.Q = 0;
            if (this.N != null) {
                this.N.i = n;
            }
            this.N = n;
        }
    }
    
    public void visitLdcInsn(final Object o) {
        this.Y = this.r.b;
        final $Item a = this.b.a(o);
        if (this.O != null) {
            if (this.L == 0 || this.L == 1) {
                this.O.h.a(18, 0, this.b, a);
            }
            else {
                int n;
                if (a.b == 5 || a.b == 6) {
                    n = this.P + 2;
                }
                else {
                    n = this.P + 1;
                }
                if (n > this.Q) {
                    this.Q = n;
                }
                this.P = n;
            }
        }
        final int a2 = a.a;
        if (a.b == 5 || a.b == 6) {
            this.r.b(20, a2);
        }
        else if (a2 >= 256) {
            this.r.b(19, a2);
        }
        else {
            this.r.a(18, a2);
        }
    }
    
    public void visitIincInsn(final int n, final int n2) {
        this.Y = this.r.b;
        if (this.O != null && (this.L == 0 || this.L == 1)) {
            this.O.h.a(132, n, null, null);
        }
        if (this.L != 3) {
            final int t = n + 1;
            if (t > this.t) {
                this.t = t;
            }
        }
        if (n > 255 || n2 > 127 || n2 < -128) {
            this.r.putByte(196).b(132, n).putShort(n2);
        }
        else {
            this.r.putByte(132).a(n, n2);
        }
    }
    
    public void visitTableSwitchInsn(final int n, final int n2, final $Label $Label, final $Label... array) {
        this.Y = this.r.b;
        final int b = this.r.b;
        this.r.putByte(170);
        this.r.putByteArray(null, 0, (4 - this.r.b % 4) % 4);
        $Label.a(this, this.r, b, true);
        this.r.putInt(n).putInt(n2);
        for (int i = 0; i < array.length; ++i) {
            array[i].a(this, this.r, b, true);
        }
        this.a($Label, array);
    }
    
    public void visitLookupSwitchInsn(final $Label $Label, final int[] array, final $Label[] array2) {
        this.Y = this.r.b;
        final int b = this.r.b;
        this.r.putByte(171);
        this.r.putByteArray(null, 0, (4 - this.r.b % 4) % 4);
        $Label.a(this, this.r, b, true);
        this.r.putInt(array2.length);
        for (int i = 0; i < array2.length; ++i) {
            this.r.putInt(array[i]);
            array2[i].a(this, this.r, b, true);
        }
        this.a($Label, array2);
    }
    
    private void a(final $Label $Label, final $Label[] array) {
        if (this.O != null) {
            if (this.L == 0) {
                this.O.h.a(171, 0, null, null);
                this.a(0, $Label);
                final $Label a = $Label.a();
                a.a |= 0x10;
                for (int i = 0; i < array.length; ++i) {
                    this.a(0, array[i]);
                    final $Label a2 = array[i].a();
                    a2.a |= 0x10;
                }
            }
            else {
                this.a(--this.P, $Label);
                for (int j = 0; j < array.length; ++j) {
                    this.a(this.P, array[j]);
                }
            }
            this.e();
        }
    }
    
    public void visitMultiANewArrayInsn(final String s, final int n) {
        this.Y = this.r.b;
        final $Item a = this.b.a(s);
        if (this.O != null) {
            if (this.L == 0 || this.L == 1) {
                this.O.h.a(197, n, this.b, a);
            }
            else {
                this.P += 1 - n;
            }
        }
        this.r.b(197, a.a).putByte(n);
    }
    
    public $AnnotationVisitor visitInsnAnnotation(int n, final $TypePath $TypePath, final String s, final boolean b) {
        final $ByteVector $ByteVector = new $ByteVector();
        n = ((n & 0xFF0000FF) | this.Y << 8);
        $AnnotationWriter.a(n, $TypePath, $ByteVector);
        $ByteVector.putShort(this.b.newUTF8(s)).putShort(0);
        final $AnnotationWriter $AnnotationWriter = new $AnnotationWriter(this.b, true, $ByteVector, $ByteVector, $ByteVector.b - 2);
        if (b) {
            $AnnotationWriter.g = this.W;
            this.W = $AnnotationWriter;
        }
        else {
            $AnnotationWriter.g = this.X;
            this.X = $AnnotationWriter;
        }
        return $AnnotationWriter;
    }
    
    public void visitTryCatchBlock(final $Label a, final $Label b, final $Label c, final String d) {
        ++this.A;
        final $Handler c2 = new $Handler();
        c2.a = a;
        c2.b = b;
        c2.c = c;
        c2.d = d;
        c2.e = ((d != null) ? this.b.newClass(d) : 0);
        if (this.C == null) {
            this.B = c2;
        }
        else {
            this.C.f = c2;
        }
        this.C = c2;
    }
    
    public $AnnotationVisitor visitTryCatchAnnotation(final int n, final $TypePath $TypePath, final String s, final boolean b) {
        final $ByteVector $ByteVector = new $ByteVector();
        $AnnotationWriter.a(n, $TypePath, $ByteVector);
        $ByteVector.putShort(this.b.newUTF8(s)).putShort(0);
        final $AnnotationWriter $AnnotationWriter = new $AnnotationWriter(this.b, true, $ByteVector, $ByteVector, $ByteVector.b - 2);
        if (b) {
            $AnnotationWriter.g = this.W;
            this.W = $AnnotationWriter;
        }
        else {
            $AnnotationWriter.g = this.X;
            this.X = $AnnotationWriter;
        }
        return $AnnotationWriter;
    }
    
    public void visitLocalVariable(final String s, final String s2, final String s3, final $Label $Label, final $Label $Label2, final int n) {
        if (s3 != null) {
            if (this.G == null) {
                this.G = new $ByteVector();
            }
            ++this.F;
            this.G.putShort($Label.c).putShort($Label2.c - $Label.c).putShort(this.b.newUTF8(s)).putShort(this.b.newUTF8(s3)).putShort(n);
        }
        if (this.E == null) {
            this.E = new $ByteVector();
        }
        ++this.D;
        this.E.putShort($Label.c).putShort($Label2.c - $Label.c).putShort(this.b.newUTF8(s)).putShort(this.b.newUTF8(s2)).putShort(n);
        if (this.L != 3) {
            final char char1 = s2.charAt(0);
            final int t = n + ((char1 == 'J' || char1 == 'D') ? 2 : 1);
            if (t > this.t) {
                this.t = t;
            }
        }
    }
    
    public $AnnotationVisitor visitLocalVariableAnnotation(final int n, final $TypePath $TypePath, final $Label[] array, final $Label[] array2, final int[] array3, final String s, final boolean b) {
        final $ByteVector $ByteVector = new $ByteVector();
        $ByteVector.putByte(n >>> 24).putShort(array.length);
        for (int i = 0; i < array.length; ++i) {
            $ByteVector.putShort(array[i].c).putShort(array2[i].c - array[i].c).putShort(array3[i]);
        }
        if ($TypePath == null) {
            $ByteVector.putByte(0);
        }
        else {
            $ByteVector.putByteArray($TypePath.a, $TypePath.b, $TypePath.a[$TypePath.b] * 2 + 1);
        }
        $ByteVector.putShort(this.b.newUTF8(s)).putShort(0);
        final $AnnotationWriter $AnnotationWriter = new $AnnotationWriter(this.b, true, $ByteVector, $ByteVector, $ByteVector.b - 2);
        if (b) {
            $AnnotationWriter.g = this.W;
            this.W = $AnnotationWriter;
        }
        else {
            $AnnotationWriter.g = this.X;
            this.X = $AnnotationWriter;
        }
        return $AnnotationWriter;
    }
    
    public void visitLineNumber(final int n, final $Label $Label) {
        if (this.I == null) {
            this.I = new $ByteVector();
        }
        ++this.H;
        this.I.putShort($Label.c);
        this.I.putShort(n);
    }
    
    public void visitMaxs(final int s, final int t) {
        if (this.L == 0) {
            for ($Handler $Handler = this.B; $Handler != null; $Handler = $Handler.f) {
                $Label $Label = $Handler.a.a();
                final $Label a = $Handler.c.a();
                final $Label a2 = $Handler.b.a();
                final int a3 = 0x1700000 | this.b.c(($Handler.d == null) ? "java/lang/Throwable" : $Handler.d);
                final $Label $Label2 = a;
                $Label2.a |= 0x10;
                while ($Label != a2) {
                    final $Edge j = new $Edge();
                    j.a = a3;
                    j.b = a;
                    j.c = $Label.j;
                    $Label.j = j;
                    $Label = $Label.i;
                }
            }
            final $Frame h = this.M.h;
            h.a(this.b, this.c, $Type.getArgumentTypes(this.f), this.t);
            this.b(h);
            int max = 0;
            $Label k = this.M;
            while (k != null) {
                final $Label $Label3 = k;
                k = k.k;
                $Label3.k = null;
                final $Frame h2 = $Label3.h;
                if (($Label3.a & 0x10) != 0x0) {
                    final $Label $Label4 = $Label3;
                    $Label4.a |= 0x20;
                }
                final $Label $Label5 = $Label3;
                $Label5.a |= 0x40;
                final int n = h2.d.length + $Label3.g;
                if (n > max) {
                    max = n;
                }
                for ($Edge $Edge = $Label3.j; $Edge != null; $Edge = $Edge.c) {
                    final $Label a4 = $Edge.b.a();
                    if (h2.a(this.b, a4.h, $Edge.a) && a4.k == null) {
                        a4.k = k;
                        k = a4;
                    }
                }
            }
            for ($Label $Label6 = this.M; $Label6 != null; $Label6 = $Label6.i) {
                final $Frame h3 = $Label6.h;
                if (($Label6.a & 0x20) != 0x0) {
                    this.b(h3);
                }
                if (($Label6.a & 0x40) == 0x0) {
                    final $Label i = $Label6.i;
                    final int c = $Label6.c;
                    final int n2 = ((i == null) ? this.r.b : i.c) - 1;
                    if (n2 >= c) {
                        max = Math.max(max, 1);
                        for (int l = c; l < n2; ++l) {
                            this.r.a[l] = 0;
                        }
                        this.r.a[n2] = -65;
                        this.z[this.a(c, 0, 1)] = (0x1700000 | this.b.c("java/lang/Throwable"));
                        this.b();
                        this.B = $Handler.a(this.B, $Label6, i);
                    }
                }
            }
            $Handler $Handler2 = this.B;
            this.A = 0;
            while ($Handler2 != null) {
                ++this.A;
                $Handler2 = $Handler2.f;
            }
            this.s = max;
        }
        else if (this.L == 2) {
            for ($Handler $Handler3 = this.B; $Handler3 != null; $Handler3 = $Handler3.f) {
                $Label $Label7 = $Handler3.a;
                final $Label c2 = $Handler3.c;
                while ($Label7 != $Handler3.b) {
                    final $Edge $Edge2 = new $Edge();
                    $Edge2.a = Integer.MAX_VALUE;
                    $Edge2.b = c2;
                    if (($Label7.a & 0x80) == 0x0) {
                        $Edge2.c = $Label7.j;
                        $Label7.j = $Edge2;
                    }
                    else {
                        $Edge2.c = $Label7.j.c.c;
                        $Label7.j.c.c = $Edge2;
                    }
                    $Label7 = $Label7.i;
                }
            }
            if (this.K > 0) {
                int n3 = 0;
                this.M.b(null, 1L, this.K);
                for ($Label $Label8 = this.M; $Label8 != null; $Label8 = $Label8.i) {
                    if (($Label8.a & 0x80) != 0x0) {
                        final $Label b = $Label8.j.c.b;
                        if ((b.a & 0x400) == 0x0) {
                            ++n3;
                            b.b(null, n3 / 32L << 32 | 1L << n3 % 32, this.K);
                        }
                    }
                }
                for ($Label $Label9 = this.M; $Label9 != null; $Label9 = $Label9.i) {
                    if (($Label9.a & 0x80) != 0x0) {
                        for ($Label $Label10 = this.M; $Label10 != null; $Label10 = $Label10.i) {
                            final $Label $Label11 = $Label10;
                            $Label11.a &= 0xFFFFF7FF;
                        }
                        $Label9.j.c.b.b($Label9, 0L, this.K);
                    }
                }
            }
            int n4 = 0;
            $Label m = this.M;
            while (m != null) {
                final $Label $Label12 = m;
                m = m.k;
                final int f = $Label12.f;
                final int n5 = f + $Label12.g;
                if (n5 > n4) {
                    n4 = n5;
                }
                $Edge $Edge3 = $Label12.j;
                if (($Label12.a & 0x80) != 0x0) {
                    $Edge3 = $Edge3.c;
                }
                while ($Edge3 != null) {
                    final $Label b2 = $Edge3.b;
                    if ((b2.a & 0x8) == 0x0) {
                        b2.f = (($Edge3.a == Integer.MAX_VALUE) ? 1 : (f + $Edge3.a));
                        final $Label $Label13 = b2;
                        $Label13.a |= 0x8;
                        b2.k = m;
                        m = b2;
                    }
                    $Edge3 = $Edge3.c;
                }
            }
            this.s = Math.max(s, n4);
        }
        else {
            this.s = s;
            this.t = t;
        }
    }
    
    public void visitEnd() {
    }
    
    private void a(final int a, final $Label b) {
        final $Edge j = new $Edge();
        j.a = a;
        j.b = b;
        j.c = this.O.j;
        this.O.j = j;
    }
    
    private void e() {
        if (this.L == 0) {
            final $Label n = new $Label();
            n.h = new $Frame();
            (n.h.b = n).a(this, this.r.b, this.r.a);
            this.N.i = n;
            this.N = n;
        }
        else {
            this.O.g = this.Q;
        }
        if (this.L != 1) {
            this.O = null;
        }
    }
    
    private void b(final $Frame $Frame) {
        int n = 0;
        int i = 0;
        int n2 = 0;
        final int[] c = $Frame.c;
        final int[] d = $Frame.d;
        for (int j = 0; j < c.length; ++j) {
            final int n3 = c[j];
            if (n3 == 16777216) {
                ++n;
            }
            else {
                i += n + 1;
                n = 0;
            }
            if (n3 == 16777220 || n3 == 16777219) {
                ++j;
            }
        }
        for (int k = 0; k < d.length; ++k) {
            final int n4 = d[k];
            ++n2;
            if (n4 == 16777220 || n4 == 16777219) {
                ++k;
            }
        }
        int a = this.a($Frame.b.c, i, n2);
        int n5 = 0;
        while (i > 0) {
            final int n6 = c[n5];
            this.z[a++] = n6;
            if (n6 == 16777220 || n6 == 16777219) {
                ++n5;
            }
            ++n5;
            --i;
        }
        for (int l = 0; l < d.length; ++l) {
            final int n7 = d[l];
            this.z[a++] = n7;
            if (n7 == 16777220 || n7 == 16777219) {
                ++l;
            }
        }
        this.b();
    }
    
    private void f() {
        int a = this.a(0, this.f.length() + 1, 0);
        if ((this.c & 0x8) == 0x0) {
            if ((this.c & 0x80000) == 0x0) {
                this.z[a++] = (0x1700000 | this.b.c(this.b.I));
            }
            else {
                this.z[a++] = 6;
            }
        }
        int n = 1;
        while (true) {
            final int n2 = n;
            switch (this.f.charAt(n++)) {
                case 'B':
                case 'C':
                case 'I':
                case 'S':
                case 'Z': {
                    this.z[a++] = 1;
                    continue;
                }
                case 'F': {
                    this.z[a++] = 2;
                    continue;
                }
                case 'J': {
                    this.z[a++] = 4;
                    continue;
                }
                case 'D': {
                    this.z[a++] = 3;
                    continue;
                }
                case '[': {
                    while (this.f.charAt(n) == '[') {
                        ++n;
                    }
                    if (this.f.charAt(n) == 'L') {
                        ++n;
                        while (this.f.charAt(n) != ';') {
                            ++n;
                        }
                    }
                    this.z[a++] = (0x1700000 | this.b.c(this.f.substring(n2, ++n)));
                    continue;
                }
                case 'L': {
                    while (this.f.charAt(n) != ';') {
                        ++n;
                    }
                    this.z[a++] = (0x1700000 | this.b.c(this.f.substring(n2 + 1, n++)));
                    continue;
                }
                default: {
                    this.z[1] = a - 3;
                    this.b();
                }
            }
        }
    }
    
    private int a(final int n, final int n2, final int n3) {
        final int n4 = 3 + n2 + n3;
        if (this.z == null || this.z.length < n4) {
            this.z = new int[n4];
        }
        this.z[0] = n;
        this.z[1] = n2;
        this.z[2] = n3;
        return 3;
    }
    
    private void b() {
        if (this.x != null) {
            if (this.v == null) {
                this.v = new $ByteVector();
            }
            this.c();
            ++this.u;
        }
        this.x = this.z;
        this.z = null;
    }
    
    private void c() {
        final int n = this.z[1];
        final int n2 = this.z[2];
        if ((this.b.b & 0xFFFF) < 50) {
            this.v.putShort(this.z[0]).putShort(n);
            this.a(3, 3 + n);
            this.v.putShort(n2);
            this.a(3 + n, 3 + n + n2);
            return;
        }
        int n3 = this.x[1];
        int n4 = 255;
        int n5 = 0;
        int n6;
        if (this.u == 0) {
            n6 = this.z[0];
        }
        else {
            n6 = this.z[0] - this.x[0] - 1;
        }
        if (n2 == 0) {
            n5 = n - n3;
            switch (n5) {
                case -3:
                case -2:
                case -1: {
                    n4 = 248;
                    n3 = n;
                    break;
                }
                case 0: {
                    n4 = ((n6 < 64) ? 0 : 251);
                    break;
                }
                case 1:
                case 2:
                case 3: {
                    n4 = 252;
                    break;
                }
            }
        }
        else if (n == n3 && n2 == 1) {
            n4 = ((n6 < 63) ? 64 : 247);
        }
        if (n4 != 255) {
            int n7 = 3;
            for (int i = 0; i < n3; ++i) {
                if (this.z[n7] != this.x[n7]) {
                    n4 = 255;
                    break;
                }
                ++n7;
            }
        }
        switch (n4) {
            case 0: {
                this.v.putByte(n6);
                break;
            }
            case 64: {
                this.v.putByte(64 + n6);
                this.a(3 + n, 4 + n);
                break;
            }
            case 247: {
                this.v.putByte(247).putShort(n6);
                this.a(3 + n, 4 + n);
                break;
            }
            case 251: {
                this.v.putByte(251).putShort(n6);
                break;
            }
            case 248: {
                this.v.putByte(251 + n5).putShort(n6);
                break;
            }
            case 252: {
                this.v.putByte(251 + n5).putShort(n6);
                this.a(3 + n3, 3 + n);
                break;
            }
            default: {
                this.v.putByte(255).putShort(n6).putShort(n);
                this.a(3, 3 + n);
                this.v.putShort(n2);
                this.a(3 + n, 3 + n + n2);
                break;
            }
        }
    }
    
    private void a(final int n, final int n2) {
        for (int i = n; i < n2; ++i) {
            final int n3 = this.z[i];
            final int n4 = n3 & 0xF0000000;
            if (n4 == 0) {
                final int n5 = n3 & 0xFFFFF;
                switch (n3 & 0xFF00000) {
                    case 24117248: {
                        this.v.putByte(7).putShort(this.b.newClass(this.b.H[n5].g));
                        break;
                    }
                    case 25165824: {
                        this.v.putByte(8).putShort(this.b.H[n5].c);
                        break;
                    }
                    default: {
                        this.v.putByte(n5);
                        break;
                    }
                }
            }
            else {
                final StringBuffer sb = new StringBuffer();
                int n6 = n4 >> 28;
                while (n6-- > 0) {
                    sb.append('[');
                }
                if ((n3 & 0xFF00000) == 0x1700000) {
                    sb.append('L');
                    sb.append(this.b.H[n3 & 0xFFFFF].g);
                    sb.append(';');
                }
                else {
                    switch (n3 & 0xF) {
                        case 1: {
                            sb.append('I');
                            break;
                        }
                        case 2: {
                            sb.append('F');
                            break;
                        }
                        case 3: {
                            sb.append('D');
                            break;
                        }
                        case 9: {
                            sb.append('Z');
                            break;
                        }
                        case 10: {
                            sb.append('B');
                            break;
                        }
                        case 11: {
                            sb.append('C');
                            break;
                        }
                        case 12: {
                            sb.append('S');
                            break;
                        }
                        default: {
                            sb.append('J');
                            break;
                        }
                    }
                }
                this.v.putByte(7).putShort(this.b.newClass(sb.toString()));
            }
        }
    }
    
    private void a(final Object o) {
        if (o instanceof String) {
            this.v.putByte(7).putShort(this.b.newClass((String)o));
        }
        else if (o instanceof Integer) {
            this.v.putByte((int)o);
        }
        else {
            this.v.putByte(8).putShort((($Label)o).c);
        }
    }
    
    final int a() {
        if (this.h != 0) {
            return 6 + this.i;
        }
        int n = 8;
        if (this.r.b > 0) {
            if (this.r.b > 65535) {
                throw new RuntimeException("Method code too large!");
            }
            this.b.newUTF8("Code");
            n += 18 + this.r.b + 8 * this.A;
            if (this.E != null) {
                this.b.newUTF8("LocalVariableTable");
                n += 8 + this.E.b;
            }
            if (this.G != null) {
                this.b.newUTF8("LocalVariableTypeTable");
                n += 8 + this.G.b;
            }
            if (this.I != null) {
                this.b.newUTF8("LineNumberTable");
                n += 8 + this.I.b;
            }
            if (this.v != null) {
                this.b.newUTF8(((this.b.b & 0xFFFF) >= 50) ? "StackMapTable" : "StackMap");
                n += 8 + this.v.b;
            }
            if (this.W != null) {
                this.b.newUTF8("RuntimeVisibleTypeAnnotations");
                n += 8 + this.W.a();
            }
            if (this.X != null) {
                this.b.newUTF8("RuntimeInvisibleTypeAnnotations");
                n += 8 + this.X.a();
            }
            if (this.J != null) {
                n += this.J.a(this.b, this.r.a, this.r.b, this.s, this.t);
            }
        }
        if (this.j > 0) {
            this.b.newUTF8("Exceptions");
            n += 8 + 2 * this.j;
        }
        if ((this.c & 0x1000) != 0x0 && ((this.b.b & 0xFFFF) < 49 || (this.c & 0x40000) != 0x0)) {
            this.b.newUTF8("Synthetic");
            n += 6;
        }
        if ((this.c & 0x20000) != 0x0) {
            this.b.newUTF8("Deprecated");
            n += 6;
        }
        if (this.g != null) {
            this.b.newUTF8("Signature");
            this.b.newUTF8(this.g);
            n += 8;
        }
        if (this.$ != null) {
            this.b.newUTF8("MethodParameters");
            n += 7 + this.$.b;
        }
        if (this.l != null) {
            this.b.newUTF8("AnnotationDefault");
            n += 6 + this.l.b;
        }
        if (this.m != null) {
            this.b.newUTF8("RuntimeVisibleAnnotations");
            n += 8 + this.m.a();
        }
        if (this.n != null) {
            this.b.newUTF8("RuntimeInvisibleAnnotations");
            n += 8 + this.n.a();
        }
        if (this.U != null) {
            this.b.newUTF8("RuntimeVisibleTypeAnnotations");
            n += 8 + this.U.a();
        }
        if (this.V != null) {
            this.b.newUTF8("RuntimeInvisibleTypeAnnotations");
            n += 8 + this.V.a();
        }
        if (this.o != null) {
            this.b.newUTF8("RuntimeVisibleParameterAnnotations");
            n += 7 + 2 * (this.o.length - this.R);
            for (int i = this.o.length - 1; i >= this.R; --i) {
                n += ((this.o[i] == null) ? 0 : this.o[i].a());
            }
        }
        if (this.p != null) {
            this.b.newUTF8("RuntimeInvisibleParameterAnnotations");
            n += 7 + 2 * (this.p.length - this.R);
            for (int j = this.p.length - 1; j >= this.R; --j) {
                n += ((this.p[j] == null) ? 0 : this.p[j].a());
            }
        }
        if (this.q != null) {
            n += this.q.a(this.b, null, 0, -1, -1);
        }
        return n;
    }
    
    final void a(final $ByteVector $ByteVector) {
        $ByteVector.putShort(this.c & ~(0xE0000 | (this.c & 0x40000) / 64)).putShort(this.d).putShort(this.e);
        if (this.h != 0) {
            $ByteVector.putByteArray(this.b.K.b, this.h, this.i);
            return;
        }
        int n = 0;
        if (this.r.b > 0) {
            ++n;
        }
        if (this.j > 0) {
            ++n;
        }
        if ((this.c & 0x1000) != 0x0 && ((this.b.b & 0xFFFF) < 49 || (this.c & 0x40000) != 0x0)) {
            ++n;
        }
        if ((this.c & 0x20000) != 0x0) {
            ++n;
        }
        if (this.g != null) {
            ++n;
        }
        if (this.$ != null) {
            ++n;
        }
        if (this.l != null) {
            ++n;
        }
        if (this.m != null) {
            ++n;
        }
        if (this.n != null) {
            ++n;
        }
        if (this.U != null) {
            ++n;
        }
        if (this.V != null) {
            ++n;
        }
        if (this.o != null) {
            ++n;
        }
        if (this.p != null) {
            ++n;
        }
        if (this.q != null) {
            n += this.q.a();
        }
        $ByteVector.putShort(n);
        if (this.r.b > 0) {
            int n2 = 12 + this.r.b + 8 * this.A;
            if (this.E != null) {
                n2 += 8 + this.E.b;
            }
            if (this.G != null) {
                n2 += 8 + this.G.b;
            }
            if (this.I != null) {
                n2 += 8 + this.I.b;
            }
            if (this.v != null) {
                n2 += 8 + this.v.b;
            }
            if (this.W != null) {
                n2 += 8 + this.W.a();
            }
            if (this.X != null) {
                n2 += 8 + this.X.a();
            }
            if (this.J != null) {
                n2 += this.J.a(this.b, this.r.a, this.r.b, this.s, this.t);
            }
            $ByteVector.putShort(this.b.newUTF8("Code")).putInt(n2);
            $ByteVector.putShort(this.s).putShort(this.t);
            $ByteVector.putInt(this.r.b).putByteArray(this.r.a, 0, this.r.b);
            $ByteVector.putShort(this.A);
            if (this.A > 0) {
                for ($Handler $Handler = this.B; $Handler != null; $Handler = $Handler.f) {
                    $ByteVector.putShort($Handler.a.c).putShort($Handler.b.c).putShort($Handler.c.c).putShort($Handler.e);
                }
            }
            int n3 = 0;
            if (this.E != null) {
                ++n3;
            }
            if (this.G != null) {
                ++n3;
            }
            if (this.I != null) {
                ++n3;
            }
            if (this.v != null) {
                ++n3;
            }
            if (this.W != null) {
                ++n3;
            }
            if (this.X != null) {
                ++n3;
            }
            if (this.J != null) {
                n3 += this.J.a();
            }
            $ByteVector.putShort(n3);
            if (this.E != null) {
                $ByteVector.putShort(this.b.newUTF8("LocalVariableTable"));
                $ByteVector.putInt(this.E.b + 2).putShort(this.D);
                $ByteVector.putByteArray(this.E.a, 0, this.E.b);
            }
            if (this.G != null) {
                $ByteVector.putShort(this.b.newUTF8("LocalVariableTypeTable"));
                $ByteVector.putInt(this.G.b + 2).putShort(this.F);
                $ByteVector.putByteArray(this.G.a, 0, this.G.b);
            }
            if (this.I != null) {
                $ByteVector.putShort(this.b.newUTF8("LineNumberTable"));
                $ByteVector.putInt(this.I.b + 2).putShort(this.H);
                $ByteVector.putByteArray(this.I.a, 0, this.I.b);
            }
            if (this.v != null) {
                $ByteVector.putShort(this.b.newUTF8(((this.b.b & 0xFFFF) >= 50) ? "StackMapTable" : "StackMap"));
                $ByteVector.putInt(this.v.b + 2).putShort(this.u);
                $ByteVector.putByteArray(this.v.a, 0, this.v.b);
            }
            if (this.W != null) {
                $ByteVector.putShort(this.b.newUTF8("RuntimeVisibleTypeAnnotations"));
                this.W.a($ByteVector);
            }
            if (this.X != null) {
                $ByteVector.putShort(this.b.newUTF8("RuntimeInvisibleTypeAnnotations"));
                this.X.a($ByteVector);
            }
            if (this.J != null) {
                this.J.a(this.b, this.r.a, this.r.b, this.t, this.s, $ByteVector);
            }
        }
        if (this.j > 0) {
            $ByteVector.putShort(this.b.newUTF8("Exceptions")).putInt(2 * this.j + 2);
            $ByteVector.putShort(this.j);
            for (int i = 0; i < this.j; ++i) {
                $ByteVector.putShort(this.k[i]);
            }
        }
        if ((this.c & 0x1000) != 0x0 && ((this.b.b & 0xFFFF) < 49 || (this.c & 0x40000) != 0x0)) {
            $ByteVector.putShort(this.b.newUTF8("Synthetic")).putInt(0);
        }
        if ((this.c & 0x20000) != 0x0) {
            $ByteVector.putShort(this.b.newUTF8("Deprecated")).putInt(0);
        }
        if (this.g != null) {
            $ByteVector.putShort(this.b.newUTF8("Signature")).putInt(2).putShort(this.b.newUTF8(this.g));
        }
        if (this.$ != null) {
            $ByteVector.putShort(this.b.newUTF8("MethodParameters"));
            $ByteVector.putInt(this.$.b + 1).putByte(this.Z);
            $ByteVector.putByteArray(this.$.a, 0, this.$.b);
        }
        if (this.l != null) {
            $ByteVector.putShort(this.b.newUTF8("AnnotationDefault"));
            $ByteVector.putInt(this.l.b);
            $ByteVector.putByteArray(this.l.a, 0, this.l.b);
        }
        if (this.m != null) {
            $ByteVector.putShort(this.b.newUTF8("RuntimeVisibleAnnotations"));
            this.m.a($ByteVector);
        }
        if (this.n != null) {
            $ByteVector.putShort(this.b.newUTF8("RuntimeInvisibleAnnotations"));
            this.n.a($ByteVector);
        }
        if (this.U != null) {
            $ByteVector.putShort(this.b.newUTF8("RuntimeVisibleTypeAnnotations"));
            this.U.a($ByteVector);
        }
        if (this.V != null) {
            $ByteVector.putShort(this.b.newUTF8("RuntimeInvisibleTypeAnnotations"));
            this.V.a($ByteVector);
        }
        if (this.o != null) {
            $ByteVector.putShort(this.b.newUTF8("RuntimeVisibleParameterAnnotations"));
            $AnnotationWriter.a(this.o, this.R, $ByteVector);
        }
        if (this.p != null) {
            $ByteVector.putShort(this.b.newUTF8("RuntimeInvisibleParameterAnnotations"));
            $AnnotationWriter.a(this.p, this.R, $ByteVector);
        }
        if (this.q != null) {
            this.q.a(this.b, null, 0, -1, -1, $ByteVector);
        }
    }
}
