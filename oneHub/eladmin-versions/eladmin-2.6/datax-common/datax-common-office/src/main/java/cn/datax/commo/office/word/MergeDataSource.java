package cn.datax.commo.office.word;

import com.aspose.words.Document;
import com.aspose.words.MailMerge;

import java.util.List;
import java.util.Map;

public class MergeDataSource {

    /**
     * word模板普通数据填充
     * @param name
     * @param value
     * @param modelPath
     * @return
     * @throws Exception
     */
    public Document load(String[] name, Object[] value, String modelPath) throws Exception {
        Document doc = new Document(modelPath);
        // 这里可以做特殊字段处理（如：图片插入、字符对应的特殊符号[https://wenku.baidu.com/view/81b41244336c1eb91a375dcb.html]）
//        DocumentBuilder builder = new DocumentBuilder(doc);
//        builder.moveToMergeField(key);
//        builder.insertImage((BufferedImage) value);
        MailMerge merge = doc.getMailMerge();
        merge.execute(name, value);
        return doc;
    }


    /**
     * word模板里有集合的表格填充
     * @param name
     * @param value
     * @param modelPath
     * @param dataList
     * @return
     * @throws Exception
     */
    public Document load(String[] name, Object[] value, String modelPath, List<Map<String, Object>> dataList, String tableName) throws Exception {
        Document doc = new Document(modelPath);
        MailMerge merge = doc.getMailMerge();
        merge.execute(name, value);
        merge.executeWithRegions(new MapMailMergeDataSource(dataList, tableName));
        return doc;
    }
}
