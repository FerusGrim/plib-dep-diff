package com.comphenix.protocol.reflect.compiler;

import com.comphenix.protocol.reflect.StructureModifier;

public interface CompileListener<TKey>
{
    void onCompiled(final StructureModifier<TKey> p0);
}
