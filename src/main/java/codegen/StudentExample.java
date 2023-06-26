package codegen;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import codegen.condition.ConditionExpUtil;
import lombok.Builder;

/**
 * TODO 类描述
 *
 * @author: baotingyu
 * @date: 2023/6/26
 **/
public class StudentExample {

    String conditionExp = "";

    private Integer id;
    private String name;
    private Long number;

    private void appendAnd(){
        if(StringUtils.isBlank(conditionExp)){
            return;
        }
        conditionExp+=" and ";
    }

    public StudentExample idGte(Integer val){
        appendAnd();
        conditionExp+= ConditionExpUtil.intGte("id",val);
        return this;
    }

}
