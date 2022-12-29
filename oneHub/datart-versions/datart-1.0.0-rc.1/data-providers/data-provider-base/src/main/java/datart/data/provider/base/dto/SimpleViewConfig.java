package datart.data.provider.base.dto;

import lombok.Data;

import java.util.List;

@Data
public class SimpleViewConfig {

    private String mainTable;

    private List<SimpleViewJoinDto> joinTables;
}
