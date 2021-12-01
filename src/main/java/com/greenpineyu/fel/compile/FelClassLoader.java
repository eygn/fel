package com.greenpineyu.fel.compile;

import org.codehaus.commons.compiler.util.reflect.ByteArrayClassLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

/**
 * @author yiming
 * @date 2018/1/29
 */
public class FelClassLoader extends URLClassLoader{

    public final static String CLASS_OUTPUT = "felclasses" + File.separator;

    /**
     * 默认路径是user.dir的，可以自定义
     */
    public static String felclassesPath = System.getProperty("user.dir");

    private static FelClassLoader m_instance = null;
    private static ByteArrayClassLoader mem_instance = null;

    public FelClassLoader(URL[] urls, ClassLoader classLoader) {super(urls, classLoader);}

    public static FelClassLoader getInstance() {
        if (m_instance == null) {
            try {
                synchronized (URLClassLoader.class) {
                    if (m_instance == null) {
                        URL url = new URL("file:///" + felclassesPath + File.separator + CLASS_OUTPUT);
                        m_instance = new FelClassLoader(new URL[] {url}, FelClassLoader.class.getClassLoader());
                    }
                }
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        }
        return m_instance;
    }

    public static ByteArrayClassLoader getMemoryInstance(Map<String,byte[]> clazzByte) {
        if (mem_instance == null) {
            synchronized (URLClassLoader.class) {
                if (mem_instance == null) {
                    mem_instance=new ByteArrayClassLoader(clazzByte);
                }
            }
        }
        return mem_instance;
    }



    public Class<?> defineClassSelf(String name, byte[] b, int off, int len) {
        return this.defineClass(name, b, off, len);
    }
}
