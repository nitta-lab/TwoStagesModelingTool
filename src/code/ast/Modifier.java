package code.ast;

public class Modifier extends ASTNode {
	public static final int ABSTRACT = 0x0400;
	public static final int PRIVATE = 0x0002;
	public static final int PROTECTED = 0x0004;
	public static final int PUBLIC = 0x0001;
	public static final int STATIC = 0x0008;

	public static boolean isAbstract(int flags) {
		return (flags & ABSTRACT) != 0;
	}
	
	public static boolean isPrivate(int flags) {
		return (flags & PRIVATE) != 0;
	}
	
	public static boolean isProtected(int flags) {
		return (flags & PROTECTED) != 0;
	}

	public static boolean isPublic(int flags) {
		return (flags & PUBLIC) != 0;
	}
	
	public static boolean isStatic(int flags) {
		return (flags & STATIC) != 0;
	}
}
