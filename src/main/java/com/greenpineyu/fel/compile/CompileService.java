package com.greenpineyu.fel.compile;

import com.greenpineyu.fel.Expression;
import com.greenpineyu.fel.context.FelContext;
import com.greenpineyu.fel.parser.FelNode;
import com.greenpineyu.fel.util.FelSwitcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompileService {

    private static final Logger log = LoggerFactory.getLogger(CompileService.class);

    private SourceGenerator srcGen;

    private FelCompiler complier;

    public SourceGenerator getSrcGen() {
        return srcGen;
    }

    public void setSrcGen(SourceGenerator srcGen) {
        this.srcGen = srcGen;
    }

    public FelCompiler getComplier() {
        return complier;
    }

    public void setComplier(FelCompiler complier) {
        this.complier = complier;
    }

    {
        srcGen = new SourceGeneratorImpl();
        String name = getCompilerClassName();
        FelCompiler comp = newCompiler(name);
        complier = comp;
    }

    public static List<String> getClassPath(ClassLoader cl) {
        List<String> paths = new ArrayList<>();
        while (cl != null) {
            boolean isUrlClassloader = cl instanceof URLClassLoader;
            if (isUrlClassloader) {
                URLClassLoader urlClassLoader = (URLClassLoader) cl;
                for (URL url : urlClassLoader.getURLs()) {
                    paths.add(url.getFile());
                }
            } else {
                Enumeration<URL> resources = null;
                try {
                    resources = cl.getResources("/");
                } catch (IOException e) {
                    if (FelSwitcher.errorLog) {
                        log.error("getClassPath error, ClassLoader:" + cl.getClass().getSimpleName(), e);
                    }
                }
                if (resources != null) {
                    while (resources.hasMoreElements()) {
                        URL resource = resources.nextElement();
                        paths.add(resource.getFile());
                    }
                }
            }
            cl = cl.getParent();
        }
        return paths;
    }

    private FelCompiler newCompiler(String name) {
        FelCompiler comp = null;
        try {
            @SuppressWarnings("unchecked")
            Class<FelCompiler> cls = (Class<FelCompiler>) Class.forName(name);
            comp = cls.newInstance();
        } catch (ClassNotFoundException e) {
            if (FelSwitcher.errorLog) {
                log.error("newCompiler structure error, name:" + name, e);
            }
        } catch (InstantiationException e) {
            if (FelSwitcher.errorLog) {
                log.error("newCompiler structure error, name:" + name, e);
            }
        } catch (IllegalAccessException e) {
            if (FelSwitcher.errorLog) {
                log.error("newCompiler structure error, name:" + name, e);
            }
        } finally {
        }
        return comp;
    }

    private String getCompilerClassName() {
        String version = System.getProperty("java.version");
        String compileClassName = FelCompiler.class.getName();
        if (version != null && version.startsWith("1.5")) {
            compileClassName += "15";
        } else {
            compileClassName += "16";
        }
        return compileClassName;
    }

    /**
     * 并发编译
     *
     * @param fel
     * @param felnode
     * @param origin
     * @return
     */
    public Map<String, Expression> paralleCompiler(Map<String, FelContext> fel, Map<String, FelNode> felnode, Map<String, String> origin) {
        Map<String, JavaSource> input = new ConcurrentHashMap<>(fel.size());

        Map<String, Expression> $res = new ConcurrentHashMap<>(fel.size());
        Map<String, String> fileName2Expr = new ConcurrentHashMap<>(fel.size());
        for (Map.Entry<String, FelContext> entry : fel.entrySet()) {
            //fel biz->fel
            //felnode biz-node  entry.getkey->bizcode
            //expr biz-exp
            JavaSource src = srcGen.getSource(entry.getValue(), felnode.get(entry.getKey()));
            if (src instanceof ConstExpSrc) {
                ConstExpSrc s = (ConstExpSrc) src;
                $res.put(origin.get(entry.getKey()), s.getValue());
            }
            src.setSource("// expr powered by zhaodong.xzd:" + origin.get(entry.getKey()) + "\n" + src.getSource());
            //文件名-->expr
            fileName2Expr.put(src.getName(), origin.get(entry.getKey()));
            //expr->source
            input.put(origin.get(entry.getKey()), src);
        }
        log.warn("[FEL]begin to parallel compiler FEL size is {}", fel.size());
        log.warn("[FEL]begin to parallel compiler FEL key is ", input.entrySet());
        long l = System.currentTimeMillis();
        try {
            $res = JaninoCompolerImpl.mutilCompiler(input, fileName2Expr);
        } catch (IllegalAccessException e) {
            log.error("[fel]IllegalAccessException" , e);
        } catch (InstantiationException e) {
            log.error("[FEL]InstantiationException" , e);
        } catch (Exception e) {
            log.error("[FEL]Exception" , e);
        }
        log.warn("[FEL]end to parallel compiler cost time is {}, size is {}", (System.currentTimeMillis() - l),$res.size());
        return $res;

    }


    public Expression compile(FelContext ctx, FelNode node, String originalExp) {
        try {
            JavaSource src = srcGen.getSource(ctx, node);
            if (src instanceof ConstExpSrc) {
                ConstExpSrc s = (ConstExpSrc) src;
                return s.getValue();
            }
            src.setSource("// expr:" + originalExp + "\n" + src.getSource());

            return complier.compile(src);
        } catch (Exception e) {
            if (FelSwitcher.errorLog) {
                log.error("compile error", e);
            }
        }
        return null;
    }

}
