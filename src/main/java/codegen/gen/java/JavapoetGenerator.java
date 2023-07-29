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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import com.squareup.javapoet.TypeName;
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
 * @date: 2023/6/25
 **/
public class JavapoetGenerator implements CodeGenerator {

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
        generateCriterion();
        Map<String, TypeSpec> queryExampleSpecs = generateQueryExample(tables);

        generateMapperInterface(tables, entityClassSpecs, queryExampleSpecs);

    }

    private void generateCriterion() {
        TypeSpec criterion = TypeSpec.classBuilder("Criterion")
                .addAnnotation(Data.class)
                .addField(FieldSpec.builder(String.class, "condition", Modifier.PRIVATE).build())
                .addField(FieldSpec.builder(Object.class, "value", Modifier.PRIVATE).build())
                .addField(FieldSpec.builder(Object.class, "secondValue", Modifier.PRIVATE).build())
                .addField(FieldSpec.builder(TypeName.BOOLEAN, "noValue", Modifier.PRIVATE).build())
                .addField(FieldSpec.builder(TypeName.BOOLEAN, "singleValue", Modifier.PRIVATE).build())
                .addField(FieldSpec.builder(TypeName.BOOLEAN, "betweenValue", Modifier.PRIVATE).build())
                .addField(FieldSpec.builder(TypeName.BOOLEAN, "listValue", Modifier.PRIVATE).build())
                .addField(FieldSpec.builder(String.class, "typeHandler", Modifier.PRIVATE).build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterSpec.builder(String.class, "condition").build())
                        .addStatement("    this.condition = condition;\n"
                                + "this.typeHandler = null;\n"
                                + "this.noValue = true")
                        .build()
                )
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterSpec.builder(String.class, "condition").build())
                        .addParameter(ParameterSpec.builder(Object.class, "value").build())
                        .addParameter(ParameterSpec.builder(String.class, "typeHandler").build())
                        .addStatement("    this.condition = condition;\n"
                                + "this.value = value;\n"
                                + "this.typeHandler = typeHandler;\n"
                                + "if (value instanceof $T) {\n"
                                + "    this.listValue = true;\n"
                                + "} else {\n"
                                + "    this.singleValue = true;\n"
                                + "}",List.class)
                        .build()
                )
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterSpec.builder(String.class, "condition").build())
                        .addParameter(ParameterSpec.builder(Object.class, "value").build())
                        .addParameter(ParameterSpec.builder(Object.class, "secondValue").build())
                        .addParameter(ParameterSpec.builder(String.class, "typeHandler").build())
                        .addStatement("    this.condition = condition;\n"
                                + "this.value = value;\n"
                                + "this.secondValue = secondValue;\n"
                                + "this.typeHandler = typeHandler;\n"
                                + "this.betweenValue = true")
                        .build()
                )
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterSpec.builder(String.class, "condition").build())
                        .addParameter(ParameterSpec.builder(Object.class, "value").build())
                        .addStatement("this(condition, value, null)")
                        .build()
                )
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterSpec.builder(String.class, "condition").build())
                        .addParameter(ParameterSpec.builder(Object.class, "value").build())
                        .addParameter(ParameterSpec.builder(Object.class, "secondValue").build())
                        .addStatement("this(condition, value, secondValue, null)")
                        .build()
                ).build();

        persistTypeSpec(this.configProperties.getMapperInterfaceGenPkg(),Collections.singletonList(criterion));

    }


    private void createDirsIfNecessary() throws MojoExecutionException {
        prepareDir(getAbsolutePathForPkg(this.configProperties.getBaseDir(), configProperties.getMapperInterfaceGenPkg()));
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

    private Map<String, TypeSpec> generateQueryExample(Set<Table> tables) {
        Map<String, TypeSpec> queryExampleSpecs = buildQueryExamples(tables);
        persistTypeSpec(this.configProperties.getMapperInterfaceGenPkg(), queryExampleSpecs.values());
        return queryExampleSpecs;
    }

    private Map<String, TypeSpec> buildQueryExamples(Set<Table> tables) {
        Map<String, TypeSpec> examples = Maps.newHashMap();
        tables.forEach(t -> {
            try {
                examples.putAll(buildQueryExampleForTable(t));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        return examples;
    }

    private Map<String, TypeSpec> buildQueryExampleForTable(Table table) throws ClassNotFoundException {
        String simpleClassName = mapUnderScoreToUpperCamelCase(table.getName())+"Example";

        ClassName criteria = ClassName.get(configProperties.getMapperInterfaceGenPkg()+"."+simpleClassName,"Criteria");
        ClassName criterion = ClassName.get(configProperties.getMapperInterfaceGenPkg(),"Criterion");
        ClassName thisClass = ClassName.get(configProperties.getMapperInterfaceGenPkg(),simpleClassName);

        List<MethodSpec> ms = new ArrayList<>();
        for (TableColumn column : table.getColumns()) {
            String upperCamelName = mapUnderScoreToUpperCamelCase(column.getColumnName());
            String lowerCamelName = mapUnderScoreToLowerCamelCase(column.getColumnName());
            Class<?> type = convertJDBCTypetoClass(column.getDataType());

            MethodSpec _1 = MethodSpec.methodBuilder("and" + upperCamelName + "IsNull")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(criteria)
                    .addStatement("    addCriterion(\" "+lowerCamelName+" is null \");\n"
                            + "return this")
                    .build();
            MethodSpec _2 = MethodSpec.methodBuilder("and" + upperCamelName + "IsNotNull")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(criteria)
                    .addStatement("    addCriterion( \" "+lowerCamelName+" is not null \");\n"
                            + "return this")
                    .build();
            MethodSpec _3 = MethodSpec.methodBuilder("and" + upperCamelName + "Equals")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(criteria)
                    .addParameter(ParameterSpec.builder(type,"value").build())
                    .addStatement("    addCriterion( \" "+lowerCamelName+" = \",value,\" "+lowerCamelName+" \" );\n"
                            + "return this")
                    .build();
            MethodSpec _4 = MethodSpec.methodBuilder("and" + upperCamelName + "NotEquals")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(criteria)
                    .addParameter(ParameterSpec.builder(type,"value").build())
                    .addStatement("    addCriterion(\" "+lowerCamelName+" <> \" ,value, \" "+lowerCamelName+" \" );\n"
                            + "return this")
                    .build();

            MethodSpec _5 = MethodSpec.methodBuilder("and" + upperCamelName + "Gt")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(criteria)
                    .addParameter(ParameterSpec.builder(type,"value").build())
                    .addStatement("    addCriterion(\" "+lowerCamelName+" > \" ,value, \" "+lowerCamelName+" \");\n"
                            + "return this")
                    .build();
            MethodSpec _6 = MethodSpec.methodBuilder("and" + upperCamelName + "Gte")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(criteria)
                    .addParameter(ParameterSpec.builder(type,"value").build())
                    .addStatement("    addCriterion(\" "+lowerCamelName+" >= \" ,value, \" "+lowerCamelName+" \" );\n"
                            + "return this")
                    .build();

            MethodSpec _7 = MethodSpec.methodBuilder("and" + upperCamelName + "Lt")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(criteria)
                    .addParameter(ParameterSpec.builder(type,"value").build())
                    .addStatement("    addCriterion(\" "+lowerCamelName+" < \" ,value, \" "+lowerCamelName+" \" );\n"
                            + "return this")
                    .build();
            MethodSpec _8 = MethodSpec.methodBuilder("and" + upperCamelName + "Lte")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(criteria)
                    .addParameter(ParameterSpec.builder(type,"value").build())
                    .addStatement("    addCriterion(\" "+lowerCamelName+" <= \" ,value, \" "+lowerCamelName+" \");\n"
                            + "return this")
                    .build();

            MethodSpec _9 = MethodSpec.methodBuilder("and" + upperCamelName + "In")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(criteria)
                    .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(type)),"values").build())
                    .addStatement("    addCriterion(\" "+lowerCamelName+" in \" ,values, \" "+lowerCamelName+" \");\n"
                            + "return this")
                    .build();

            MethodSpec _10 = MethodSpec.methodBuilder("and" + upperCamelName + "NotIn")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(criteria)
                    .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(type)),"values").build())
                    .addStatement("    addCriterion(\" "+lowerCamelName+" not in \" ,values, \" "+lowerCamelName+" \");\n"
                            + "return this")
                    .build();

            MethodSpec _11 = MethodSpec.methodBuilder("and" + upperCamelName + "Between")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(criteria)
                    .addParameter(ParameterSpec.builder(type,"value1").build())
                    .addParameter(ParameterSpec.builder(type,"value2").build())
                    .addStatement("    addCriterion(\" "+lowerCamelName+" between \" ,value1,value2, \" "+lowerCamelName+" \");\n"
                            + "return this")
                    .build();
            MethodSpec _12 = MethodSpec.methodBuilder("and" + upperCamelName + "NotBetween")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(criteria)
                    .addParameter(ParameterSpec.builder(type,"value1").build())
                    .addParameter(ParameterSpec.builder(type,"value2").build())
                    .addStatement("    addCriterion(\" "+lowerCamelName+" not between \" ,value1,value2, \" "+lowerCamelName+" \");\n"
                            + "return this")
                    .build();
            ms.addAll(Arrays.asList(_1,_2,_3,_4,_5,_6,_7,_8,_9,_10,_11,_12));
        }


        TypeSpec criteriaSpec = TypeSpec.classBuilder("Criteria")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addField(FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(List.class),criterion),"criteria",Modifier.PRIVATE).build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("criteria = new $T<>()",ArrayList.class)
                        .build())
                .addMethod(MethodSpec.methodBuilder("isValid")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeName.BOOLEAN)
                        .addStatement("return criteria!=null && criteria.size() > 0")
                        .build())
                .addMethod(MethodSpec.methodBuilder("getCriteria")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(ParameterizedTypeName.get(ClassName.get(List.class),criterion))
                        .addStatement("return criteria")
                        .build())
                .addMethod(MethodSpec.methodBuilder("addCriterion")
                        .addModifiers(Modifier.PRIVATE)
                        .returns(TypeName.VOID)
                        .addParameter(String.class,"condition")
                        .addStatement("    if (condition == null) {\n"
                                + "    throw new $T(\"Value for condition cannot be null \");\n"
                                + "}\n"
                                + "criteria.add(new $T(condition));",RuntimeException.class,criterion)
                        .build())
                .addMethod(MethodSpec.methodBuilder("addCriterion")
                        .addModifiers(Modifier.PRIVATE)
                        .returns(TypeName.VOID)
                        .addParameter(String.class,"condition")
                        .addParameter(Object.class,"value")
                        .addParameter(String.class,"property")
                        .addStatement("    if (value == null) {\n"
                                + "    throw new $T(\"Value for condition cannot be null \");\n"
                                + "}\n"
                                + "criteria.add(new $T(condition,value));",RuntimeException.class,criterion)
                        .build())
                .addMethod(MethodSpec.methodBuilder("addCriterion")
                        .addModifiers(Modifier.PRIVATE)
                        .returns(TypeName.VOID)
                        .addParameter(String.class,"condition")
                        .addParameter(Object.class,"value1")
                        .addParameter(Object.class,"value2")
                        .addParameter(String.class,"property")
                        .addStatement("    if (value1 == null || value2 == null) {\n"
                                + "    throw new $T(\"between values for condition cannot be null \");\n"
                                + "}\n"
                                + "criteria.add(new $T(condition,value1,value2));",RuntimeException.class,criterion)
                        .build())
                .addMethods(ms)
                .build();





        Builder builder = TypeSpec.classBuilder(simpleClassName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Getter.class)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value",CodeBlock.of("$S","all")).build())
                .addField(FieldSpec.builder(String.class,"orderByClause",Modifier.PRIVATE).build())
                .addField(FieldSpec.builder(TypeName.BOOLEAN,"distinct",Modifier.PRIVATE).build())
                .addField(FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(List.class),criteria),"oredCriteria",Modifier.PRIVATE).build())
                .addField(FieldSpec.builder(Integer.class,"limit",Modifier.PRIVATE).build())
                .addField(FieldSpec.builder(Integer.class,"offset",Modifier.PRIVATE).build())

                .addMethod(MethodSpec.methodBuilder("or")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(criteria)
                        .addStatement(
                                "    Criteria criteria = createCriteriaInternal();\n"
                                + "oredCriteria.add(criteria);\n"
                                + "return criteria")
                        .build())
                .addMethod(MethodSpec.methodBuilder("createCriteria")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(criteria)
                        .addStatement("    Criteria criteria = createCriteriaInternal();\n"
                                + "if (oredCriteria.size() == 0) {\n"
                                + "    oredCriteria.add(criteria);\n"
                                + "}\n"
                                + "return criteria")
                        .build())
                .addMethod(MethodSpec.methodBuilder("createCriteriaInternal")
                        .addModifiers(Modifier.PRIVATE)
                        .returns(criteria)
                        .addStatement("return new Criteria()")
                        .build())
                .addMethod(MethodSpec.methodBuilder("clear")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeName.VOID)
                        .addStatement("    oredCriteria.clear();\n"
                                + "orderByClause = null;\n"
                                + "distinct = false")
                        .build())
                .addMethod(MethodSpec.methodBuilder("setDistinct")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(thisClass)
                        .addParameter(TypeName.BOOLEAN,"distinct")
                        .addStatement("this.distinct = distinct;\n"
                                + "return this")
                        .build())
                .addMethod(MethodSpec.methodBuilder("setOrderByClause")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(thisClass)
                        .addParameter(String.class,"orderByClause")
                        .addStatement("    this.orderByClause = orderByClause;\n"
                                + "return this")
                        .build())
                .addMethod(MethodSpec.methodBuilder("setLimit")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(thisClass)
                        .addParameter(ParameterSpec.builder(Integer.class,"limit").build())
                        .addStatement("    this.limit = limit;\n"
                                + "return this")
                        .build())

                .addMethod(MethodSpec.methodBuilder("setOffset")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(thisClass)
                        .addParameter(ParameterSpec.builder(Integer.class,"offset").build())
                        .addStatement("    this.offset = offset;\n"
                                + "return this")
                        .build())

                .addType(criteriaSpec)
                ;





        return Collections.singletonMap(simpleClassName,builder.build());
    }

    @SuppressWarnings("all")
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
            if(entityClassSpecs.get(simpleClassName) !=null && queryExampleSpecs.get(simpleClassName + "Example")!=null){
                entities.putAll(buildMapperInterfaceForTable(t, entityClassSpecs.get(simpleClassName),
                        queryExampleSpecs.get(simpleClassName + "Example")));
            }
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

        ParameterSpec entityParamSpec =
                ParameterSpec.builder(entityClassName, mapUnderScoreToLowerCamelCase(table.getName()))
                        .addAnnotation(AnnotationSpec.builder(Param.class)
                                .addMember("value", "\"" + mapUnderScoreToLowerCamelCase(table.getName()) + "\"")
                                .build())
                        .build();

        ParameterSpec exampleParamSpec =
                ParameterSpec.builder(exampleClassName, "example")
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
