package com.greenpineyu.fel.function.operator;

import com.greenpineyu.fel.context.FelContext;
import com.greenpineyu.fel.parser.FelNode;

import java.util.List;

public class Or extends And {
	
	/** 
	 * 求逻辑或(||)
	 * @see com.greenpineyu.fel.function.operator.And#logic(FelContext, List)
	 */
	Boolean logic(FelContext context, List<FelNode> children) {
		Boolean leftValue = toBoolean(context, children.get(0));
		if (leftValue.booleanValue()) {
			return leftValue;
		}
		return toBoolean(context, children.get(1));
	}
	
	@Override
	public String getName() {
		return "||";
	}

}
