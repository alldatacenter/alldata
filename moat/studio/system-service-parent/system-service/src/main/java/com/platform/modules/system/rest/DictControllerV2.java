
package com.platform.modules.system.rest;

import com.platform.annotation.Log;
import com.platform.exception.BadRequestException;
import com.platform.modules.system.domain.Dict;
import com.platform.modules.system.service.DictService;
import com.platform.modules.system.service.dto.DictQueryCriteria;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author AllDataDC
 * @date 2023-01-27
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "系统：字典管理")
@RequestMapping("/api/dict")
public class DictControllerV2 {

    private final DictService dictService;
    private static final String ENTITY_NAME = "dict";

    @ApiOperation("导出字典数据")
    @GetMapping(value = "/download")
    @PreAuthorize("@el.check('dict:list')")
    public void exportDict(HttpServletResponse response, DictQueryCriteria criteria) throws IOException {
        downloadExcel(response);

//        dictService.download(dictService.queryAll(criteria), response);
    }

    public void downloadExcel(HttpServletResponse response) throws IOException {
        // 模拟数据
        List<Map<String, Object>> dataList = new ArrayList<>();

        Map<String, Object> row1 = new HashMap<>();
        row1.put("ID", 1);
        row1.put("Name", "张三");
        row1.put("Age", 25);
        dataList.add(row1);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("ID", 2);
        row2.put("Name", "李四");
        row2.put("Age", 30);
        dataList.add(row2);

        String formattedDateTime = fetchFileNameTime();
        // 生成 .xlsx 文件
        String projectPath = System.getProperty("user.dir");
        String filePath = String.format("%s/%s.xlsx", projectPath, formattedDateTime);
        File file = new File(filePath);
        writeExcelFile(dataList, filePath, true); // true 表示生成 .xlsx 文件


        returnFile(file, response);

        deleteXlsxFile(file);

        System.out.println("Excel 文件已生成: " + filePath);
    }

    private String fetchFileNameTime() {
        // 获取当前的日期和时间
        LocalDateTime currentDateTime = LocalDateTime.now();

        // 格式化日期和时间
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("-yyyyMMddHHmmss");
        String formattedDateTime = currentDateTime.format(formatter);
        return formattedDateTime;
    }

    private void deleteXlsxFile(File file) {
        try {
            FileUtils.forceDelete(file);
            System.out.println("文件删除成功: " + file.getName());
        } catch (IOException e) {
            System.err.println("文件删除失败: " + e.getMessage());
        }
    }

    private void returnFile(File file, HttpServletResponse response) throws IOException {
        //response为HttpServletResponse对象
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        //test.xls是弹出下载对话框的文件名，不能为中文，中文请自行编码
        response.setHeader("Content-Disposition", "attachment;filename=file.xlsx");

        // 获取输出流
        OutputStream outputStream = response.getOutputStream();
        // 读取文件内容并写入响应流
        try (InputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[4096 * 10000];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            // 刷新并关闭流
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeExcelFile(List<Map<String, Object>> dataList, String filePath, boolean isXlsx) {
        Workbook workbook;
        if (isXlsx) {
            workbook = new XSSFWorkbook(); // 生成 .xlsx 文件
        } else {
            workbook = new HSSFWorkbook(); // 生成 .xls 文件
        }

        try {
            Sheet sheet = workbook.createSheet("Sheet1");

            if (!dataList.isEmpty()) {
                // 创建标题行
                Row headerRow = sheet.createRow(0);
                int colNum = 0;
                for (String key : dataList.get(0).keySet()) {
                    headerRow.createCell(colNum++).setCellValue(key);
                }

                // 填充数据
                int rowNum = 1;
                for (Map<String, Object> rowData : dataList) {
                    Row row = sheet.createRow(rowNum++);
                    colNum = 0;
                    for (Object value : rowData.values()) {
                        Cell cell = row.createCell(colNum++);
                        if (value != null) {
                            if (value instanceof Number) {
                                cell.setCellValue(((Number) value).doubleValue());
                            } else if (value instanceof Boolean) {
                                cell.setCellValue((Boolean) value);
                            } else {
                                cell.setCellValue(value.toString());
                            }
                        } else {
                            cell.setCellValue("");
                        }
                    }
                }
            }

            // 写入到文件
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @ApiOperation("查询字典")
    @GetMapping(value = "/all")
    @PreAuthorize("@el.check('dict:list')")
    public ResponseEntity<Object> queryAllDict(){
        return new ResponseEntity<>(dictService.queryAll(new DictQueryCriteria()),HttpStatus.OK);
    }

    @ApiOperation("查询字典")
    @GetMapping
    @PreAuthorize("@el.check('dict:list')")
    public ResponseEntity<Object> queryDict(DictQueryCriteria resources, Pageable pageable){
        return new ResponseEntity<>(dictService.queryAll(resources,pageable),HttpStatus.OK);
    }

    @Log("新增字典")
    @ApiOperation("新增字典")
    @PostMapping
    @PreAuthorize("@el.check('dict:add')")
    public ResponseEntity<Object> createDict(@Validated @RequestBody Dict resources){
        if (resources.getId() != null) {
            throw new BadRequestException("A new "+ ENTITY_NAME +" cannot already have an ID");
        }
        dictService.create(resources);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Log("修改字典")
    @ApiOperation("修改字典")
    @PutMapping
    @PreAuthorize("@el.check('dict:edit')")
    public ResponseEntity<Object> updateDict(@Validated(Dict.Update.class) @RequestBody Dict resources){
        dictService.update(resources);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("删除字典")
    @ApiOperation("删除字典")
    @DeleteMapping
    @PreAuthorize("@el.check('dict:del')")
    public ResponseEntity<Object> deleteDict(@RequestBody Set<Long> ids){
        dictService.delete(ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}