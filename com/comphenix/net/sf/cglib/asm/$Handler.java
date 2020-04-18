package com.comphenix.net.sf.cglib.asm;

class $Handler
{
    $Label a;
    $Label b;
    $Label c;
    String d;
    int e;
    $Handler f;
    
    static $Handler a($Handler f, final $Label $Label, final $Label $Label2) {
        if (f == null) {
            return null;
        }
        f.f = a(f.f, $Label, $Label2);
        final int c = f.a.c;
        final int c2 = f.b.c;
        final int c3 = $Label.c;
        final int n = ($Label2 == null) ? Integer.MAX_VALUE : $Label2.c;
        if (c3 < c2 && n > c) {
            if (c3 <= c) {
                if (n >= c2) {
                    f = f.f;
                }
                else {
                    f.a = $Label2;
                }
            }
            else if (n >= c2) {
                f.b = $Label;
            }
            else {
                final $Handler f2 = new $Handler();
                f2.a = $Label2;
                f2.b = f.b;
                f2.c = f.c;
                f2.d = f.d;
                f2.e = f.e;
                f2.f = f.f;
                f.b = $Label;
                f.f = f2;
            }
        }
        return f;
    }
}
