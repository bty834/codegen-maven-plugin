package codegen.table;

import java.util.Set;

import codegen.config.ConfigProperties;

/**
 * TODO 类描述
 *
 * @author: baotingyu
 * @date: 2023/6/25
 **/
public interface TableFetcher {

    Set<Table> fetch(ConfigProperties configProperties) throws Exception;

    boolean supports(ConfigProperties configProperties);
}
