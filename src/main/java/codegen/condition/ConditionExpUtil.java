package codegen.condition;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * TODO 类描述
 *
 * @author: baotingyu
 * @date: 2023/6/26
 **/
public class ConditionExpUtil {

    public static String intGte(String column,Integer value){
        return column + ">=" + value;
    }
    public static String longGte(String column,Long value){
        return column + ">=" + value;
    }
    public static String intGt(String column,Integer value){
        return column + ">" + value;
    }
    public static String longGt(String column,Long value){
        return column + ">" + value;
    }

    public static String intLte(String column,Integer value){
        return column + "<=" + value;
    }
    public static String longLte(String column,Long value){
        return column + "<=" + value;
    }
    public static String intLt(String column,Integer value){
        return column + "<" + value;
    }
    public static String longLt(String column,Long value){
        return column + "<" + value;
    }

    public static String intEquals(String column,Integer value){
        return column+"="+value;
    }
    public static String longEquals(String column,Long value){
        return column+"="+value;
    }
    public static String StringEquals(String column,String value){
        return column+"="+value;
    }

    public static String intNotEquals(String column,Integer value){
        return column+"!="+value;
    }
    public static String longNotEquals(String column,Long value){
        return column+"!="+value;
    }
    public static String stringNotEquals(String column,String value){
        return column+"!="+value;
    }


    public static String intIn(String column, List<Integer> in){
        List<String> collect = in.stream().map(String::valueOf).collect(Collectors.toList());
        String join = String.join(",", collect);
        return column+"in ("+join+")";
    }


}
