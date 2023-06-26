package codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import codegen.gen.CodeGenerator;
import codegen.table.Table;
import codegen.parser.ConfigFileParser;
import codegen.table.TableFetcher;

/**
 * TODO 类描述
 *
 * @author: baotingyu
 * @date: 2023/6/24
 **/
@Mojo(name = "codegen", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class SQLTableGenMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    // 跳过，默认不跳过
    @Parameter(property = "skip", defaultValue = "false")
    private boolean skip;

    // 配置文件绝对路径，参见resource目录下sample.yaml，目前只支持yaml/yml
    @Parameter(property = "absoluteFilePath", required = true, readonly = true)
    private String absoluteFilePath;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("codegen is skipped!");
            return;
        }
        if (StringUtils.isBlank(absoluteFilePath) || !Files.exists(Paths.get(absoluteFilePath))) {
            throw new MojoExecutionException("Failed to generate code: config file does not exist");
        }
        getLog().info("codegen-maven-plugin for " + project.getName() + " starting!");
        doExecute(project.getBasedir().getAbsolutePath(),absoluteFilePath);
    }

    public void doExecute(String baseAbsoluteDir,String filePath) throws MojoExecutionException {
        ConfigProperties configProperties = null;
        try {
            configProperties = parseConfigFile(filePath);
            configProperties.setBaseDir(baseAbsoluteDir);
        } catch (IOException | MojoExecutionException e) {
            throw new MojoExecutionException("Failed to generate code: "+e);
        }
        Set<Table> tables = null;
        try {
            tables = fetchTableInfo(configProperties);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate code: db error "+e);
        }
        if(Objects.isNull(tables) || tables.size()==0){
            getLog().warn("No tables to generate");
            return;
        }
        CodeGenerator codeGenerator = codeGenerator();
        codeGenerator.generate(configProperties,tables);

    }

    private CodeGenerator codeGenerator() throws MojoExecutionException {
        ServiceLoader<CodeGenerator> generators = ServiceLoader.load(CodeGenerator.class);
        for (CodeGenerator generator : generators) {
            return generator;
        }
        throw new MojoExecutionException("Failed to generate code: unsupported codegenerator");
    }

    private Set<Table> fetchTableInfo(ConfigProperties configProperties) throws Exception {
        ServiceLoader<TableFetcher> fetchers = ServiceLoader.load(TableFetcher.class);
        for (TableFetcher fetcher : fetchers) {
            if(fetcher.supports(configProperties)){
                return fetcher.fetch(configProperties);
            }
        }
        throw new MojoExecutionException("Failed to generate code: unsupported fetcher");
    }



    private ConfigProperties parseConfigFile(String configFilePath) throws MojoExecutionException, IOException {
        ServiceLoader<ConfigFileParser> parsers = ServiceLoader.load(ConfigFileParser.class);
        for (ConfigFileParser parser : parsers) {
            if (parser.supports(configFilePath)) {
                ConfigProperties parse = parser.parse(configFilePath);
                if(parse.validate()){
                    return parse;
                }
                throw new MojoExecutionException("Failed to generate code: config file's not completed");
            }
        }
        throw new MojoExecutionException("Failed to generate code: unsupported config file type");
    }

    public MavenProject getProject() {
        return project;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public String getAbsoluteFilePath() {
        return absoluteFilePath;
    }

    public void setAbsoluteFilePath(String absoluteFilePath) {
        this.absoluteFilePath = absoluteFilePath;
    }
}
