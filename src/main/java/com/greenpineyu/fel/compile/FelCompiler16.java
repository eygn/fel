package com.greenpineyu.fel.compile;

import com.greenpineyu.fel.Expression;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author byg
 * @date 2021-09-09 16:27:16
 */
public class FelCompiler16<T> implements FelCompiler {

    private static final Logger log = LoggerFactory.getLogger(FelCompiler16.class);

    private CustomJavaCompiler javaCompiler;
    private GenericObjectPool<CustomJavaCompiler> compilerPoll = new GenericObjectPool<>(new PooledObjectFactory<CustomJavaCompiler>() {
        @Override
        public PooledObject<CustomJavaCompiler> makeObject() throws Exception {
            return new DefaultPooledObject<>(new CustomJavaCompilerImpl());
        }

        @Override
        public void destroyObject(PooledObject<CustomJavaCompiler> pooledObject) throws Exception {

        }

        @Override
        public boolean validateObject(PooledObject<CustomJavaCompiler> pooledObject) {
            return true;
        }

        @Override
        public void activateObject(PooledObject<CustomJavaCompiler> pooledObject) throws Exception {

        }

        @Override
        public void passivateObject(PooledObject<CustomJavaCompiler> pooledObject) throws Exception {

        }
    });

    public FelCompiler16() {
        this.javaCompiler = new CustomJavaCompilerImpl();
        compilerPoll.setConfig(new GenericObjectPoolConfig() {{
            setMaxTotal(8);
        }});
    }

    /**
     * @param src
     * @return
     */
    @Override
    public Expression compile(JavaSource src) {
        Class<T> compile;
        try {
            compile = compileToClass(src);
            return (Expression) compile.newInstance();
        } catch (InstantiationException e) {
            log.error("compile error, JavaSource:" + src.getSource(), e);
        } catch (IllegalAccessException e) {
            log.error("compile error, JavaSource:" + src.getSource(), e);
        } catch (Throwable e) {
            log.error("compile error, JavaSource:" + src.getSource(), e);
        }
        //diagnostics.getDiagnostics().clear();
        return null;
    }


    public synchronized Class<T> compileToClass(final JavaSource src) {
        long l = System.currentTimeMillis();
        try {
            log.warn("[FEL]sync compiler source {}", src.getSimpleName());
            String dPath = FelClassLoader.felclassesPath + File.separator + FelClassLoader.CLASS_OUTPUT;
            File dirFile = new File(dPath);
            if (!dirFile.exists()) {
                boolean success = dirFile.mkdirs();
                if (!success) {
                    throw new RuntimeException("output directory for process class can't be created");
                }
            }
            byte[] res;
            File javaSourceFile = writeJavaFile(dirFile, src.getName(), src.getSource());
            javaCompiler.compile(javaSourceFile, "UTF-8", src.getName(), new File(dPath));
            File classFile = new File(dirFile, src.getName().replace('.', File.separatorChar) + ".class");
            res = FileUtils.readFileToByteArray(classFile);
            Class<T> clazz = (Class<T>) FelClassLoader.getInstance().defineClassSelf(src.getName(), res, 0, res.length);
            return clazz;
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            log.warn("[FEL]compiler {} cost time is:{}", src.getName(), (System.currentTimeMillis() - l));
        }
    }

    private File writeJavaFile(File dirFile, String className, String javaCode) {
        int index = className.lastIndexOf(".");
        if (index >= 0) {
            String packageName = className.substring(0, index);
            createDependDir(dirFile, packageName);
        }
        File file = new File(dirFile, className.replace('.', File.separatorChar) + ".java");
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileOutputStream(file));
            out.write(javaCode.toString());
            out.flush();
            return file;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private void createDependDir(File parentDir, String packageName) {
        String[] list = StringUtils.split(packageName, '.');
        for (int i = 0; i < list.length; i++) {
            parentDir = new File(parentDir, list[i]);
            if (parentDir.exists() == false) {
                parentDir.mkdir();
            }
        }
    }

    static URI toUri(String name) {
        try {
            return new URI(name);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}

