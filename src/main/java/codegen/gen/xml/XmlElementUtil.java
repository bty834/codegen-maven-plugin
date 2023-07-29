package codegen.gen.xml;

import java.util.ArrayList;
import java.util.List;
import org.jdom2.Content;
import org.jdom2.Element;
import codegen.gen.CommonUtil;

/**
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

    public static Element exampleWhereClauseSqlRef(){
        Element anIf = new Element("if");
        anIf.setAttribute("test","_parameter != null");

        Element include = new Element("include");
        include.setAttribute("refid","Example_Where_Clause");

        anIf.addContent(include);

        return anIf;
    }

    public static Element exampleWhereClause(){
        Element whereClause = new Element("sql");
        whereClause.setAttribute("id","Example_Where_Clause");

        Element where = new Element("where");
        whereClause.addContent(where);

        Element foreach = new Element("foreach");
        foreach.setAttribute("collection","oredCriteria");
        foreach.setAttribute("item","criteria");
        foreach.setAttribute("separator","or");
        where.addContent(foreach);


        Element validIf = new Element("if");
        validIf.setAttribute("test","criteria.valid");
        foreach.addContent(validIf);

        Element trim = new Element("trim");
        trim.setAttribute("prefix","(");
        trim.setAttribute("prefixOverrides","and");
        trim.setAttribute("suffix",")");
        validIf.addContent(trim);

        Element innerForeach = new Element("foreach");
        innerForeach.setAttribute("collection","criteria.criteria");
        innerForeach.setAttribute("item","criterion");
        trim.addContent(innerForeach);


        Element choose = new Element("choose");
        innerForeach.addContent(choose);

        Element when1 = new Element("when");
        when1.setAttribute("test","criterion.noValue");
        when1.addContent("and ${criterion.condition}");
        Element when2 = new Element("when");
        when2.setAttribute("test","criterion.singleValue");
        when2.addContent("and ${criterion.condition} #{criterion.value}");
        Element when3 = new Element("when");
        when3.setAttribute("test","criterion.betweenValue");
        when3.addContent("and ${criterion.condition} #{criterion.value} and #{criterion.secondValue}");
        Element when4 = new Element("when");
        when4.setAttribute("test","criterion.listValue!=null and criterion.listValue.size>0");
        when4.addContent("and ${criterion.condition}");
        Element otherwise = new Element("otherwise");
        otherwise.addContent("false");

        Element whenForeach = new Element("foreach");
        whenForeach.setAttribute("open","(");
        whenForeach.setAttribute("separator",",");
        whenForeach.setAttribute("close",")");
        whenForeach.setAttribute("collection","criterion.value");
        whenForeach.setAttribute("item","listItem");
        whenForeach.addContent("#{listItem}");
        when4.addContent(whenForeach);

        List<Content> contents = new ArrayList<>();
        contents.add(when1);
        contents.add(when2);
        contents.add(when3);
        contents.add(when4);
        contents.add(otherwise);
        choose.addContent(contents);

        return whereClause;
    }


}
