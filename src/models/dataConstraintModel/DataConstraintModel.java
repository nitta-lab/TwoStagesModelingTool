package models.dataConstraintModel;

import java.util.Collection;
import java.util.HashMap;

import models.algebra.Symbol;
import models.algebra.Type;
import parser.Parser;

public class DataConstraintModel {
	protected HashMap<String, IdentifierTemplate> identifierTemplates = null;
	protected HashMap<String, ChannelGenerator> channelGenerators = null;
	protected HashMap<String, ChannelGenerator> ioChannelGenerators = null;
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
		snd.setSignature(new Type[] {null, typeTuple});
		insert.setSignature(new Type[] {typeMap, typeMap, null, null});
		lookup.setSignature(new Type[] {null, typeMap, null});
	}
	
	public DataConstraintModel() {
		identifierTemplates = new HashMap<>();
		channelGenerators = new HashMap<>();
		ioChannelGenerators = new HashMap<>();
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
	}
	
	public Collection<IdentifierTemplate> getIdentifierTemplates() {
		return identifierTemplates.values();
	}
	
	public IdentifierTemplate getIdentifierTemplate(String resourceName) {
		return identifierTemplates.get(resourceName);
	}
	
	public void addIdentifierTemplate(IdentifierTemplate identifierTemplate) {
		identifierTemplates.put(identifierTemplate.getResourceName(), identifierTemplate);
	}
	
	public void setIdentifierTemplates(HashMap<String, IdentifierTemplate> identifierTemplates) {
		this.identifierTemplates = identifierTemplates;
	}
	
	public void removeIdentifierTemplate(String resourceName) {
		IdentifierTemplate id = identifierTemplates.get(resourceName);
		identifierTemplates.remove(resourceName);
		for (ChannelGenerator ch: channelGenerators.values()) {
			ch.removeChannelMember(id);
		}
		for (ChannelGenerator ch: ioChannelGenerators.values()) {
			ch.removeChannelMember(id);
		}
	}

	public Collection<ChannelGenerator> getChannelGenerators() {
		return channelGenerators.values();
	}
		
	public ChannelGenerator getChannelGenerator(String channelName) {
		return channelGenerators.get(channelName);
	}
	
	public void setChannelGenerators(HashMap<String, ChannelGenerator> channelGenerators) {
		this.channelGenerators = channelGenerators;
		for (ChannelGenerator g: channelGenerators.values()) {
			for (IdentifierTemplate id: g.getIdentifierTemplates()) {
				identifierTemplates.put(id.getResourceName(), id);				
			}
		}
	}
	
	public void addChannelGenerator(ChannelGenerator channelGenerator) {
		channelGenerators.put(channelGenerator.getChannelName(), channelGenerator);
		for (IdentifierTemplate id: channelGenerator.getIdentifierTemplates()) {
			identifierTemplates.put(id.getResourceName(), id);				
		}
	}
	
	public void removeChannelGenerator(String channelName) {
		channelGenerators.remove(channelName);
	}
	
	public Collection<ChannelGenerator> getIOChannelGenerators() {
		return ioChannelGenerators.values();
	}
	
	public ChannelGenerator getIOChannelGenerator(String channelName) {
		return ioChannelGenerators.get(channelName);
	}
	
	public void setIOChannelGenerators(HashMap<String, ChannelGenerator> ioChannelGenerators) {
		this.ioChannelGenerators = ioChannelGenerators;
		for (ChannelGenerator g: ioChannelGenerators.values()) {
			for (IdentifierTemplate id: g.getIdentifierTemplates()) {
				identifierTemplates.put(id.getResourceName(), id);				
			}
		}
	}
	
	public void addIOChannelGenerator(ChannelGenerator ioChannelGenerator) {
		ioChannelGenerators.put(ioChannelGenerator.getChannelName(), ioChannelGenerator);
		for (IdentifierTemplate id: ioChannelGenerator.getIdentifierTemplates()) {
			identifierTemplates.put(id.getResourceName(), id);				
		}
	}
	
	public void removeIOChannelGenerator(String ioChannelName) {
		ioChannelGenerators.remove(ioChannelName);
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
		for (ChannelGenerator channelGenerator: ioChannelGenerators.values()) {
			out += channelGenerator.toString();
		}
		for (ChannelGenerator channelGenerator: channelGenerators.values()) {
			out += channelGenerator.toString();
		}
		return out;
	}
	
	public String getSourceText() {
		String out = "";
		String init = "";
		for (IdentifierTemplate identifierTemplate: identifierTemplates.values()) {
			String initializer = identifierTemplate.getInitText();
			if (initializer != null) {
				init += initializer;
			}
		}
		if (init.length() > 0) {
			out += "init {\n" + init + "}\n";
		}
		for (ChannelGenerator channelGenerator: ioChannelGenerators.values()) {
			out += channelGenerator.getSourceText();
		}
		for (ChannelGenerator channelGenerator: channelGenerators.values()) {
			out += channelGenerator.getSourceText();
		}
		return out;
	}
}
