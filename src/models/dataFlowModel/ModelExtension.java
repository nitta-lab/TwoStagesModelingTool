package models.dataFlowModel;

import models.algebra.Symbol;
import models.algebra.Type;
import models.dataConstraintModel.DataConstraintModel;

public class ModelExtension {
	private static Symbol.Memento floorMem;
	private static Symbol.Memento sumMem;
	private static Symbol.Memento mergeMem;
	private static Symbol.Memento extractFaceDownMem;
	private static Symbol.Memento sortByKeyMem;

	public static void extendModel(DataTransferModel model) {
		Symbol floor = model.getSymbol("floor");
		floorMem = null;
		if (floor != null) {
			floorMem = floor.createMemento();
			floor.setImplName("(int)Math.floor");
			floor.setImplOperatorType(Symbol.Type.PREFIX);
		}
		Symbol sum = model.getSymbol("sum");
		sumMem = null;
		if (sum != null) {
			sumMem = sum.createMemento();
			final int[] count = new int[] {0};
			sum.setGenerator(new Symbol.IImplGenerator() {
				@Override
				public String generate(Type type, String[] children, String[] childrenSideEffects, String[] sideEffect) {
					String compType = "Integer";
					if (type != null) {
						String interfaceType = type.getInterfaceTypeName();
						if (interfaceType.contains("<")) {
							compType = interfaceType.substring(interfaceType.indexOf("<") + 1, interfaceType.lastIndexOf(">"));
						}						
					}
					count[0]++;
					String impl = compType + " " + "temp_sum" + count[0] + " = 0;\n";
					impl += "for (" + compType + " x: " + children[0] + ") {\n";
					impl += "\t" + "temp_sum" + count[0] + " += x;\n";
					impl += "}\n";
					sideEffect[0] = sideEffect[0] + impl;
					return "temp_sum" + count[0];
				}
			});
			sum.setImplOperatorType(Symbol.Type.GENERATIVE);
			sum.setSignature(new Type[] {DataConstraintModel.typeInt, DataConstraintModel.typeList});
//			sum.setImplName("stream().mapToInt(x->x).sum");
//			sum.setImplOperatorType(Symbol.Type.METHOD);
		}
		Symbol merge = model.getSymbol("merge");
		mergeMem = null;
		if (merge != null) {
			mergeMem = merge.createMemento();
			merge.setArity(2);
			final int[] count = new int[] {0};
			merge.setGenerator(new Symbol.IImplGenerator() {
				@Override
				public String generate(Type type, String[] childrenImpl, String[] childrenSideEffects, String[] sideEffect) {
					String implType = "ArrayList<>";
					String interfaceType = "List<Integer>";
					String compType = "Integer";
					if (type != null) {
						implType = type.getImplementationTypeName();
						interfaceType = type.getInterfaceTypeName();
						if (interfaceType.contains("<")) {
							compType = interfaceType.substring(interfaceType.indexOf("<") + 1, interfaceType.lastIndexOf(">"));
						}
					}
					String idxGetter = "";
					if (compType.startsWith("Map.Entry")) {
						idxGetter = ".getKey()";
					}
					count[0]++;
					String impl = "";
					impl += "" + interfaceType + " temp_l" + count[0] + " = new " + implType + "();\n";
					impl += "{\n";
					impl += "\tIterator<" + compType + "> i1 = " + childrenImpl[0] + ".iterator();\n";
					impl += "\tIterator<" + compType + "> i2 = " + childrenImpl[1] + ".iterator();\n";
					impl += "\t" + compType + " t1 = null;\n";
					impl += "\t" + compType + " t2 = null;\n";
					impl += "\twhile (i1.hasNext() || i2.hasNext() || t1 != null || t2 != null) {\n";
					impl += "\t\tif (t1 == null && i1.hasNext()) {\n";
					impl += "\t\t\tt1 = i1.next();\n";
					impl += "\t\t}\n";
					impl += "\t\tif (t2 == null && i2.hasNext()) {\n";
					impl += "\t\t\tt2 = i2.next();\n";
					impl += "\t\t}\n";
					impl += "\t\tif (t1 == null || (t2 != null && t1" + idxGetter + " < t2" + idxGetter + ")) {\n";
					impl += "\t\t\ttemp_l"  + count[0] +".add(t2);\n";
					impl += "\t\t\tt2 = null;\n";
					impl += "\t\t} else {\n";
					impl += "\t\t\ttemp_l" + count[0] + ".add(t1);\n";
					impl += "\t\t\tt1 = null;\n";
					impl += "\t\t}\n";
					impl += "\t}\n";
					impl += "}\n";
					sideEffect[0] = sideEffect[0] + impl;
					return "temp_l" + count[0];
				}
			});
			merge.setImplOperatorType(Symbol.Type.GENERATIVE);
			merge.setSignature(new Type[] {DataConstraintModel.typeList, DataConstraintModel.typeList, DataConstraintModel.typeList});
		}		
		Symbol extractFaceDown = model.getSymbol("extractFaceDown");
		extractFaceDownMem = null;
		if (extractFaceDown != null) {
			extractFaceDownMem = extractFaceDown.createMemento();
			extractFaceDown.setArity(1);
			extractFaceDown.setGenerator(new Symbol.IImplGenerator() {
				@Override
				public String generate(Type type, String[] children, String[] childrenSideEffects, String[] sideEffect) {			
					return children[0]+".stream().filter(item -> item.getValue()==false).collect(Collectors.toList())";
				}
			});
			extractFaceDown.setImplOperatorType(Symbol.Type.GENERATIVE);
			extractFaceDown.setSignature(new Type[] {DataConstraintModel.typeList, null});
		}
		
		Symbol sortByKey = model.getSymbol("sortByKey");
		sortByKeyMem = null;
		if(sortByKey != null) {
			sortByKeyMem = sortByKey.createMemento();
			sortByKey.setArity(1);
			sortByKey.setGenerator(new Symbol.IImplGenerator() {
				@Override
				public String generate(Type type, String[] children, String[] childrenSideEffects, String[] sideEffect) {			
					String compType = "";
					String temp_sort="temp_sort";
					if (type != null) {
						String interfaceType = type.getInterfaceTypeName();
						if (interfaceType.contains("<")) {
							compType = interfaceType.substring(interfaceType.indexOf("<") + 1, interfaceType.lastIndexOf(">"));
						}
						String implType = type.getImplementationTypeName();
						if (implType.indexOf('<') >= 0) {
							implType = implType.substring(0, implType.indexOf('<'));
						}
					
					}	
					for (String s: childrenSideEffects) {
						sideEffect[0] += s;
					}
					temp_sort=children[0]+".sort(Comparator.comparing("+compType+"::getKey));\n";
					return temp_sort;
				}
			});
			sortByKey.setSignature(new Type[] {DataConstraintModel.typeList, DataConstraintModel.typeList});
			sortByKey.setImplOperatorType(Symbol.Type.GENERATIVE);
		}
	}
	
	public static void recoverModel(DataTransferModel model) {
		Symbol floor = model.getSymbol("floor");		
		if (floor != null) floor.setMemento(floorMem);
		Symbol sum = model.getSymbol("sum");
		if (sum != null) sum.setMemento(sumMem);
		Symbol merge = model.getSymbol("merge");
		if (merge != null) merge.setMemento(mergeMem);		
		Symbol extractFaceDown = model.getSymbol("extractFaceDown");
		if (extractFaceDown != null) extractFaceDown.setMemento(extractFaceDownMem);
		Symbol sortByKey = model.getSymbol("sortByKey");
		if (sortByKey != null) sortByKey.setMemento(sortByKeyMem);
	}
}
