自定义Maven插件生成MyBatis实体类、Mapper、XML

Java类生成使用javapoet

# 示例
## entity
![](doc/image/entity.png)
## mapper
![](doc/image/mapper.png)

TODO: 查询用的 Example实体类 和 mapper XML

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
                    <groupId>org.mybatis</groupId>
                    <artifactId>mybatis</artifactId>
                    <version>${mybatis.version}</version>
                </dependency>
                <dependency>
                    <groupId>org.projectlombok</groupId>
                    <artifactId>lombok</artifactId>
                    <version>${lombok.version}</version>
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
