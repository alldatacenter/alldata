package datart.data.provider.calcite.custom;

import org.apache.calcite.sql.type.SqlTypeFamily;
import org.apache.calcite.util.Util;

import java.util.Map;

public enum CustomSqlTypeName {
    ANY(SqlTypeFamily.ANY),
    DATETIME(SqlTypeFamily.DATETIME),
    DATETIME2(SqlTypeFamily.DATETIME),
    INT(SqlTypeFamily.INTEGER);

    public static final Map<String, CustomSqlTypeName> SQL_TYPE_FAMILY_MAP = Util.enumConstants(CustomSqlTypeName.class);
    private final SqlTypeFamily family;

    CustomSqlTypeName(SqlTypeFamily sqlTypeFamily) {
        this.family = sqlTypeFamily;
    }

    public SqlTypeFamily getFamily() {
        return family;
    }
}
