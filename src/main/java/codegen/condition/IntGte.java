package codegen.condition;

import lombok.Builder;
import lombok.Data;

/**
 * TODO 类描述
 *
 * @author: baotingyu
 * @date: 2023/6/26
 **/
public class IntGte implements Condition{

    int value;

    @Override
    public String getExpression() {
        return null;
    }
}
