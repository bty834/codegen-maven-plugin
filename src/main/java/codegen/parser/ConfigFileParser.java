package codegen.parser;

import java.io.IOException;
import codegen.ConfigProperties;

/**
 *
 * @author: baotingyu
 * @date: 2023/6/25
 **/
public interface ConfigFileParser {

    ConfigProperties parse(String filePath) throws IOException;

    boolean supports(String filePath);

    default String fileSuffix(String filePath){
        String typeSuffix = "";

        int i = filePath.lastIndexOf('.');
        if (i > 0) {
            typeSuffix = filePath.substring(i+1);
        }
       return typeSuffix;
    }


}
