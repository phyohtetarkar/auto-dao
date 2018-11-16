package com.phyohtet.autodao.processor;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import com.phyohtet.autodao.AutoDaoException;
import com.phyohtet.autodao.annotation.Dao;
import com.phyohtet.autodao.annotation.Query;
import com.phyohtet.autodao.core.JDBCDao;
import com.phyohtet.autodao.utils.ProcessorUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
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

						TypeSpec.Builder typeSpecBuilder = typeSpecBuilder(element);
						
						for (Element e : element.getEnclosedElements()) {
							if (!(e instanceof ExecutableElement)) {
								continue;
							}

							if (!e.getModifiers().contains(Modifier.ABSTRACT)) {
								continue;
							}
							
							MethodSpec.Builder meBuilder = methodSpecBuilder(e);

							ExecutableElement executableElement = (ExecutableElement) e;
							
							if (ProcessorUtils.hasReturnType(e)) {
								if (ProcessorUtils.hasAnnotation(executableElement.getReturnType(), Dao.class)) {
									DeclaredType declaredType = (DeclaredType) executableElement.getReturnType();
									generateDao(declaredType.asElement());
									
									FieldSpec fieldSpec = basicFieldSpec(declaredType.asElement());
									typeSpecBuilder.addField(fieldSpec);
									
									String daoClass = declaredType.toString().concat("Impl");
									String pkg = daoClass.substring(0, daoClass.lastIndexOf('.'));
									String name = daoClass.substring(daoClass.lastIndexOf('.') + 1);
									
									meBuilder.beginControlFlow("if (" + fieldSpec.name + " == null)")
										.addStatement("return new $T(this)", ClassName.get(pkg, name))
										.endControlFlow();
									
									meBuilder.addStatement("return " + fieldSpec.name);
								} else {
									meBuilder.addStatement("return null");
								}
							} 

							typeSpecBuilder.addMethod(meBuilder.build());
						}
						
						writeJavaFile(packageName, typeSpecBuilder.build());
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

		TypeSpec.Builder typeSpecBuilder = typeSpecBuilder(element);

		typeSpecBuilder.addField(JDBCDao.class, "db", Modifier.PRIVATE);

		MethodSpec constructorMethodSpec = MethodSpec.constructorBuilder()
				.addModifiers(Modifier.PUBLIC)
				.addParameter(JDBCDao.class, "db")
				.addStatement("this.db = db").build();

		typeSpecBuilder.addMethod(constructorMethodSpec);

		element.getEnclosedElements().forEach(e -> {
			MethodSpec.Builder meBuilder = methodSpecBuilder(e);
			if (e.getAnnotation(Query.class) != null) {
				meBuilder.addCode("try {\n");
				meBuilder.addStatement("\t$T rs = db.executeQuery(null, null)", ResultSet.class);
				meBuilder.beginControlFlow("while (rs.next())")
					.endControlFlow();
				meBuilder.addCode("} catch ($T e) {\n", SQLException.class);
				meBuilder.addStatement("throw new $T(e)", AutoDaoException.class);
				meBuilder.addCode("}\n");
			}
			if (ProcessorUtils.hasReturnType(e)) {
				meBuilder.addStatement("return null");
			}
			typeSpecBuilder.addMethod(meBuilder.build());
		});
		
		writeJavaFile(packageName, typeSpecBuilder.build());
	}

	private MethodSpec.Builder methodSpecBuilder(Element element) {
		String methodName = element.getSimpleName().toString();
		ExecutableElement executableElement = (ExecutableElement) element;

		MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName);

		if (ElementKind.CONSTRUCTOR != element.getKind() && !element.getModifiers().contains(Modifier.STATIC)) {
			builder.addAnnotation(Override.class);
		}

		builder.addModifiers(element.getModifiers().stream().filter(m -> !Modifier.ABSTRACT.equals(m))
				.collect(Collectors.toSet()));
		
		if (ProcessorUtils.hasReturnType(executableElement)) {
			builder.returns(TypeName.get(executableElement.getReturnType()));
		}

		executableElement.getParameters().forEach(ve -> {
			builder.addParameter(TypeName.get(ve.asType()), ve.getSimpleName().toString());
		});
		
		executableElement.getThrownTypes().forEach(tm -> {
			builder.addException(TypeName.get(tm));
		});

		return builder;
	}
	
	private FieldSpec basicFieldSpec(Element element) {
		char[] ary = element.getSimpleName().toString().toCharArray();
		ary[0] = Character.toLowerCase(ary[0]);
		
		StringBuilder sb = new StringBuilder();
		sb.append(ary);
		
		return FieldSpec.builder(TypeName.get(element.asType()), sb.toString(), Modifier.PRIVATE).build();
	}
	
	private TypeSpec.Builder typeSpecBuilder(Element element) {
		String className = element.getSimpleName().toString().concat("Impl");

		TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(className)
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
		
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
		
		return typeSpecBuilder;
	}
	
	private void writeJavaFile(String packageName, TypeSpec typeSpec) throws IOException {
		JavaFile daoClass = JavaFile.builder(packageName, typeSpec)
				.indent("	")
				.skipJavaLangImports(true)
				.build();

		daoClass.writeTo(processingEnv.getFiler());
	}

}
