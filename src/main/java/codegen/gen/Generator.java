package codegen.gen;

import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;

import codegen.ConfigProperties;
import codegen.table.Table;

/**
 * TODO 类描述
 *
 * @author: baotingyu
 * @date: 2023/6/27
 **/
public interface Generator {
    void generate(ConfigProperties configProperties, Set<Table> tables) throws MojoExecutionException;

}
