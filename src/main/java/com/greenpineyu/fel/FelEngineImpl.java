package com.greenpineyu.fel;

import com.greenpineyu.fel.common.FelBuilder;
import com.greenpineyu.fel.compile.CompileService;
import com.greenpineyu.fel.compile.FelClassLoader;
import com.greenpineyu.fel.context.ArrayCtxImpl;
import com.greenpineyu.fel.context.FelContext;
import com.greenpineyu.fel.context.Var;
import com.greenpineyu.fel.function.FunMgr;
import com.greenpineyu.fel.function.Function;
import com.greenpineyu.fel.optimizer.Optimizer;
import com.greenpineyu.fel.optimizer.VarVisitOpti;
import com.greenpineyu.fel.parser.AntlrParser;
import com.greenpineyu.fel.parser.FelNode;
import com.greenpineyu.fel.parser.Parser;
import com.greenpineyu.fel.security.SecurityMgr;
import com.greenpineyu.fel.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 执行引擎
 * 
 * @author yqs
 * 
 */
public class FelEngineImpl implements FelEngine {
	Logger logger= LoggerFactory.getLogger(FelEngineImpl.class);

	private FelContext context;

	private CompileService compiler;

	private Parser parser;
	
	private FunMgr funMgr;
	
	private SecurityMgr securityMgr;

	public SecurityMgr getSecurityMgr() {
		return securityMgr;
	}

	public void setSecurityMgr(SecurityMgr securityMgr) {
		this.securityMgr = securityMgr;
	}

	public FelEngineImpl(FelContext context) {
		this.context = context;
		compiler = new CompileService();
		parser = new AntlrParser(this);
		this.funMgr=new FunMgr();
	}

	{
		this.securityMgr = FelBuilder.newSecurityMgr();
	}

	public FelEngineImpl() {
		this(new ArrayCtxImpl());
		// this(new MapContext());
	}

	public void setFelclassesPath(String felclassesPath) {
		FelClassLoader.felclassesPath = felclassesPath;
	}

	@Override
	public FelNode parse(String exp) {
		return parser.parse(exp);
	}

	@Override
	public Object eval(String exp) {
		return this.eval(exp, this.context);
	}

	public Object eval(String exp, Var... vars) {
		FelNode node = parse(exp);
		Optimizer opt = new VarVisitOpti(vars);
		node = opt.call(context, node);
		return node.eval(context);
	}

	@Override
	public Object eval(String exp, FelContext ctx) {
		return parse(exp).eval(ctx);
	}

	public Expression compile(String exp, Var... vars) {
		return compile(exp, null, new VarVisitOpti(vars));
	}

	@Override
	public Expression compile(String exp, FelContext ctx, Optimizer... opts) {
		if (ctx == null) {
			ctx = this.context;
		}
		FelNode node = parse(exp);
		if (opts != null) {
			for (Optimizer opt : opts) {
				if (opt != null) {
					node = opt.call(ctx, node);
				}
			}
		}
		return compiler.compile(ctx, node, exp);
	}

	@Override
	public Map<String, Expression> parallelCompile(Map<String, Pair<String, FelContext>> nodeOrigin, Optimizer... opts) {
		Map<String,FelContext > fel=new HashMap<>();//context
		Map<String,FelNode> felnode=new HashMap<>();//node
		Map<String,String> origin=new HashMap<>();//biz
		for (Map.Entry<String,Pair<String,FelContext>> entry:nodeOrigin.entrySet()){
			if (entry.getValue().getRight() == null) {
				entry.getValue().setRight(this.context);
			}
			FelNode node = parse(entry.getValue().getLeft());
			if (opts != null) {
				for (Optimizer opt : opts) {
					if (opt != null) {
						node = opt.call(entry.getValue().getRight(), node);
					}
				}
			}
			felnode.put(entry.getKey(),node);
			origin.put(entry.getKey(),entry.getValue().getLeft());
			fel.put(entry.getKey(),entry.getValue().getRight());
			logger.warn("[FEL] fel put {} to compile,value is {}",entry.getKey(),entry.getValue());
		}
		Map<String, Expression> result = compiler.paralleCompiler(fel, felnode, origin);
		return result;
	}

	@Override
	public String toString() {
		return "FelEngine";
	}

	@Override
	public void addFun(Function fun) {
		this.funMgr.add(fun);
	}
	
	@Override
	public FelContext getContext() {
		return this.context;
	}
	
	@Override
	public CompileService getCompiler() {
		return compiler;
	}


	@Override
	public void setCompiler(CompileService compiler) {
		this.compiler = compiler;
	}


	@Override
	public Parser getParser() {
		return parser;
	}


	@Override
	public void setParser(Parser parser) {
		this.parser = parser;
	}


	@Override
	public FunMgr getFunMgr() {
		return funMgr;
	}


	@Override
	public void setFunMgr(FunMgr funMgr) {
		this.funMgr = funMgr;
	}


	@Override
	public void setContext(FelContext context) {
		this.context = context;
	}

}
