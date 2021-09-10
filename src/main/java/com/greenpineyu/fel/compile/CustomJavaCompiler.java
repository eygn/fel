package com.greenpineyu.fel.compile;

import java.io.File;

/**
 * @author byg
 * @date 2021-09-09 16:52:54
 */
public interface CustomJavaCompiler {
    /**
     * 编译java,生成class文件
     *
     * @param javaSourceFile  读取java源文件，扩展名不一定是.java
     * @param encode          文件编码
     * @param targetClassName 生成的class完整名称，如java.lang.String．
     * @param outputPath      编译出来的class文件存放目录
     * @return 编译是否成功
     */
    byte[] compile(File javaSourceFile, String encode, String targetClassName, File outputPath) throws Exception;


}