/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.common.tool.excel;

import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.tool.excel.annotation.ExcelEntity;
import org.apache.inlong.manager.common.tool.excel.annotation.ExcelField;
import org.apache.inlong.manager.common.tool.excel.annotation.Font;
import org.apache.inlong.manager.common.tool.excel.annotation.Style;
import org.apache.inlong.manager.common.tool.excel.validator.ExcelCellValidator;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationConstraint;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.inlong.manager.common.util.Preconditions.expectTrue;

/**
 * Utility class for working with Excel files.
 */
public class ExcelTool {

    private static final int CONSTRAINT_MAX_LENGTH = 255;

    private ExcelTool() {

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelTool.class);
    private static final String DEFAULT_SHEET_NAME = "Sheet 1";
    private static final int DEFAULT_ROW_COUNT = 30;

    /**
     * Extracts the header row from a given class and returns it as a list of strings.
     */
    public static List<String> extractHeader(List<Pair<Field, ExcelField>> fieldMetas) {
        return fieldMetas.stream().map(fieldMeta -> fieldMeta.getRight().name()).collect(Collectors.toList());
    }

    /**
     * Writes the given content to an Excel document.
     *
     * @param contents the list of contents to write
     * @param out      the output stream to write to
     * @throws IOException if there is an error writing to the output stream
     */
    public static <T> void write(List<T> contents, OutputStream out) throws IOException {
        Preconditions.expectNotEmpty(contents, "Content can not be empty!");
        Class<?> clazz = contents.get(0).getClass();
        List<Map<String, String>> maps = write2List(contents);
        doWrite(maps, clazz, out);
    }

    public static <T> void doWrite(List<Map<String, String>> maps, Class<T> clazz, OutputStream out)
            throws IOException {
        Field[] fields = clazz.getDeclaredFields();
        List<Pair<Field, ExcelField>> fieldMetas = extractFieldMetas(fields);
        if (fieldMetas.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one field must be marked as Excel Field by annotation @ExcelField in class " + clazz);
        }
        List<String> headNames = extractHeader(fieldMetas);
        try (XSSFWorkbook hwb = new XSSFWorkbook()) {
            // Set sheet name
            ExcelEntity excelEntity = clazz.getAnnotation(ExcelEntity.class);
            String sheetName = excelEntity.name();
            if (StringUtils.isBlank(sheetName)) {
                sheetName = DEFAULT_SHEET_NAME;
            }
            XSSFSheet sheet = hwb.createSheet(sheetName);
            // Set width of column
            for (int index = 0; index < fieldMetas.size(); index++) {
                Pair<Field, ExcelField> fieldMeta = fieldMetas.get(index);
                sheet.setColumnWidth(index, fieldMeta.getRight().style().width());
            }
            // Fill header with cellStyle
            List<XSSFCellStyle> headerStyles =
                    createContentCellStyle(hwb, fieldMetas, ExcelField::headerStyle, ExcelField::headerFont);
            fillSheetHeader(sheet.createRow(0), headNames, headerStyles);
            // Fill validation
            fillSheetValidation(sheet, fieldMetas, clazz.getCanonicalName());
            // Fill content if data exist.
            List<XSSFCellStyle> contentStyles =
                    createContentCellStyle(hwb, fieldMetas, ExcelField::style, ExcelField::font);
            if (CollectionUtils.isNotEmpty(maps)) {
                fillSheetContent(sheet, headNames, maps, contentStyles);
            } else {
                fillEmptySheetContent(sheet, headNames.size(), contentStyles);
            }
            hwb.write(out);
        }
        out.close();
        LOGGER.info("Database export succeeded");
    }

    private static List<Pair<Field, ExcelField>> extractFieldMetas(Field[] fields) {
        return Arrays.stream(fields)
                .peek(field -> field.setAccessible(true))
                .map(field -> Pair.of(field, field.getAnnotation(ExcelField.class)))
                .filter(fieldMeta -> fieldMeta.getRight() != null)
                .collect(Collectors.toList());
    }

    /**
     * Fills the output stream with the provided class meta.
     */
    public static <T> void write(Class<T> clazz, OutputStream out) throws IOException {
        Preconditions.expectNotNull(clazz, "Class can not be empty!");
        doWrite(null, clazz, out);
    }

    private static List<XSSFCellStyle> createContentCellStyle(XSSFWorkbook workbook,
            List<Pair<Field, ExcelField>> fieldMetas,
            Function<ExcelField, Style> styleGenerator,
            Function<ExcelField, Font> fontGenerator) {
        return fieldMetas.stream().map(fieldMeta -> {
            XSSFCellStyle style = workbook.createCellStyle();
            ExcelField excelField = fieldMeta.getRight();
            Style excelStyle = styleGenerator.apply(excelField);
            Font excelFont = fontGenerator.apply(excelField);
            // Set foreground color
            style.setFillForegroundColor(excelStyle.bgColor().getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            // Set font
            XSSFFont font = workbook.createFont();
            font.setFontName(excelFont.name());
            font.setColor(excelFont.color().getIndex());
            font.setFontHeightInPoints(excelFont.size());
            font.setBold(excelFont.bold());
            font.setItalic(excelFont.italic());
            style.setFont(font);
            // Set border
            BorderStyle borderBottom = excelStyle.bottomBorderStyle();
            BorderStyle borderTop = excelStyle.topBorderStyle();
            BorderStyle borderLeft = excelStyle.leftBorderStyle();
            BorderStyle borderRight = excelStyle.rightBorderStyle();
            BorderStyle borderAll = excelStyle.allBorderStyle();
            if (borderAll != BorderStyle.NONE) {
                borderBottom = borderTop = borderLeft = borderRight = borderAll;
            }
            style.setBorderBottom(borderBottom);
            style.setBorderTop(borderTop);
            style.setBorderLeft(borderLeft);
            style.setBorderRight(borderRight);
            // Set border color
            IndexedColors bottomBorderColor = excelStyle.bottomBorderColor();
            IndexedColors topBorderColor = excelStyle.topBorderColor();
            IndexedColors leftBorderColor = excelStyle.leftBorderColor();
            IndexedColors rightBorderColor = excelStyle.rightBorderColor();
            IndexedColors allBorderColor = excelStyle.allBorderColor();
            if (allBorderColor != IndexedColors.BLACK) {
                bottomBorderColor = topBorderColor = leftBorderColor = rightBorderColor = allBorderColor;
            }
            style.setBottomBorderColor(bottomBorderColor.getIndex());
            style.setTopBorderColor(topBorderColor.getIndex());
            style.setLeftBorderColor(leftBorderColor.getIndex());
            style.setRightBorderColor(rightBorderColor.getIndex());
            return style;
        }).collect(Collectors.toList());

    }

    private static void fillEmptySheetContent(XSSFSheet sheet, int colCount, List<XSSFCellStyle> contentCellStyles) {
        for (int index = 1; index < DEFAULT_ROW_COUNT; index++) {
            XSSFRow row = sheet.createRow(index);
            for (int colIndex = 0; colIndex < colCount; colIndex++) {
                XSSFCell cell = row.createCell(colIndex);
                cell.setCellStyle(contentCellStyles.get(colIndex));
            }
        }
    }

    /**
     * Fills the content rows of a given sheet with the provided content maps and headers.
     */
    private static void fillSheetContent(XSSFSheet sheet, List<String> heads, List<Map<String, String>> contents,
            List<XSSFCellStyle> contentStyles) {
        if (CollectionUtils.isEmpty(contents)) {
            return;
        }
        int rowSize = contents.size();
        for (int lineId = 0; lineId < rowSize; lineId++) {
            Map<String, String> line = contents.get(lineId);
            Row row = sheet.createRow(lineId + 1);
            int headSize = heads.size();
            for (int colId = 0; colId < headSize; colId++) {
                String title = heads.get(colId);
                String originValue = line.get(title);
                String value = StringUtils.isNotBlank(originValue) ? originValue : "";
                Cell cell = row.createCell(colId);
                cell.setCellValue(value);
                cell.setCellStyle(contentStyles.get(colId));
            }
        }
    }

    private static void fillSheetHeader(XSSFRow row, List<String> heads, List<XSSFCellStyle> headerStyles) {
        int headSize = heads.size();
        for (int index = 0; index < headSize; index++) {
            XSSFCell cell = row.createCell(index);
            cell.setCellValue(new XSSFRichTextString(heads.get(index)));
            cell.setCellStyle(headerStyles.get(index));
        }
    }

    /**
     * Fills the validation constraints for a given sheet based on the provided class and fieldMetas.
     *
     * @param sheet      the sheet to fill with validation constraints
     * @param fieldMetas the fieldMetas to use for validation constraints
     * @param className  the class to use for validation constraints
     */
    private static void fillSheetValidation(XSSFSheet sheet, List<Pair<Field, ExcelField>> fieldMetas,
            String className) {
        int bound = fieldMetas.size();
        for (int index = 0; index < bound; index++) {
            Pair<Field, ExcelField> excelFieldPair = fieldMetas.get(index);
            Class<? extends ExcelCellValidator> validator = excelFieldPair.getRight().validator();

            Optional<List<String>> optionalList = Optional.ofNullable(validator)
                    .filter(v -> v != ExcelCellValidator.class)
                    .map(v -> {
                        try {
                            return (ExcelCellValidator<?>) v.newInstance();
                        } catch (InstantiationException | IllegalAccessException e) {
                            LOGGER.error("Can not properly create ExcelCellValidator", e);
                            return null;
                        }
                    })
                    .map(ExcelCellValidator::constraint);
            List<String> valueOfCol = optionalList.orElseGet(Collections::emptyList);
            if (valueOfCol.isEmpty()) {
                continue;
            }
            if (String.join("\n", valueOfCol).length() > CONSTRAINT_MAX_LENGTH) {
                throw new IllegalArgumentException(
                        "field '" + excelFieldPair.getLeft().getName() + "' in class '" + className
                                + "' valid message length must be less than 255 characters");
            }

            CellRangeAddressList regions = new CellRangeAddressList(1, CONSTRAINT_MAX_LENGTH, index, index);
            XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper(sheet);
            XSSFDataValidationConstraint explicitListConstraint = (XSSFDataValidationConstraint) dvHelper
                    .createExplicitListConstraint(valueOfCol.toArray(new String[0]));
            XSSFDataValidation dataValidation =
                    (XSSFDataValidation) dvHelper.createValidation(explicitListConstraint, regions);
            sheet.addValidationData(dataValidation);
        }
    }

    /**
     * Convert a list of objects to a list of maps, where each map represents an object's fields and values.
     *
     * @param objects The list of objects to be converted.
     * @param <E>     The type of the objects to be converted.
     * @return A list of maps, where each map represents an object's fields and values.
     */
    public static <E> List<Map<String, String>> write2List(List<E> objects) {
        E firstInstance = objects.get(0);
        Class<?> firstClass = firstInstance.getClass();
        Field[] fields = firstClass.getDeclaredFields();
        expectTrue(fields.length > 0, "No method was found in the class '" + firstClass.getSimpleName() + "'");

        List<Triple<Field, String, ExcelCellDataTransfer>> fieldMeta = Arrays.stream(fields).map(field -> {
            field.setAccessible(true);
            return Pair.of(field, field.getAnnotation(ExcelField.class));
        }).filter(p -> p.getRight() != null)
                .map(p -> Triple.of(p.getLeft(), p.getRight().name(), p.getRight().x2oTransfer()))
                .collect(Collectors.toList());

        return objects.stream()
                .map(obj -> fieldMeta.stream().map(fm -> {
                    Object fieldValue;
                    try {
                        fieldValue = fm.getLeft().get(obj);
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                    String value = fm.getRight().parse2Text(fieldValue);
                    String name = fm.getMiddle();
                    return Pair.of(name, value);
                })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(Pair::getKey, Pair::getValue)))
                .collect(Collectors.toList());
    }

    /**
     * Read data from an Excel file and convert it to a list of objects of the specified class.
     *
     * @param is    The input stream of the Excel file.
     * @param clazz The class of the objects to be converted.
     * @param <E>   The type of the objects to be converted.
     * @return A list of objects of the specified class.
     * @throws IOException            If an I/O error occurs.
     * @throws IllegalAccessException If the class or its nullable constructor is not accessible.
     * @throws InstantiationException If the class that declares the underlying field is an interface or is abstract.
     */
    public static <E> List<E> read(InputStream is, Class<E> clazz)
            throws IOException, IllegalAccessException, InstantiationException, NoSuchMethodException {
        ClassMeta<E> classMeta = ClassMeta.of(clazz);
        int fieldCount = classMeta.fieldCount();

        expectTrue(fieldCount > 0, "The class contains at least one field with a @ExcelField annotation");
        XSSFWorkbook hssfWorkbook = new XSSFWorkbook(is);
        List<E> result = new LinkedList<>();
        for (int sheetIndex = 0; sheetIndex < hssfWorkbook.getNumberOfSheets(); ++sheetIndex) {
            XSSFSheet sheet = hssfWorkbook.getSheetAt(sheetIndex);
            if (sheet != null) {
                XSSFRow headerRow = sheet.getRow(0);
                // According to the name of the header row, determine the column
                for (int colIndex = 0; colIndex < fieldCount + 10 && !classMeta.matchedAll(); colIndex++) {
                    XSSFCell cell = headerRow.getCell(colIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    if (cell != null) {
                        classMeta.setFieldLocation(cell.getStringCellValue(), colIndex);
                    }
                }
                expectTrue(classMeta.matchedAll(),
                        "The first line field must be the same number of @ExcelMeta annotated fields in the class");

                int lastRowNum = sheet.getLastRowNum();
                List<E> currentResult = new ArrayList<>(lastRowNum);
                List<String> validateResult = new ArrayList<>(lastRowNum);
                for (int rowNum = 1; rowNum <= lastRowNum; ++rowNum) {
                    XSSFRow row = sheet.getRow(rowNum);
                    if (row == null) {
                        continue;
                    }
                    // Mark the row only if all cells are not null
                    Map<Integer, FieldMeta> positionFieldMetaMap = classMeta.positionFieldMetaMap;
                    boolean rowNotNull = positionFieldMetaMap.keySet()
                            .stream()
                            .map(colIndex -> row.getCell(colIndex,
                                    Row.MissingCellPolicy.RETURN_BLANK_AS_NULL) != null)
                            .reduce(false, (left, right) -> left || right);

                    // Skip if all columns are null
                    if (!rowNotNull) {
                        continue;
                    }

                    E instance = null;
                    StringBuilder colValidateResult = new StringBuilder();
                    boolean hasValueInRow = false;
                    for (Map.Entry<Integer, FieldMeta> entry : positionFieldMetaMap.entrySet()) {
                        Integer colIndex = entry.getKey();
                        FieldMeta fieldMeta = entry.getValue();
                        XSSFCell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

                        hasValueInRow = true;
                        ExcelCellDataTransfer cellDataTransfer = fieldMeta.getCellDataTransfer();
                        Object value = parseCellValue(cellDataTransfer, cell);
                        if (instance == null) {
                            instance = clazz.newInstance();
                        }
                        validateCellValue(fieldMeta, value).ifPresent(info -> colValidateResult.append("Column ")
                                .append(colIndex + 1).append(":").append(info).append(";"));
                        fieldMeta.getField().setAccessible(true);
                        fieldMeta.getField().set(instance, value);
                    }
                    if (hasValueInRow) {
                        currentResult.add(instance);
                    }
                    if (colValidateResult.length() > 0) {
                        String lineValidateResult =
                                String.format("Error in Row: %d, %s", (rowNum + 1), colValidateResult);
                        validateResult.add(lineValidateResult);
                    }
                }
                Preconditions.expectEmpty(validateResult, ErrorCodeEnum.INVALID_PARAMETER,
                        String.join("\n", validateResult));
                result.addAll(currentResult);
            }
        }
        return result;
    }

    /**
     * Parse the cell value of a given field in the Excel sheet
     *
     * @param cellDataTransfer the data transfer type of the cell
     * @param cell             the cell to parse
     * @return the parsed cell value
     */
    private static Object parseCellValue(ExcelCellDataTransfer cellDataTransfer, XSSFCell cell) {
        Object o = null;
        if (cellDataTransfer == ExcelCellDataTransfer.DATE) {
            CellType cellTypeEnum = cell.getCellType();
            if (cellTypeEnum == CellType.STRING) {
                String cellValue = cell.getStringCellValue();
                o = cellDataTransfer.parseFromText(cellValue);
            } else if (cellTypeEnum == CellType.NUMERIC) {
                o = cell.getDateCellValue();
            }
        } else {
            String value = parseCellValue(cell);
            o = value;
            if (cellDataTransfer != ExcelCellDataTransfer.NONE) {
                o = cellDataTransfer.parseFromText(value);
            }
        }
        return o;
    }

    /**
     * Parse the cell value of a given field in the Excel sheet
     *
     * @param cell the cell to parse
     * @return the parsed cell value
     */
    private static String parseCellValue(Cell cell) {
        String cellValue;
        if (cell != null) {
            cell.setCellType(CellType.STRING);
            cellValue = cell.getStringCellValue();
            if (!StringUtils.isEmpty(cellValue)) {
                cellValue = cellValue.trim();
                cellValue = cellValue.replace("\n", "");
                cellValue = cellValue.replace("\r", "");
                cellValue = cellValue.replace("\\", "/");
            }
        } else {
            cellValue = "";
        }
        return cellValue;
    }

    /**
     * Validate the cell value of a given field in the Excel sheet
     *
     * @param fieldMeta the meta information of the field to validate
     * @param value     the value of the field to validate
     */
    private static Optional<String> validateCellValue(
            FieldMeta fieldMeta,
            Object value) {
        ExcelCellValidator cellValidator = fieldMeta.getCellValidator();
        if (cellValidator != null && !cellValidator.validate(value)) {
            return Optional.of(cellValidator.getInvalidTip());
        } else {
            return Optional.empty();
        }
    }

    @Data
    static class FieldMeta implements Serializable {

        /**
         * The name of the field
         */
        private String name;

        /**
         * The name of the field in the Excel sheet
         */
        private String excelName;

        /**
         * The type of the field
         */
        private Class<?> fieldType;

        /**
         * The data transfer for the cell
         */
        private ExcelCellDataTransfer cellDataTransfer;

        /**
         * The validator for the cell
         */
        private ExcelCellValidator<?> cellValidator;

        /**
         * The field object
         */
        private transient Field field;

        /**
         * The meta of class file.
         */
        public FieldMeta() {
            // build meta
        }
    }

    /**
     * The ClassMeta class represents the metadata of a class that is used to import data from an Excel sheet.
     */
    @Data
    static class ClassMeta<T> {

        /**
         * The name of the class.
         */
        private String name;

        /**
         * The template class.
         */
        private Class<T> tClass;

        /**
         * The metadata of the fields in the class.
         */
        private List<FieldMeta> classFieldMetas;

        /**
         * The mapping of field names to their metadata.
         */
        private Map<String, FieldMeta> fieldNameMetaMap;

        /**
         * The mapping of Excel names to their metadata.
         */
        private Map<String, FieldMeta> excelNameMetaMap;

        /**
         * Whether the fields have been sorted.
         */
        private boolean isSorted = false;

        /**
         * The mapping of positions to their metadata.
         */
        private Map<Integer, FieldMeta> positionFieldMetaMap = new HashMap<>();

        private ClassMeta() {
        }

        /**
         * Create a ClassMeta object from a given template class.
         */
        public static <T> ClassMeta<T> of(Class<T> clazz)
                throws InstantiationException, IllegalAccessException, NoSuchMethodException {
            ClassMeta<T> meta = new ClassMeta<>();
            meta.setTClass(clazz);
            ExcelEntity excelEntity = clazz.getAnnotation(ExcelEntity.class);
            if (excelEntity != null) {
                meta.name = excelEntity.name();
            }

            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                ExcelField excelField = field.getAnnotation(ExcelField.class);
                if (excelField != null) {
                    Class<? extends ExcelCellValidator> validatorClass = excelField.validator();
                    ExcelCellDataTransfer excelCellDataTransfer = excelField.x2oTransfer();
                    ExcelCellValidator excelCellValidator = null;
                    if (validatorClass != ExcelCellValidator.class) {
                        excelCellValidator = validatorClass.newInstance();
                    }
                    meta.addField(field.getName(), excelField.name(), field, field.getType(),
                            excelCellDataTransfer, excelCellValidator);
                }
            }

            return meta;
        }

        private void addField(String fieldName, String excelName, Field field, Class<?> fieldType,
                ExcelCellDataTransfer cellDataTransfer, ExcelCellValidator cellValidator) {
            if (this.classFieldMetas == null) {
                this.classFieldMetas = new ArrayList<>();
            }

            if (this.excelNameMetaMap == null) {
                this.excelNameMetaMap = new HashMap<>();
            }

            if (this.fieldNameMetaMap == null) {
                this.fieldNameMetaMap = new HashMap<>();
            }

            FieldMeta fieldMeta = new FieldMeta();
            fieldMeta.setName(fieldName);
            fieldMeta.setExcelName(excelName);
            fieldMeta.setFieldType(fieldType);
            fieldMeta.setCellDataTransfer(cellDataTransfer);
            fieldMeta.setCellValidator(cellValidator);
            fieldMeta.setField(field);
            this.fieldNameMetaMap.put(fieldName, fieldMeta);
            this.excelNameMetaMap.put(excelName, fieldMeta);
            this.classFieldMetas.add(fieldMeta);
        }

        /**
         * Get the metadata of a field at a given position.
         */
        public FieldMeta field(int position) {
            return this.positionFieldMetaMap.get(position);
        }

        public int fieldCount() {
            return classFieldMetas == null ? 0 : classFieldMetas.size();
        }

        public boolean matchedAll() {
            return positionFieldMetaMap.size() == this.excelNameMetaMap.size();
        }

        public void setFieldLocation(String excelName, int colIndex) {
            if (this.excelNameMetaMap.containsKey(excelName)) {
                positionFieldMetaMap.put(colIndex, excelNameMetaMap.get(excelName));
            }
        }
    }
}
