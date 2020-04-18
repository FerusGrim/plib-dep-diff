package com.comphenix.net.sf.cglib.core;

import java.lang.reflect.Modifier;
import java.lang.reflect.Member;
import com.comphenix.net.sf.cglib.asm.$Type;

public class VisibilityPredicate implements Predicate
{
    private boolean protectedOk;
    private String pkg;
    private boolean samePackageOk;
    
    public VisibilityPredicate(final Class source, final boolean protectedOk) {
        this.protectedOk = protectedOk;
        this.samePackageOk = (source.getClassLoader() != null);
        this.pkg = TypeUtils.getPackageName($Type.getType(source));
    }
    
    public boolean evaluate(final Object arg) {
        final Member member = (Member)arg;
        final int mod = member.getModifiers();
        return !Modifier.isPrivate(mod) && (Modifier.isPublic(mod) || (Modifier.isProtected(mod) && this.protectedOk) || (this.samePackageOk && this.pkg.equals(TypeUtils.getPackageName($Type.getType(member.getDeclaringClass())))));
    }
}
