package codegen.gen;

import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;
import codegen.config.ConfigProperties;
import codegen.table.Table;

/**
 *
 *
 * @author: baotingyu
 * @date: 2023/6/25
 **/
public interface CodeGenerator {

    void generate(ConfigProperties configProperties,Set<Table> tables) throws MojoExecutionException;
}
