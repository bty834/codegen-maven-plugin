package codegen.table;

import java.util.Objects;

/**
 * TODO 类描述
 *
 * @author: baotingyu
 * @date: 2023/6/25
 **/

public class TableColumn {

    private String columnName;
    /**
     * {@link java.sql.Types}
     */
    private int dataType;
    private String typeName;

    private int columnSize;

    private boolean nullable;

    private boolean isAutoIncrement;

    private boolean indexed;

    private boolean uniqIndexed;

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public void setColumnSize(int columnSize) {
        this.columnSize = columnSize;
    }

    public boolean isIndexed() {
        return indexed;
    }

    public void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }

    public boolean isUniqIndexed() {
        return uniqIndexed;
    }

    public void setUniqIndexed(boolean uniqIndexed) {
        this.uniqIndexed = uniqIndexed;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Integer getDataType() {
        return dataType;
    }

    public void setDataType(Integer dataType) {
        this.dataType = dataType;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Integer getColumnSize() {
        return columnSize;
    }

    public void setColumnSize(Integer columnSize) {
        this.columnSize = columnSize;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public boolean isAutoIncrement() {
        return isAutoIncrement;
    }

    public void setAutoIncrement(boolean autoIncrement) {
        isAutoIncrement = autoIncrement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TableColumn that = (TableColumn) o;
        return Objects.equals(columnName, that.columnName) && Objects.equals(dataType, that.dataType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnName, dataType);
    }
}
