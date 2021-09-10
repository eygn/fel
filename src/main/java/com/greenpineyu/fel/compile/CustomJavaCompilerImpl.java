package com.greenpineyu.fel.compile;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.*;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author byg
 * @date 2021-09-09 17:38:38
 */
public class CustomJavaCompilerImpl implements CustomJavaCompiler {
    private static final Logger log = LoggerFactory.getLogger(CustomJavaCompilerImpl.class);

    /**
     * java源码版本
     */
    public static final String DEFUALT_JAVA_SOURCE_VERSION = System.getProperty("java.specification.version");

    /**
     * 目标class运行的版本
     */
    public static final String DEFAULT_JAVA_TARGET_VERSION = System.getProperty("java.specification.version");

    /**
     * java源码版本
     */
    private String javaSourceVersion = DEFUALT_JAVA_SOURCE_VERSION;

    /**
     * 目标class运行的版本
     */
    private String javaTargetVersion = DEFAULT_JAVA_TARGET_VERSION;

    /**
     * 允许的java规范版本
     */
    private Set<String> allowedSpecVersion = new HashSet<>(Arrays.asList(
            "1.5",
            "1.6",
            "1.7",
            "1.8"
    ));

    @Override
    public byte[] compile(final File javaSourceFile, final String encode, final String targetClassName,
                          final File outputPath) throws Exception {
        final ClassLoader classLoader = this.getClass().getClassLoader();
        String[] fileNames = new String[]{javaSourceFile.getAbsolutePath()}; // 要编译的源文件
        String[] classNames = new String[]{targetClassName}; // 要编译的Class全名称
        final List<IProblem> problemList = new ArrayList<IProblem>(); // 搜集问题

        final INameEnvironment env = new INameEnvironment() {

            @Override
            public NameEnvironmentAnswer findType(char[][] compoundTypeName) {
                String result = "";
                String sep = "";
                for (int i = 0; i < compoundTypeName.length; i++) {
                    result += sep;
                    result += new String(compoundTypeName[i]);
                    sep = ".";
                }
                return findType(result);
            }

            @Override
            public NameEnvironmentAnswer findType(char[] typeName, char[][] packageName) {
                String result = "";
                String sep = "";
                for (int i = 0; i < packageName.length; i++) {
                    result += sep;
                    result += new String(packageName[i]);
                    sep = ".";
                }
                result += sep;
                result += new String(typeName);
                return findType(result);
            }

            private NameEnvironmentAnswer findType(String className) {

                InputStream is = null;
                try {
                    if (className.equals(targetClassName)) {
                        ICompilationUnit compilationUnit = new CompilationUnit(javaSourceFile.getAbsolutePath(),
                                className, encode);
                        return new NameEnvironmentAnswer(compilationUnit, null);
                    }
                    String resourceName = className.replace('.', '/') + ".class";
                    is = classLoader.getResourceAsStream(resourceName);
                    if (is != null) {
                        byte[] classBytes;
                        byte[] buf = new byte[8192];
                        ByteArrayOutputStream baos = new ByteArrayOutputStream(buf.length);
                        int count;
                        while ((count = is.read(buf, 0, buf.length)) > 0) {
                            baos.write(buf, 0, count);
                        }
                        baos.flush();
                        classBytes = baos.toByteArray();
                        char[] fileName = className.toCharArray();
                        ClassFileReader classFileReader = new ClassFileReader(classBytes, fileName, true);
                        return new NameEnvironmentAnswer(classFileReader, null);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException exc) {
                            // Ignore
                        }
                    }
                }
                return null;
            }

            private boolean isPackage(String result) {
                if (result.equals(targetClassName)) {
                    return false;
                }
                String resourceName = result.replace('.', '/') + ".class";
                InputStream is = classLoader.getResourceAsStream(resourceName);
                return is == null;
            }

            @Override
            public boolean isPackage(char[][] parentPackageName, char[] packageName) {
                String result = "";
                String sep = "";
                if (parentPackageName != null) {
                    for (int i = 0; i < parentPackageName.length; i++) {
                        result += sep;
                        String str = new String(parentPackageName[i]);
                        result += str;
                        sep = ".";
                    }
                }
                String str = new String(packageName);
                if (Character.isUpperCase(str.charAt(0))) {
                    if (!isPackage(result)) {
                        return false;
                    }
                }
                result += sep;
                result += str;
                return isPackage(result);
            }

            @Override
            public void cleanup() {
            }

        };

        final IErrorHandlingPolicy policy = DefaultErrorHandlingPolicies.proceedWithAllProblems();

        final Map<String, String> settings = new HashMap<>();
        settings.put(CompilerOptions.OPTION_LineNumberAttribute, CompilerOptions.GENERATE);
        settings.put(CompilerOptions.OPTION_SourceFileAttribute, CompilerOptions.GENERATE);
        settings.put(CompilerOptions.OPTION_ReportDeprecation, CompilerOptions.IGNORE);
        settings.put(CompilerOptions.OPTION_LocalVariableAttribute, CompilerOptions.GENERATE);

        // 源码
        if (allowedSpecVersion.contains(javaSourceVersion)) {
            settings.put(CompilerOptions.OPTION_Source, javaSourceVersion);
        } else {
            settings.put(CompilerOptions.OPTION_Source, DEFUALT_JAVA_SOURCE_VERSION);
        }

        // Target JVM
        if (allowedSpecVersion.contains(javaTargetVersion)) {
            settings.put(CompilerOptions.OPTION_TargetPlatform, javaTargetVersion);
        } else {
            settings.put(CompilerOptions.OPTION_TargetPlatform, DEFAULT_JAVA_TARGET_VERSION);
        }

        final IProblemFactory problemFactory = new DefaultProblemFactory(Locale.getDefault());
        AtomicReference<byte[]> $result = new AtomicReference<>();
        final ICompilerRequestor requestor = result -> {
            try {
                if (result.hasProblems()) {
                    for (IProblem problem : result.getProblems()) {
                        if (problem.isError()) {
                            try {
                                problemList.add(problem);
                            } catch (Exception e) {
                                log.error(e.getMessage(), e);
                            }
                        }
                    }
                }
                if (problemList.isEmpty()) {
                    ClassFile[] classFiles = result.getClassFiles();
                    for (int i = 0; i < classFiles.length; i++) {
                        ClassFile classFile = classFiles[i];
                        char[][] compoundName = classFile.getCompoundName();
                        String className = "";
                        String sep = "";
                        for (int j = 0; j < compoundName.length; j++) {
                            className += sep;
                            className += new String(compoundName[j]);
                            sep = ".";
                        }
                        byte[] byte2 = classFile.getBytes();
                        $result.set(byte2);
                        String finalClassName = className;
                        //zhaodong.xzd 异步写文件
//                        executorService.execute(()->{
                        try {
                            String outFile = outputPath + "/" + finalClassName.replace('.', '/') + ".class";

                            File packagePath = new File(outFile.substring(0, outFile.lastIndexOf("/")));
                            if (!packagePath.exists()) {
                                packagePath.mkdirs();
                            }
                            FileOutputStream fout = new FileOutputStream(outFile);
                            BufferedOutputStream bos = new BufferedOutputStream(fout);
                            bos.write(byte2);
                            bos.close();
                        } catch (Throwable e) {
                            log.error("写编译文件失败:", e);
                        }

//                        });

                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        };

        ICompilationUnit[] compilationUnits = new ICompilationUnit[classNames.length];
        for (int i = 0; i < compilationUnits.length; i++) {
            String className = classNames[i];
            compilationUnits[i] = new CompilationUnit(fileNames[i], className, encode);
        }
        CompilerOptions cOptions = new CompilerOptions(settings);
        cOptions.parseLiteralExpressionsAsConstants = true;

        org.eclipse.jdt.internal.compiler.Compiler compiler = new org.eclipse.jdt.internal.compiler.Compiler(env,
                policy, cOptions, requestor, problemFactory);

        compiler.compile(compilationUnits);

        if (!problemList.isEmpty()) {
            throw new Exception(getErrorMsg(javaSourceFile, targetClassName, problemList));
        }
        return $result.get();
    }

    private ExecutorService executorService = Executors.newFixedThreadPool(8);

    private String getErrorMsg(File javaFile, String className, Collection<IProblem> errors) {
        StringBuilder sb = new StringBuilder();

        sb.append("complie file[").append(javaFile.getAbsoluteFile()).append("] to class[").append(className)
                .append("] failure,");

        for (IProblem problem : errors) {
            sb.append(problem).append(System.getProperty("line.separator"));
        }

        return sb.toString();
    }

    static class CompilationUnit implements ICompilationUnit {
        private String className;
        private String sourceFile;
        private String encode;

        CompilationUnit(String sourceFile, String className, String encode) {
            this.className = className;
            this.sourceFile = sourceFile;
            this.encode = encode;
        }

        @Override
        public char[] getFileName() {
            return sourceFile.toCharArray();
        }

        @Override
        public char[] getContents() {
            char[] result = null;
            FileInputStream is = null;
            try {
                is = new FileInputStream(sourceFile);
                Reader reader = new BufferedReader(new InputStreamReader(is, encode));
                char[] chars = new char[8192];
                StringBuilder buf = new StringBuilder();
                int count;
                while ((count = reader.read(chars, 0, chars.length)) > 0) {
                    buf.append(chars, 0, count);
                }
                result = new char[buf.length()];
                buf.getChars(0, result.length, result, 0);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException exc) {
                        // Ignore
                    }
                }
            }
            return result;
        }

        @Override
        public char[] getMainTypeName() {
            int dot = className.lastIndexOf('.');
            if (dot > 0) {
                return className.substring(dot + 1).toCharArray();
            }
            return className.toCharArray();
        }

        @Override
        public char[][] getPackageName() {
            StringTokenizer izer = new StringTokenizer(className, ".");
            char[][] result = new char[izer.countTokens() - 1][];
            for (int i = 0; i < result.length; i++) {
                String tok = izer.nextToken();
                result[i] = tok.toCharArray();
            }
            return result;
        }

        @Override
        public boolean ignoreOptionalProblems() {
            return false;
        }
    }
}
