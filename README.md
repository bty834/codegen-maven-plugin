自定义Maven插件生成MyBatis实体类、Mapper、XML

Java类生成使用javapoet

# 示例
## entity
![](doc/image/entity.png)
## mapper
![](doc/image/mapper.png)
## xml
TODO

# 使用
1. 引入build
```xml
<build>
    <plugins>
        <plugin>
            <groupId>site.btyhub</groupId>
            <artifactId>codegen-maven-plugin</artifactId>
            <version>1.0-SNAPSHOT</version>
            <executions>
                <execution>
                    <goals>
                        <goal>
                            codegen
                        </goal>
                    </goals>
                </execution>
            </executions>
            <dependencies>
                <dependency>
                    <groupId>com.mysql</groupId>
                    <artifactId>mysql-connector-j</artifactId>
                    <version>${mysql.version}</version>
                </dependency>
                <dependency>
                    <groupId>org.mybatis.spring.boot</groupId>
                    <artifactId>mybatis-spring-boot-starter</artifactId>
                    <version>${mybatis.version}</version>
                </dependency>
            </dependencies>
            <configuration>
                <absoluteFilePath>/Users/mac/IdeaProjects/java-maven-sample/src/main/resources/sample.yaml</absoluteFilePath>
                <skip>false</skip>
            </configuration>
        </plugin>
    </plugins>
</build>
```

2. 配置文件：参见resource目录下sample.yaml。目前仅支持yaml/yml

TODO: XML生成
