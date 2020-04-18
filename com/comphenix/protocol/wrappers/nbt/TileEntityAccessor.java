package com.comphenix.protocol.wrappers.nbt;

import com.google.common.collect.Maps;
import java.util.Iterator;
import com.comphenix.net.sf.cglib.proxy.Enhancer;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.net.sf.cglib.proxy.Callback;
import com.comphenix.net.sf.cglib.proxy.MethodProxy;
import java.lang.reflect.Method;
import com.comphenix.net.sf.cglib.proxy.MethodInterceptor;
import com.comphenix.protocol.utility.EnhancerFactory;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.net.sf.cglib.asm.$MethodVisitor;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import com.comphenix.net.sf.cglib.asm.$ClassReader;
import com.comphenix.protocol.utility.MinecraftReflection;
import java.io.IOException;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.block.BlockState;

class TileEntityAccessor<T extends BlockState>
{
    private static final TileEntityAccessor<BlockState> EMPTY_ACCESSOR;
    private static final ConcurrentMap<Class<?>, TileEntityAccessor<?>> cachedAccessors;
    private FieldAccessor tileEntityField;
    private MethodAccessor readCompound;
    private MethodAccessor writeCompound;
    private boolean writeDetected;
    private boolean readDetected;
    
    TileEntityAccessor() {
    }
    
    private TileEntityAccessor(final FieldAccessor tileEntityField, final T state) {
        if (tileEntityField != null) {
            this.tileEntityField = tileEntityField;
            final Class<?> type = tileEntityField.getField().getType();
            this.findMethods(type, state);
        }
    }
    
    void findMethods(final Class<?> type, final T state) {
        try {
            this.findMethodsUsingASM();
        }
        catch (IOException ex3) {
            try {
                this.findMethodUsingCGLib(state);
            }
            catch (Exception ex2) {
                throw new RuntimeException("Cannot find read/write methods in " + type, ex2);
            }
        }
        if (this.readCompound == null) {
            throw new RuntimeException("Unable to find read method in " + type);
        }
        if (this.writeCompound == null) {
            throw new RuntimeException("Unable to find write method in " + type);
        }
    }
    
    private void findMethodsUsingASM() throws IOException {
        final Class<?> nbtCompoundClass = MinecraftReflection.getNBTCompoundClass();
        final Class<?> tileEntityClass = MinecraftReflection.getTileEntityClass();
        final $ClassReader reader = new $ClassReader(tileEntityClass.getCanonicalName());
        final String tagCompoundName = getJarName(MinecraftReflection.getNBTCompoundClass());
        final String expectedDesc = "(L" + tagCompoundName + ";)";
        reader.accept(new $ClassVisitor(327680) {
            @Override
            public $MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
                final String methodName = name;
                if (desc.startsWith(expectedDesc)) {
                    return new $MethodVisitor(327680) {
                        private int readMethods;
                        private int writeMethods;
                        
                        @Override
                        public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc, final boolean intf) {
                            if (opcode == 182 && tagCompoundName.equals(owner) && desc.startsWith("(Ljava/lang/String")) {
                                if (desc.endsWith(")V")) {
                                    ++this.writeMethods;
                                }
                                else {
                                    ++this.readMethods;
                                }
                            }
                        }
                        
                        @Override
                        public void visitEnd() {
                            if (this.readMethods > this.writeMethods) {
                                TileEntityAccessor.this.readCompound = Accessors.getMethodAccessor(tileEntityClass, methodName, nbtCompoundClass);
                            }
                            else if (this.writeMethods > this.readMethods) {
                                TileEntityAccessor.this.writeCompound = Accessors.getMethodAccessor(tileEntityClass, methodName, nbtCompoundClass);
                            }
                            super.visitEnd();
                        }
                    };
                }
                return null;
            }
        }, 0);
    }
    
    private void findMethodUsingCGLib(final T blockState) throws IOException {
        final Class<?> nbtCompoundClass = MinecraftReflection.getNBTCompoundClass();
        final Enhancer enhancer = EnhancerFactory.getInstance().createEnhancer();
        enhancer.setSuperclass(nbtCompoundClass);
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(final Object obj, final Method method, final Object[] args, final MethodProxy proxy) throws Throwable {
                if (method.getReturnType().equals(Void.TYPE)) {
                    TileEntityAccessor.this.writeDetected = true;
                }
                else {
                    TileEntityAccessor.this.readDetected = true;
                }
                throw new RuntimeException("Stop execution.");
            }
        });
        final Object compound = enhancer.create();
        final Object tileEntity = this.tileEntityField.get(blockState);
        for (final Method method : FuzzyReflection.fromObject(tileEntity, true).getMethodListByParameters(Void.TYPE, new Class[] { nbtCompoundClass })) {
            try {
                this.readDetected = false;
                this.writeDetected = false;
                method.invoke(tileEntity, compound);
            }
            catch (Exception e) {
                if (this.readDetected) {
                    this.readCompound = Accessors.getMethodAccessor(method, true);
                }
                if (!this.writeDetected) {
                    continue;
                }
                this.writeCompound = Accessors.getMethodAccessor(method, true);
            }
        }
    }
    
    private static String getJarName(final Class<?> clazz) {
        return clazz.getCanonicalName().replace('.', '/');
    }
    
    public NbtCompound readBlockState(final T state) {
        final NbtCompound output = NbtFactory.ofCompound("");
        final Object tileEntity = this.tileEntityField.get(state);
        this.writeCompound.invoke(tileEntity, NbtFactory.fromBase((NbtBase<Object>)output).getHandle());
        return output;
    }
    
    public void writeBlockState(final T state, final NbtCompound compound) {
        final Object tileEntity = this.tileEntityField.get(state);
        this.readCompound.invoke(tileEntity, NbtFactory.fromBase((NbtBase<Object>)compound).getHandle());
    }
    
    public static <T extends BlockState> TileEntityAccessor<T> getAccessor(final T state) {
        final Class<?> craftBlockState = state.getClass();
        TileEntityAccessor<?> accessor = TileEntityAccessor.cachedAccessors.get(craftBlockState);
        if (accessor == null) {
            TileEntityAccessor<?> created = null;
            FieldAccessor field = null;
            try {
                field = Accessors.getFieldAccessor(craftBlockState, MinecraftReflection.getTileEntityClass(), true);
            }
            catch (Exception e) {
                created = TileEntityAccessor.EMPTY_ACCESSOR;
            }
            if (field != null) {
                created = new TileEntityAccessor<Object>(field, state);
            }
            accessor = TileEntityAccessor.cachedAccessors.putIfAbsent(craftBlockState, created);
            if (accessor == null) {
                accessor = created;
            }
        }
        return (TileEntityAccessor<T>)((accessor != TileEntityAccessor.EMPTY_ACCESSOR) ? accessor : null);
    }
    
    static {
        EMPTY_ACCESSOR = new TileEntityAccessor<BlockState>();
        cachedAccessors = Maps.newConcurrentMap();
    }
}
