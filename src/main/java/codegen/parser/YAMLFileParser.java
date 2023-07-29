package codegen.parser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import codegen.ConfigProperties;

/**
 *
 * @author: baotingyu
 * @date: 2023/6/25
 **/
public class YAMLFileParser implements ConfigFileParser {

    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @Override
    public ConfigProperties parse(String filePath) throws IOException {
        InputStream inputStream = Files.newInputStream(Paths.get(filePath));
        return yamlMapper.readValue(inputStream, ConfigProperties.class);
    }

    @Override
    public boolean supports(String filePath) {
        String suffix = fileSuffix(filePath);
        return Objects.equals(suffix, "yml") || Objects.equals(suffix, "yaml");
    }
}
