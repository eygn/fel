package com.greenpineyu.fel.compile;

import com.greenpineyu.fel.Expression;

import java.util.Map;

public interface FelCompiler {
	
	/**
	 * 
	 * 编译代码，并创建Expression
	 * @param expr
	 * @return
	 */
	public Expression compile(JavaSource src);

	/**
	 * 并行编译
	 * @param src
	 * @return
	 */
	Map<String,Expression> parallelCompile(Map<String,JavaSource> src);

}
