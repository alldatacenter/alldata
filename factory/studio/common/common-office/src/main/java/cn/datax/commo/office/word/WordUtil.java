package cn.datax.commo.office.word;

import com.aspose.words.*;
import com.aspose.words.Shape;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class WordUtil {

    private WordUtil() {}

    private static volatile WordUtil instance;

    public static WordUtil getInstance() {
        if(instance == null) {
            synchronized (WordUtil.class) {
                if(instance == null) {
                    instance = new WordUtil();
                }
            }
        }
        return instance;
    }

    /**
     * 去除水印
     */
    static {
        String license =
                "<License>\n" +
                "  <Data>\n" +
                "    <Products>\n" +
                "      <Product>Aspose.Cells for Java</Product>\n" +
                "      <Product>Aspose.Words for Java</Product>\n" +
                "      <Product>Aspose.Slides for Java</Product>\n" +
                "    </Products>\n" +
                "    <EditionType>Enterprise</EditionType>\n" +
                "    <SubscriptionExpiry>20991231</SubscriptionExpiry>\n" +
                "    <LicenseExpiry>20991231</LicenseExpiry>\n" +
                "    <SerialNumber>8bfe198c-7f0c-4ef8-8ff0-acc3237bf0d7</SerialNumber>\n" +
                "  </Data>\n" +
                "  <Signature>datax</Signature>\n" +
                "</License>";
        try {
            new License().setLicense(new ByteArrayInputStream(license.getBytes("UTF-8")));
        } catch (Exception e) {}
    }

    /**
     * 获取文档
     *
     * @param fileName 模板文件 F:\模板.docx
     * @return
     * @throws Exception
     */
    public Document getDocument(String fileName) throws Exception {
        return new Document(fileName);
    }

    /**
     * 获取文档
     *
     * @param inputStream 模板文件输入流
     * @return
     * @throws Exception
     */
    public Document getDocument(InputStream inputStream) throws Exception {
        return new Document(inputStream);
    }

    /**
     * 普通数据模板 返回缓冲输入流
     *
     * @param name
     * @param value
     * @param modelPath 模板文件 F:\模板.docx
     * @return 缓冲输入流 供controller层下载
     * @throws Exception
     */
    public ByteArrayInputStream fillWordData(String[] name, Object[] value, String modelPath) throws Exception {
        Document doc = new MergeDataSource().load(name, value, modelPath);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        doc.save(bos, SaveOptions.createSaveOptions(SaveFormat.DOCX));
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        return bis;
    }

    /**
     * 普通数据模板 直接保存到指定位置
     *
     * @param name
     * @param value
     * @param modelPath 模板文件 F:\模板.docx
     * @param destPath 保存文件 F:\测试.docx
     * @throws Exception
     */
    public void fillWordData(String[] name, Object[] value, String modelPath, String destPath) throws Exception {
        Document doc = new MergeDataSource().load(name, value, modelPath);
        doc.save(destPath, SaveOptions.createSaveOptions(SaveFormat.DOCX));
    }

    /**
     * 带集合的数据模板 返回缓冲输入流
     *
     * @param name
     * @param value
     * @param modelPath 模板文件 F:\模板.docx
     * @param dataList 集合数据
     * @param tableName 集合名称
     * @throws Exception
     */
    public ByteArrayInputStream fillWordListData(String[] name, Object[] value, String modelPath, List<Map<String, Object>> dataList, String tableName) throws Exception {
        Document doc = new MergeDataSource().load(name, value, modelPath, dataList, tableName);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        doc.save(bos, SaveOptions.createSaveOptions(SaveFormat.DOCX));
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        return bis;
    }

    /**
     * 带集合的数据模板 直接保存到指定位置
     *
     * @param name
     * @param value
     * @param modelPath 模板文件 F:\模板.docx
     * @param destPath 保存文件 F:\测试.docx
     * @param dataList 集合数据
     * @param tableName 集合名称
     * @throws Exception
     */
    public void fillWordListData(String[] name, Object[] value, String modelPath, String destPath, List<Map<String, Object>> dataList, String tableName) throws Exception {
        Document doc = new MergeDataSource().load(name, value, modelPath, dataList, tableName);
        doc.save(destPath, SaveOptions.createSaveOptions(SaveFormat.DOCX));
    }

    /**
     * word转pdf
     * @param srcPath 文件路径 F:\\test\\审批流提交.docx
     * @param destPath 目标路径 F:\\test\\20200420.pdf
     * @throws Exception
     */
    public void word2pdf(String srcPath, String destPath) throws Exception {
        // 转换开始前时间
        long old = System.currentTimeMillis();
        // 要转换的word文档的路径
        Document doc = new Document(srcPath);
        // 全面支持DOC, DOCX, OOXML, RTF HTML, OpenDocument, PDF, EPUB, XPS, SWF 相互转换
        doc.save(destPath, SaveOptions.createSaveOptions(SaveFormat.PDF));
        // 转换结束后时间
        long now = System.currentTimeMillis();
        System.out.println("共耗时：" + ((now - old) / 1000.0) + "秒");
    }

    /**
     * 创建空文档
     *
     * @param destPath 文件路径 F:\\test\\审批流提交.docx
     * @return
     */
    public void createWord(String destPath) throws Exception {
        Document doc = new Document();
        doc.save(destPath, SaveOptions.createSaveOptions(SaveFormat.DOCX));
    }

    /**
     * 加水印方法
     *
     * @param doc           word文件流
     * @param watermarkText 水印内容
     */
    public void insertWatermarkText(Document doc, String watermarkText) throws Exception {
        Shape watermark = new Shape(doc, ShapeType.TEXT_PLAIN_TEXT);
        watermark.setName("WaterMark");
        watermark.getTextPath().setText(watermarkText);
        watermark.getTextPath().setFontFamily("Arial");
        watermark.setWidth(500);
        watermark.setHeight(100);
        watermark.setRotation(-40);
        watermark.getFill().setColor(Color.GRAY);
        watermark.setStrokeColor(Color.GRAY);
        watermark.setRelativeHorizontalPosition(RelativeHorizontalPosition.PAGE);
        watermark.setRelativeVerticalPosition(RelativeVerticalPosition.PAGE);
        watermark.setWrapType(WrapType.NONE);
        watermark.setVerticalAlignment(VerticalAlignment.CENTER);
        watermark.setHorizontalAlignment(HorizontalAlignment.CENTER);
        Paragraph watermarkPara = new Paragraph(doc);
        watermarkPara.appendChild(watermark);
        for (Section sect : doc.getSections()) {
            insertWatermarkIntoHeader(watermarkPara, sect, HeaderFooterType.HEADER_PRIMARY);
            insertWatermarkIntoHeader(watermarkPara, sect, HeaderFooterType.HEADER_FIRST);
            insertWatermarkIntoHeader(watermarkPara, sect, HeaderFooterType.HEADER_EVEN);
        }
    }

    private void insertWatermarkIntoHeader(Paragraph watermarkPara, Section sect, int headerType) throws Exception {
        HeaderFooter header = sect.getHeadersFooters().getByHeaderFooterType(headerType);
        if (header == null) {
            header = new HeaderFooter(sect.getDocument(), headerType);
            sect.getHeadersFooters().add(header);
        }
        header.appendChild(watermarkPara.deepClone(true));
    }

    public static void main(String[] args) throws Exception {
//        Map<String, Object> map = new HashMap<>();
//        map.put("companyName", "测试");
//        map.put("totalSalary", new BigDecimal("12.34"));
//        List<Map<String, Object>> list = new ArrayList<>();
//        Map<String, Object> map1 = new HashMap<>();
//        map1.put("id", "1");
//        map1.put("name", "测试1");
//        map1.put("age", 12);
//        map1.put("sex", "男");
//        map1.put("salary", new BigDecimal("5.0"));
//        list.add(map1);
//        Map<String, Object> map2 = new HashMap<>();
//        map2.put("id", "2");
//        map2.put("name", "测试2");
//        map2.put("age", 14);
//        map2.put("sex", "女");
//        map2.put("salary", new BigDecimal("7.34"));
//        list.add(map2);
//        List<String> objects1 = new ArrayList<>();
//        List<Object> objects2 = new ArrayList<>();
//        for(Map.Entry<String, Object> entry : map.entrySet()){
//            objects1.add(entry.getKey());
//            objects2.add(entry.getValue());
//        }
//        WordUtil.getInstance().fillWordListData(objects1.toArray(new String[objects1.size()]), objects2.toArray(new Object[objects2.size()]), "F:\\test\\模板.docx", "F:\\test\\123.docx", list, "workerList");
//        WordUtil.getInstance().word2pdf("F:\\test.docx", "F:\\20200420.pdf");
//
//        // 用户表（子表） TableStart:UserList TableEnd:UserList
//        DataTable userTable = new DataTable("UserList");
//        userTable.getColumns().add("id");
//        userTable.getColumns().add("name");
//        userTable.getColumns().add("age");
//        userTable.getColumns().add("sex");
//        userTable.getColumns().add("salary");
//        for (int i = 1; i < 3; i++) {
//            DataRow row = userTable.newRow();
//            row.set(0, i);
//            row.set(1, "name" + i);
//            row.set(2, "age" + i);
//            row.set(3, "sex" + i);
//            row.set(4, "salary" + i);
//            userTable.getRows().add(row);
//        }
//        // 分数表（子表） TableStart:ScoreList TableEnd:ScoreList
//        DataTable scoreTable = new DataTable("ScoreList");
//        scoreTable.getColumns().add("id");
//        scoreTable.getColumns().add("uid");
//        scoreTable.getColumns().add("score");
//        for (int i = 1; i < 3; i++) {
//            DataRow row = scoreTable.newRow();
//            row.set(0, i);
//            row.set(1, i);
//            row.set(2, 10*i);
//            scoreTable.getRows().add(row);
//        }
//        // 提供数据源
//        DataSet dataSet = new DataSet();
//        dataSet.getTables().add(userTable);
//        dataSet.getTables().add(scoreTable);
//        DataRelation dataRelation = new DataRelation("UserScoreRelation", userTable.getColumns().get("id"), scoreTable.getColumns().get("uid"));
//        dataSet.getRelations().add(dataRelation);
//        // 合并模版
//        Document doc = new Document("F:\\test.docx");
//        //提供数据源
//        String[] fieldNames = new String[] {"name", "address"};
//        Object[] fieldValues = new Object[] {"张三", "陕西咸阳"};
//        //合并模版，相当于页面的渲染
//        MailMerge mailMerge = doc.getMailMerge();
//        mailMerge.execute(fieldNames, fieldValues);
//        mailMerge.executeWithRegions(dataSet);
//        doc.save("F:\\test_r.docx", SaveOptions.createSaveOptions(SaveFormat.DOCX));
    }
}
