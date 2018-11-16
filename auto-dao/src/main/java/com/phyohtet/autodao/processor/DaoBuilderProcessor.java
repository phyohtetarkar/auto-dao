package com.phyohtet.autodao.processor;

import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import com.phyohtet.autodao.annotation.Dao;
import com.phyohtet.autodao.core.JDBCDao;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

@SupportedAnnotationTypes("com.phyohtet.autodao.annotation.AutoDao")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class DaoBuilderProcessor extends AbstractProcessor {

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for (TypeElement annotation : annotations) {
			Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);

			annotatedElements.stream().forEach(element -> {
				try {
					TypeElement typeElement = (TypeElement) element;
					String superClazz = typeElement.getSuperclass().toString();
					if (JDBCDao.class.getName().equals(superClazz)) {

						String packageName = element.getEnclosingElement().toString();
						String className = element.getSimpleName().toString().concat("Impl");

						TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(className)
								.addModifiers(Modifier.PUBLIC).superclass(TypeName.get(element.asType()));

						for (Element e : element.getEnclosedElements()) {
							if (!(e instanceof ExecutableElement)) {
								continue;
							}

							if (!e.getModifiers().contains(Modifier.ABSTRACT)) {
								continue;
							}

							ExecutableElement executableElement = (ExecutableElement) e;
							
							if (!executableElement.getReturnType().getKind().equals(TypeKind.VOID)) {
								DeclaredType declaredType = (DeclaredType) executableElement.getReturnType();
								if (declaredType.asElement().getAnnotation(Dao.class) != null) {
									generateDao(declaredType.asElement());
								} 
							}

							typeSpecBuilder.addMethod(generateMethodSpec(element));
						}

						JavaFile daoClass = JavaFile.builder(packageName, typeSpecBuilder.build())
								.indent("	")
								.skipJavaLangImports(true)
								.build();

						daoClass.writeTo(processingEnv.getFiler());
					}
				} catch (Exception e) {
					e.printStackTrace();
					processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), element);
				}
			});
		}
		return true;
	}

	private void generateDao(Element element) throws Exception {
		String packageName = element.getEnclosingElement().toString();
		String className = element.getSimpleName().toString().concat("Impl");

		TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC,
				Modifier.FINAL);

		TypeMirror tm = element.asType();

		switch (element.getKind()) {
		case INTERFACE:
			typeSpecBuilder.addSuperinterface(TypeName.get(tm));
			break;
		case CLASS:
			typeSpecBuilder.superclass(TypeName.get(tm));
			break;
		default:
			break;
		}

		typeSpecBuilder.addField(JDBCDao.class, "db", Modifier.PRIVATE);

		MethodSpec constructorMethodSpec = MethodSpec.constructorBuilder()
				.addModifiers(Modifier.PUBLIC)
				.addParameter(JDBCDao.class, "db")
				.addStatement("this.db = db").build();

		typeSpecBuilder.addMethod(constructorMethodSpec);

		element.getEnclosedElements().forEach(e -> {
			typeSpecBuilder.addMethod(generateMethodSpec(e));
		});

		JavaFile daoClass = JavaFile.builder(packageName, typeSpecBuilder.build())
				.indent("	")
				.skipJavaLangImports(true)
				.build();

		daoClass.writeTo(processingEnv.getFiler());
	}

	private MethodSpec generateMethodSpec(Element element) {
		String methodName = element.getSimpleName().toString();
		ExecutableElement executableElement = (ExecutableElement) element;

		MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName);

		if (ElementKind.CONSTRUCTOR != element.getKind() && !element.getModifiers().contains(Modifier.STATIC)) {
			builder.addAnnotation(Override.class);
		}

		builder.addModifiers(
				element.getModifiers().stream().filter(m -> !Modifier.ABSTRACT.equals(m)).collect(Collectors.toSet()));

		executableElement.getParameters().forEach(ve -> {
			builder.addParameter(TypeName.get(ve.asType()), ve.getSimpleName().toString());
		});

		if (!executableElement.getReturnType().getKind().equals(TypeKind.VOID)) {
			builder.returns(TypeName.get(executableElement.getReturnType()));
			builder.addStatement("return null");
		}

		executableElement.getThrownTypes().forEach(tm -> {
			builder.addException(TypeName.get(tm));
		});

		return builder.build();
	}

}
