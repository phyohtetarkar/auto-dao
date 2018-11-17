package com.phyohtet.autodao.processor;

import java.io.IOException;
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
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import com.phyohtet.autodao.AutoDaoException;
import com.phyohtet.autodao.annotation.Count;
import com.phyohtet.autodao.annotation.Dao;
import com.phyohtet.autodao.annotation.Entity;
import com.phyohtet.autodao.annotation.Insert;
import com.phyohtet.autodao.annotation.Query;
import com.phyohtet.autodao.annotation.Transaction;
import com.phyohtet.autodao.annotation.Update;
import com.phyohtet.autodao.core.ConnectionConfiguration;
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
					String packageName = element.getEnclosingElement().toString();

					TypeSpec.Builder typeSpecBuilder = typeSpecBuilder(element);
					typeSpecBuilder.addField(JDBCDao.class, "db", Modifier.PRIVATE);

					typeSpecBuilder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
							.addParameter(ConnectionConfiguration.class, "cc")
							.addStatement("this.db = new $T()", JDBCDao.class).addStatement("this.db.init(cc)")
							.build());

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

								meBuilder.beginControlFlow("if (" + fieldSpec.name + " == null)")
										.addStatement("return new $T(db)",
												getClassName(declaredType.toString().concat("Impl")))
										.endControlFlow();

								meBuilder.addStatement("return " + fieldSpec.name);
							} else {
								meBuilder.addStatement("return null");
							}
						}

						typeSpecBuilder.addMethod(meBuilder.build());
					}

					writeJavaFile(packageName, typeSpecBuilder.build());
				} catch (Exception e) {
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

		MethodSpec constructorMethodSpec = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
				.addParameter(JDBCDao.class, "db").addStatement("this.db = db").build();

		typeSpecBuilder.addMethod(constructorMethodSpec);

		element.getEnclosedElements().stream().filter(e -> e instanceof ExecutableElement)
				.filter(e -> !e.getModifiers().contains(Modifier.STATIC))
				.forEach(e -> {
					MethodSpec.Builder meBuilder = methodSpecBuilder(e);
					Query query = e.getAnnotation(Query.class);
					Count count = e.getAnnotation(Count.class);
					Insert insert = e.getAnnotation(Insert.class);
					Update update = e.getAnnotation(Update.class);
					Transaction trans = e.getAnnotation(Transaction.class);
					
					ExecutableElement ee = (ExecutableElement) e;
					String params = ee.getParameters().stream().map(ve -> ve.getSimpleName().toString())
							.collect(Collectors.joining(", "));
					params = params.isEmpty() ? params : ", ".concat(params);
					
					String returnn = ProcessorUtils.hasReturnType(ee) ? "return " : "";
					
					if (query != null) {
						meBuilder.addStatement("String sql = $S", query.sql());
						if (ee.getReturnType().getKind().isPrimitive()) {
							PrimitiveType primitiveType = (PrimitiveType) ee.getReturnType();
							meBuilder.addStatement(returnn + "db.querySingleResult(sql, $N.class" + params + ")",
									primitiveType.toString());
						} else {
							DeclaredType declaredType = (DeclaredType) ee.getReturnType();
							if (declaredType.asElement().toString().equals("java.util.List")) {
								meBuilder.addStatement(returnn + "db.queryResultList(sql, $T.class" + params + ")",
										getClassName(declaredType.getTypeArguments().get(0).toString()));
							} else {
								meBuilder.addStatement(returnn + "db.querySingleResult(sql, $T.class" + params + ")",
										getClassName(declaredType.toString()));
							}
						}
					} else if (count != null) {
						if (ee.getReturnType().getKind() != TypeKind.INT) {
							throw new AutoDaoException("Count return type must be primitive integer.");
						}
						meBuilder.addStatement("String sql = $S", count.sql());
						meBuilder.addStatement(returnn + "db.queryCount(sql" + params + ")");
					} else if (insert != null || update != null) {
						checkParamsForInsertOrUpdate(ee);
						meBuilder.addCode("try {\n");
						meBuilder.addStatement("\tdb.$N($N)", insert != null ? "insert" : "update", 
								ee.getParameters().get(0).getSimpleName().toString());
						meBuilder.addCode("} catch ($T e) {\n"
								+ "\tdb.rollbackTransaction();\n"
								+ "} finally {\n"
								+ "\tdb.commitTransaction();\n"
								+ "}\n", AutoDaoException.class);
					} else if (trans != null) {
					
					} else {
						if (ee.getReturnType().getKind().isPrimitive()) {
							meBuilder.addStatement("return 0");
						} else if (ee.getReturnType().getKind() == TypeKind.BOOLEAN) {
							meBuilder.addStatement("return false");
						} else if (ee.getReturnType().getKind() != TypeKind.VOID) {
							meBuilder.addStatement("return null");
						}
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

		builder.addModifiers(
				element.getModifiers().stream().filter(m -> !Modifier.ABSTRACT.equals(m)).collect(Collectors.toSet()));

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

		return typeSpecBuilder;
	}

	private void writeJavaFile(String packageName, TypeSpec typeSpec) throws IOException {
		JavaFile daoClass = JavaFile.builder(packageName, typeSpec).indent("	").skipJavaLangImports(true).build();

		daoClass.writeTo(processingEnv.getFiler());
	}

	private ClassName getClassName(String className) {
		String pkg = className.substring(0, className.lastIndexOf('.'));
		String name = className.substring(className.lastIndexOf('.') + 1);
		return ClassName.get(pkg, name);
	}
	
	private void checkParamsForInsertOrUpdate(ExecutableElement ee) {
		if (ee.getParameters().isEmpty()) {
			throw new AutoDaoException("Must have one parameter for insert or update");
		}
		
		if (ee.getParameters().size() > 1) {
			throw new AutoDaoException("Multiple parameters are not allowed for insert or update.");
		}
		
		DeclaredType dt = (DeclaredType) ee.getParameters().get(0).asType();
		
		if (dt.asElement().getAnnotation(Entity.class) == null) {
			throw new AutoDaoException("Insert or update parameter must annotated with com.phyohtet.autodao.annotation.Entity.");
		}
	}

}
