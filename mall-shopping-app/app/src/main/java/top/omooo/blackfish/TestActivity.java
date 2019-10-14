package top.omooo.blackfish;

import android.content.Context;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import top.omooo.blackfish.view.TagsLayout;

/**
 * Created by SSC on 2018/3/26.
 */

public class TestActivity extends NewBaseActivity {

    private static final String TAG = "TestActivity";
    @BindView(R.id.tags_layout)
    TagsLayout mTagsLayout;

    private Context mContext;

    @Override
    public int getLayoutId() {

        return R.layout.test_2;
    }

    @Override
    public void initViews() {
        mContext = TestActivity.this;

        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        String[] string={"从我写代码那天起，我就没有打算写代码","从我写代码那天起","我就没有打算写代码","没打算","写代码"};
        for (int i = 0; i < string.length; i++) {
            TextView textView = new TextView(this);
            textView.setText(string[i]);
            textView.setTextColor(Color.WHITE);
            textView.setBackgroundColor(getResources().getColor(R.color.colorQQEmailText));
            mTagsLayout.addView(textView, lp);
        }
    }

    @Override
    protected void initData() {

    }

}
