package com.comphenix.net.sf.cglib.transform;

import java.util.Iterator;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import java.util.Map;
import java.io.File;
import java.util.HashMap;
import java.util.Collection;
import org.apache.tools.ant.types.FileSet;
import java.util.Vector;
import org.apache.tools.ant.Task;

public abstract class AbstractProcessTask extends Task
{
    private Vector filesets;
    
    public AbstractProcessTask() {
        this.filesets = new Vector();
    }
    
    public void addFileset(final FileSet set) {
        this.filesets.addElement(set);
    }
    
    protected Collection getFiles() {
        final Map fileMap = new HashMap();
        final Project p = this.getProject();
        for (int i = 0; i < this.filesets.size(); ++i) {
            final FileSet fs = this.filesets.elementAt(i);
            final DirectoryScanner ds = fs.getDirectoryScanner(p);
            final String[] srcFiles = ds.getIncludedFiles();
            final File dir = fs.getDir(p);
            for (int j = 0; j < srcFiles.length; ++j) {
                final File src = new File(dir, srcFiles[j]);
                fileMap.put(src.getAbsolutePath(), src);
            }
        }
        return fileMap.values();
    }
    
    public void execute() throws BuildException {
        this.beforeExecute();
        final Iterator it = this.getFiles().iterator();
        while (it.hasNext()) {
            try {
                this.processFile(it.next());
                continue;
            }
            catch (Exception e) {
                throw new BuildException((Throwable)e);
            }
            break;
        }
    }
    
    protected void beforeExecute() throws BuildException {
    }
    
    protected abstract void processFile(final File p0) throws Exception;
}
