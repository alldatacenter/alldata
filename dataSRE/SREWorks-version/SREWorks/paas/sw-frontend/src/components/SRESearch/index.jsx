import React from "react";
import ReactDOM from 'react-dom';
// import {  Search } from '@alife/next';

import { Select, Tooltip, Divider, Badge, Input,message } from "antd";
import { SearchOutlined } from '@ant-design/icons';
import PropTypes from "prop-types";

import $ from "jquery";
import _ from "lodash";

// t-search 迁移来的

import SearchService from "./services/service";

import TabsRender from "./components/TabsRender";
import FlatList from "./components/FlatList"
import appService from '../../core/services/appService';

import onClickOutside from "react-onclickoutside";
import classNames from "classnames";

import "./main.scss";

const Option = Select.Option;
const OptGroup = Select.OptGroup;
const { Search } = Input;

class SRESearch extends React.Component {

  constructor(props) {
    super(props);
    this.currentRef = React.createRef();
    this.navType = '';
    this.state = {
      overlayVisible: false,
      hotKeyWordsVisible: false,
      temp_search_content: "",
      search_content: "",
      is_force: false,
      hotKeyOptions: [],
      options: [],
      keywords: [],
      isNeedSuggestion: false,
      isloading: false,
      textValue: '',
      suggestList: [],
      searchServiceFlag: true,
    };
    this.onSearchKeyword = this.onSearchKeyword.bind(this);
    this.onSearch = this.onSearch.bind(this);
    // this.onFocus = this.onFocus.bind(this);
    // this.onChange = this.onChange.bind(this);
    this.onKeyDown = this.onKeyDown.bind(this);
    // this.getHotKeywords = this.getHotKeywords.bind(this);
    // this.getSuggestionList = this.getSuggestionList.bind(this);
    // this.getSuggestionListDebounce = _.debounce(this.getSuggestionList, 150);
  }
  checkSearchService=()=> {
    appService.testSearchService().then(res => {
      if(res && res.version && res.version === 'v1') {
        this.setState({
          searchServiceFlag: false
        })
      }
    })
  }
  getRelationSuggestList = () => {
    const { textValue } = this.state;
    if (!textValue) {
      this.getHotKeywords()
    } else {
      this.getSuggestionList()
    }
    this.setState({
      hotKeyWordsVisible: true,
      overlayVisible: false
    })
  }
  getHotKeywords = () => {
    const { userEmpId, sreworksSearchPath, category } = this.props;
    if(!this.state.searchServiceFlag) {
      message.warn("搜索服务未部署");
      return false
    }
    SearchService.getHotKeywords(userEmpId, sreworksSearchPath, category, 10)
      .then(res => {
        this.setState({
          hotKeyOptions: res,
        });

      }).catch(err => {
      });
  }
  getSuggestionList() {
    const { userEmpId, sreworksSearchPath, category } = this.props;
    const { textValue } = this.state;
    if(!this.state.searchServiceFlag) {
      message.warn("搜索服务未部署");
      return false
    }
    if (textValue !== "") {
      SearchService.searchSuggestions(userEmpId, sreworksSearchPath, category, textValue, 1, 10)
        .then(res => {
          this.setState({
            hotKeyOptions: res,
          });
        }).catch(err => {
          this.setState({
            hotKeyOptions: []
          });
        });
    }
  }
  onSearchKeyword(value) {
    const { hotKeyOptions } = this.state;
    let targetObj = hotKeyOptions.find(item => item.type === value)
    this.navType = targetObj ? targetObj['__type'] : ''
    this.setState({
      textValue: value,
      hotKeyOptions: []
    }, () => {
      this.enterSearch()
    });
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
  handleChange = (e) => {
    this.setState({
      textValue: e.target.value,
      overlayVisible: true,
    }, () => this.getRelationSuggestList());
    if (!e.target.value) {
      this.getHotKeywords()
    }
  };
  enterSearch = () => {
    if(!this.state.searchServiceFlag) {
      message.warn("搜索服务未部署");
      return false
    }
    this.navType = '';
    this.getAllSuggetPathList();
    this.setState({
      hotKeyWordsVisible: false,
      overlayVisible: true
    })
  }
  handleClickOutside = evt => {
    const { overlayVisible, hotKeyWordsVisible } = this.state;
    const area = ReactDOM.findDOMNode(this.currentRef.current);
    if (!area.contains(evt.target) && (hotKeyWordsVisible || overlayVisible)) {
      this.navType = '';
      this.setState({
        overlayVisible: false,
        hotKeyWordsVisible: false,
        hotKeyOptions: []
      });
    }
  };

  onKeyDown(e) {
    if (e.keyCode === 13 && !this.hasSearch) {
      // this.refs['search-select'].blur();
      this.onSearch(this.state.search_content)
      this.setState({ overlayVisible: true })
    }
    this.hasSearch = false
  }

  componentWillMount() {
    // this.getHotKeywords(this.props.userEmpId, this.props.sreworksSearchPath, this.props.category, 10);
    // if (this.props.isShowKeywords) {
    //   this.getCommonKeywords();
    // }
  }

  focusChange = () => {
    const { textValue } = this.state;
    if (!textValue) {
      this.getHotKeywords();
    }
    this.setState({
      hotKeyWordsVisible: true,
      overlayVisible: false
    })
  }
  componentDidMount() {
    let { includeSearchBtn = true } = this.props;
    this.checkSearchService()
    setTimeout(function () {
      $(`.biz-tSearch-deskstop.${this.props.className} input`).css({ width: includeSearchBtn ? "100%" : '484px' }).after("<i class=\"anticon anticon-search ant-input-search-icon\"></i>");
    }.bind(this), 0);
  }
  getAllSuggetPathList = (value) => {
    if(!this.state.searchServiceFlag) {
      message.warn("搜索服务未部署");
      return false
    }
    const { textValue } = this.state;
    const { userEmpId, category } = this.props;
    appService.desktopKeySearch(userEmpId, category, textValue, this.navType).then(res => {
      this.setState({ suggestList: res })
    }).catch(error => {
      message.error(error);
    })
  }
  render() {
    RegExp.quote = function (str) {
      return (str && str.replace(/([.?*+^$[\]\\(){}|-])/g, "\\$1")) || '';
    };
    let { includeSearchBtn = true, customSize = 'large', zIndexNum = 1000 } = this.props;
    let { search_content, overlayVisible, isloading, textValue, suggestList = [], hotKeyWordsVisible, hotKeyOptions = [] } = this.state;
    return (
      <div ref={this.currentRef} style={{ zIndex: zIndexNum }} className={classNames("biz-tSearch-deskstop", this.props.className)} onKeyDown={this.onKeyDown}>
        {
          includeSearchBtn ? <Search enterButton='运维搜索' onSearch={this.enterSearch} value={textValue} ref="searchInput" placeholder="团队、集群、应用、事件、指标..." onFocus={this.focusChange} onChange={this.handleChange} onPressEnter={this.enterSearch} size={customSize} />
            : <Input enterButton='运维搜索' onSearch={this.enterSearch} value={textValue} ref="searchInput" placeholder="团队、集群、应用、事件、指标..." onFocus={this.focusChange} onChange={this.handleChange} onPressEnter={this.enterSearch} size={customSize} />
        }
        <div style={{ display: hotKeyWordsVisible && hotKeyOptions.length ? "" : "none", position: 'absolute' }} className="panel-container globalBackground">
          {
            textValue ? <div style={{ textAlign: "center", borderBottom: "1px dashed", }}>
              <span style={{ color: "#2db7f5", fontSize: "14px" }}>✩✩前10推荐列表✩✩</span>
              <span>回车搜索更多</span>
            </div> :
              <div style={{ textAlign: "center", borderBottom: "1px dashed" }}>
                <span style={{ color: "#2db7f5", fontSize: "14px" }}>✩✩前10热门搜索✩✩</span>
              </div>
          }
          <div className="hotKey-panel">
            {
              hotKeyOptions.map((kw, index) => {
                return <div className="hotKeyItem" key={index} onClick={e => this.onSearchKeyword(kw.content)}>
                  <Badge offset={[8, -2]} count={index + 1} style={index > 2 ? {
                    backgroundColor: "#fff",
                    color: "#999",
                    boxShadow: "0 0 0 1px #d9d9d9 inset",
                  } : {}} />
                  <span style={{ display: 'inline-block', width: 20 }}></span>
                  {kw.content}
                </div>
              })
            }
          </div>
        </div>
        <div style={{ display: overlayVisible ? "" : "none", position: 'absolute' }} className="panel-container">
          <FlatList {...this.props}
            suggestList={suggestList}
            is_force={this.state.is_force}
            search_content={textValue} />

        </div>
      </div>
    );
  }
}

// var clickOutsideConfig = {
//   handleClickOutside: function(instance) {
//     return instance.handleClickOutside;
//   },
//   excludeScrollbar: true,
// };

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


export default onClickOutside(SRESearch);
// export default SRESearch;
