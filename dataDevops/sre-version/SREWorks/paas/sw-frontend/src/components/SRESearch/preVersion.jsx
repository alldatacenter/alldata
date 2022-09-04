import React from "react";
// import {  Search } from '@alife/next';

import { Select, Tooltip, Divider, Badge, Input } from "antd";
import PropTypes from "prop-types";

import $ from "jquery";
import _ from "lodash";

// t-search 迁移来的

import SearchService from "./services/service";

import TabsRender from "./components/TabsRender";
import FlatList from "./components/FlatList"

import onClickOutside from "react-onclickoutside";
import classNames from "classnames";

import "./main.scss";

const Option = Select.Option;
const OptGroup = Select.OptGroup;
const { Search } = Input;

class SRESearch extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      overlayVisible: false,
      temp_search_content: "",
      search_content: "",
      is_force: false,
      hotKeyOptions: [],
      options: [],
      keywords: [],
      isNeedSuggestion: false,
    };
    this.onBlur = this.onBlur.bind(this);
    this.onSearchKeyword = this.onSearchKeyword.bind(this);
    this.onSearch = this.onSearch.bind(this);
    this.onFocus = this.onFocus.bind(this);
    this.onChange = this.onChange.bind(this);
    this.handleClickOutside = this.handleClickOutside.bind(this);
    this.onKeyDown = this.onKeyDown.bind(this);
    this.getHotKeywords = this.getHotKeywords.bind(this);
    this.getSuggestionList = this.getSuggestionList.bind(this);
    this.getSuggestionListDebounce = _.debounce(this.getSuggestionList, 150);
  }


  getHotKeywords(userEmpId, sreworksSearchPath, category, limit) {
    SearchService.getHotKeywords(userEmpId, sreworksSearchPath, category, limit)
      .then(res => {
        this.setState({
          hotKeyOptions: res,
        });

      }).catch(err => {
      });
  }

  getCommonKeywords(userEmpId, sreworksSearchPath, category, limit) {
    SearchService.getCommonKeywords(userEmpId, sreworksSearchPath, category, limit)
      .then(res => {
        this.setState({
          keywords: res,
        });
      }).catch(err => {
        this.setState({
          keywords: [],
        });
      });
  }

  getSuggestionList(userEmpId, sreworksSearchPath, category, search_content, page, pageSize) {
    if (search_content !== "") {
      SearchService.searchSuggestions(userEmpId, sreworksSearchPath, category, search_content, page, pageSize)
        .then(res => {
          this.setState({
            options: res,
            is_force: false,
          });
        }).catch(err => {
          this.setState({
            options: [],
            is_force: false,

          });
        });
    } else if (search_content === "") {
      this.getHotKeywords(userEmpId, sreworksSearchPath, category, 10);
    }

  }


  onSearchKeyword(value) {
    this.setState({
      temp_search_content: value,
    });
    this.onSearch(value);
  }

  onSearch(value) {
    this.setState({ temp_search_content: value });
    if (value !== "") {
      this.setState({
        // isNeedSuggestion: false,
        overlayVisible: true,
        is_force: true,
        options: [],
      });
    }
    this.hasSearch = true;
  }

  handleSearch = (value) => {
    this.setState({
      search_content: value,
      is_force: false,
    });
    this.getSuggestionListDebounce(this.props.userEmpId, this.props.sreworksSearchPath, this.props.category, value, 1, 10);
  };

  onChange(value, options = undefined) {
    if (!options) {
      this.setState({
        search_content: value,
        isNeedSuggestion: true,
      });
      this.getSuggestionListDebounce(this.props.userEmpId, this.props.sreworksSearchPath, this.props.category, value, 1, 10);
    } else {
      this.setState({
        isNeedSuggestion: false,
      })
    }
  }

  handleClickOutside = evt => {
    this.setState({
      overlayVisible: false,
      is_force: false,
      // isNeedSuggestion: false
    });
  };

  onBlur() {
    let { temp_search_content } = this.state;
    if (!this.state.search_content) {
      temp_search_content = "";
    }
    this.setState({
      temp_search_content,
      // overlayVisible:false,
      isNeedSuggestion: false,
    });
  }
  onClear = () => {
    this.setState({
      temp_search_content: '',
      search_content: '',
      isNeedSuggestion: false,
    })
  }
  onFocus() {
    this.setState({ temp_search_content: '' });
    if (this.state.search_content !== "") {
      this.setState({
        overlayVisible: false,
        is_force: false,
      });
    } else {
      this.setState({
        overlayVisible: false,
        isNeedSuggestion: true,
      });
    }
    this.getSuggestionListDebounce(this.props.userEmpId, this.props.sreworksSearchPath, this.props.category, this.state.temp_search_content, 1, 10);

  }

  onKeyDown(e) {

    console.log(e.keyCode === 13 && !this.hasSearch);
    console.log(this.state.temp_search_content)
    if (e.keyCode === 13 && !this.hasSearch) {
      // this.refs['search-select'].blur();
      this.onSearch(this.state.search_content)
      this.setState({ overlayVisible: true })
    }
    this.hasSearch = false
  }

  componentWillMount() {
    // this.getHotKeywords(this.props.userEmpId, this.props.sreworksSearchPath, this.props.category, 10);
    if (this.props.isShowKeywords) {
      this.getCommonKeywords(this.props.userEmpId, this.props.sreworksSearchPath, this.props.category, this.props.keywordNum);
    }
  }

  handleChange = (value, options) => {
    this.setState({
      // isNeedSuggestion: true,
      temp_search_content: value,
      is_force: false,
    });

    // console.log("value,options",value,options)
    // console.log("typeof options.props.children", typeof options.props.children);

    if (typeof options.props.children === "object") {
      //说明选中了option，此时不需要去推荐了
      this.setState({
        isNeedSuggestion: false,
      });
    } else {
      this.setState({
        // search_content: "",
        isNeedSuggestion: true,
      });
      this.getSuggestionListDebounce(this.props.userEmpId, this.props.sreworksSearchPath, this.props.category, value, 1, 10);
    }
  };


  componentDidMount() {

    setTimeout(function () {
      $(`.biz-tSearch.${this.props.className} input`).css({ width: "93%" }).after("<i class=\"anticon anticon-search ant-input-search-icon\"></i>");
    }.bind(this), 0);
  }

  render() {
    RegExp.quote = function (str) {
      return (str && str.replace(/([.?*+^$[\]\\(){}|-])/g, "\\$1")) || '';
    };
    let { search_content, overlayVisible, temp_search_content } = this.state;
    return (
      <div className={classNames("biz-tSearch", this.props.className)} onKeyDown={this.onKeyDown}>
        {/*<Select showSearch  onChange={this.onChange}*/}
        {/*          onSelect={this.onSearch}/>*/}
        <Select
          allowClear
          showSearch
          filterOption={false}
          defaultActiveFirstOption={false}
          placeholder={this.props.placeholder}
          value={temp_search_content || undefined}
          onFocus={this.onFocus}
          onBlur={this.onBlur}
          // onChange={this.onChange}
          onSelect={this.onSearch}
          onSearch={this.handleSearch}
          ref="search-select"
          onClear={this.onClear}
          size={this.props.size}
          dropdownMatchSelectWidth={true}
          dropdownStyle={{ display: !this.state.overlayVisible ? '' : 'none' }}
          open={this.state.isNeedSuggestion}
        >
          <OptGroup label={
            !this.state.overlayVisible && ((this.state.search_content !== "") ?
              <div style={{ textAlign: "center", borderBottom: "1px dashed" }}>
                <span style={{ color: "#2db7f5", fontSize: "14px" }}>✩✩前10推荐列表✩✩</span>
                <span>回车搜索更多</span>
              </div> :
              <div style={{ textAlign: "center", borderBottom: "1px dashed" }}>
                <span style={{ color: "#2db7f5", fontSize: "14px" }}>✩✩前10热门搜索✩✩</span>
              </div>)
          }>
            {this.state.search_content !== "" ?
              //搜索匹配列表
              this.state.options.map(item =>
                <Option key={_.toString(item.content)} value={_.toString(item.content)}>
                  {item.type ? <span>
                    <span style={{ fontSize: "12px" }}>{item.type}</span>
                    <Divider type="vertical" />
                  </span> : null}
                  <Tooltip title={_.toString(item.content)} placement="topLeft" overlayStyle={{ wordBreak: "break-all" }}>
                    <span
                      dangerouslySetInnerHTML={{ __html: _.toString(item.content).replace(new RegExp(RegExp.quote(temp_search_content), "igm"), "<span style=\"color: red\">" + _.toString(item.content).match(new RegExp(RegExp.quote(temp_search_content), "i")) + "</span>") }}
                      onClick={e => console.log("e", e)}></span>
                  </Tooltip>
                </Option>,
              ) :
              //热门搜索列表
              this.state.hotKeyOptions.map((item, index) =>
                <Option key={_.toString(item.content)} value={item.content}>
                  <Badge offset={[8, -2]} count={index + 1} style={index > 2 ? {
                    backgroundColor: "#fff",
                    color: "#999",
                    boxShadow: "0 0 0 1px #d9d9d9 inset",
                  } : {}} />
                  <Divider type="vertical" />
                  <Tooltip title={_.toString(item.content)} placement="topLeft" overlayStyle={{ wordBreak: "break-all" }}>
                    <span
                      dangerouslySetInnerHTML={{ __html: _.toString(item.content).replace(new RegExp(RegExp.quote(temp_search_content), "igm"), "<span style=\"color: red\">" + _.toString(item.content).match(new RegExp(RegExp.quote(temp_search_content), "i")) + "</span>") }}></span>
                  </Tooltip>
                </Option>,
              )

            }
          </OptGroup>
        </Select>
        <div className="panel-keyword">
          <p>
            {
              this.state.keywords.map(function (kw) {
                return <a style={{ paddingRight: "10px" }}
                  onClick={e => this.onSearchKeyword(kw.content)}>{kw.content}</a>;
              }.bind(this))
            }
          </p>
        </div>
        <div style={{ display: overlayVisible ? "" : "none", position: 'absolute' }} className="panel-container">
          <FlatList {...this.props}
            is_force={this.state.is_force}
            search_content={search_content} />

        </div>
      </div>
    );
  }
}

var clickOutsideConfig = {
  excludeScrollbar: true,
};

SRESearch.propTypes = {
  userEmpId: PropTypes.string,
  category: PropTypes.string,
  sreworksSearchPath: PropTypes.string,
  placeholder: PropTypes.string,
  moreLinkPrefix: PropTypes.string,
  className: PropTypes.string,
  size: PropTypes.string,
  isShowKeywords: PropTypes.bool,
  keywordNum: PropTypes.number,
};

SRESearch.defaultProps = {
  size: "default",
  isShowKeywords: false,
  keywordNum: 5,
};


export default onClickOutside(SRESearch, clickOutsideConfig);
