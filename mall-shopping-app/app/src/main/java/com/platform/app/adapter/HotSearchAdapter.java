package com.platform.app.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.platform.app.R;

import java.util.List;

/**
 * <pre>
 *   author : wulinhao
 *   e-mail : 2572694660@qq.com
 *   time   : 2019/08/28
 *   desc   : 热门搜索的适配器
 * </pre>
 */

public class HotSearchAdapter extends BaseQuickAdapter<String, BaseViewHolder> {

    public HotSearchAdapter(List<String> datas) {
        super(R.layout.item_search, datas);
    }

    @Override
    protected void convert(BaseViewHolder holder, String item) {
        holder.setText(R.id.tv_content, item);
    }
}


