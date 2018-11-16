package com.phyohtet.autodao.utils;

import java.lang.annotation.Annotation;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

public class ProcessorUtils {

	public static boolean hasReturnType(Element element) {
		if (element instanceof ExecutableElement) {
			return !((ExecutableElement) element).getReturnType().getKind().equals(TypeKind.VOID);
		}

		return false;
	}

	public static <A extends Annotation> boolean hasAnnotation(TypeMirror typeMirror, Class<A> clazz) {
		if (typeMirror instanceof DeclaredType) {
			DeclaredType declaredType = (DeclaredType) typeMirror;
			return declaredType.asElement().getAnnotation(clazz) != null;
		} else if (typeMirror instanceof TypeVariable) {
			TypeVariable typeVariable = (TypeVariable) typeMirror;
			return typeVariable.asElement().getAnnotation(clazz) != null;
		} else if (typeMirror instanceof ExecutableType) {
			ExecutableType executableType = (ExecutableType) typeMirror;
			return executableType.getAnnotation(clazz) != null;
		} 
		return false;
	}

}
