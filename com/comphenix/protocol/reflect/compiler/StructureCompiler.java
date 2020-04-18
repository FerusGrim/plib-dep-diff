package com.comphenix.protocol.reflect.compiler;

import com.google.common.base.Objects;
import com.comphenix.net.sf.cglib.asm.$MethodVisitor;
import com.google.common.primitives.Primitives;
import com.comphenix.net.sf.cglib.asm.$Label;
import com.comphenix.net.sf.cglib.asm.$FieldVisitor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import java.util.List;
import com.comphenix.net.sf.cglib.asm.$Type;
import com.comphenix.net.sf.cglib.asm.$ClassWriter;
import java.lang.reflect.InvocationTargetException;
import com.comphenix.protocol.error.Report;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.reflect.StructureModifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.lang.reflect.Method;
import com.comphenix.protocol.error.ReportType;

public final class StructureCompiler
{
    public static final ReportType REPORT_TOO_MANY_GENERATED_CLASSES;
    private static volatile Method defineMethod;
    private Map<StructureKey, Class> compiledCache;
    private ClassLoader loader;
    private static String PACKAGE_NAME;
    private static String SUPER_CLASS;
    private static String COMPILED_CLASS;
    private static String FIELD_EXCEPTION_CLASS;
    public static boolean attemptClassLoad;
    
    StructureCompiler(final ClassLoader loader) {
        this.compiledCache = new ConcurrentHashMap<StructureKey, Class>();
        this.loader = loader;
    }
    
    public <TField> boolean lookupClassLoader(final StructureModifier<TField> source) {
        final StructureKey key = new StructureKey(source);
        if (this.compiledCache.containsKey(key)) {
            return true;
        }
        if (!StructureCompiler.attemptClassLoad) {
            return false;
        }
        try {
            final String className = this.getCompiledName(source);
            final Class<?> before = this.loader.loadClass(StructureCompiler.PACKAGE_NAME.replace('/', '.') + "." + className);
            if (before != null) {
                this.compiledCache.put(key, before);
                return true;
            }
        }
        catch (ClassNotFoundException ex) {}
        return false;
    }
    
    public synchronized <TField> StructureModifier<TField> compile(final StructureModifier<TField> source) {
        if (!this.isAnyPublic(source.getFields())) {
            return source;
        }
        final StructureKey key = new StructureKey(source);
        Class<?> compiledClass = this.compiledCache.get(key);
        if (!this.compiledCache.containsKey(key)) {
            compiledClass = this.generateClass(source);
            this.compiledCache.put(key, compiledClass);
        }
        try {
            return (StructureModifier<TField>)compiledClass.getConstructor(StructureModifier.class, StructureCompiler.class).newInstance(source, this);
        }
        catch (OutOfMemoryError e) {
            ProtocolLibrary.getErrorReporter().reportWarning(this, Report.newBuilder(StructureCompiler.REPORT_TOO_MANY_GENERATED_CLASSES).messageParam(this.compiledCache.size()));
            throw e;
        }
        catch (IllegalArgumentException e2) {
            throw new IllegalStateException("Used invalid parameters in instance creation", e2);
        }
        catch (SecurityException e3) {
            throw new RuntimeException("Security limitation!", e3);
        }
        catch (InstantiationException e4) {
            throw new RuntimeException("Error occured while instancing generated class.", e4);
        }
        catch (IllegalAccessException e5) {
            throw new RuntimeException("Security limitation! Cannot create instance of dynamic class.", e5);
        }
        catch (InvocationTargetException e6) {
            throw new RuntimeException("Error occured while instancing generated class.", e6);
        }
        catch (NoSuchMethodException e7) {
            throw new IllegalStateException("Cannot happen.", e7);
        }
    }
    
    private String getSafeTypeName(final Class<?> type) {
        return type.getCanonicalName().replace("[]", "Array").replace(".", "_");
    }
    
    private String getCompiledName(final StructureModifier<?> source) {
        final Class<?> targetType = (Class<?>)source.getTargetType();
        return "CompiledStructure$" + this.getSafeTypeName(targetType) + "$" + this.getSafeTypeName(source.getFieldType());
    }
    
    private <TField> Class<?> generateClass(final StructureModifier<TField> source) {
        final $ClassWriter cw = new $ClassWriter(0);
        final Class<?> targetType = (Class<?>)source.getTargetType();
        final String className = this.getCompiledName(source);
        final String targetSignature = $Type.getDescriptor(targetType);
        final String targetName = targetType.getName().replace('.', '/');
        cw.visit(50, 33, StructureCompiler.PACKAGE_NAME + "/" + className, null, StructureCompiler.COMPILED_CLASS, null);
        this.createFields(cw, targetSignature);
        this.createConstructor(cw, className, targetSignature, targetName);
        this.createReadMethod(cw, className, source.getFields(), targetSignature, targetName);
        this.createWriteMethod(cw, className, source.getFields(), targetSignature, targetName);
        cw.visitEnd();
        final byte[] data = cw.toByteArray();
        try {
            if (StructureCompiler.defineMethod == null) {
                final Method defined = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, Integer.TYPE, Integer.TYPE);
                defined.setAccessible(true);
                StructureCompiler.defineMethod = defined;
            }
            final Class clazz = (Class)StructureCompiler.defineMethod.invoke(this.loader, null, data, 0, data.length);
            return (Class<?>)clazz;
        }
        catch (SecurityException e) {
            throw new RuntimeException("Cannot use reflection to dynamically load a class.", e);
        }
        catch (NoSuchMethodException e2) {
            throw new IllegalStateException("Incompatible JVM.", e2);
        }
        catch (IllegalArgumentException e3) {
            throw new IllegalStateException("Cannot call defineMethod - wrong JVM?", e3);
        }
        catch (IllegalAccessException e4) {
            throw new RuntimeException("Security limitation! Cannot dynamically load class.", e4);
        }
        catch (InvocationTargetException e5) {
            throw new RuntimeException("Error occured in code generator.", e5);
        }
    }
    
    private boolean isAnyPublic(final List<Field> fields) {
        for (int i = 0; i < fields.size(); ++i) {
            if (this.isPublic(fields.get(i))) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isPublic(final Field field) {
        return Modifier.isPublic(field.getModifiers());
    }
    
    private boolean isNonFinal(final Field field) {
        return !Modifier.isFinal(field.getModifiers());
    }
    
    private void createFields(final $ClassWriter cw, final String targetSignature) {
        final $FieldVisitor typedField = cw.visitField(2, "typedTarget", targetSignature, null, null);
        typedField.visitEnd();
    }
    
    private void createWriteMethod(final $ClassWriter cw, final String className, final List<Field> fields, final String targetSignature, final String targetName) {
        final String methodDescriptor = "(ILjava/lang/Object;)L" + StructureCompiler.SUPER_CLASS + ";";
        final String methodSignature = "(ILjava/lang/Object;)L" + StructureCompiler.SUPER_CLASS + "<Ljava/lang/Object;>;";
        final $MethodVisitor mv = cw.visitMethod(4, "writeGenerated", methodDescriptor, methodSignature, new String[] { StructureCompiler.FIELD_EXCEPTION_CLASS });
        final BoxingHelper boxingHelper = new BoxingHelper(mv);
        final String generatedClassName = StructureCompiler.PACKAGE_NAME + "/" + className;
        mv.visitCode();
        mv.visitVarInsn(25, 0);
        mv.visitFieldInsn(180, generatedClassName, "typedTarget", targetSignature);
        mv.visitVarInsn(58, 3);
        mv.visitVarInsn(21, 1);
        final $Label[] $Labels = new $Label[fields.size()];
        final $Label error$Label = new $Label();
        final $Label return$Label = new $Label();
        for (int i = 0; i < fields.size(); ++i) {
            $Labels[i] = new $Label();
        }
        mv.visitTableSwitchInsn(0, $Labels.length - 1, error$Label, $Labels);
        for (int i = 0; i < fields.size(); ++i) {
            final Field field = fields.get(i);
            final Class<?> outputType = field.getType();
            final Class<?> inputType = (Class<?>)Primitives.wrap((Class)outputType);
            final String typeDescriptor = $Type.getDescriptor(outputType);
            final String inputPath = inputType.getName().replace('.', '/');
            mv.visitLabel($Labels[i]);
            if (i == 0) {
                mv.visitFrame(1, 1, new Object[] { targetName }, 0, null);
            }
            else {
                mv.visitFrame(3, 0, null, 0, null);
            }
            if (this.isPublic(field) && this.isNonFinal(field)) {
                mv.visitVarInsn(25, 3);
                mv.visitVarInsn(25, 2);
                if (!outputType.isPrimitive()) {
                    mv.visitTypeInsn(192, inputPath);
                }
                else {
                    boxingHelper.unbox($Type.getType(outputType));
                }
                mv.visitFieldInsn(181, targetName, field.getName(), typeDescriptor);
            }
            else {
                mv.visitVarInsn(25, 0);
                mv.visitVarInsn(21, 1);
                mv.visitVarInsn(25, 2);
                mv.visitMethodInsn(182, generatedClassName, "writeReflected", "(ILjava/lang/Object;)V");
            }
            mv.visitJumpInsn(167, return$Label);
        }
        mv.visitLabel(error$Label);
        mv.visitFrame(3, 0, null, 0, null);
        mv.visitTypeInsn(187, StructureCompiler.FIELD_EXCEPTION_CLASS);
        mv.visitInsn(89);
        mv.visitTypeInsn(187, "java/lang/StringBuilder");
        mv.visitInsn(89);
        mv.visitLdcInsn("Invalid index ");
        mv.visitMethodInsn(183, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
        mv.visitVarInsn(21, 1);
        mv.visitMethodInsn(182, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;");
        mv.visitMethodInsn(182, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
        mv.visitMethodInsn(183, StructureCompiler.FIELD_EXCEPTION_CLASS, "<init>", "(Ljava/lang/String;)V");
        mv.visitInsn(191);
        mv.visitLabel(return$Label);
        mv.visitFrame(3, 0, null, 0, null);
        mv.visitVarInsn(25, 0);
        mv.visitInsn(176);
        mv.visitMaxs(5, 4);
        mv.visitEnd();
    }
    
    private void createReadMethod(final $ClassWriter cw, final String className, final List<Field> fields, final String targetSignature, final String targetName) {
        final $MethodVisitor mv = cw.visitMethod(4, "readGenerated", "(I)Ljava/lang/Object;", null, new String[] { "com/comphenix/protocol/reflect/FieldAccessException" });
        final BoxingHelper boxingHelper = new BoxingHelper(mv);
        final String generatedClassName = StructureCompiler.PACKAGE_NAME + "/" + className;
        mv.visitCode();
        mv.visitVarInsn(25, 0);
        mv.visitFieldInsn(180, generatedClassName, "typedTarget", targetSignature);
        mv.visitVarInsn(58, 2);
        mv.visitVarInsn(21, 1);
        final $Label[] $Labels = new $Label[fields.size()];
        final $Label error$Label = new $Label();
        for (int i = 0; i < fields.size(); ++i) {
            $Labels[i] = new $Label();
        }
        mv.visitTableSwitchInsn(0, fields.size() - 1, error$Label, $Labels);
        for (int i = 0; i < fields.size(); ++i) {
            final Field field = fields.get(i);
            final Class<?> outputType = field.getType();
            final String typeDescriptor = $Type.getDescriptor(outputType);
            mv.visitLabel($Labels[i]);
            if (i == 0) {
                mv.visitFrame(1, 1, new Object[] { targetName }, 0, null);
            }
            else {
                mv.visitFrame(3, 0, null, 0, null);
            }
            if (this.isPublic(field)) {
                mv.visitVarInsn(25, 2);
                mv.visitFieldInsn(180, targetName, field.getName(), typeDescriptor);
                boxingHelper.box($Type.getType(outputType));
            }
            else {
                mv.visitVarInsn(25, 0);
                mv.visitVarInsn(21, 1);
                mv.visitMethodInsn(182, generatedClassName, "readReflected", "(I)Ljava/lang/Object;");
            }
            mv.visitInsn(176);
        }
        mv.visitLabel(error$Label);
        mv.visitFrame(3, 0, null, 0, null);
        mv.visitTypeInsn(187, StructureCompiler.FIELD_EXCEPTION_CLASS);
        mv.visitInsn(89);
        mv.visitTypeInsn(187, "java/lang/StringBuilder");
        mv.visitInsn(89);
        mv.visitLdcInsn("Invalid index ");
        mv.visitMethodInsn(183, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
        mv.visitVarInsn(21, 1);
        mv.visitMethodInsn(182, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;");
        mv.visitMethodInsn(182, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
        mv.visitMethodInsn(183, StructureCompiler.FIELD_EXCEPTION_CLASS, "<init>", "(Ljava/lang/String;)V");
        mv.visitInsn(191);
        mv.visitMaxs(5, 3);
        mv.visitEnd();
    }
    
    private void createConstructor(final $ClassWriter cw, final String className, final String targetSignature, final String targetName) {
        final $MethodVisitor mv = cw.visitMethod(1, "<init>", "(L" + StructureCompiler.SUPER_CLASS + ";L" + StructureCompiler.PACKAGE_NAME + "/StructureCompiler;)V", "(L" + StructureCompiler.SUPER_CLASS + "<Ljava/lang/Object;>;L" + StructureCompiler.PACKAGE_NAME + "/StructureCompiler;)V", null);
        final String fullClassName = StructureCompiler.PACKAGE_NAME + "/" + className;
        mv.visitCode();
        mv.visitVarInsn(25, 0);
        mv.visitMethodInsn(183, StructureCompiler.COMPILED_CLASS, "<init>", "()V");
        mv.visitVarInsn(25, 0);
        mv.visitVarInsn(25, 1);
        mv.visitMethodInsn(182, fullClassName, "initialize", "(L" + StructureCompiler.SUPER_CLASS + ";)V");
        mv.visitVarInsn(25, 0);
        mv.visitVarInsn(25, 1);
        mv.visitMethodInsn(182, StructureCompiler.SUPER_CLASS, "getTarget", "()Ljava/lang/Object;");
        mv.visitFieldInsn(181, fullClassName, "target", "Ljava/lang/Object;");
        mv.visitVarInsn(25, 0);
        mv.visitVarInsn(25, 0);
        mv.visitFieldInsn(180, fullClassName, "target", "Ljava/lang/Object;");
        mv.visitTypeInsn(192, targetName);
        mv.visitFieldInsn(181, fullClassName, "typedTarget", targetSignature);
        mv.visitVarInsn(25, 0);
        mv.visitVarInsn(25, 2);
        mv.visitFieldInsn(181, fullClassName, "compiler", "L" + StructureCompiler.PACKAGE_NAME + "/StructureCompiler;");
        mv.visitInsn(177);
        mv.visitMaxs(2, 3);
        mv.visitEnd();
    }
    
    static {
        REPORT_TOO_MANY_GENERATED_CLASSES = new ReportType("Generated too many classes (count: %s)");
        StructureCompiler.PACKAGE_NAME = "com/comphenix/protocol/reflect/compiler";
        StructureCompiler.SUPER_CLASS = "com/comphenix/protocol/reflect/StructureModifier";
        StructureCompiler.COMPILED_CLASS = StructureCompiler.PACKAGE_NAME + "/CompiledStructureModifier";
        StructureCompiler.FIELD_EXCEPTION_CLASS = "com/comphenix/protocol/reflect/FieldAccessException";
        StructureCompiler.attemptClassLoad = false;
    }
    
    static class StructureKey
    {
        private Class targetType;
        private Class fieldType;
        
        public StructureKey(final StructureModifier<?> source) {
            this(source.getTargetType(), source.getFieldType());
        }
        
        public StructureKey(final Class targetType, final Class fieldType) {
            this.targetType = targetType;
            this.fieldType = fieldType;
        }
        
        @Override
        public int hashCode() {
            return Objects.hashCode(new Object[] { this.targetType, this.fieldType });
        }
        
        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof StructureKey) {
                final StructureKey other = (StructureKey)obj;
                return Objects.equal((Object)this.targetType, (Object)other.targetType) && Objects.equal((Object)this.fieldType, (Object)other.fieldType);
            }
            return false;
        }
    }
}
