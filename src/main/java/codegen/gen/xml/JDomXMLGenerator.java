package codegen.gen.xml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        // TODO

        return Collections.singletonMap(xmlName,xml);
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
