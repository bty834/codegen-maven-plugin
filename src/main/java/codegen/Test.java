package codegen;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * 测试
 *
 * @author: baotingyu
 * @date: 2023/6/25
 **/
public class Test {

    public static void main(String[] args) throws MojoExecutionException, MojoFailureException {

        SQLTableGenMojo mojo = new SQLTableGenMojo();

        mojo.doExecute("/Users/mac/IdeaProjects/codegen-maven-plugin","/Users/mac/IdeaProjects/codegen-maven-plugin/src/main/resources/sample.yaml");

    }

}
