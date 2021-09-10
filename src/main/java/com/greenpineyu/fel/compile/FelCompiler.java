package com.greenpineyu.fel.compile;

import com.greenpineyu.fel.Expression;

public interface FelCompiler {
	
	/**
	 * 
	 * 编译代码，并创建Expression
	 * @param src
	 * @return
	 */
	public Expression compile(JavaSource src);

}
