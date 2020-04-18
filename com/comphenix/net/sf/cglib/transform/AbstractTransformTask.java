package com.comphenix.net.sf.cglib.transform;

import java.io.ByteArrayOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.FileNotFoundException;
import com.comphenix.net.sf.cglib.asm.$ClassReader;
import java.io.FileOutputStream;
import com.comphenix.net.sf.cglib.asm.$ClassVisitor;
import com.comphenix.net.sf.cglib.core.ClassGenerator;
import com.comphenix.net.sf.cglib.core.DebuggingClassWriter;
import com.comphenix.net.sf.cglib.core.ClassNameReader;
import java.io.File;
import com.comphenix.net.sf.cglib.asm.$Attribute;

public abstract class AbstractTransformTask extends AbstractProcessTask
{
    private static final int ZIP_MAGIC = 1347093252;
    private static final int CLASS_MAGIC = -889275714;
    private boolean verbose;
    
    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }
    
    protected abstract ClassTransformer getClassTransformer(final String[] p0);
    
    protected $Attribute[] attributes() {
        return null;
    }
    
    @Override
    protected void processFile(final File file) throws Exception {
        if (this.isClassFile(file)) {
            this.processClassFile(file);
        }
        else if (this.isJarFile(file)) {
            this.processJarFile(file);
        }
        else {
            this.log("ignoring " + file.toURI(), 1);
        }
    }
    
    private void processClassFile(final File file) throws Exception, FileNotFoundException, IOException, MalformedURLException {
        final $ClassReader reader = getClassReader(file);
        final String[] name = ClassNameReader.getClassInfo(reader);
        final DebuggingClassWriter w = new DebuggingClassWriter(2);
        final ClassTransformer t = this.getClassTransformer(name);
        if (t != null) {
            if (this.verbose) {
                this.log("processing " + file.toURI());
            }
            new TransformingClassGenerator(new ClassReaderGenerator(getClassReader(file), this.attributes(), this.getFlags()), t).generateClass(w);
            final FileOutputStream fos = new FileOutputStream(file);
            try {
                fos.write(w.toByteArray());
            }
            finally {
                fos.close();
            }
        }
    }
    
    protected int getFlags() {
        return 0;
    }
    
    private static $ClassReader getClassReader(final File file) throws Exception {
        final InputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            final $ClassReader r = new $ClassReader(in);
            return r;
        }
        finally {
            in.close();
        }
    }
    
    protected boolean isClassFile(final File file) throws IOException {
        return this.checkMagic(file, -889275714L);
    }
    
    protected void processJarFile(final File file) throws Exception {
        if (this.verbose) {
            this.log("processing " + file.toURI());
        }
        final File tempFile = File.createTempFile(file.getName(), null, new File(file.getAbsoluteFile().getParent()));
        try {
            final ZipInputStream zip = new ZipInputStream(new FileInputStream(file));
            try {
                final FileOutputStream fout = new FileOutputStream(tempFile);
                try {
                    final ZipOutputStream out = new ZipOutputStream(fout);
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        byte[] bytes = this.getBytes(zip);
                        if (!entry.isDirectory()) {
                            final DataInputStream din = new DataInputStream(new ByteArrayInputStream(bytes));
                            if (din.readInt() == -889275714) {
                                bytes = this.process(bytes);
                            }
                            else if (this.verbose) {
                                this.log("ignoring " + entry.toString());
                            }
                        }
                        final ZipEntry outEntry = new ZipEntry(entry.getName());
                        outEntry.setMethod(entry.getMethod());
                        outEntry.setComment(entry.getComment());
                        outEntry.setSize(bytes.length);
                        if (outEntry.getMethod() == 0) {
                            final CRC32 crc = new CRC32();
                            crc.update(bytes);
                            outEntry.setCrc(crc.getValue());
                            outEntry.setCompressedSize(bytes.length);
                        }
                        out.putNextEntry(outEntry);
                        out.write(bytes);
                        out.closeEntry();
                        zip.closeEntry();
                    }
                    out.close();
                }
                finally {
                    fout.close();
                }
            }
            finally {
                zip.close();
            }
            if (!file.delete()) {
                throw new IOException("can not delete " + file);
            }
            final File newFile = new File(tempFile.getAbsolutePath());
            if (!newFile.renameTo(file)) {
                throw new IOException("can not rename " + tempFile + " to " + file);
            }
        }
        finally {
            tempFile.delete();
        }
    }
    
    private byte[] process(final byte[] bytes) throws Exception {
        final $ClassReader reader = new $ClassReader(new ByteArrayInputStream(bytes));
        final String[] name = ClassNameReader.getClassInfo(reader);
        final DebuggingClassWriter w = new DebuggingClassWriter(2);
        final ClassTransformer t = this.getClassTransformer(name);
        if (t != null) {
            if (this.verbose) {
                this.log("processing " + name[0]);
            }
            new TransformingClassGenerator(new ClassReaderGenerator(new $ClassReader(new ByteArrayInputStream(bytes)), this.attributes(), this.getFlags()), t).generateClass(w);
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(w.toByteArray());
            return out.toByteArray();
        }
        return bytes;
    }
    
    private byte[] getBytes(final ZipInputStream zip) throws IOException {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final InputStream in = new BufferedInputStream(zip);
        int b;
        while ((b = in.read()) != -1) {
            bout.write(b);
        }
        return bout.toByteArray();
    }
    
    private boolean checkMagic(final File file, final long magic) throws IOException {
        final DataInputStream in = new DataInputStream(new FileInputStream(file));
        try {
            final int m = in.readInt();
            return magic == m;
        }
        finally {
            in.close();
        }
    }
    
    protected boolean isJarFile(final File file) throws IOException {
        return this.checkMagic(file, 1347093252L);
    }
}
