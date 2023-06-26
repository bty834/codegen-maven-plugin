package codegen;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author: baotingyu
 * @date: 2023/6/24
 **/
public class ConfigProperties {

    // 0:JDBC
    private Integer fetchType = 0;

    private String driver;

    private String jdbcUrl;

    private String username;

    private String password;

    private String baseDir;

    private String[] excludedTables;

    private String entityGenPkg;

    private String mapperInterfaceGenPkg;

    private String mapperXmlGenAbsPath;

    public boolean validate(){
        if(fetchType==0){
            if(StringUtils.isBlank(driver) ||
                    StringUtils.isBlank(jdbcUrl) ||
                    StringUtils.isBlank(username) ||
                    StringUtils.isBlank(password)){
                return false;
            }
        }

        return true;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public Integer getFetchType() {
        return fetchType;
    }

    public void setFetchType(Integer fetchType) {
        this.fetchType = fetchType;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String[] getExcludedTables() {
        return excludedTables;
    }

    public void setExcludedTables(String[] excludedTables) {
        this.excludedTables = excludedTables;
    }

    public String getEntityGenPkg() {
        return entityGenPkg;
    }

    public void setEntityGenPkg(String entityGenPkg) {
        this.entityGenPkg = entityGenPkg;
    }

    public String getMapperInterfaceGenPkg() {
        return mapperInterfaceGenPkg;
    }

    public void setMapperInterfaceGenPkg(String mapperInterfaceGenPkg) {
        this.mapperInterfaceGenPkg = mapperInterfaceGenPkg;
    }

    public String getMapperXmlGenAbsPath() {
        return mapperXmlGenAbsPath;
    }

    public void setMapperXmlGenAbsPath(String mapperXmlGenAbsPath) {
        this.mapperXmlGenAbsPath = mapperXmlGenAbsPath;
    }
}
