package com.platform.app.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.platform.app.R;
import com.platform.app.bean.Category;

import java.util.List;

/**
 * <pre>
 *     author : wulinhao
 *     time   : 2019/08/08
 *     desc   : 分类一级菜单.
 *     version: 1.0
 * </pre>
 */


public class CategoryAdapter extends BaseQuickAdapter<Category, BaseViewHolder> {

    public CategoryAdapter(List<Category> datas) {
        super(R.layout.template_single_text, datas);
    }

    @Override
    protected void convert(BaseViewHolder holder, Category item) {
        holder.setText(R.id.textView, item.getName());
    }
}
