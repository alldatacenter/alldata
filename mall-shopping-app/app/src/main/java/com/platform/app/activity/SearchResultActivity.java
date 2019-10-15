package com.platform.app.activity;

import android.text.TextUtils;
import android.widget.TextView;

import com.platform.app.R;

import butterknife.BindView;

/**
 * <pre>
 *   author : wulinhao
 *   e-mail : 2572694660@qq.com
 *   time   : 2019/08/30
 *   desc   : 搜索结果
 * </pre>
 */

public class SearchResultActivity extends BaseActivity {

    @BindView(R.id.tv_result)
    TextView mTextView;

    @Override
    protected void init() {

        String search = getIntent().getStringExtra("search");
        if (!TextUtils.isEmpty(search)) {
            mTextView.setText(search);
        }

    }

    @Override
    protected int getContentResourseId() {
        return R.layout.activity_search_result;
    }
}
