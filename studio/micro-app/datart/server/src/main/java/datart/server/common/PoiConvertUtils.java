package datart.server.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONValidator;
import com.google.common.collect.Lists;
import datart.core.base.consts.ValueType;
import datart.core.data.provider.Column;
import datart.core.data.provider.Dataframe;
import datart.core.entity.poi.ColumnSetting;
import datart.core.entity.poi.POISettings;
import datart.server.base.dto.chart.ChartColumn;
import datart.server.base.dto.chart.ChartConfigDTO;
import datart.server.base.dto.chart.ChartDataConfigDTO;
import datart.server.base.dto.chart.ChartStyleConfigDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class PoiConvertUtils {

    public static POISettings covertToPoiSetting(String chartConfigStr, Dataframe dataframe) {
        ChartConfigDTO chartConfigDTO = JSONValidator.from(chartConfigStr).validate() ?
                JSON.parseObject(chartConfigStr, ChartConfigDTO.class) : new ChartConfigDTO();
        boolean isNormalTable = "mingxi-table".equals(chartConfigDTO.getChartGraphId());
        List<ChartColumn> chartColumns = getColumnsFromConfig(chartConfigDTO.getChartConfig().getDatas()); //获取列信息

        //支持钻取的图表，数据集列数量少于图表配置列数量
        if (chartColumns.size() != dataframe.getColumns().size()) {
            Map<String, Void> nameMap = new CaseInsensitiveMap<>();
            for (Column column : dataframe.getColumns()) {
                nameMap.put(column.columnKey(), null);
            }
            chartColumns = chartColumns
                    .stream()
                    .filter(chartColumn -> nameMap.containsKey(chartColumn.getDisplayName()))
                    .collect(Collectors.toList());
        }
        List<ChartColumn> groupColumns = Lists.newArrayList();
        Map<Integer, ColumnSetting> columnSetting = new HashMap<>();
        if (isNormalTable) { // 若为普通表格，获取表头分组信息
            groupColumns = getTableGroupList(chartConfigDTO, chartColumns);
            columnSetting = queryColumnSetting(groupColumns, dataframe);
        }
        if (columnSetting.size() != chartColumns.size()) {
            groupColumns.clear();
            columnSetting = queryColumnSetting(chartColumns, dataframe);
        }
        POISettings poiSettings = buildTableHeaderInfo(chartColumns, groupColumns); //构造表头
        poiSettings.setColumnSetting(columnSetting);
        if (columnSetting.size() != chartColumns.size() || CollectionUtils.isEmpty(chartColumns)) {
            log.warn("column setting parse failed, download with no style.");
            Map<Integer, List<Column>> map = new HashMap<>();
            map.put(0, dataframe.getColumns());
            poiSettings.setHeaderRows(map);
            poiSettings.getMergeCells().clear();
            poiSettings.getColumnSetting().clear();
        }
        poiSettings.setHeaderRows(replaceColumnAlias(chartColumns, poiSettings.getHeaderRows())); //替换别名
        return poiSettings;
    }

    /**
     * 获取图表列信息
     */
    private static List<ChartColumn> getColumnsFromConfig(List<ChartDataConfigDTO> configData) {
        List<ChartColumn> results = Lists.newArrayList();
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        for (ChartDataConfigDTO data : configData) {
            for (ChartColumn row : data.getRows()) {
                if (!map.containsKey(row.getDisplayName()) && StringUtils.isNotBlank(row.getColName())) {
                    results.add(row);
                }
                map.put(row.getDisplayName(), 0);
            }
        }
        return results;
    }

    /**
     * 获取列序及配置信息
     */
    private static Map<Integer, ColumnSetting> queryColumnSetting(List<ChartColumn> viewColumns, Dataframe dataframe) {
        Map<Integer, ColumnSetting> settingMap = new HashMap<>();
        List<ChartColumn> leafViewColumns = new ArrayList<>(); // 展示顺序
        for (ChartColumn viewColumn : viewColumns) {
            leafViewColumns.addAll(viewColumn.getLeafNodes());
        }
        CaseInsensitiveMap<String, Integer> viewColumnMap = new CaseInsensitiveMap<>();
        for (int i = 0; i < leafViewColumns.size(); i++) {
            ChartColumn chartColumn = leafViewColumns.get(i);
            viewColumnMap.put(chartColumn.getDisplayName(), i);
        }
        for (int i = 0; i < dataframe.getColumns().size(); i++) {
            Column column = dataframe.getColumns().get(i);
            ColumnSetting columnSetting = new ColumnSetting();
            columnSetting.setLength(0);
            int index = viewColumnMap.getOrDefault(column.columnKey(), -1);
            if (index < 0) {
                continue;
            }
            columnSetting.setIndex(index);
            columnSetting.setNumFormat(leafViewColumns.get(index).getNumFormat());
            settingMap.put(i, columnSetting);
        }
        return settingMap;
    }

    /**
     * 替换列别名
     */
    private static Map<Integer, List<Column>> replaceColumnAlias(List<ChartColumn> dataColumns, Map<Integer, List<Column>> tableHeaders) {
        Map<String, String> aliasMap = new HashMap<>();
        dataColumns.forEach(item -> {
            if (StringUtils.isNotBlank(item.getAlias().getName())) {
                aliasMap.put(item.getDisplayName(), item.getAlias().getName());
            }
        });

        for (int i = 0; i < dataColumns.size(); i++) {
            for (int j = tableHeaders.size() - 1; j >= 0; j--) {
                Column column = tableHeaders.get(j).get(i);
                if (StringUtils.isNotBlank(column.columnName())) {
                    column.setName(aliasMap.getOrDefault(column.columnKey(), column.columnName()));
                    break;
                }
            }
        }
        return tableHeaders;
    }

    /**
     * 获取表头分组列信息
     */
    private static List<ChartColumn> getTableGroupList(ChartConfigDTO chartConfigDTO, List<ChartColumn> chartColumns) {
        List<ChartColumn> groupColumns = new ArrayList<>();
        List<ChartStyleConfigDTO> styles = chartConfigDTO.getChartConfig().getStyles();
        ChartStyleConfigDTO tableHeaders = getTableStyleMap(new HashMap<>(), styles).getOrDefault("tableHeaders", new ChartStyleConfigDTO());
        if (null != tableHeaders.getValue() && JSONValidator.Type.Array.equals(JSONValidator.from(tableHeaders.getValue().toString()).getType())) {
            groupColumns = JSON.parseArray(tableHeaders.getValue().toString(), ChartColumn.class);
        }
        List<ChartColumn> leafNode = new ArrayList<>();
        for (ChartColumn groupColumn : groupColumns) {
            leafNode.addAll(groupColumn.getLeafNodes());
        }
        if (leafNode.size() == chartColumns.size()) {
            return groupColumns;
        }
        //处理分组后添加/删除列情况
        CaseInsensitiveMap<String, Integer> colNameMap = new CaseInsensitiveMap<>();
        chartColumns.forEach(item -> colNameMap.put(item.getDisplayName(), 0));
        checkDeleteGroupColumn(groupColumns, colNameMap);
        Map<String, ChartColumn> leafNodeMap = leafNode.stream().collect(Collectors.toMap(ChartColumn::getDisplayName, item -> item, (oldVal, newVal) -> newVal));
        for (ChartColumn chartColumn : chartColumns) {
            if (!leafNodeMap.containsKey(chartColumn.getDisplayName())) {
                groupColumns.add(chartColumn);
            }
        }
        return groupColumns;
    }

    private static Map<String, ChartStyleConfigDTO> getTableStyleMap(Map<String, ChartStyleConfigDTO> res, List<ChartStyleConfigDTO> styles) {
        for (ChartStyleConfigDTO style : styles) {
            res.put(style.getKey(), style);
            if (!style.getRows().isEmpty()) {
                getTableStyleMap(res, style.getRows());
            }
        }
        return res;
    }

    private static void checkDeleteGroupColumn(List<ChartColumn> chartColumns, CaseInsensitiveMap<String, Integer> colNameMap) {
        for (int i = 0; i < chartColumns.size(); i++) {
            ChartColumn chartColumn = chartColumns.get(i);
            if (chartColumn.getLeafNum() == 0 && !chartColumn.isGroup()) {
                if (colNameMap.getOrDefault(chartColumn.getDisplayName(), 1) > 0) {
                    chartColumns.remove(chartColumn);
                    i--;
                } else {
                    colNameMap.put(chartColumn.getDisplayName(), 1);
                }
            } else if (chartColumn.getChildren().size() > 0) {
                checkDeleteGroupColumn(chartColumn.getChildren(), colNameMap);
                chartColumn.setChildren(chartColumn.getChildren());
                if (chartColumn.isGroup() && chartColumn.getChildren().isEmpty()) {
                    chartColumns.remove(chartColumn);
                    i--;
                }
            }
        }
    }

    /**
     * 构建excel表头信息
     */
    private static POISettings buildTableHeaderInfo(List<ChartColumn> columns, List<ChartColumn> groupColumns) {
        POISettings poiSettings = new POISettings();
        Map<Integer, List<Column>> headerRowMap = new HashMap<>();
        List<CellRangeAddress> mergeCells = new ArrayList<>();
        if (!CollectionUtils.isEmpty(groupColumns)) { //分组表头
            int deep = groupColumns.stream().map(ChartColumn::getDeepNum).max(Integer::compareTo).get();
            for (int i = 1; i < deep; i++) {
                headerRowMap.put(i, new ArrayList<>());
            }
            convertGroupHeaderData(groupColumns, headerRowMap, 0, mergeCells);
        } else {
            for (ChartColumn column : columns) {
                putDataIntoColumnMap(headerRowMap, 0, column);
            }
        }
        poiSettings.setHeaderRows(headerRowMap);
        poiSettings.setMergeCells(mergeCells);
        return poiSettings;
    }

    private static void convertGroupHeaderData(List<ChartColumn> dataStyles, Map<Integer, List<Column>> rowMap, int rowNum, List<CellRangeAddress> cellRangeAddresses) {
        for (ChartColumn dataStyle : dataStyles) {
            int columnNum = putDataIntoColumnMap(rowMap, rowNum, dataStyle);
            if (dataStyle.getLeafNum() == 0 && !dataStyle.isGroup()) {//纵向合并
                for (int i = rowNum + 1; i < rowMap.size(); i++) {
                    putDataIntoColumnMap(rowMap, i, new ChartColumn());
                }
                if (rowMap.size() - 1 > rowNum) {
                    cellRangeAddresses.add(new CellRangeAddress(rowNum, rowMap.size() - 1, columnNum, columnNum));
                }
            } else if (dataStyle.getLeafNum() > 1) {//横向合并
                for (int i = 1; i < dataStyle.getLeafNum(); i++) {
                    putDataIntoColumnMap(rowMap, rowNum, new ChartColumn());
                }
                cellRangeAddresses.add(new CellRangeAddress(rowNum, rowNum, columnNum, columnNum + dataStyle.getLeafNum() - 1));
            }
            if (dataStyle.getChildren().size() > 0) {//递归遍历所有节点
                int row = rowNum + 1;
                convertGroupHeaderData(dataStyle.getChildren(), rowMap, row, cellRangeAddresses);
            }
        }
    }

    private static int putDataIntoColumnMap(Map<Integer, List<Column>> rowMap, Integer key, ChartColumn val) {
        if (!rowMap.containsKey(key)) {
            rowMap.put(key, new ArrayList<>());
        }
        Column column = new Column();
        column.setName(val.getDisplayName());
        column.setType(ValueType.STRING);
        rowMap.get(key).add(column);
        return rowMap.get(key).size() - 1;
    }
}
