package codegen.gen;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
public class JavapoetGenerator extends AbstractCodeGenerator {


    @Override
    public void generate(ConfigProperties configProperties, Set<Table> tables) throws MojoExecutionException {

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
        String conditionUtilClassName = "ConditionUtil";

        Builder builder = TypeSpec
                .classBuilder(conditionUtilClassName)
                .addModifiers(Modifier.PUBLIC);

        ParameterSpec columnSpec = ParameterSpec.builder(String.class, "column").build();
        ParameterSpec intSpec = ParameterSpec.builder(Integer.class, "value").build();
        ParameterSpec longSpec = ParameterSpec.builder(Long.class, "value").build();
        ParameterSpec stringSpec = ParameterSpec.builder(String.class, "value").build();
        ParameterSpec dateSpec = ParameterSpec.builder(Date.class, "value").build();
        ParameterSpec dateTimeSpec = ParameterSpec.builder(Time.class, "value").build();

        String inStatement = "   List<String> collect = in.stream().map(String::valueOf).collect($T.toList());\n"
                + "        String join = String.join(\",\", collect);\n"
                + "        return column+\" in (\"+join+\")\";";
        String notInStatement = "   List<String> collect = in.stream().map(String::valueOf).collect($T.toList());\n"
                + "        String join = String.join(\",\", collect);\n"
                + "        return column+\" not in (\"+join+\")\";";

        MethodSpec intGte = MethodSpec.methodBuilder("intGte")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(intSpec)
                .addStatement("return column + \">=\" + value")
                .returns(String.class).build();
        MethodSpec intGt = MethodSpec.methodBuilder("intGt")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(intSpec)
                .addStatement("return column + \">\" + value")
                .returns(String.class).build();
        MethodSpec intLte = MethodSpec.methodBuilder("intLte")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(intSpec)
                .addStatement("return column + \"<=\" + value")
                .returns(String.class).build();
        MethodSpec intLt = MethodSpec.methodBuilder("intLt")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(intSpec)
                .addStatement("return column + \"<\" + value")
                .returns(String.class).build();
        MethodSpec intEquals = MethodSpec.methodBuilder("intEquals")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(intSpec)
                .addStatement("return column+\"=\"+value")
                .returns(String.class).build();
        MethodSpec intIn = MethodSpec.methodBuilder("intIn")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(Integer.class)), "in")
                .addCode(CodeBlock.builder().add(inStatement, Collectors.class).build())
                .returns(String.class).build();
        MethodSpec intNotIn = MethodSpec.methodBuilder("intNotIn")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(Integer.class)), "in")
                .addCode(CodeBlock.builder().add(notInStatement, Collectors.class).build())
                .returns(String.class).build();

        MethodSpec longGte = MethodSpec.methodBuilder("longGte")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(longSpec)
                .addStatement("return column + \">=\" + value")
                .returns(String.class).build();
        MethodSpec longGt = MethodSpec.methodBuilder("longGt")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(longSpec)
                .addStatement("return column + \">\" + value")
                .returns(String.class).build();
        MethodSpec longLte = MethodSpec.methodBuilder("longLte")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(longSpec)
                .addStatement("return column + \"<=\" + value")
                .returns(String.class).build();
        MethodSpec longLt = MethodSpec.methodBuilder("longLt")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(longSpec)
                .addStatement("return column + \"<\" + value")
                .returns(String.class).build();
        MethodSpec longEquals = MethodSpec.methodBuilder("longEquals")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(longSpec)
                .addStatement("return column+\"=\"+value")
                .returns(String.class).build();
        MethodSpec longIn = MethodSpec.methodBuilder("longIn")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(Long.class)), "in")
                .addCode(CodeBlock.builder().add(inStatement, Collectors.class).build())
                .returns(String.class).build();
        MethodSpec longNotIn = MethodSpec.methodBuilder("longNotIn")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(Long.class)), "in")
                .addCode(CodeBlock.builder().add(notInStatement, Collectors.class).build())
                .returns(String.class).build();


        MethodSpec stringGte = MethodSpec.methodBuilder("stringGte")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(stringSpec)
                .addStatement("return column + \">=\" + value")
                .returns(String.class).build();
        MethodSpec stringGt = MethodSpec.methodBuilder("stringGt")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(stringSpec)
                .addStatement("return column + \">\" + value")
                .returns(String.class).build();
        MethodSpec stringLte = MethodSpec.methodBuilder("stringLte")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(stringSpec)
                .addStatement("return column + \"<=\" + value")
                .returns(String.class).build();
        MethodSpec stringLt = MethodSpec.methodBuilder("stringLt")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(stringSpec)
                .addStatement("return column + \"<\" + value")
                .returns(String.class).build();
        MethodSpec stringEquals = MethodSpec.methodBuilder("stringEquals")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(stringSpec)
                .addStatement("return column+\"=\"+value")
                .returns(String.class).build();
        MethodSpec stringIn = MethodSpec.methodBuilder("stringIn")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class)), "in")
                .addCode(CodeBlock.builder().add(inStatement, Collectors.class).build())
                .returns(String.class).build();
        MethodSpec stringNotIn = MethodSpec.methodBuilder("stringNotIn")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(columnSpec)
                .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class)), "in")
                .addCode(CodeBlock.builder().add(notInStatement, Collectors.class).build())
                .returns(String.class).build();

        ArrayList<MethodSpec> methodSpecs = Lists.newArrayList(
                intGte, intGt, intLte, intLt, intEquals, intIn, intNotIn,
                longGte, longGt, longLte, longLt, longEquals, longIn, longNotIn,
                stringGte, stringGt, stringLte, stringLt, stringEquals, stringIn, stringNotIn
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
                .addField(FieldSpec.builder(Integer.class, "pageNum", Modifier.PRIVATE).build())
                .addField(FieldSpec.builder(Integer.class, "pageSize", Modifier.PRIVATE).build())
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

        MethodSpec nonArgsConstructor = MethodSpec.methodBuilder(exampleSimpleName)
                .addModifiers(Modifier.PRIVATE)
                .build();
        MethodSpec newExample = MethodSpec.methodBuilder("newExample")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addCode(CodeBlock.of("return new " + exampleSimpleName + "();"))
                .returns(exampleClassName)
                .build();
        builder.addMethod(nonArgsConstructor);
        builder.addMethod(newExample);

        Set<TableColumn> columns = table.getColumns();

        ParameterSpec intParamSpec = ParameterSpec.builder(Integer.class, "value").build();
        ParameterSpec longParamSpec = ParameterSpec.builder(Long.class, "value").build();
        ParameterSpec stringParamSpec = ParameterSpec.builder(String.class, "value").build();


        ParameterSpec intListParamSpec =
                ParameterSpec.builder(ParameterizedTypeName.get(List.class, Integer.class), "list").build();
        ParameterSpec longListParamSpec =
                ParameterSpec.builder(ParameterizedTypeName.get(List.class, Long.class), "list").build();
        ParameterSpec stringListParamSpec =
                ParameterSpec.builder(ParameterizedTypeName.get(List.class, String.class), "list").build();

        for (TableColumn column : columns) {
            String fieldName = mapUnderScoreToLowerCamelCase(column.getColumnName());
            Class<?> aClass = convertJDBCTypetoClass(column.getDataType());

            if (aClass.equals(Integer.class)) {
                MethodSpec equals = MethodSpec.methodBuilder(fieldName + "Equals")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(intParamSpec)
                        .addCode("whereClause.add( $T.intEquals(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec gte = MethodSpec.methodBuilder(fieldName + "Gte")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(intParamSpec)
                        .addCode("whereClause.add( $T.intGte(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec gt = MethodSpec.methodBuilder(fieldName + "Gt")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(intParamSpec)
                        .addCode("whereClause.add( $T.intGt(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec lte = MethodSpec.methodBuilder(fieldName + "Lte")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(intParamSpec)
                        .addCode("whereClause.add( $T.intLte(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();
                MethodSpec lt = MethodSpec.methodBuilder(fieldName + "Lt")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(intParamSpec)
                        .addCode("whereClause.add( $T.intLt(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec in = MethodSpec.methodBuilder(fieldName + "In")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(intListParamSpec)
                        .addCode("whereClause.add( $T.intIn(" + "\"" + column.getColumnName() + "\"" + ",list));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec notIn = MethodSpec.methodBuilder(fieldName + "NotIn")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(intListParamSpec)
                        .addCode("whereClause.add( $T.intNotIn(" + "\"" + column.getColumnName() + "\"" + ",list));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                builder.addMethods(Lists.newArrayList(equals,gte,gt,lte,lt,in,notIn));
            }

            if (aClass.equals(Long.class)) {
                MethodSpec equals = MethodSpec.methodBuilder(fieldName + "Equals")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(longParamSpec)
                        .addCode(
                                "whereClause.add( $T.longEquals(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                        + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec gte = MethodSpec.methodBuilder(fieldName + "Gte")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(longParamSpec)
                        .addCode("whereClause.add( $T.longGte(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec gt = MethodSpec.methodBuilder(fieldName + "Gt")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(longParamSpec)
                        .addCode("whereClause.add( $T.longGt(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec lte = MethodSpec.methodBuilder(fieldName + "Lte")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(longParamSpec)
                        .addCode("whereClause.add( $T.longLte(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();
                MethodSpec lt = MethodSpec.methodBuilder(fieldName + "Lt")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(longParamSpec)
                        .addCode("whereClause.add( $T.longLt(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec in = MethodSpec.methodBuilder(fieldName + "In")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(longListParamSpec)
                        .addCode("whereClause.add( $T.longIn(" + "\"" + column.getColumnName() + "\"" + ",list));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec notIn = MethodSpec.methodBuilder(fieldName + "NotIn")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(longListParamSpec)
                        .addCode("whereClause.add( $T.longNotIn(" + "\"" + column.getColumnName() + "\"" + ",list));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();
                builder.addMethods(Lists.newArrayList(equals,gte,gt,lte,lt,in,notIn));
            }

            if (aClass.equals(String.class)) {
                MethodSpec equals = MethodSpec.methodBuilder(fieldName + "Equals")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(stringParamSpec)
                        .addCode("whereClause.add( $T.stringEquals(" + "\"" + column.getColumnName() + "\""
                                + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec gte = MethodSpec.methodBuilder(fieldName + "Gte")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(stringParamSpec)
                        .addCode("whereClause.add( $T.stringGte(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec gt = MethodSpec.methodBuilder(fieldName + "Gt")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(stringParamSpec)
                        .addCode("whereClause.add( $T.stringGt(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec lte = MethodSpec.methodBuilder(fieldName + "Lte")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(stringParamSpec)
                        .addCode("whereClause.add( $T.stringLte(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();
                MethodSpec lt = MethodSpec.methodBuilder(fieldName + "Lt")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(stringParamSpec)
                        .addCode("whereClause.add( $T.stringLt(" + "\"" + column.getColumnName() + "\"" + ",value));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec in = MethodSpec.methodBuilder(fieldName + "In")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(stringListParamSpec)
                        .addCode("whereClause.add( $T.stringIn(" + "\"" + column.getColumnName() + "\"" + ",list));\n"
                                + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();

                MethodSpec notIn = MethodSpec.methodBuilder(fieldName + "NotIn")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(stringListParamSpec)
                        .addCode(
                                "whereClause.add( $T.stringNotIn(" + "\"" + column.getColumnName() + "\"" + ",list));\n"
                                        + "return this;", conditionUtilClassName)
                        .returns(exampleClassName)
                        .build();
                builder.addMethods(Lists.newArrayList(equals,gte,gt,lte,lt,in,notIn));
            }

            if(aClass.equals(Float.class)){
                // TODO
            }

            if(aClass.equals(Double.class)){
                // TODO
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

        ParameterSpec limitParamSpec =
                ParameterSpec.builder(Integer.class, "limit")
                        .addAnnotation(
                                AnnotationSpec.builder(Param.class).addMember("value", "\"" + "limit" + "\"").build())
                        .build();

        ParameterSpec pKeyParamSpec =
                ParameterSpec.builder(convertJDBCTypetoClass(primaryKeyColumn.getDataType()),
                                mapUnderScoreToLowerCamelCase(primaryKeyColumnName))
                        .addAnnotation(AnnotationSpec.builder(Param.class)
                                .addMember("value", "\"" + mapUnderScoreToLowerCamelCase(primaryKeyColumnName) + "\"")
                                .build())
                        .build();

        ParameterSpec batchEntityParamSpec =
                ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(List.class), entityClassName), "list")
                        .addAnnotation(
                                AnnotationSpec.builder(Param.class).addMember("value", "\"" + "list" + "\"").build())
                        .build();

        ParameterSpec pKey2EntityMapParamSpec =
                ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(Map.class),
                                ClassName.get(convertJDBCTypetoClass(primaryKeyColumn.getDataType())),
                                entityClassName), "map")
                        .addAnnotation(
                                AnnotationSpec.builder(Param.class).addMember("value", "\"" + "map" + "\"").build())
                        .build();

        MethodSpec insert = MethodSpec.methodBuilder("insert" + entityClassSpec.name)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(entityParamSpec)
                .returns(Integer.class)
                .build();

        MethodSpec batchInsert = MethodSpec.methodBuilder("batchInsert" + entityClassSpec.name + "s")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(batchEntityParamSpec)
                .returns(Integer.class)
                .build();

        MethodSpec updateByPKey = MethodSpec.methodBuilder(
                        "update" + entityClassSpec.name + "By" + mapUnderScoreToUpperCamelCase(primaryKeyColumnName))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(pKeyParamSpec)
                .returns(Integer.class)
                .addParameter(entityParamSpec)
                .build();

        MethodSpec batchUpdateByPKey = MethodSpec.methodBuilder(
                        "batchUpdate" + entityClassSpec.name + "By" + mapUnderScoreToUpperCamelCase(primaryKeyColumnName) + "s")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(pKey2EntityMapParamSpec)
                .returns(Integer.class)
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
                .addParameter(limitParamSpec)
                .build();


        MethodSpec delete = MethodSpec.methodBuilder("delete" + entityClassSpec.name)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(exampleParamSpec)
                .returns(Integer.class)
                .build();

        List<MethodSpec> methodSpecs = Lists.newArrayList(
                insert,
                batchInsert,
                updateByPKey,
                batchUpdateByPKey,
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
                    .addJavadoc(CodeBlock.of( (isPk?"primaryKey":"")+" | "+"indexed:" + column.isIndexed() + " | " + "uniqIndexed:" + column.isUniqIndexed() + " | "
                                    + "size:" + column.getColumnSize() + " | " + "nullable:" + column.isNullable()
                                    + " | " + "autoIncrement:" + column.isAutoIncrement()))
                    .build();
            fieldSpecs.add(build);

        }
        builder.addFields(fieldSpecs);

        return Collections.singletonMap(simpleClassName, builder.build());
    }
}
