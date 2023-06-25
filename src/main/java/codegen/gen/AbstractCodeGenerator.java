package codegen.gen;

import java.io.File;
import java.sql.Types;
import java.util.Objects;
import org.apache.maven.plugin.MojoExecutionException;
import com.google.common.base.CaseFormat;
import codegen.config.ConfigProperties;

/**
 * TODO 类描述
 *
 * @author: baotingyu
 * @date: 2023/6/25
 **/
public abstract class AbstractCodeGenerator implements CodeGenerator {

    protected ConfigProperties configProperties;

    protected String mapUnderScoreToLowerCamelCase(String underScoreStr) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, underScoreStr);
    }

    protected String mapUnderScoreToUpperCamelCase(String underScoreStr) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, underScoreStr);
    }

    protected String getAbsolutePathForPkg(String baseDir, String pkgPath) throws MojoExecutionException {
        String osName = System.getProperty("os.name");
        boolean isWindows = osName.toLowerCase().contains("win");

        if (isWindows) {
            pkgPath = pkgPath.replace(".","\\");
            baseDir = baseDir.replace("/","\\");
            return baseDir + "\\src\\main\\java\\"+pkgPath;
        } else {
            pkgPath = pkgPath.replace('.','/');
            return baseDir + "/src/main/java/" + pkgPath;
        }
    }

    protected String getAbsolutePathForSrcMainJava(String baseDir) {
        String osName = System.getProperty("os.name");
        boolean isWindows = osName.toLowerCase().contains("win");

        if (isWindows) {
            baseDir = baseDir.replace("/","\\");
            return baseDir + "\\src\\main\\java";
        } else {
            return baseDir + "/src/main/java/";
        }
    }

    protected void prepareDir(String fileDir) throws MojoExecutionException {
        try {
            File file = new File(fileDir);
            if (file.exists() && file.isDirectory()) {
                File[] files = file.listFiles();
                if (Objects.isNull(files) || files.length > 0) {
                    throw new MojoExecutionException("Failed to generate code: directory not empty " + fileDir);
                }
            }
            file.mkdirs();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate code: " + e.getMessage());
        }
    }

    public String getterMethodNameFromColumnName(String columnName) {
        String s = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, columnName);
        return "get" + s;
    }

    public String setterMethodNameFromColumnName(String columnName) {
        String s = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, columnName);
        return "set" + s;
    }

    public Class<?> convertJDBCTypetoClass(int jdbcType) {
        Class<?> result = Object.class;
        switch (jdbcType) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
                result = String.class;
                break;
            case Types.NUMERIC:
            case Types.DECIMAL:
                result = java.math.BigDecimal.class;
                break;
            case Types.BIT:
                result = Boolean.class;
                break;
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
                result = Integer.class;
                break;
            case Types.BIGINT:
                result = Long.class;
                break;
            case Types.REAL:
            case Types.FLOAT:
                result = Float.class;
                break;
            case Types.DOUBLE:
                result = Double.class;
                break;
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                result = Byte[].class;
                break;
            case Types.DATE:
                result = java.sql.Date.class;
                break;
            case Types.TIME:
                result = java.sql.Time.class;
                break;
            case Types.TIMESTAMP:
                result = java.sql.Timestamp.class;
                break;
        }
        return result;
    }

}
