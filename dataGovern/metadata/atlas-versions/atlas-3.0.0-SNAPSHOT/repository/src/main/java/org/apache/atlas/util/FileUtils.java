/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.util;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.apache.atlas.repository.Constants.SupportedFileExtensions.*;

public class FileUtils {
    public static final String PIPE_CHARACTER   = "|";
    public static final String COLON_CHARACTER  = ":";
    public static final String ESCAPE_CHARACTER = "\\";

    //BusinessMetadata attributes association uploads
    public static final int TYPENAME_COLUMN_INDEX = 0;
    public static final int UNIQUE_ATTR_VALUE_COLUMN_INDEX = 1;
    public static final int BM_ATTR_NAME_COLUMN_INDEX = 2;
    public static final int BM_ATTR_VALUE_COLUMN_INDEX = 3;
    public static final int UNIQUE_ATTR_NAME_COLUMN_INDEX = 4;

    public static List<String[]> readFileData(String fileName, InputStream inputStream) throws AtlasBaseException {
        List<String[]>                        ret;
        String                                extension     = FilenameUtils.getExtension(fileName);

        if (extension.equalsIgnoreCase(CSV.name())) {
            ret = readCSV(inputStream);
        } else if (extension.equalsIgnoreCase(XLS.name()) || extension.equalsIgnoreCase(XLSX.name())) {
            ret = readExcel(inputStream, extension);
        } else {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_FILE_TYPE, fileName);
        }

        if (CollectionUtils.isEmpty(ret)) {
            throw new AtlasBaseException(AtlasErrorCode.NO_DATA_FOUND);
        }

        return ret;
    }

    public static List<String[]> readCSV(InputStream inputStream) throws AtlasBaseException {
        List<String[]> ret = new ArrayList<>();

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream))) {
            String[] header = csvReader.readNext();

            if (header == null || header.length == 0) {
                return ret;
            }

            String[] data;

            while ((data = csvReader.readNext()) != null) {
                if (data.length > 1) {
                    ret.add(data);
                }
            }
        } catch (CsvValidationException | IOException e) {
            throw new AtlasBaseException(AtlasErrorCode.NOT_VALID_FILE, CSV.name());
        }

        return ret;
    }

    public static List<String[]> readExcel(InputStream inputStream, String extension) throws AtlasBaseException {
        List<String[]> ret        = new ArrayList<>();

        try (Workbook excelBook = extension.equalsIgnoreCase(XLS.name()) ? new HSSFWorkbook(inputStream) : new XSSFWorkbook(inputStream)) {
            Sheet          excelSheet = excelBook.getSheetAt(0);
            Iterator       itr        = excelSheet.rowIterator();
            Row            headerRow  = (Row) itr.next();

            if (isRowEmpty(headerRow)) {
                return ret;
            }

            while (itr.hasNext()) {
                Row row = (Row) itr.next();

                if (!isRowEmpty(row)) {
                    String[] data = new String[row.getLastCellNum()];

                    for (int i = 0; i < row.getLastCellNum(); i++) {
                        data[i] = (row.getCell(i) != null) ? row.getCell(i).getStringCellValue().trim() : null;
                    }

                    ret.add(data);
                }
            }
        } catch (IOException e) {
            throw new AtlasBaseException(AtlasErrorCode.NOT_VALID_FILE, XLS.name());
        }

        return ret;
    }

    private static boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);

            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }

        return true;
    }

    public static String getBusinessMetadataHeaders() {
        List<String> bMHeader = new ArrayList<>();

        bMHeader.add("EntityType");
        bMHeader.add("EntityUniqueAttributeValue");
        bMHeader.add("BusinessAttributeName");
        bMHeader.add("BusinessAttributeValue");
        bMHeader.add("EntityUniqueAttributeName[optional]");

        return StringUtils.join(bMHeader, ",");
    }
}