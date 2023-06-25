package codegen.table;

import java.util.HashSet;
import java.util.Set;

/**
 * TODO 类描述
 *
 * @author: baotingyu
 * @date: 2023/6/25
 **/
public class Table {
    private String name;

    private TableColumn primaryKeyColumn;

    private Set<TableColumn> columns = new HashSet<>();

    public void addColumn(TableColumn column){
        columns.add(column);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<TableColumn> getColumns() {
        return columns;
    }

    public void setColumns(Set<TableColumn> columns) {
        this.columns = columns;
    }

    public TableColumn getPrimaryKeyColumn() {
        return primaryKeyColumn;
    }

    public void setPrimaryKeyColumn(TableColumn primaryKeyColumn) {
        this.primaryKeyColumn = primaryKeyColumn;
    }
}
