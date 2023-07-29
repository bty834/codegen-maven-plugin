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
4. Example use case
```java
       PeopleExample peopleExample = new PeopleExample();
        Criteria criteria = peopleExample.createCriteria();
        criteria.andCreateTimeGt("2023").andIdBetween(1,10).andNameIsNull();
        Criteria or = peopleExample.or();
        or.andIdBetween(100,1000);
        peopleExample.setDistinct(true).setLimit(1).setOffset(199);

```
