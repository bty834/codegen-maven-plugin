package codegen.gen.xml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.plugin.MojoExecutionException;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import codegen.ConfigProperties;
import codegen.gen.CommonUtil;
import codegen.table.Table;
import codegen.table.TableColumn;
import edu.emory.mathcs.backport.java.util.Collections;

/**
 * TODO 类描述
 *
 * @author: baotingyu
 * @date: 2023/6/27
 **/
public class JDomXMLGenerator implements XMLGenerator{

    ConfigProperties configProperties;

    @Override
    public void generate(ConfigProperties configProperties, Set<Table> tables) throws MojoExecutionException {
        if(Objects.isNull(tables) || tables.size()==0){
            return;
        }
        this.configProperties = configProperties;

        Map<String,Document> xmlList = new HashMap<>();

        tables.forEach(table-> xmlList.putAll(createXMLForTable(table)));

        persistXML(xmlList);


    }

    private void persistXML(Map<String,Document> name2XML) {
        XMLOutputter out = new XMLOutputter() ;
        Format format = Format.getPrettyFormat();
        out.setFormat(format);
        name2XML.forEach((name, xml) -> {
            try {
                out.output(xml, Files.newOutputStream(Paths.get(configProperties.getMapperXmlGenAbsPath()+ (CommonUtil.isWin()?"\\":"/"+name+".xml"))));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public Map<String,Document> createXMLForTable(Table table){

        String xmlName = CommonUtil.mapUnderScoreToUpperCamelCase(table.getName()) + "Mapper";

        Document xml = new Document();

        DocType mybatisDocType = new DocType("mapper", "-//mybatis.org//DTD Mapper 3.0//EN",
                "http://mybatis.org/dtd/mybatis-3-mapper.dtd");
        xml.setDocType(mybatisDocType);

        Element mapper = new Element("mapper");
        mapper.setAttribute("namespace", configProperties.getMapperInterfaceGenPkg()+"."+xmlName);
        xml.addContent(mapper);

        Element insert = insert(table);
        mapper.addContent(insert);

        Element batchInsert = batchInsert(table);
        mapper.addContent(batchInsert);

        Element update =  update(table);
        mapper.addContent(update);

        Element count = count(table);
        mapper.addContent(count);

        Element select = select(table);
        mapper.addContent(select);

        Element resultMap = resultMap(table);
        mapper.addContent(resultMap);

        Element delete = delete(table);
        mapper.addContent(delete);

        return Collections.singletonMap(xmlName,xml);
    }

    private Element delete(Table table) {

        Element delete = new Element("delete");
        delete.setAttribute("id","delete"+CommonUtil.mapUnderScoreToUpperCamelCase(table.getName()));
        delete.setAttribute("parameterType", configProperties.getMapperInterfaceGenPkg()+"."+CommonUtil.mapUnderScoreToUpperCamelCase(table.getName())+"QueryExample");

        delete.addContent(" delete from "+table.getName());
        delete.addContent(XmlElementUtil.example());
        return delete;
    }


    private Element select(Table table) {
        Element select = new Element("select");

        select.setAttribute("id","select"+CommonUtil.mapUnderScoreToUpperCamelCase(table.getName())+"s");
        select.setAttribute("resultMap","BaseResultMap");

        List<String> columnNames = table.getColumns().stream().map(TableColumn::getColumnName).collect(Collectors.toList());

        select.addContent("select "+ String.join(",",columnNames)+" from "+table.getName());
        select.addContent(XmlElementUtil.example());

        Element orderIf = new Element("if");
        orderIf.setAttribute("test","sort!=null and sort.fieldName != null");

        Element orderIfAsc = new Element("if");
        orderIfAsc.setAttribute("test"," page.isAsc=null or page.isAsc=true ");
        orderIfAsc.addContent(" order by sort.fieldName asc ");
        orderIf.addContent(orderIfAsc);

        Element orderIfDesc = new Element("if");
        orderIfDesc.setAttribute("test"," page.isAsc!=null or page.isAsc=false ");
        orderIfDesc.addContent(" order by sort.fieldName desc ");
        orderIf.addContent(orderIfDesc);

        select.addContent(orderIf);


        Element ifEle = new Element("if");
        ifEle.setAttribute("test", "page!=null and  page.limit !=null");

        Element offsetIfNull = new Element("if");
        offsetIfNull.setAttribute("test"," page.offset=null");
        offsetIfNull.addContent("limit ${page.limit}");
        ifEle.addContent(offsetIfNull);

        Element offsetIfNotNull = new Element("if");
        offsetIfNotNull.setAttribute("test"," page.offset!=null");
        offsetIfNotNull.addContent("limit ${page.offset},${page.limit}");
        ifEle.addContent(offsetIfNotNull);

        select.addContent(ifEle);


        return select;
    }

    private Element resultMap(Table table){
        Element resultMap = new Element("resultMap");
        resultMap.setAttribute("id","BaseResultMap");
        resultMap.setAttribute("type", configProperties.getEntityGenPkg()+"."+CommonUtil.mapUnderScoreToUpperCamelCase(table.getName()));

        TableColumn pkColumn = table.getPrimaryKeyColumn();
        if(Objects.nonNull(pkColumn)){
            Element id = new Element("id");
            id.setAttribute("column", pkColumn.getColumnName());
            id.setAttribute("property",CommonUtil.mapUnderScoreToLowerCamelCase(pkColumn.getColumnName()));
            resultMap.addContent(id);
        }

        table.getColumns().forEach(c->{
            if(Objects.nonNull(pkColumn) && c.equals(pkColumn)){
                return;
            }
            Element result = new Element("result");
            result.setAttribute("column",c.getColumnName());
            result.setAttribute("property",CommonUtil.mapUnderScoreToLowerCamelCase(c.getColumnName()));
            resultMap.addContent(result);
        });
        return resultMap;
    }


    private Element count(Table table) {
        Element select = new Element("select");

        select.setAttribute("id","count"+CommonUtil.mapUnderScoreToUpperCamelCase(table.getName())+"s");
        select.setAttribute("parameterType", configProperties.getMapperInterfaceGenPkg()+"."+CommonUtil.mapUnderScoreToUpperCamelCase(table.getName())+"QueryExample");
        select.setAttribute("resultType","integer");

        select.addContent("count(1) from "+ table.getName());
        select.addContent(XmlElementUtil.example());

        return select;
    }


    private Element update(Table table) {
        Element update = new Element("update");
        update.setAttribute("id","update"+
                CommonUtil.mapUnderScoreToUpperCamelCase(table.getName())+"By"+
                CommonUtil.mapUnderScoreToUpperCamelCase(table.getPrimaryKeyColumn().getColumnName()));
        update.addContent(" update "+table.getName());
        List<String> columnNames = table.getColumns().stream().map(TableColumn::getColumnName).collect(Collectors.toList());
        update.addContent(XmlElementUtil.updateSetOfIfList(CommonUtil.mapUnderScoreToLowerCamelCase(table.getName()),columnNames));
        String pkColumnName = table.getPrimaryKeyColumn().getColumnName();
        update.addContent(" where " + pkColumnName + " = #{" + CommonUtil.mapUnderScoreToLowerCamelCase(table.getName())+"."+CommonUtil.mapUnderScoreToLowerCamelCase(pkColumnName)+"}");
        return update;
    }

    private Element insert(Table table){
        Element insert = new Element("insert");
        insert.setAttribute("id","insert"+CommonUtil.mapUnderScoreToUpperCamelCase(table.getName()));
        insert.setAttribute("parameterType", configProperties.getEntityGenPkg()+"."+CommonUtil.mapUnderScoreToUpperCamelCase(table.getName()));
        insert.addContent(" insert into "+ table.getName());
        List<String> columnNames = table.getColumns().stream().map(TableColumn::getColumnName).collect(Collectors.toList());
        insert.addContent(XmlElementUtil.insertColumnTrimOfIfList(CommonUtil.mapUnderScoreToLowerCamelCase(table.getName()),columnNames));
        insert.addContent(" values ");
        insert.addContent(XmlElementUtil.insertFieldTrimOfIfList(CommonUtil.mapUnderScoreToLowerCamelCase(table.getName()),columnNames));
        return insert;
    }

    private Element batchInsert(Table table){
        Element batchInsert = new Element("insert");
        batchInsert.setAttribute("id","batchInsert"+CommonUtil.mapUnderScoreToUpperCamelCase(table.getName())+"s");
        batchInsert.setAttribute("parameterType", configProperties.getEntityGenPkg()+"."+CommonUtil.mapUnderScoreToUpperCamelCase(table.getName()));
        batchInsert.addContent(" insert into "+ table.getName());
        List<String> columnNames = table.getColumns().stream().map(TableColumn::getColumnName).collect(Collectors.toList());


        batchInsert.addContent(" (");
        batchInsert.addContent(String.join(",",columnNames));
        batchInsert.addContent(") ");

        batchInsert.addContent(" values ");

        Element foreach = new Element("foreach");
        foreach.setAttribute("collection","list");
        foreach.setAttribute("item","item");
        foreach.setAttribute("separator",",");

        batchInsert.addContent(foreach);

        List<String> input = columnNames.stream()
                .map(columnName -> "#{item." + CommonUtil.mapUnderScoreToLowerCamelCase(columnName) + "}").collect(
                        Collectors.toList());
        foreach.addContent(" (");
        foreach.addContent(String.join(",",input));
        foreach.addContent(") ");

        return batchInsert;

    }





}