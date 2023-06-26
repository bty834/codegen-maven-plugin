package codegen.table;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.collect.Sets;

import codegen.config.ConfigProperties;

/**
 * @author: baotingyu
 * @date: 2023/6/25
 **/
public class JDBCTableFetcher implements TableFetcher {

    @Override
    public Set<Table> fetch(ConfigProperties configProperties) throws Exception {
        HashSet<Table> tables = new HashSet<>();

        Set<String> excludedTables = new HashSet<>();
        if (Objects.nonNull(configProperties.getExcludedTables()) && configProperties.getExcludedTables().length > 0) {
            excludedTables.addAll(Arrays.asList(configProperties.getExcludedTables()));
        }

        Class.forName(configProperties.getDriver());

        try (Connection conn = DriverManager.getConnection(configProperties.getJdbcUrl(),
                configProperties.getUsername(),
                configProperties.getPassword());) {
            String dbName = extractDbFromUrl(configProperties.getJdbcUrl());

            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet tableResultSet = metaData.getTables(dbName, dbName, null, new String[] {"TABLE"});


            while (tableResultSet.next()) {
                String tableName = tableResultSet.getString(3);
                if (excludedTables.contains(tableName)) {
                    continue;
                }

                Table table = new Table();
                table.setName(tableName);

                String primaryKeyColumnName = "";
                ResultSet primaryKeyResultSet = metaData.getPrimaryKeys(dbName, dbName, tableName);

                Set<@Nullable String> uniqIndexColumnNames = Sets.newHashSet();
                ResultSet uniqIndexResultSet = metaData.getIndexInfo(dbName, dbName, tableName, true, true);
                while (uniqIndexResultSet.next()) {
                    uniqIndexColumnNames.add(uniqIndexResultSet.getString(9));
                }

                Set<@Nullable Object> indexColumnNames = Sets.newHashSet();
                ResultSet indexResultSet = metaData.getIndexInfo(dbName, dbName, tableName, false, true);
                while (indexResultSet.next()) {
                    indexColumnNames.add(indexResultSet.getString(9));
                }


                while (primaryKeyResultSet.next()) {
                    primaryKeyColumnName = primaryKeyResultSet.getString(4);
                }


                ResultSet columns = metaData.getColumns(null, null, tableName, null);
                while (columns.next()) {
                    TableColumn c = new TableColumn();
                    c.setColumnName(columns.getString(4));
                    c.setDataType(columns.getInt(5));
                    c.setTypeName(columns.getString(6));
                    c.setColumnSize(columns.getInt(7));
                    c.setNullable(columns.getBoolean(11));
                    c.setAutoIncrement(columns.getBoolean(23));
                    c.setIndexed(indexColumnNames.contains(c.getColumnName()));
                    c.setUniqIndexed(uniqIndexColumnNames.contains(c.getColumnName()));

                    if (c.getColumnName().equals(primaryKeyColumnName)) {
                        table.setPrimaryKeyColumn(c);
                    }

                    table.addColumn(c);
                }
                if (Objects.isNull(table.getPrimaryKeyColumn())) {
                    throw new MojoExecutionException("require a primary key in table : " + table.getName());
                }

                tables.add(table);
            }
        }

        return tables;
    }

    @Override
    public boolean supports(ConfigProperties configProperties) {
        return configProperties.getFetchType().equals(0);
    }

    public static String extractDbFromUrl(String url) throws MojoExecutionException {
        int second = url.indexOf("?");
        int first = second;
        for (int i = second; i > 0; i--) {
            if (url.charAt(i) == '/') {
                first = i;
                break;
            }
        }
        if (first == second) {
            throw new MojoExecutionException("Failed to generate code: illegal db url");
        }
        return url.substring(first + 1, second);
    }
}
