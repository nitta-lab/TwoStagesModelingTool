package models.dataConstraintModel;

import java.util.Collection;
import java.util.HashMap;

import models.algebra.Expression;
import models.algebra.LambdaAbstraction;
import models.algebra.Symbol;
import models.algebra.Term;
import models.algebra.Type;
import models.algebra.Variable;
import parser.Parser;

public class DataConstraintModel {
	protected HashMap<String, ResourcePath> resourcePaths = null;
	protected HashMap<String, Channel> channels = null;
	protected HashMap<String, Channel> ioChannels = null;
	protected HashMap<String, Type> types = null;
	protected HashMap<String, Symbol> symbols = null;
	public static final Type typeInt = new Type("Int", "int");
	public static final Type typeLong = new Type("Long", "long", typeInt);
	public static final Type typeFloat = new Type("Float", "float", typeInt);
	public static final Type typeDouble = new Type("Double", "double", typeFloat);
	public static final Type typeBoolean = new Type("Bool", "boolean");
	public static final Type typeString = new Type("Str", "String");
	public static final Type typeList = new Type("List", "ArrayList", "List");
	public static final Type typeListInt = new Type("List", "ArrayList<>", "List<Integer>", typeList);
	public static final Type typeListStr = new Type("List", "ArrayList<>", "List<String>", typeList);
	public static final Type typeTuple = new Type("Tuple", "AbstractMap.SimpleEntry", "Map.Entry");
	public static final Type typePair = new Type("Pair", "Pair", "Pair");
	public static final Type typePairInt = new Type("Pair", "Pair<Integer>", "Pair<Integer>", typePair);
	public static final Type typePairStr = new Type("Pair", "Pair<String>", "Pair<String>", typePair);
	public static final Type typePairDouble = new Type("Pair", "Pair<Double>", "Pair<Double>", typePair);
	public static final Type typeMap = new Type("Map", "HashMap", "Map");
	public static final Symbol add = new Symbol(Parser.ADD, 2, Symbol.Type.INFIX);
	public static final Symbol mul = new Symbol(Parser.MUL, 2, Symbol.Type.INFIX);;
	public static final Symbol sub = new Symbol(Parser.SUB, 2, Symbol.Type.INFIX);
	public static final Symbol div = new Symbol(Parser.DIV, 2, Symbol.Type.INFIX);
	public static final Symbol minus = new Symbol(Parser.MINUS, 1);
	public static final Symbol cons = new Symbol("cons", 2, Symbol.Type.PREFIX, "($x,$y)->$x.add(0, $y)", Symbol.Type.LAMBDA_WITH_SIDE_EFFECT, new int[] {1, 0});
	public static final Symbol head = new Symbol("head", 1, Symbol.Type.PREFIX, "($x)->$x.get(0)", Symbol.Type.LAMBDA);
	public static final Symbol tail = new Symbol("tail", 1, Symbol.Type.PREFIX, "($x)->$x.subList(1, $x.size())", Symbol.Type.LAMBDA);
	public static final Symbol length = new Symbol("length", 1, Symbol.Type.PREFIX, "($x)->$x.size()", Symbol.Type.LAMBDA);
	public static final Symbol get = new Symbol("get", 2, Symbol.Type.PREFIX, "get", Symbol.Type.METHOD);
	public static final Symbol set = new Symbol("set", 3, Symbol.Type.PREFIX, "set", Symbol.Type.METHOD_WITH_SIDE_EFFECT);
	public static final Symbol contains = new Symbol("contains", 2, Symbol.Type.PREFIX, "contains", Symbol.Type.METHOD);
	public static final Symbol nil = new Symbol("nil", 0, Symbol.Type.PREFIX, new Symbol.IImplGenerator() {
		@Override
		public String generate(Type type, String[] children, String[] childrenSideEffects, String[] sideEffect) {
			String compType = "";
			if (type != null) {
				String interfaceType = type.getInterfaceTypeName();
				if (interfaceType.contains("<")) {
					compType = interfaceType.substring(interfaceType.indexOf("<") + 1, interfaceType.lastIndexOf(">"));
				}
				String implType = type.getImplementationTypeName();
				if (implType.indexOf('<') >= 0) {
					implType = implType.substring(0, implType.indexOf('<'));
				}
				return "new " + implType + "<" + compType + ">()";
			}			
			return "new ArrayList<" + compType + ">()";
		}
	});
	public static final Symbol null_ = new Symbol("null", 0, Symbol.Type.PREFIX, "null", Symbol.Type.PREFIX);
	public static final Symbol cond = new Symbol("if", 3, Symbol.Type.PREFIX, new Symbol.IImplGenerator() {
		final int count[] = {0};
		@Override
		public String generate(Type type, String[] children, String[] childrenSideEffects, String[] sideEffect) {
			String temp = "temp_if" + count[0];
			String impl = ""; 
			
			impl += type.getInterfaceTypeName() + " " + temp + ";\n";
			if (childrenSideEffects[0] != null && childrenSideEffects[0].length() > 0) impl += childrenSideEffects[0];
			impl += "if (" + children[0] + ") {\n";
			if (childrenSideEffects[1] != null && childrenSideEffects[1].length() > 0) impl += "\t" + childrenSideEffects[1];
			impl += "\t" + temp + " = " + children[1] + ";\n";
			impl += "} else {\n";
			if (childrenSideEffects[2] != null && childrenSideEffects[2].length() > 0) impl += "\t" + childrenSideEffects[2];
			impl += "\t" + temp + " = " + children[2] + ";\n";
			impl += "}\n";
			
			sideEffect[0] += impl;
			
			count[0]++;
			return temp;
		}
	});
	
	
	public static final Symbol mod = new Symbol("mod", 2, Symbol.Type.PREFIX, "%", Symbol.Type.INFIX);
	public static final Symbol eq = new Symbol("eq", 2, Symbol.Type.PREFIX, "==", Symbol.Type.INFIX);
	public static final Symbol neq = new Symbol("neq", 2, Symbol.Type.PREFIX, "!=", Symbol.Type.INFIX);
	public static final Symbol gt = new Symbol("gt", 2, Symbol.Type.PREFIX, ">", Symbol.Type.INFIX);
	public static final Symbol lt = new Symbol("lt", 2, Symbol.Type.PREFIX, "<", Symbol.Type.INFIX);
	public static final Symbol ge = new Symbol("ge", 2, Symbol.Type.PREFIX, ">=", Symbol.Type.INFIX);
	public static final Symbol le = new Symbol("le", 2, Symbol.Type.PREFIX, "<=", Symbol.Type.INFIX);
	public static final Symbol and = new Symbol("and", 2, Symbol.Type.PREFIX, "&&", Symbol.Type.INFIX);
	public static final Symbol or = new Symbol("or", 2, Symbol.Type.PREFIX, "||", Symbol.Type.INFIX);
	public static final Symbol neg = new Symbol("neg", 1, Symbol.Type.PREFIX, "!", Symbol.Type.PREFIX);
	public static final Symbol true_ = new Symbol("true", 0, Symbol.Type.PREFIX, "true", Symbol.Type.PREFIX);
	public static final Symbol false_ = new Symbol("false", 0, Symbol.Type.PREFIX, "false", Symbol.Type.PREFIX);
	public static final Symbol pair = new Symbol("pair", -1, Symbol.Type.PREFIX, new Symbol.IImplGenerator() {
		@Override
		public String generate(Type type, String[] childrenImpl, String[] childrenSideEffects, String[] sideEffect) {
			for (String s: childrenSideEffects) {
				sideEffect[0] += s;
			}
			String impl = "new Pair<>(" + childrenImpl[0] + "," + childrenImpl[1] + ")";
			return impl;
		}
	});
	public static final Symbol tuple = new Symbol("tuple", -1, Symbol.Type.PREFIX, new Symbol.IImplGenerator() {
		@Override
		public String generate(Type type, String[] childrenImpl, String[] childrenSideEffects, String[] sideEffect) {
			for (String s: childrenSideEffects) {
				sideEffect[0] += s;
			}
			String impl = "new AbstractMap.SimpleEntry<>(" + childrenImpl[0] + "$x)";
			for (int i = 1; i < childrenImpl.length - 1; i++) {
				impl = impl.replace("$x", ", new AbstractMap.SimpleEntry<>(" + childrenImpl[i] + "$x)");
			}
			impl = impl.replace("$x", ", " + childrenImpl[childrenImpl.length - 1]);
			return impl;
		}
	});
	public static final Symbol fst = new Symbol("fst", 1, Symbol.Type.PREFIX, "getKey", Symbol.Type.METHOD);
	public static final Symbol snd = new Symbol("snd", 1, Symbol.Type.PREFIX, "getValue", Symbol.Type.METHOD);
	public static final Symbol left = new Symbol("left", 1, Symbol.Type.PREFIX, "getLeft", Symbol.Type.METHOD);
	public static final Symbol right = new Symbol("right", 1, Symbol.Type.PREFIX, "getRight", Symbol.Type.METHOD);
	public static final Symbol insert = new Symbol("insert", 3, Symbol.Type.PREFIX, "put", Symbol.Type.METHOD_WITH_SIDE_EFFECT);
//	public static final Symbol lookup = new Symbol("lookup", 2, Symbol.Type.PREFIX, "get", Symbol.Type.METHOD);
	public static final Symbol lookup = new Symbol("lookup", 2, Symbol.Type.PREFIX, new Symbol.IImplGenerator() {
		final int count[] = {0};
		@Override
		public String generate(Type type, String[] childrenImpl, String[] childrenSideEffects, String[] sideEffect) {
			String temp = "temp_get" + count[0];
			String impl = childrenSideEffects[0] + childrenSideEffects[1];
			impl += type.getInterfaceTypeName() + " " + temp + ";\n";
			impl += "if (" + childrenImpl[0] + ".get(" + childrenImpl[1] + ") != null) {\n";
			impl += "\t" + temp + " = " + childrenImpl[0] + ".get(" + childrenImpl[1] + ");\n";
			impl += "} else {\n";
			impl += "\t" + temp + " = " + getDefaultValue(type) + ";\n";
			impl += "}\n";
			sideEffect[0] = impl;
			count[0]++;
			return temp;
		}
	});
	public static final Symbol pi = new Symbol("PI", 0, Symbol.Type.PREFIX, "Math.PI", Symbol.Type.PREFIX);
	public static final Symbol E = new Symbol("E", 0, Symbol.Type.PREFIX, "Math.E", Symbol.Type.PREFIX);
	public static final Symbol sqrt = new Symbol("sqrt", 1, Symbol.Type.PREFIX, "Math.sqrt", Symbol.Type.PREFIX);
	public static final Symbol sin = new Symbol("sin", 1, Symbol.Type.PREFIX, "Math.sin", Symbol.Type.PREFIX);
	public static final Symbol cos = new Symbol("cos", 1, Symbol.Type.PREFIX, "Math.cos", Symbol.Type.PREFIX);
	public static final Symbol tan = new Symbol("tan", 1, Symbol.Type.PREFIX, "Math.tan", Symbol.Type.PREFIX);
	public static final Symbol asin = new Symbol("asin", 1, Symbol.Type.PREFIX, "Math.asin", Symbol.Type.PREFIX);
	public static final Symbol acos = new Symbol("acos", 1, Symbol.Type.PREFIX, "Math.acos", Symbol.Type.PREFIX);
	public static final Symbol atan = new Symbol("atan", 1, Symbol.Type.PREFIX, "Math.atan", Symbol.Type.PREFIX);
	public static final Symbol pow = new Symbol("pow", 2, Symbol.Type.PREFIX, "Math.pow", Symbol.Type.PREFIX);
	public static final Symbol exp = new Symbol("exp", 1, Symbol.Type.PREFIX, "Math.exp", Symbol.Type.PREFIX);
	public static final Symbol log = new Symbol("log", 1, Symbol.Type.PREFIX, "Math.log", Symbol.Type.PREFIX);
	public static final Symbol abs = new Symbol("abs", 1, Symbol.Type.PREFIX, "Math.abs", Symbol.Type.PREFIX);
		
	static {
		add.setInverses(new Symbol[] {sub, sub});
		mul.setInverses(new Symbol[] {div, div});
		sub.setInverses(new Symbol[] {add});
		div.setInverses(new Symbol[] {mul});
		minus.setInverses(new Symbol[] {minus});
		mod.setSignature(new Type[] {typeInt, null, null});
		cons.setInverses(new Symbol[] {head, tail});
		cons.setSignature(new Type[] {typeList, null, typeList});
		contains.setSignature(new Type[] {typeBoolean, typeList, null});
		length.setSignature(new Type[] {typeInt, null});
		get.setSignature(new Type[] {null, typeList, typeInt});
		set.setSignature(new Type[] {typeList, typeList, typeInt, null});
		eq.setSignature(new Type[] {typeBoolean, null, null});
		neq.setSignature(new Type[] {typeBoolean, null, null});
		gt.setSignature(new Type[] {typeBoolean, null, null});
		lt.setSignature(new Type[] {typeBoolean, null, null});
		ge.setSignature(new Type[] {typeBoolean, null, null});
		le.setSignature(new Type[] {typeBoolean, null, null});
		and.setSignature(new Type[] {typeBoolean, typeBoolean, typeBoolean});
		or.setSignature(new Type[] {typeBoolean, typeBoolean, typeBoolean});
		neg.setSignature(new Type[] {typeBoolean, typeBoolean});
		true_.setSignature(new Type[] {typeBoolean});
		false_.setSignature(new Type[] {typeBoolean});
		null_.setSignature(new Type[] {null});
		pair.setSignature(new Type[] {typePair,null,null});
		pair.setInverses(new Symbol[] {left, right});
		left.setSignature(new Type[] {null, typePair});
		right.setSignature(new Type[] {null, typePair});
		tuple.setSignature(new Type[] {typeTuple, null, null});
		tuple.setInverses(new Symbol[] {fst, snd});
		fst.setSignature(new Type[] {null, typeTuple});
		fst.setInverses(new Symbol[] {new LambdaAbstraction(new Variable("x"), new Term(tuple, new Expression[] {new Variable("x"), new Variable("y")}))});
		snd.setSignature(new Type[] {null, typeTuple});
		snd.setInverses(new Symbol[] {new LambdaAbstraction(new Variable("y"), new Term(tuple, new Expression[] {new Variable("x"), new Variable("y")}))});
		insert.setSignature(new Type[] {typeMap, typeMap, null, null});
		lookup.setSignature(new Type[] {null, typeMap, null});
		pi.setSignature(new Type[] {typeDouble});
		E.setSignature(new Type[] {typeDouble});
		sqrt.setSignature(new Type[] {typeDouble, typeDouble});
		sin.setSignature(new Type[] {typeDouble, typeDouble});
		cos.setSignature(new Type[] {typeDouble, typeDouble});
		tan.setSignature(new Type[] {typeDouble, typeDouble});
		asin.setSignature(new Type[] {typeDouble, typeDouble});
		asin.setInverses(new Symbol[] {sin});
		acos.setSignature(new Type[] {typeDouble, typeDouble});
		acos.setInverses(new Symbol[] {cos});
		atan.setSignature(new Type[] {typeDouble, typeDouble});
		atan.setInverses(new Symbol[] {tan});
		pow.setSignature(new Type[] {typeDouble, typeDouble, typeDouble});
		exp.setSignature(new Type[] {typeDouble, typeDouble});
		exp.setInverses(new Symbol[] {log});
		log.setSignature(new Type[] {typeDouble, typeDouble});
		log.setInverses(new Symbol[] {exp});
		abs.setSignature(new Type[] {typeDouble, typeDouble});
	}
	
	public DataConstraintModel() {
		resourcePaths = new HashMap<>();
		channels = new HashMap<>();
		ioChannels = new HashMap<>();
		types = new HashMap<>();
		addType(typeInt);
		addType(typeLong);
		addType(typeFloat);
		addType(typeDouble);
		addType(typeBoolean);
		addType(typeString);
		addType(typeList);
		addType(typePair);
		addType(typeTuple);
		addType(typeMap);
		symbols = new HashMap<>();
		addSymbol(add);
		addSymbol(mul);
		addSymbol(sub);
		addSymbol(div);
		addSymbol(minus);
		addSymbol(mod);
		addSymbol(cons);
		addSymbol(head);
		addSymbol(tail);
		addSymbol(length);
		addSymbol(contains);
		addSymbol(get);
		addSymbol(set);
		addSymbol(nil);
		addSymbol(cond);
		addSymbol(eq);
		addSymbol(neq);
		addSymbol(gt);
		addSymbol(lt);
		addSymbol(ge);
		addSymbol(le);
		addSymbol(and);
		addSymbol(or);
		addSymbol(neg);
		addSymbol(true_);
		addSymbol(false_);
		addSymbol(null_);
		addSymbol(pair);
		addSymbol(left);
		addSymbol(right);
		addSymbol(tuple);
		addSymbol(fst);
		addSymbol(snd);
		addSymbol(insert);
		addSymbol(lookup);
		addSymbol(pi);
		addSymbol(E);
		addSymbol(sqrt);
		addSymbol(sin);
		addSymbol(cos);
		addSymbol(tan);
		addSymbol(asin);
		addSymbol(acos);
		addSymbol(atan);
		addSymbol(pow);
		addSymbol(exp);
		addSymbol(log);
		addSymbol(abs);
	}
	
	public Collection<ResourcePath> getResourcePaths() {
		return resourcePaths.values();
	}
	
	public ResourcePath getResourcePath(String resourceName) {
		return resourcePaths.get(resourceName);
	}
	
	public void addResourcePath(ResourcePath resourcePath) {
		resourcePaths.put(resourcePath.getResourceName(), resourcePath);
	}
	
	public void setResourcePaths(HashMap<String, ResourcePath> resourcePaths) {
		this.resourcePaths = resourcePaths;
	}
	
	public void removeResourcePath(String resourceName) {
		ResourcePath id = resourcePaths.get(resourceName);
		resourcePaths.remove(resourceName);
		for (Channel ch: channels.values()) {
			ch.removeChannelMember(id);
		}
		for (Channel ch: ioChannels.values()) {
			ch.removeChannelMember(id);
		}
	}

	public Collection<Channel> getChannels() {
		return channels.values();
	}
		
	public Channel getChannel(String channelName) {
		return channels.get(channelName);
	}
	
	public void setChannels(HashMap<String, Channel> channels) {
		this.channels = channels;
		for (Channel g: channels.values()) {
			for (ResourcePath id: g.getResources()) {
				resourcePaths.put(id.getResourceName(), id);				
			}
		}
	}
	
	public void addChannel(Channel channel) {
		channels.put(channel.getChannelName(), channel);
		for (ResourcePath id: channel.getResources()) {
			resourcePaths.put(id.getResourceName(), id);				
		}
	}
	
	public void removeChannel(String channelName) {
		channels.remove(channelName);
	}
	
	public Collection<Channel> getIOChannels() {
		return ioChannels.values();
	}
	
	public Channel getIOChannel(String channelName) {
		return ioChannels.get(channelName);
	}
	
	public void setIOChannels(HashMap<String, Channel> ioChannels) {
		this.ioChannels = ioChannels;
		for (Channel g: ioChannels.values()) {
			for (ResourcePath id: g.getResources()) {
				resourcePaths.put(id.getResourceName(), id);				
			}
		}
	}
	
	public void addIOChannel(Channel ioChannel) {
		ioChannels.put(ioChannel.getChannelName(), ioChannel);
		for (ResourcePath id: ioChannel.getResources()) {
			resourcePaths.put(id.getResourceName(), id);				
		}
	}
	
	public void removeIOChannel(String ioChannelName) {
		ioChannels.remove(ioChannelName);
	}
	
	public void addType(Type type) {
		types.put(type.getTypeName(), type);
	}
	
	public Type getType(String name) {
		return types.get(name);
	}
	
	public void addSymbol(Symbol symbol) {
		symbols.put(symbol.getName(), symbol);
	}
	
	public Symbol getSymbol(String name) {
		return symbols.get(name);
	}
	
	public static String getWrapperType(Type type) {
		if (type == typeInt) {
			return "Integer";
		} else if (type == typeLong) {
			return "Long";
		} else if (type == typeFloat) {
			return "Float";
		} else if (type == typeDouble) {
			return "Double";
		} else if (type == typeBoolean) {
			return "Boolean";
		}
		return null;
	}
	
	public boolean isPrimitiveType(Type type) {
		if (type == typeInt 
			|| type == typeLong
			|| type == typeFloat
			|| type == typeDouble
			|| type == typeBoolean) {
			return true;
		}
		return false;
	}

	public static boolean isListType(Type type) {
		return typeList.isAncestorOf(type);
	}

	public static String getDefaultValue(Type type) {
		if (type == typeInt) {
			return "0";
		} else if (type == typeLong) {
			return "0L";
		} else if (type == typeFloat) {
			return "0.0f";
		} else if (type == typeDouble) {
			return "0.0";
		} else if (type == typeBoolean) {
			return "false";
		} else if (type == typeString) {
			return "\"\"";
		}
		return "new " + type.getImplementationTypeName() + "()";
	}
	
	@Override
	public String toString() {
		String out = "";
		for (Channel channel: ioChannels.values()) {
			out += channel.toString();
		}
		for (Channel channel: channels.values()) {
			out += channel.toString();
		}
		return out;
	}
	
	public String getSourceText() {
		String out = "";
		String init = "";
		for (ResourcePath resource: resourcePaths.values()) {
			String initializer = resource.getInitText();
			if (initializer != null) {
				init += initializer;
			}
		}
		if (init.length() > 0) {
			out += "init {\n" + init + "}\n";
		}
		for (Channel channel: ioChannels.values()) {
			out += channel.getSourceText();
		}
		for (Channel channel: channels.values()) {
			out += channel.getSourceText();
		}
		return out;
	}
}
