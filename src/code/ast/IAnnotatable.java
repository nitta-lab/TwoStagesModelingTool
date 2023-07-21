package code.ast;

import java.util.Collection;

public interface IAnnotatable {
	Annotation getAnnotation(String name);
	Collection<Annotation> getAnnotations();
	void addAnnotation(Annotation annotation);
}
