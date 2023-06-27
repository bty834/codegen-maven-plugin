package codegen;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * 测试
 *
 * @author: baotingyu
 * @date: 2023/6/25
 **/
public class Test {

    public static void main(String[] args) throws MojoExecutionException, MojoFailureException, IOException {

//        SQLTableGenMojo mojo = new SQLTableGenMojo();
//
//        mojo.doExecute("/Users/mac/IdeaProjects/codegen-maven-plugin","/Users/mac/IdeaProjects/codegen-maven-plugin/src/main/resources/sample.yaml");
        String namespace = "hello namespace";
        String type = "type";

        Document xml = new Document();
        DocType mybatisDocType = new DocType("mapper", "-//mybatis.org//DTD Mapper 3.0//EN",
                "http://mybatis.org/dtd/mybatis-3-mapper.dtd");
        xml.setDocType(mybatisDocType);

        Element mapper = new Element("mapper");
        mapper.setAttribute("namespace",namespace);
        xml.addContent(mapper);

        Element resultMap = new Element("resultMap");
        resultMap.setAttribute("id","BaseResultMap");
        resultMap.setAttribute("type",type);
        mapper.addContent(resultMap);

        Element sql = new Element("sql");
        sql.setAttribute("id","BaseColumn");
        mapper.addContent(sql);

        XMLOutputter out = new XMLOutputter() ;
        Format format = Format.getPrettyFormat();
        out.setFormat(format);
        out.output(xml, Files.newOutputStream(Paths.get("test.xml")));

    }

}
