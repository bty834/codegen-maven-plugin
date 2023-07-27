Auto-generate db entity, mapper interface, mapper XML of MyBatis

- Java source code: javapoet
- mapper XML: jdom2 

# Example
## entity
![](doc/image/entity.png)
## mapper
![](doc/image/mapper.png)
## xml
![](doc/image/xml.png)


# Quick Start
1. configure build in pom.xml
```xml
<build>
    <plugins>
        <plugin>
            <groupId>site.btyhub</groupId>
            <artifactId>codegen-maven-plugin</artifactId>
            <version>1.0</version>
            <dependencies>
                <dependency>
                    <groupId>com.mysql</groupId>
                    <artifactId>mysql-connector-j</artifactId>
                    <version>${mysql.version}</version>
                </dependency>
            </dependencies>
            <configuration>
                <absoluteFilePath>${absolute path of your yaml/yml config file}</absoluteFilePath>
                <skip>false</skip>
            </configuration>
        </plugin>
    </plugins>
</build>
```

2. config file：see sample.yaml in resources dir. only supports yaml/yml for now.

3.  run `mvn codegen:codegen`
4. QueryExample convert all time types in db to String.class in Java. All where clauses supports 'and' only，concat by ${} for convenient implementation.And，only supports single column of primary key, which could cover most of our cases
```java
PeopleQueryExample example = PeopleQueryExample.newExample()
                .updateTimeGt("2023-06-20")
                .nameIn(Arrays.asList("name_1", "name_2"))
                .numberLte(10000);
```
