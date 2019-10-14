package top.omooo.blackfish.MallPagerActivity;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import top.omooo.blackfish.NewBaseActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.utils.KeyBoardUtil;
import top.omooo.blackfish.view.CustomToast;
import top.omooo.blackfish.view.TagsLayout;
import top.omooo.router_annotations.Router;

/**
 * Created by SSC on 2018/4/1.
 */

@Router("search")
public class SearchActivity extends NewBaseActivity {


    @BindView(R.id.tv_search_cancel)
    TextView mTvSearchCancel;
    @BindView(R.id.tags_recent)
    TagsLayout mTagsRecent;
    @BindView(R.id.tags_hot)
    TagsLayout mTagsHot;
    @BindView(R.id.et_search)
    EditText mEtSearch;

    private Context mContext;
    private static final String TAG = "SearchActivity";

    private ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    private String[] hotText = new String[]{"OPPO R15", "面膜", "Iphone X", "AirPods", "防晒", "蓝牙耳机", "一加5T"};
    private List<String> recentText = new ArrayList<>();
    private String[] strings = new String[15];



    @Override
    public int getLayoutId() {
        return R.layout.activity_mall_search_layout;
    }

    @Override
    public void initViews() {
        mContext = SearchActivity.this;

        // TODO: 2018/4/2 点击EditText软键盘不显示？？？
        mEtSearch.setFocusableInTouchMode(true);
        mEtSearch.requestFocus();
        KeyBoardUtil.showKeyBoard(mEtSearch);

        mEtSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_SEARCH:
                        // TODO: 2018/4/27 倒序排列，并去重
                        if (!mEtSearch.getText().toString().equals("")) {
                            CustomToast.show(mContext, "搜索中...");
                            recentText.clear();
                            recentText.add(mEtSearch.getText().toString());
                            for (int i = 0; i < recentText.size(); i++) {
                                strings[i] = recentText.get(i);
                            }
                            addSearchView(strings, false, mTagsRecent);
                            mEtSearch.setText("");
                        } else {
                            CustomToast.show(mContext, "请输入搜索关键字");
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    @Override
    protected void initData() {

        //初始化热门搜索
        addSearchView(hotText, true, mTagsHot);
    }


    @Override
    public void onBackPressed() {
        KeyBoardUtil.closeKeyBoard(mEtSearch);
        super.onBackPressed();
    }


    @OnClick({R.id.et_search, R.id.tv_search_cancel})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.et_search:
                // TODO: 2018/4/27 点击EditText不会自动弹出软键盘
                mEtSearch.setFocusableInTouchMode(true);
                mEtSearch.requestFocus();
                KeyBoardUtil.showKeyBoard(mEtSearch);
                break;
            case R.id.tv_search_cancel:
                KeyBoardUtil.closeKeyBoard(mEtSearch);
                finshActivity();
                break;
            default:
                break;
        }
    }

    private void addSearchView(String[] strings, boolean isHotSearch,TagsLayout tagsLayout) {
        for (int i = 0; i < strings.length; i++) {
            TextView textView = new TextView(mContext);
            final String text = "   " + strings[i] + "   ";
            textView.setText(text);
            textView.setPadding(10, 10, 10, 10);
            if (i < 3 && isHotSearch) {
                textView.setTextColor(getColor(R.color.colorSugType));
                textView.setBackground(getDrawable(R.drawable.shape_hot_search));
            } else if (!text.equals("   null   ")) {
                textView.setTextColor(getColor(R.color.splash_main_title_color));
                textView.setBackground(getDrawable(R.drawable.shape_search));
            } else {
                break;
            }
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSearchTextClick.onClick(text);
                }
            });
            tagsLayout.addView(textView, lp);
        }
    }

    private OnSearchTextClick mSearchTextClick=new OnSearchTextClick() {
        @Override
        public void onClick(String text) {
            CustomToast.show(mContext, "搜索 " + text.trim() + " 中...");
        }
    };

    private interface OnSearchTextClick {
        void onClick(String text);
    }

}
