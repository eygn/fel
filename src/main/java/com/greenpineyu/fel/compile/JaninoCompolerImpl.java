package com.greenpineyu.fel.compile;

import com.google.common.collect.Maps;
import com.greenpineyu.fel.Expression;
import org.apache.commons.io.FileUtils;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.Compiler;
import org.codehaus.janino.util.resource.MapResourceCreator;
import org.codehaus.janino.util.resource.MapResourceFinder;
import org.codehaus.janino.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class JaninoCompolerImpl implements CustomJavaCompiler {


    private static final Logger log = LoggerFactory.getLogger(FelCompiler16.class);

    @Override
    public byte[] compile(File javaSourceFile, String encode, String targetClassName, File outputPath) throws Exception {
        throw new UnsupportedOperationException("暂时不支持janno单个文件编译");
    }

    private static AtomicBoolean delete = new AtomicBoolean(false);
    private static Object lock = new Object();
    private static boolean success = false;

    static Map<String, Expression> mutilCompiler(Map<String, JavaSource> src, Map<String, String> fileName2Biz) throws IllegalAccessException, InstantiationException, IOException {
        log.error("mutilCompiler input is {},file2biz is {}", src, fileName2Biz);
        MapResourceFinder sourceFinder = new MapResourceFinder();
        Map<String, Expression> $r = Maps.newHashMap();
        String dPath = FelClassLoader.felclassesPath + File.separator + FelClassLoader.CLASS_OUTPUT;
        src.forEach((k, v) -> {
            sourceFinder.addResource(v.getName(), v.getSource());
        });
        Map<String, Class> clazzs = Maps.newHashMap();
        //编译资源 文件名称(全限定类名)->字节码
        Map<String, byte[]> resultMap = compile(sourceFinder);
        File dirFile = new File(dPath);

        synchronized (lock) {
            try {
                // 只有第一次启动的时候会删除一次已有文件
                if (delete.compareAndSet(false, true)) {
                    FileUtils.deleteDirectory(dirFile);
                    delete.set(true);
                }
            } catch (Throwable e) {

            }
            if (!success) {
                if (!success) {
                    if (!dirFile.exists()) {
                        boolean fileSuccess = dirFile.mkdirs();
                        if (!fileSuccess) {
                            throw new RuntimeException("output directory for process class can't be created");
                        }
                        success = true;
                    }
                }
            }
            //生成字节码classloader
            log.warn("comile result map is {}", resultMap);
            for (Map.Entry<String, byte[]> entry : resultMap.entrySet()) {
                try {
                    //com/fel/A.class
                    String fileName = entry.getKey();
                    String objectName = fileName.replace("/", ".").replace(".class", "");
                    File classFile = new File(dirFile, fileName);

                    FileUtils.writeByteArrayToFile(new File(dPath + "/" + fileName), entry.getValue());

                    log.warn("write file {} success", fileName);

                    byte[] classData = FileUtils.readFileToByteArray(classFile);

                    //com.greenpineyu.fel.compile.Fel_2466_281_1547109871722_5 -> class
                    clazzs.put(objectName, (Class) FelClassLoader.getInstance().defineClassSelf(objectName, classData, 0,
                            classData.length));
                } catch (Throwable e) {
                    log.error("写编译文件失败:", e);
                }
            }
            log.warn("filename ->class {} ，sizeis {}", clazzs, clazzs.size());
            for (Map.Entry<String, Class> clazz0 : clazzs.entrySet()) {
                //对文件映射遍历，得到表达式和expri的对应关系
                String fileName = clazz0.getKey();
                String expr = fileName2Biz.get(fileName);
                $r.put(expr, (Expression) (clazz0.getValue()).newInstance());
            }
            log.warn("result is {}  size is {}", $r, $r.size());
        }
        return $r;
    }

    static private Map<String, byte[]> compile(MapResourceFinder sourceFinder) {

        // Set up the compiler.
        Compiler compiler = new org.codehaus.janino.Compiler(
                sourceFinder,                                                    // sourceFinder
                new ClassLoaderIClassLoader(JaninoCompolerImpl.class.getClassLoader()) // parentIClassLoader
        );

        // Storage for generated bytecode.
        final Map<String, byte[]> classes = new HashMap<String, byte[]>();
        compiler.setClassFileCreator(new MapResourceCreator(classes));
        compiler.setClassFileFinder(new MapResourceFinder(classes));

        // Compile all sources.
        try {
            compiler.compile(sourceFinder.resources().toArray(new Resource[0]));
        } catch (CompileException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("", e);
        }

        return classes;
    }

}
