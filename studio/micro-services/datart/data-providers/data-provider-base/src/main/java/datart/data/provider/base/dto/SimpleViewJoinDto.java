package datart.data.provider.base.dto;

import datart.core.data.provider.sql.FilterOperator;
import lombok.Data;
import org.apache.calcite.sql.JoinType;

import java.util.List;

@Data
public class SimpleViewJoinDto {

    private JoinType joinType;

    private String tableName;

    private List<FilterOperator> conditions;

}
