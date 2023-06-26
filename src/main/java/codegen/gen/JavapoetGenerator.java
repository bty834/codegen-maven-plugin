package codegen.gen;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import codegen.config.ConfigProperties;
import codegen.table.Table;
import codegen.table.TableColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 *
 *
 * @author: baotingyu
 * @date: 2023/6/25
 **/
public class JavapoetGenerator extends AbstractCodeGenerator{


    @Override
    public void generate(ConfigProperties configProperties,Set<Table> tables) throws MojoExecutionException {

        if(StringUtils.isBlank(configProperties.getEntityGenPkg()) ||
                StringUtils.isBlank(configProperties.getMapperInterfaceGenPkg()) ||
                StringUtils.isBlank(configProperties.getMapperXmlGenAbsPath()) ){
            throw  new MojoExecutionException("entityGenPkg or mapperInterfaceGenPkg or mapperXmlGenAbsPath blank");
        }
        this.configProperties = configProperties;
        createDirsIfNecessary();

        Map<String, TypeSpec> entityClassSpecs = generateEntity(tables);
        generatePageAndSort();
        Map<String, TypeSpec> queryExampleSpecs = generateQueryExample(tables,entityClassSpecs);
        Map<String, TypeSpec> interfaceSpecs = generateMapperInterface(tables, entityClassSpecs,queryExampleSpecs);

    }


    private void createDirsIfNecessary() throws MojoExecutionException {
        prepareDir(getAbsolutePathForPkg(this.configProperties.getBaseDir(), configProperties.getMapperInterfaceGenPkg()));
        prepareDir(getAbsolutePathForPkg(this.configProperties.getBaseDir(), this.configProperties.getEntityGenPkg()));
        prepareDir(this.configProperties.getMapperXmlGenAbsPath());
    }

    private void persistTypeSpec(String genPkg, Collection<TypeSpec> typeSpecs){
        typeSpecs.forEach(ts->{
            JavaFile file = JavaFile.builder(genPkg, ts).build();
            try {
                file.writeTo(Paths.get(getAbsolutePathForSrcMainJava(this.configProperties.getBaseDir())));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
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

        persistTypeSpec(this.configProperties.getMapperInterfaceGenPkg(),Lists.newArrayList(page,sort));
    }

    private Map<String, TypeSpec> generateQueryExample(Set<Table> tables, Map<String, TypeSpec> entityClassSpecs) {
        Map<String, TypeSpec> queryExampleSpecs = buildQueryExamples(tables,entityClassSpecs);
        persistTypeSpec(this.configProperties.getMapperInterfaceGenPkg(),queryExampleSpecs.values());
       return queryExampleSpecs;
    }

    private Map<String, TypeSpec> buildQueryExamples(Set<Table> tables, Map<String, TypeSpec> entityClassSpecs) {
        Map<String,TypeSpec> examples = Maps.newHashMap();
        tables.forEach(t->{
            String simpleClassName = mapUnderScoreToUpperCamelCase(t.getName());
            examples.putAll(buildQueryExampleForTable(t,entityClassSpecs.get(simpleClassName)));
        });
        return examples;
    }

    private Map<String, TypeSpec> buildQueryExampleForTable(Table table, TypeSpec entitySpec) {
        String exampleClassName = mapUnderScoreToUpperCamelCase(table.getName())+"QueryExample";
        Builder builder = TypeSpec
                .classBuilder(exampleClassName)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc(JAVA_DOC+LocalDateTime.now());
        Set<FieldSpec> fieldSpecs = new HashSet<>();

        return Collections.singletonMap(exampleClassName,builder.build());
    }


    public Map<String, TypeSpec> generateMapperInterface(Set<Table> tables,Map<String, TypeSpec> entityClassSpecs,Map<String, TypeSpec> queryExampleSpecs) {
        Map<String, TypeSpec> interfaceSpecs = buildMapperInterfaces(tables,entityClassSpecs,queryExampleSpecs);
        persistTypeSpec(this.configProperties.getMapperInterfaceGenPkg(),interfaceSpecs.values());
        return interfaceSpecs;

    }

    private Map<String, TypeSpec> buildMapperInterfaces(Set<Table> tables,Map<String, TypeSpec> entityClassSpecs,Map<String, TypeSpec> queryExampleSpecs){
        Map<String,TypeSpec> entities = Maps.newHashMap();
        tables.forEach(t->{
            String simpleClassName = mapUnderScoreToUpperCamelCase(t.getName());
            entities.putAll(buildMapperInterfaceForTable(t,entityClassSpecs.get(simpleClassName),queryExampleSpecs.get(simpleClassName+"QueryExample")));
        });
        return entities;
    }


    private Map<String, TypeSpec> buildMapperInterfaceForTable(Table table,TypeSpec entityClassSpec,TypeSpec queryExampleSpec){
        String interfaceName = entityClassSpec.name + "Mapper";

        Builder interfaceBuilder = TypeSpec.interfaceBuilder(interfaceName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Mapper.class)
                .addJavadoc(JAVA_DOC+ LocalDateTime.now());

        TableColumn primaryKeyColumn = table.getPrimaryKeyColumn();
        String primaryKeyColumnName = primaryKeyColumn.getColumnName();

        ClassName entityClassName = ClassName.get(this.configProperties.getEntityGenPkg(), entityClassSpec.name);
        ClassName exampleClassName = ClassName.get(this.configProperties.getMapperInterfaceGenPkg(), queryExampleSpec.name);
        ClassName pageClassName = ClassName.get(this.configProperties.getMapperInterfaceGenPkg(),"Page");
        ClassName sortClassName = ClassName.get(this.configProperties.getMapperInterfaceGenPkg(),"Sort");

        ParameterSpec entityParamSpec =
                ParameterSpec.builder(entityClassName, mapUnderScoreToLowerCamelCase(table.getName()))
                        .addAnnotation(AnnotationSpec.builder(Param.class).addMember("value","\""+mapUnderScoreToLowerCamelCase(table.getName())+"\"").build())
                        .build();

        ParameterSpec exampleParamSpec =
                ParameterSpec.builder(exampleClassName, "example")
                        .addAnnotation(AnnotationSpec.builder(Param.class).addMember("value","\""+"example"+"\"").build())
                        .build();

        ParameterSpec sortParamSpec =
                ParameterSpec.builder(sortClassName,"sort")
                        .addAnnotation(AnnotationSpec.builder(Param.class).addMember("value","\""+"sort"+"\"").build())
                        .build();

        ParameterSpec pageParamSpec =
                ParameterSpec.builder(pageClassName,"page")
                        .addAnnotation(AnnotationSpec.builder(Param.class).addMember("value","\""+"page"+"\"").build())
                        .build();

        ParameterSpec limitParamSpec =
                ParameterSpec.builder(Integer.class,"limit")
                        .addAnnotation(AnnotationSpec.builder(Param.class).addMember("value","\""+"limit"+"\"").build())
                        .build();

        ParameterSpec pKeyParamSpec =
                ParameterSpec.builder(convertJDBCTypetoClass(primaryKeyColumn.getDataType()), mapUnderScoreToLowerCamelCase(primaryKeyColumnName))
                        .addAnnotation(AnnotationSpec.builder(Param.class).addMember("value","\""+mapUnderScoreToLowerCamelCase(primaryKeyColumnName)+"\"").build())
                        .build();

        ParameterSpec batchEntityParamSpec =
                ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(List.class), entityClassName), "list")
                        .addAnnotation(AnnotationSpec.builder(Param.class).addMember("value","\""+"list"+"\"").build())
                        .build();

        ParameterSpec batchPKeyParamSpec =
                ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(List.class), entityClassName),"list")
                        .addAnnotation(AnnotationSpec.builder(Param.class).addMember("value","\""+"list"+"\"").build())
                        .build();

        ParameterSpec pKey2EntityMapParamSpec =
                ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(Map.class),ClassName.get(convertJDBCTypetoClass(primaryKeyColumn.getDataType())),entityClassName),"map")
                        .addAnnotation(AnnotationSpec.builder(Param.class).addMember("value","\""+"map"+"\"").build())
                        .build();

        MethodSpec insert = MethodSpec.methodBuilder("insert" + entityClassSpec.name)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(entityParamSpec)
                .returns(Integer.class)
                .build();

        MethodSpec batchInsert = MethodSpec.methodBuilder("batchInsert" + entityClassSpec.name +"s")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(batchEntityParamSpec)
                .returns(Integer.class)
                .build();

        MethodSpec updateByPKey = MethodSpec.methodBuilder("update" + entityClassSpec.name+"By"+mapUnderScoreToUpperCamelCase(primaryKeyColumnName))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(pKeyParamSpec)
                .returns(Integer.class)
                .addParameter(entityParamSpec)
                .build();

        MethodSpec batchUpdateByPKey = MethodSpec.methodBuilder("batchUpdate" + entityClassSpec.name+"By"+mapUnderScoreToUpperCamelCase(primaryKeyColumnName)+"s")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(pKey2EntityMapParamSpec)
                .returns(Integer.class)
                .build();


        MethodSpec count = MethodSpec.methodBuilder("count" + entityClassSpec.name+"s")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(Integer.class)
                .addParameter(exampleParamSpec)
                .build();

        MethodSpec batchSelect = MethodSpec.methodBuilder("select" + entityClassSpec.name+"s")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class),entityClassName))
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


        MethodSpec batchDeleteByPKey = MethodSpec.methodBuilder("batchDelete" + entityClassSpec.name+"By"+mapUnderScoreToUpperCamelCase(primaryKeyColumnName)+"s")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(batchPKeyParamSpec)
                .returns(Integer.class)
                .build();

        List<MethodSpec> methodSpecs = Lists.newArrayList(
                insert,
                batchInsert,
                updateByPKey,
                batchUpdateByPKey,
                count,
                batchSelect,
                delete,
                batchDeleteByPKey
        );

        interfaceBuilder.addMethods(methodSpecs);
        return Collections.singletonMap(interfaceName,interfaceBuilder.build());
    }


    /**
     * simpleClassName:TypeSpec
     */
    public Map<String, TypeSpec>  generateEntity(Set<Table> tables) {

        Map<String, TypeSpec> typeSpecs = buildEntities(tables);
        persistTypeSpec(this.configProperties.getEntityGenPkg(),typeSpecs.values());
        return typeSpecs;
    }

    private Map<String,TypeSpec> buildEntities(Set<Table> tables) {
        Map<String,TypeSpec> entities = Maps.newHashMap();
        tables.forEach(t->entities.putAll(buildEntityForTable(t)));
        return entities;
    }

    private Map<String,TypeSpec> buildEntityForTable(Table table){
        String simpleClassName = mapUnderScoreToUpperCamelCase(table.getName());
        Builder builder = TypeSpec
                .classBuilder(simpleClassName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Data.class)
                .addAnnotation(NoArgsConstructor.class)
                .addAnnotation(AllArgsConstructor.class)
                .addAnnotation(ToString.class)
                .addJavadoc(JAVA_DOC+LocalDateTime.now());

        Set<FieldSpec> fieldSpecs = new HashSet<>();
//        Set<MethodSpec> getters = new HashSet<>();
//        Set<MethodSpec> setters = new HashSet<>();

        Set<TableColumn> columns = table.getColumns();
        for (TableColumn column : columns) {

            String fieldName = mapUnderScoreToLowerCamelCase(column.getColumnName());
            Class<?> fieldJavaType = convertJDBCTypetoClass(column.getDataType());
            FieldSpec build = FieldSpec.builder(fieldJavaType, fieldName, Modifier.PRIVATE)
                    .addJavadoc(CodeBlock.of("indexed:"+column.isIndexed()+" | "+"uniqIndexed:"+column.isUniqIndexed()+" | "+"size:"+column.getColumnSize()+" | "+"nullable:"+column.isNullable()+" | "+"autoIncrement:"+column.isAutoIncrement()))
                    .build();
            fieldSpecs.add(build);

//            MethodSpec getter = MethodSpec.methodBuilder(getterMethodNameFromColumnName(column.getColumnName()))
//                    .addModifiers(Modifier.PUBLIC).returns(fieldJavaType)
//                    .addStatement("return this."+fieldName)
//                    .build();
//            getters.add(getter);
//
//            MethodSpec setter = MethodSpec.methodBuilder(setterMethodNameFromColumnName(column.getColumnName()))
//                    .addModifiers(Modifier.PUBLIC).returns(TypeName.VOID)
//                    .addParameter(fieldJavaType, fieldName)
//                    .addStatement("this." + fieldName+" = "+fieldName)
//                    .build();
//            setters.add(setter);
        }
        builder.addFields(fieldSpecs);
//        builder.addMethods(getters);
//        builder.addMethods(setters);
        return Collections.singletonMap(simpleClassName,builder.build());
    }
}
