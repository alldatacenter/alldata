package cn.datax.commo.office.word;

import com.aspose.words.IMailMergeDataSource;
import com.aspose.words.ref.Ref;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapMailMergeDataSource implements IMailMergeDataSource {

    private List<Map<String, Object>> dataList;

    private int index;

    /**
     * word模板中的«TableStart:tableName»«TableEnd:tableName»对应
     */
    private String tableName = null;

    /**
     * @param dataList  数据集
     * @param tableName 与模板中的Name对应
     */
    public MapMailMergeDataSource(List<Map<String, Object>> dataList, String tableName) {
        this.dataList = dataList;
        this.tableName = tableName;
        index = -1;
    }

    /**
     * @param data      单个数据集
     * @param tableName 与模板中的Name对应
     */
    public MapMailMergeDataSource(Map<String, Object> data, String tableName) {
        if (this.dataList == null) {
            this.dataList = new ArrayList<Map<String, Object>>();
            this.dataList.add(data);
        }
        this.tableName = tableName;
        index = -1;
    }

    /**
     * 获取结果集总数
     *
     * @return
     */
    private int getCount() {
        return this.dataList.size();
    }

    @Override
    public IMailMergeDataSource getChildDataSource(String arg0)
            throws Exception {
        return null;
    }

    @Override
    public String getTableName() throws Exception {
        return this.tableName;
    }

    /**
     * 实现接口
     * 获取当前index指向数据行的数据
     * 将数据存入args数组中即可
     *
     * @return ***返回false则不绑定数据***
     */
    @Override
    public boolean getValue(String key, Ref<Object> args) throws Exception {
        if (index < 0 || index >= this.getCount()) {
            return false;
        }
        if (args != null) {
            args.set(this.dataList.get(index).get(key));
            return true;
        } else {
            return false;
        }
    }

    /**
     * 实现接口
     * 判断是否还有下一条记录
     */
    @Override
    public boolean moveNext() throws Exception {
        index += 1;
        if (index >= this.getCount()) {
            return false;
        }
        return true;
    }
}
