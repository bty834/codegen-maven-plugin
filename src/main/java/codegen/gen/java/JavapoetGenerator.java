package codegen.gen.java;

import static codegen.gen.CommonUtil.JAVA_DOC;
import static codegen.gen.CommonUtil.convertJDBCTypetoClass;
import static codegen.gen.CommonUtil.getAbsolutePathForPkg;
import static codegen.gen.CommonUtil.getAbsolutePathForSrcMainJava;
import static codegen.gen.CommonUtil.mapUnderScoreToLowerCamelCase;
import static codegen.gen.CommonUtil.mapUnderScoreToUpperCamelCase;
import static codegen.gen.CommonUtil.prepareDir;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.maven.plugin.MojoExecutionException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

import codegen.ConfigProperties;
import codegen.table.Table;
import codegen.table.TableColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author: baotingyu
 * @date: 2023/6/25
 **/
public class JavapoetGenerator implements CodeGenerator {
    private final static ParameterSpec INT_PARAM = ParameterSpec.builder(Integer.class, "value").build();
    private final static ParameterSpec LONG_PARAM = ParameterSpec.builder(Long.class, "value").build();
    private final static ParameterSpec STRING_PARAM = ParameterSpec.builder(String.class, "value").build();
    private final static ParameterSpec FLOAT_PARAM = ParameterSpec.builder(Float.class, "value").build();
    private final static ParameterSpec DOUBLE_PARAM = ParameterSpec.builder(Double.class, "value").build();


    private final static ParameterSpec INT_LIST_PARAM =
            ParameterSpec.builder(ParameterizedTypeName.get(List.class, Integer.class), "list").build();
    private final static ParameterSpec LONG_LIST_PARAM =
            ParameterSpec.builder(ParameterizedTypeName.get(List.class, Long.class), "list").build();
    private final static ParameterSpec STRING_LIST_PARAM =
            ParameterSpec.builder(ParameterizedTypeName.get(List.class, String.class), "list").build();
    private final static ParameterSpec FLOAT_LIST_PARAM =
            ParameterSpec.builder(ParameterizedTypeName.get(List.class, Float.class), "list").build();
    private final static ParameterSpec DOUBLE_LIST_PARAM =
            ParameterSpec.builder(ParameterizedTypeName.get(List.class, Double.class), "list").build();

    protected ConfigProperties configProperties;

    @Override
    public void generate(ConfigProperties configProperties, Set<Table> tables) throws MojoExecutionException {
        if (Objects.isNull(tables) || tables.size() == 0) {
            return;
        }
        if (StringUtils.isBlank(configProperties.getEntityGenPkg()) ||
                StringUtils.isBlank(configProperties.getMapperInterfaceGenPkg()) ||
                StringUtils.isBlank(configProperties.getMapperXmlGenAbsPath())) {
            throw new MojoExecutionException("entityGenPkg or mapperInterfaceGenPkg or mapperXmlGenAbsPath blank");
        }
        this.configProperties = configProperties;
        createDirsIfNecessary();

        Map<String, TypeSpec> entityClassSpecs = generateEntity(tables);
        generatePageAndSort();
        generateConditionUtil();
        Map<String, TypeSpec> queryExampleSpecs = generateQueryExample(tables, entityClassSpecs);
        Map<String, TypeSpec> interfaceSpecs = generateMapperInterface(tables, entityClassSpecs, queryExampleSpecs);

    }


    private void createDirsIfNecessary() throws MojoExecutionException {
        prepareDir(
                getAbsolutePathForPkg(this.configProperties.getBaseDir(), configProperties.getMapperInterfaceGenPkg()));
        prepareDir(getAbsolutePathForPkg(this.configProperties.getBaseDir(), this.configProperties.getEntityGenPkg()));
        prepareDir(this.configProperties.getMapperXmlGenAbsPath());
    }

    private void persistTypeSpec(String genPkg, Collection<TypeSpec> typeSpecs) {
        typeSpecs.forEach(ts -> {
            JavaFile file = JavaFile.builder(genPkg, ts).build();
            try {
                file.writeTo(Paths.get(getAbsolutePathForSrcMainJava(this.configProperties.getBaseDir())));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void generateConditionUtil() {
        final String conditionUtilClassName = "ConditionUtil";

        Builder builder = TypeSpec
                .classBuilder(conditionUtilClassName)
                .addModifiers(Modifier.PUBLIC);

        ParameterSpec columnSpec = ParameterSpec.builder(String.class, "column").build();

        final String inStatement =
                "   List<String> collect = list.stream().map(String::valueOf).collect($T.toList());\n"
                        + "        String join = String.join(\",\", collect);\n"
                        + "        return column+\" in (\"+join+\")\";";
        final String notInStatement =
                "   List<String> collect = list.stream().map(String::valueOf).collect($T.toList());\n"
                        + "        String join = String.join(\",\", collect);\n"
                        + "        return column+\" not in (\"+join+\")\";";

        //<editor-fold desc="int method">
        MethodSpec intGte = MethodSpec.methodBuilder("intGte")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(INT_PARAM)
                .addStatement("return column + \">=\" + value")
                .returns(String.class).build();
        MethodSpec intGt = MethodSpec.methodBuilder("intGt")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(INT_PARAM)
                .addStatement("return column + \">\" + value")
                .returns(String.class).build();
        MethodSpec intLte = MethodSpec.methodBuilder("intLte")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(INT_PARAM)
                .addStatement("return column + \"<=\" + value")
                .returns(String.class).build();
        MethodSpec intLt = MethodSpec.methodBuilder("intLt")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(INT_PARAM)
                .addStatement("return column + \"<\" + value")
                .returns(String.class).build();
        MethodSpec intEquals = MethodSpec.methodBuilder("intEquals")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(INT_PARAM)
                .addStatement("return column+\"=\"+value")
                .returns(String.class).build();
        MethodSpec intIn = MethodSpec.methodBuilder("intIn")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(INT_LIST_PARAM)
                .addCode(CodeBlock.builder().add(inStatement, Collectors.class).build())
                .returns(String.class).build();
        MethodSpec intNotIn = MethodSpec.methodBuilder("intNotIn")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(INT_LIST_PARAM)
                .addCode(CodeBlock.builder().add(notInStatement, Collectors.class).build())
                .returns(String.class).build();
        //</editor-fold>

        //<editor-fold desc="long method">
        MethodSpec longGte = MethodSpec.methodBuilder("longGte")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(LONG_PARAM)
                .addStatement("return column + \">=\" + value")
                .returns(String.class).build();
        MethodSpec longGt = MethodSpec.methodBuilder("longGt")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(LONG_PARAM)
                .addStatement("return column + \">\" + value")
                .returns(String.class).build();
        MethodSpec longLte = MethodSpec.methodBuilder("longLte")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(LONG_PARAM)
                .addStatement("return column + \"<=\" + value")
                .returns(String.class).build();
        MethodSpec longLt = MethodSpec.methodBuilder("longLt")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(LONG_PARAM)
                .addStatement("return column + \"<\" + value")
                .returns(String.class).build();
        MethodSpec longEquals = MethodSpec.methodBuilder("longEquals")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(LONG_PARAM)
                .addStatement("return column+\"=\"+value")
                .returns(String.class).build();
        MethodSpec longIn = MethodSpec.methodBuilder("longIn")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(LONG_LIST_PARAM)
                .addCode(CodeBlock.builder().add(inStatement, Collectors.class).build())
                .returns(String.class).build();
        MethodSpec longNotIn = MethodSpec.methodBuilder("longNotIn")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(LONG_LIST_PARAM)
                .addCode(CodeBlock.builder().add(notInStatement, Collectors.class).build())
                .returns(String.class).build();
        //</editor-fold>

        //<editor-fold desc="string method">
        MethodSpec stringGte = MethodSpec.methodBuilder("stringGte")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(STRING_PARAM)
                .addStatement("return column + \">=\" + value")
                .returns(String.class).build();
        MethodSpec stringGt = MethodSpec.methodBuilder("stringGt")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(STRING_PARAM)
                .addStatement("return column + \">\" + value")
                .returns(String.class).build();
        MethodSpec stringLte = MethodSpec.methodBuilder("stringLte")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(STRING_PARAM)
                .addStatement("return column + \"<=\" + value")
                .returns(String.class).build();
        MethodSpec stringLt = MethodSpec.methodBuilder("stringLt")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(STRING_PARAM)
                .addStatement("return column + \"<\" + value")
                .returns(String.class).build();
        MethodSpec stringEquals = MethodSpec.methodBuilder("stringEquals")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(STRING_PARAM)
                .addStatement("return column+\"=\"+value")
                .returns(String.class).build();
        MethodSpec stringIn = MethodSpec.methodBuilder("stringIn")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(STRING_LIST_PARAM)
                .addCode(CodeBlock.builder().add(inStatement, Collectors.class).build())
                .returns(String.class).build();
        MethodSpec stringNotIn = MethodSpec.methodBuilder("stringNotIn")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(STRING_LIST_PARAM)
                .addCode(CodeBlock.builder().add(notInStatement, Collectors.class).build())
                .returns(String.class).build();
        //</editor-fold>

        //<editor-fold desc="float method">
        MethodSpec floatGte = MethodSpec.methodBuilder("floatGte")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(FLOAT_PARAM)
                .addStatement("return column + \">=\" + value")
                .returns(String.class).build();
        MethodSpec floatGt = MethodSpec.methodBuilder("floatGt")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(FLOAT_PARAM)
                .addStatement("return column + \">\" + value")
                .returns(String.class).build();
        MethodSpec floatLte = MethodSpec.methodBuilder("floatLte")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(FLOAT_PARAM)
                .addStatement("return column + \"<=\" + value")
                .returns(String.class).build();
        MethodSpec floatLt = MethodSpec.methodBuilder("floatLt")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(FLOAT_PARAM)
                .addStatement("return column + \"<\" + value")
                .returns(String.class).build();
        MethodSpec floatEquals = MethodSpec.methodBuilder("floatEquals")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(FLOAT_PARAM)
                .addStatement("return column+\"=\"+value")
                .returns(String.class).build();
        MethodSpec floatIn = MethodSpec.methodBuilder("floatIn")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(FLOAT_LIST_PARAM)
                .addCode(CodeBlock.builder().add(inStatement, Collectors.class).build())
                .returns(String.class).build();
        MethodSpec floatNotIn = MethodSpec.methodBuilder("floatNotIn")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(FLOAT_LIST_PARAM)
                .addCode(CodeBlock.builder().add(notInStatement, Collectors.class).build())
                .returns(String.class).build();
        //</editor-fold>

        //<editor-fold desc="double method">
        MethodSpec doubleGte = MethodSpec.methodBuilder("doubleGte")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(DOUBLE_PARAM)
                .addStatement("return column + \">=\" + value")
                .returns(String.class).build();
        MethodSpec doubleGt = MethodSpec.methodBuilder("doubleGt")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(DOUBLE_PARAM)
                .addStatement("return column + \">\" + value")
                .returns(String.class).build();
        MethodSpec doubleLte = MethodSpec.methodBuilder("doubleLte")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(DOUBLE_PARAM)
                .addStatement("return column + \"<=\" + value")
                .returns(String.class).build();
        MethodSpec doubleLt = MethodSpec.methodBuilder("doubleLt")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(DOUBLE_PARAM)
                .addStatement("return column + \"<\" + value")
                .returns(String.class).build();
        MethodSpec doubleEquals = MethodSpec.methodBuilder("doubleEquals")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(DOUBLE_PARAM)
                .addStatement("return column+\"=\"+value")
                .returns(String.class).build();
        MethodSpec doubleIn = MethodSpec.methodBuilder("doubleIn")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(DOUBLE_LIST_PARAM)
                .addCode(CodeBlock.builder().add(inStatement, Collectors.class).build())
                .returns(String.class).build();
        MethodSpec doubleNotIn = MethodSpec.methodBuilder("doubleNotIn")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(DOUBLE_LIST_PARAM)
                .addCode(CodeBlock.builder().add(notInStatement, Collectors.class).build())
                .returns(String.class).build();
        //</editor-fold>

        ArrayList<MethodSpec> methodSpecs = Lists.newArrayList(
                intGte, intGt, intLte, intLt, intEquals, intIn, intNotIn,
                longGte, longGt, longLte, longLt, longEquals, longIn, longNotIn,
                stringGte, stringGt, stringLte, stringLt, stringEquals, stringIn, stringNotIn,
                floatGte, floatGt, floatLte, floatLt, floatEquals, floatIn, floatNotIn,
                doubleGte, doubleGt, doubleLte, doubleLt, doubleEquals, doubleIn, doubleNotIn
        );
        builder.addMethods(methodSpecs);
        persistTypeSpec(this.configProperties.getMapperInterfaceGenPkg(), Collections.singletonList(builder.build()));
    }

    private void generatePageAndSort() {
        String pageClassName = "Page";
        String sortClassName = "Sort";

        TypeSpec page = TypeSpec
                .classBuilder(pageClassName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(lombok.Builder.class)
                .addAnnotation(ToString.class)
                .addField(FieldSpec.builder(Integer.class, "offset", Modifier.PRIVATE).build())
                .addField(FieldSpec.builder(Integer.class, "limit", Modifier.PRIVATE).build())
                .addJavadoc(JAVA_DOC + LocalDateTime.now())
                .build();

        TypeSpec sort = TypeSpec
                .classBuilder(sortClassName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(lombok.Builder.class)
                .addAnnotation(ToString.class)
                .addField(FieldSpec.builder(Boolean.class, "isAsc", Modifier.PRIVATE).build())
                .addField(FieldSpec.builder(String.class, "fieldName", Modifier.PRIVATE).build())
                .addJavadoc(JAVA_DOC + LocalDateTime.now())
                .build();

        persistTypeSpec(this.configProperties.getMapperInterfaceGenPkg(), Lists.newArrayList(page, sort));
    }

    private Map<String, TypeSpec> generateQueryExample(Set<Table> tables, Map<String, TypeSpec> entityClassSpecs) {
        Map<String, TypeSpec> queryExampleSpecs = buildQueryExamples(tables, entityClassSpecs);
        persistTypeSpec(this.configProperties.getMapperInterfaceGenPkg(), queryExampleSpecs.values());
        return queryExampleSpecs;
    }

    private Map<String, TypeSpec> buildQueryExamples(Set<Table> tables, Map<String, TypeSpec> entityClassSpecs) {
        Map<String, TypeSpec> examples = Maps.newHashMap();
        tables.forEach(t -> {
            String simpleClassName = mapUnderScoreToUpperCamelCase(t.getName());
            examples.putAll(buildQueryExampleForTable(t, entityClassSpecs.get(simpleClassName)));
        });
        return examples;
    }

    private Map<String, TypeSpec> buildQueryExampleForTable(Table table, TypeSpec entitySpec) {
        String exampleSimpleName = mapUnderScoreToUpperCamelCase(table.getName()) + "QueryExample";

        ClassName exampleClassName = ClassName.get(this.configProperties.getMapperInterfaceGenPkg(), exampleSimpleName);

        ClassName conditionUtilClassName =
                ClassName.get(this.configProperties.getMapperInterfaceGenPkg(), "ConditionUtil");

        Builder builder = TypeSpec
                .classBuilder(exampleSimpleName)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc(JAVA_DOC + LocalDateTime.now());

        FieldSpec whereClause =
                FieldSpec.builder(ParameterizedTypeName.get(List.class, String.class), "whereClause", Modifier.PRIVATE)
                        .addAnnotation(Getter.class).build();
        builder.addField(whereClause);

        MethodSpec nonArgsConstructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build();
        MethodSpec newExample = MethodSpec.methodBuilder("newExample")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addCode(CodeBlock.of("return new " + exampleSimpleName + "();"))
                .returns(exampleClassName)
                .build();
        builder.addMethod(nonArgsConstructor);
        builder.addMethod(newExample);

        Set<TableColumn> columns = table.getColumns();


        for (TableColumn column : columns) {
            String fieldName = mapUnderScoreToLowerCamelCase(column.getColumnName());
            Class<?> aClass = convertJDBCTypetoClass(column.getDataType());

            if (aClass.equals(Integer.class)) {
                MethodSpec equals = MethodSpec.methodBuilder(fieldName + "Equals")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(INT_PARAM)
                        .addCode("whereClause.add( $T.intEquals(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec gte = MethodSpec.methodBuilder(fieldName + "Gte")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(INT_PARAM)
                        .addCode("whereClause.add( $T.intGte(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec gt = MethodSpec.methodBuilder(fieldName + "Gt")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(INT_PARAM)
                        .addCode("whereClause.add( $T.intGt(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec lte = MethodSpec.methodBuilder(fieldName + "Lte")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(INT_PARAM)
                        .addCode("whereClause.add( $T.intLte(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();
                MethodSpec lt = MethodSpec.methodBuilder(fieldName + "Lt")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(INT_PARAM)
                        .addCode("whereClause.add( $T.intLt(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec in = MethodSpec.methodBuilder(fieldName + "In")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(INT_LIST_PARAM)
                        .addCode("whereClause.add( $T.intIn(" + "\"" + column.getColumnName() + "\"" + ",list));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec notIn = MethodSpec.methodBuilder(fieldName + "NotIn")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(INT_LIST_PARAM)
                        .addCode("whereClause.add( $T.intNotIn(" + "\"" + column.getColumnName() + "\"" + ",list));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                builder.addMethods(Lists.newArrayList(equals, gte, gt, lte, lt, in, notIn));
            }

            if (aClass.equals(Long.class)) {
                MethodSpec equals = MethodSpec.methodBuilder(fieldName + "Equals")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(LONG_PARAM)
                        .addCode(
                                "whereClause.add( $T.longEquals(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                        + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec gte = MethodSpec.methodBuilder(fieldName + "Gte")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(LONG_PARAM)
                        .addCode("whereClause.add( $T.longGte(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec gt = MethodSpec.methodBuilder(fieldName + "Gt")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(LONG_PARAM)
                        .addCode("whereClause.add( $T.longGt(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec lte = MethodSpec.methodBuilder(fieldName + "Lte")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(LONG_PARAM)
                        .addCode("whereClause.add( $T.longLte(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();
                MethodSpec lt = MethodSpec.methodBuilder(fieldName + "Lt")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(LONG_PARAM)
                        .addCode("whereClause.add( $T.longLt(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec in = MethodSpec.methodBuilder(fieldName + "In")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(LONG_LIST_PARAM)
                        .addCode("whereClause.add( $T.longIn(" + "\"" + column.getColumnName() + "\"" + ",list));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec notIn = MethodSpec.methodBuilder(fieldName + "NotIn")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(LONG_LIST_PARAM)
                        .addCode("whereClause.add( $T.longNotIn(" + "\"" + column.getColumnName() + "\"" + ",list));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();
                builder.addMethods(Lists.newArrayList(equals, gte, gt, lte, lt, in, notIn));
            }

            if (aClass.equals(String.class)) {
                MethodSpec equals = MethodSpec.methodBuilder(fieldName + "Equals")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(STRING_PARAM)
                        .addCode("whereClause.add( $T.stringEquals(" + "\"" + column.getColumnName() + "\""
                                + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec gte = MethodSpec.methodBuilder(fieldName + "Gte")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(STRING_PARAM)
                        .addCode("whereClause.add( $T.stringGte(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec gt = MethodSpec.methodBuilder(fieldName + "Gt")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(STRING_PARAM)
                        .addCode("whereClause.add( $T.stringGt(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec lte = MethodSpec.methodBuilder(fieldName + "Lte")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(STRING_PARAM)
                        .addCode("whereClause.add( $T.stringLte(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();
                MethodSpec lt = MethodSpec.methodBuilder(fieldName + "Lt")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(STRING_PARAM)
                        .addCode("whereClause.add( $T.stringLt(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec in = MethodSpec.methodBuilder(fieldName + "In")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(STRING_LIST_PARAM)
                        .addCode("whereClause.add( $T.stringIn(" + "\"" + column.getColumnName() + "\"" + ",list));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec notIn = MethodSpec.methodBuilder(fieldName + "NotIn")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(STRING_LIST_PARAM)
                        .addCode(
                                "whereClause.add( $T.stringNotIn(" + "\"" + column.getColumnName() + "\"" + ",list));\n"
                                        + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();
                builder.addMethods(Lists.newArrayList(equals, gte, gt, lte, lt, in, notIn));
            }

            if (aClass.equals(Float.class)) {
                MethodSpec equals = MethodSpec.methodBuilder(fieldName + "Equals")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(FLOAT_PARAM)
                        .addCode("whereClause.add( $T.floatEquals(" + "\"" + column.getColumnName() + "\""
                                + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec gte = MethodSpec.methodBuilder(fieldName + "Gte")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(FLOAT_PARAM)
                        .addCode("whereClause.add( $T.floatGte(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec gt = MethodSpec.methodBuilder(fieldName + "Gt")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(FLOAT_PARAM)
                        .addCode("whereClause.add( $T.floatGt(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec lte = MethodSpec.methodBuilder(fieldName + "Lte")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(FLOAT_PARAM)
                        .addCode("whereClause.add( $T.floatLte(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();
                MethodSpec lt = MethodSpec.methodBuilder(fieldName + "Lt")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(FLOAT_PARAM)
                        .addCode("whereClause.add( $T.floatLt(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec in = MethodSpec.methodBuilder(fieldName + "In")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(FLOAT_LIST_PARAM)
                        .addCode("whereClause.add( $T.floatIn(" + "\"" + column.getColumnName() + "\"" + ",list));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec notIn = MethodSpec.methodBuilder(fieldName + "NotIn")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(FLOAT_LIST_PARAM)
                        .addCode(
                                "whereClause.add( $T.floatNotIn(" + "\"" + column.getColumnName() + "\"" + ",list));\n"
                                        + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();
                builder.addMethods(Lists.newArrayList(equals, gte, gt, lte, lt, in, notIn));
            }

            if (aClass.equals(Double.class)) {
                MethodSpec equals = MethodSpec.methodBuilder(fieldName + "Equals")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(DOUBLE_PARAM)
                        .addCode("whereClause.add( $T.doubleEquals(" + "\"" + column.getColumnName() + "\""
                                + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec gte = MethodSpec.methodBuilder(fieldName + "Gte")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(DOUBLE_PARAM)
                        .addCode("whereClause.add( $T.doubleGte(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec gt = MethodSpec.methodBuilder(fieldName + "Gt")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(DOUBLE_PARAM)
                        .addCode("whereClause.add( $T.doubleGt(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec lte = MethodSpec.methodBuilder(fieldName + "Lte")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(DOUBLE_PARAM)
                        .addCode("whereClause.add( $T.doubleLte(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();
                MethodSpec lt = MethodSpec.methodBuilder(fieldName + "Lt")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(DOUBLE_PARAM)
                        .addCode("whereClause.add( $T.doubleLt(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec in = MethodSpec.methodBuilder(fieldName + "In")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(DOUBLE_LIST_PARAM)
                        .addCode("whereClause.add( $T.doubleIn(" + "\"" + column.getColumnName() + "\"" + ",list));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec notIn = MethodSpec.methodBuilder(fieldName + "NotIn")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(DOUBLE_LIST_PARAM)
                        .addCode(
                                "whereClause.add( $T.doubleNotIn(" + "\"" + column.getColumnName() + "\"" + ",list));\n"
                                        + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();
                builder.addMethods(Lists.newArrayList(equals, gte, gt, lte, lt, in, notIn));
            }
        }


        return Collections.singletonMap(exampleSimpleName, builder.build());
    }

    public Map<String, TypeSpec> generateMapperInterface(Set<Table> tables, Map<String, TypeSpec> entityClassSpecs,
            Map<String, TypeSpec> queryExampleSpecs) {
        Map<String, TypeSpec> interfaceSpecs = buildMapperInterfaces(tables, entityClassSpecs, queryExampleSpecs);
        persistTypeSpec(this.configProperties.getMapperInterfaceGenPkg(), interfaceSpecs.values());
        return interfaceSpecs;

    }

    private Map<String, TypeSpec> buildMapperInterfaces(Set<Table> tables, Map<String, TypeSpec> entityClassSpecs,
            Map<String, TypeSpec> queryExampleSpecs) {
        Map<String, TypeSpec> entities = Maps.newHashMap();
        tables.forEach(t -> {
            String simpleClassName = mapUnderScoreToUpperCamelCase(t.getName());
            entities.putAll(buildMapperInterfaceForTable(t, entityClassSpecs.get(simpleClassName),
                    queryExampleSpecs.get(simpleClassName + "QueryExample")));
        });
        return entities;
    }


    private Map<String, TypeSpec> buildMapperInterfaceForTable(Table table, TypeSpec entityClassSpec,
            TypeSpec queryExampleSpec) {
        String interfaceName = entityClassSpec.name + "Mapper";

        Builder interfaceBuilder = TypeSpec.interfaceBuilder(interfaceName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Mapper.class)
                .addJavadoc(JAVA_DOC + LocalDateTime.now());

        TableColumn primaryKeyColumn = table.getPrimaryKeyColumn();
        String primaryKeyColumnName = primaryKeyColumn.getColumnName();

        ClassName entityClassName = ClassName.get(this.configProperties.getEntityGenPkg(), entityClassSpec.name);
        ClassName exampleClassName =
                ClassName.get(this.configProperties.getMapperInterfaceGenPkg(), queryExampleSpec.name);
        ClassName pageClassName = ClassName.get(this.configProperties.getMapperInterfaceGenPkg(), "Page");
        ClassName sortClassName = ClassName.get(this.configProperties.getMapperInterfaceGenPkg(), "Sort");

        ParameterSpec entityParamSpec =
                ParameterSpec.builder(entityClassName, mapUnderScoreToLowerCamelCase(table.getName()))
                        .addAnnotation(AnnotationSpec.builder(Param.class)
                                .addMember("value", "\"" + mapUnderScoreToLowerCamelCase(table.getName()) + "\"")
                                .build())
                        .build();

        ParameterSpec exampleParamSpec =
                ParameterSpec.builder(exampleClassName, "example")
                        .addAnnotation(
                                AnnotationSpec.builder(Param.class).addMember("value", "\"" + "example" + "\"").build())
                        .build();

        ParameterSpec sortParamSpec =
                ParameterSpec.builder(sortClassName, "sort")
                        .addAnnotation(
                                AnnotationSpec.builder(Param.class).addMember("value", "\"" + "sort" + "\"").build())
                        .build();

        ParameterSpec pageParamSpec =
                ParameterSpec.builder(pageClassName, "page")
                        .addAnnotation(
                                AnnotationSpec.builder(Param.class).addMember("value", "\"" + "page" + "\"").build())
                        .build();

        ParameterSpec pKeyParamSpec =
                ParameterSpec.builder(convertJDBCTypetoClass(primaryKeyColumn.getDataType()),
                                mapUnderScoreToLowerCamelCase(primaryKeyColumnName))
                        .addAnnotation(AnnotationSpec.builder(Param.class)
                                .addMember("value", "\"" + mapUnderScoreToLowerCamelCase(primaryKeyColumnName) + "\"")
                                .build())
                        .build();

        MethodSpec insert = MethodSpec.methodBuilder("insert" + entityClassSpec.name)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(entityParamSpec)
                .returns(Integer.class)
                .build();

        MethodSpec updateByPKey = MethodSpec.methodBuilder(
                        "update" + entityClassSpec.name + "By" + mapUnderScoreToUpperCamelCase(primaryKeyColumnName))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(pKeyParamSpec)
                .returns(Integer.class)
                .addParameter(entityParamSpec)
                .build();

        MethodSpec count = MethodSpec.methodBuilder("count" + entityClassSpec.name + "s")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(Integer.class)
                .addParameter(exampleParamSpec)
                .build();

        MethodSpec batchSelect = MethodSpec.methodBuilder("select" + entityClassSpec.name + "s")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), entityClassName))
                .addParameter(exampleParamSpec)
                .addParameter(pageParamSpec)
                .addParameter(sortParamSpec)
                .build();


        MethodSpec delete = MethodSpec.methodBuilder("delete" + entityClassSpec.name)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(exampleParamSpec)
                .returns(Integer.class)
                .build();

        List<MethodSpec> methodSpecs = Lists.newArrayList(
                insert,
                updateByPKey,
                count,
                batchSelect,
                delete
        );

        interfaceBuilder.addMethods(methodSpecs);
        return Collections.singletonMap(interfaceName, interfaceBuilder.build());
    }


    /**
     * simpleClassName:TypeSpec
     */
    public Map<String, TypeSpec> generateEntity(Set<Table> tables) {

        Map<String, TypeSpec> typeSpecs = buildEntities(tables);
        persistTypeSpec(this.configProperties.getEntityGenPkg(), typeSpecs.values());
        return typeSpecs;
    }

    private Map<String, TypeSpec> buildEntities(Set<Table> tables) {
        Map<String, TypeSpec> entities = Maps.newHashMap();
        tables.forEach(t -> entities.putAll(buildEntityForTable(t)));
        return entities;
    }

    private Map<String, TypeSpec> buildEntityForTable(Table table) {
        String simpleClassName = mapUnderScoreToUpperCamelCase(table.getName());
        Builder builder = TypeSpec
                .classBuilder(simpleClassName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Data.class)
                .addAnnotation(NoArgsConstructor.class)
                .addAnnotation(AllArgsConstructor.class)
                .addAnnotation(ToString.class)
                .addJavadoc(JAVA_DOC + LocalDateTime.now());

        Set<FieldSpec> fieldSpecs = new HashSet<>();
        Set<TableColumn> columns = table.getColumns();
        for (TableColumn column : columns) {
            boolean isPk = table.getPrimaryKeyColumn().getColumnName().equals(column.getColumnName());
            String fieldName = mapUnderScoreToLowerCamelCase(column.getColumnName());
            Class<?> fieldJavaType = convertJDBCTypetoClass(column.getDataType());
            FieldSpec build = FieldSpec.builder(fieldJavaType, fieldName, Modifier.PRIVATE)
                    .addJavadoc(CodeBlock.of(
                            (isPk ? "primaryKey | " : "") + "indexed:" + column.isIndexed() + " | "
                                    + "uniqIndexed:" + column.isUniqIndexed() + " | "
                                    + "size:" + column.getColumnSize() + " | " + "nullable:" + column.isNullable()
                                    + " | " + "autoIncrement:" + column.isAutoIncrement()))
                    .build();
            fieldSpecs.add(build);

        }
        builder.addFields(fieldSpecs);

        return Collections.singletonMap(simpleClassName, builder.build());
    }
}