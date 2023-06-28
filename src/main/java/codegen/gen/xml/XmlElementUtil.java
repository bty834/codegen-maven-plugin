package codegen.gen.xml;

import java.util.List;

import org.jdom2.Element;

import codegen.gen.CommonUtil;

/**
 * TODO 类描述
 *
 * @author: baotingyu
 * @date: 2023/6/27
 **/
public class XmlElementUtil {


    public static Element insertColumnTrimOfIfList(String param,List<String> fieldNames){
        Element trim = trimElement();

        fieldNames.forEach(fieldName -> {
            Element ifEle = new Element("if");
            ifEle.setAttribute("test", param+"."+CommonUtil.mapUnderScoreToLowerCamelCase(fieldName)+"!=null");
            ifEle.addContent(fieldName+",");
            trim.addContent(ifEle);
        });

        return trim;
    }

    public static Element trimElement(){
        Element trim = new Element("trim");
        trim.setAttribute("prefix","(");
        trim.setAttribute("suffix",")");
        trim.setAttribute("suffixOverrides",",");
        return trim;
    }

    public static Element insertFieldTrimOfIfList(String param,List<String> fieldNames){
        Element trim = trimElement();

        fieldNames.forEach(fieldName -> {
            Element ifEle = new Element("if");
            ifEle.setAttribute("test", param+"."+CommonUtil.mapUnderScoreToLowerCamelCase(fieldName)+"!=null");
            ifEle.addContent("#{"+param+"."+CommonUtil.mapUnderScoreToLowerCamelCase(fieldName)+"},");

            trim.addContent(ifEle);
        });

        return trim;
    }


    public static Element updateSetOfIfList(String param,List<String> fieldNames){
        Element set = new Element("set");

        fieldNames.forEach(fieldName -> {
            Element ifEle = new Element("if");
            ifEle.setAttribute("test", CommonUtil.mapUnderScoreToLowerCamelCase(fieldName)+"!=null");
            ifEle.addContent(fieldName + "= #{"+param+"."+CommonUtil.mapUnderScoreToLowerCamelCase(fieldName)+"}"+",");

            set.addContent(ifEle);
        });
        return set;
    }

    public static Element example(){
        Element where = new Element("where");

        Element foreach = new Element("foreach");
        where.addContent(foreach);

        foreach.setAttribute("collection","example.whereClause");
        foreach.setAttribute("item","item");
        foreach.setAttribute("separator","and");

        foreach.addContent(" ${item}");
        return where;
    }


}
