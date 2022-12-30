/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datart.core.common;

import datart.core.base.consts.FileFormat;
import datart.core.base.consts.JavaType;
import datart.core.base.exception.Exceptions;
import datart.core.data.provider.Column;
import datart.core.data.provider.Dataframe;
import datart.core.entity.poi.ColumnSetting;
import datart.core.entity.poi.POISettings;
import datart.core.entity.poi.format.PoiNumFormat;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
public class POIUtils {

    private static IndexedColorMap indexedColorMap = new DefaultIndexedColorMap();

    public static void save(Workbook workbook, String path, boolean cover) throws IOException {
        if (workbook == null || path == null) {
            return;
        }
        File file = new File(path);
        if (file.exists()) {
            if (cover) {
                file.delete();
            } else {
                Exceptions.msg("file (" + path + ")  already exists");
            }
        } else {
            file.getParentFile().mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        }
    }

    public static Workbook fromTableData(Dataframe dataframe) {
        Workbook workbook = new SXSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        fillSheet(sheet, dataframe, 1);
        return workbook;
    }

    public static Workbook createEmpty() {
        return new SXSSFWorkbook();
    }

    public static void withSheet(Workbook workbook, String sheetName, Dataframe sheetData, POISettings poiSettings) {
        Sheet sheet = workbook.createSheet(sheetName);
        int rowNum = writeHeaderRows(sheet, poiSettings.getHeaderRows());
        fillSheetWithSetting(sheet, sheetData, poiSettings.getColumnSetting(), rowNum + 1);
        mergeSheetCell(sheet, poiSettings.getMergeCells());
        setColumnWidth(sheet, poiSettings.getColumnSetting());
    }

    private static void setColumnWidth(Sheet sheet, Map<Integer, ColumnSetting> columnSetting) {
        for (Integer num : columnSetting.keySet()) {
            ColumnSetting setting = columnSetting.get(num);
            sheet.setColumnWidth(setting.getIndex(), setting.getWidth());
        }
    }

    private static int writeHeaderRows(Sheet sheet, Map<Integer, List<Column>> headerRows) {
        CellStyle cellStyle = getHeaderCellStyle(sheet);
        for (int i = 0; i < headerRows.size(); i++) {
            writeHeader(sheet, headerRows.get(i), i, cellStyle);
        }
        return headerRows.size() - 1;
    }

    private static void writeHeader(Sheet sheet, List<Column> columns, int rowNum, CellStyle cellStyle) {
        Row row = sheet.createRow(rowNum);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(columns.get(i).columnKey());
            cell.setCellStyle(cellStyle);
        }
    }

    private static void fillSheet(Sheet sheet, Dataframe data, int rowIndex) {
        for (List<Object> dataRow : data.getRows()) {
            Row row = sheet.createRow(rowIndex);
            for (int i = 0; i < dataRow.size(); i++) {
                Object val = dataRow.get(i);
                Cell cell = row.createCell(i);
                cell.setCellStyle(getCellStyle(sheet, val, ""));
                setCellValue(cell, val);
            }
            rowIndex++;
        }
    }

    private static void fillSheetWithSetting(Sheet sheet, Dataframe data, Map<Integer, ColumnSetting> columnSetting, int rowIndex) {
        for (List<Object> dataRow : data.getRows()) {
            Row row = sheet.createRow(rowIndex);
            for (int i = 0; i < dataRow.size(); i++) {
                Object val = dataRow.get(i);
                int columnIndex = i;
                String fmt = "";
                CellStyle cellStyle = null;
                if (columnSetting.containsKey(i)) {
                    ColumnSetting setting = columnSetting.get(i);
                    columnIndex = setting.getIndex();
                    PoiNumFormat numFormat = setting.getNumFormat();
                    fmt = numFormat.getFormat();
                    setting.setLength(val == null ? 0 : Math.max(val.toString().length(), setting.getLength()));
                    val = numFormat.parseValue(val);
                    cellStyle = setting.getCellStyle() == null ? getCellStyle(sheet, val, fmt) : setting.getCellStyle();
                    setting.setCellStyle(cellStyle);
                } else {
                    ColumnSetting setting = new ColumnSetting();
                    setting.setIndex(i);
                    setting.setNumFormat(new PoiNumFormat());
                    setting.setLength(val == null ? 0 : val.toString().length());
                    setting.setCellStyle(getCellStyle(sheet, val, fmt));
                    columnSetting.put(i, setting);
                }
                Cell cell = row.createCell(columnIndex);
                cell.setCellStyle(cellStyle);
                setCellValue(cell, val);
            }
            rowIndex++;
        }
    }

    private static void mergeSheetCell(Sheet sheet, List<CellRangeAddress> mergeCells) {
        for (CellRangeAddress cellRangeAddress : mergeCells) {
            sheet.addMergedRegion(cellRangeAddress);
        }
    }

    public static List<List<Object>> loadExcel(String path) throws IOException {
        LinkedList<List<Object>> rows = new LinkedList<>();
        try (InputStream inputStream = new FileInputStream(path)) {
            Workbook workbook;
            if (path.toLowerCase().endsWith(FileFormat.XLS.getFormat())) {
                workbook = new HSSFWorkbook(inputStream);
            } else if (path.toLowerCase().endsWith(FileFormat.XLSX.getFormat())) {
                workbook = new XSSFWorkbook(inputStream);
            } else {
                Exceptions.msg("message.unsupported.format", path);
                return null;
            }

            if (workbook.getNumberOfSheets() < 1) {
                Exceptions.msg("empty excel :" + path);
            }
            // 只处理第一个sheet
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.rowIterator();
            Row row0 = sheet.getRow(0);
            if (row0 == null) {
                Exceptions.msg("empty excel :" + path);
            }
            int columns = row0.getPhysicalNumberOfCells();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                LinkedList<Object> cellValues = new LinkedList<>();
                for (int i = 0; i < columns; i++)
                    cellValues.add(readCellValue(row.getCell(i)));
                rows.add(cellValues);
            }
        }
        return rows;
    }

    private static Object readCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                }
                return cell.getNumericCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            default:
                return cell.getStringCellValue();
        }
    }

    private static CellStyle getCellStyle(Sheet sheet, Object val, String fmt) {
        CellStyle cellStyle = sheet.getWorkbook().createCellStyle();
        DataFormat dataFormat = sheet.getWorkbook().createDataFormat();
        if (StringUtils.isNotBlank(fmt)) {
            cellStyle.setDataFormat(dataFormat.getFormat(fmt));
        } else if (val instanceof Number) {
        } else if (val instanceof Date) {
        } else {
            cellStyle.setDataFormat(dataFormat.getFormat("General"));
        }
        return cellStyle;
    }

    private static CellStyle getHeaderCellStyle(Sheet sheet) {
        XSSFCellStyle cellStyle = (XSSFCellStyle) sheet.getWorkbook().createCellStyle();
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        XSSFColor grayColor = new XSSFColor(new java.awt.Color(220, 220, 220), indexedColorMap);
        XSSFColor whiteColor = new XSSFColor(new java.awt.Color(240, 240, 240), indexedColorMap);
        cellStyle.setTopBorderColor(whiteColor);
        cellStyle.setRightBorderColor(whiteColor);
        cellStyle.setBottomBorderColor(whiteColor);
        cellStyle.setLeftBorderColor(whiteColor);
        cellStyle.setFillForegroundColor(grayColor);
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        return cellStyle;
    }

    private static void setCellValue(Cell cell, Object val) {
        if (val == null) {
            cell.setCellValue("");
            return;
        }
        try {
            JavaType javaType = JavaType.valueOf(val.getClass().getSimpleName().toUpperCase());
            switch (javaType) {
                case BIGDECIMAL:
                case BYTE:
                case SHORT:
                case INTEGER:
                case LONG:
                case FLOAT:
                case DOUBLE:
                    cell.setCellValue(new BigDecimal(val.toString()).doubleValue());
                    break;
                case BOOLEAN:
                    cell.setCellValue((Boolean) val);
                    break;
                case DATE:
                default:
                    cell.setCellValue(val.toString());
                    break;
            }
        } catch (IllegalArgumentException e) {
            cell.setCellValue(val.toString());
        }
    }

}
