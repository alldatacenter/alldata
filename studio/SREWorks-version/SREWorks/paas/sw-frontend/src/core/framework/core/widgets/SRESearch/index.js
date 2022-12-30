import React, { Component } from 'react';
import ReactDOM from 'react-dom';
import { Select, Tooltip, Divider, Badge, Input } from "antd";
import _ from "lodash";
import $ from "jquery";
import onClickOutside from "react-onclickoutside";
import PropTypes from "prop-types";
import classNames from "classnames";
import httpClient from "../../../../../utils/httpClient"
import * as util from "../../../../../utils/utils";
import "./index.less";

const queryString = require('query-string');
const Option = Select.Option;
const OptGroup = Select.OptGroup;
const { Search } = Input;

let UNLISTEN_SEARCHFUNC;
class SRESearch extends Component {
  constructor(props) {
    super(props);
    let { widgetConfig } = props;
    this.searchPath = widgetConfig.url || '';
    this.currentRef = React.createRef();
    this.navType = '';
    this.state = {
      hotKeyWordsVisible: false,
      temp_search_content: "",
      search_content: "",
      hotKeyOptions: [],
      isloading: false,
      textValue: '',
      suggestList: []
    };
  }

  getRelationSuggestList = () => {
    this.getSuggestionList()
    this.setState({
      hotKeyWordsVisible: true
    })
  }
  getSuggestionList() {
    const { textValue } = this.state;
    let { widgetConfig } = this.props;
    let urlStr = this.searchPath;
    let obj = {};
    obj[widgetConfig.keyType] = textValue
    const { nodeParams } = this.props;
    if (urlStr.indexOf("$(") !== -1) {
      urlStr = util.renderTemplateString(urlStr, Object.assign({}, nodeParams, obj))
    }
    if (!widgetConfig.needSuggest) {
      return
    }
    httpClient.get(urlStr).then(res => {
      this.setState({ hotKeyOptions: res })
    }).catch(error => {
      message.error(error);
    })
  }
  onSearchKeyword = (value) => {
    this.setState({
      textValue: value,
      hotKeyOptions: []
    }, () => {
      this.enterSearch()
    });
  }
  handleChange = (e) => {
    _.debounce(this.setState({
      textValue: e.target.value,
    }, () => this.getRelationSuggestList()), 500)
  };
  enterSearch = () => {
    this.navType = '';
    this.getAllSuggetPathList();
    this.setState({
      hotKeyWordsVisible: false,
    })
  }
  handleClickOutside = evt => {
    const { hotKeyWordsVisible } = this.state;
    const area = ReactDOM.findDOMNode(this.currentRef.current);
    if (!area.contains(evt.target) && (hotKeyWordsVisible)) {
      this.navType = '';
      this.setState({
        hotKeyWordsVisible: false,
        hotKeyOptions: []
      });
    }
  };
  componentWillMount() {
    // this.getHotKeywords(this.props.userEmpId, this.props.sreworksSearchPath, this.props.category, 10);
    // if (this.props.isShowKeywords) {
    //   this.getCommonKeywords();
    // }
  }
  componentDidMount() {
    UNLISTEN_SEARCHFUNC = this.props.history && this.props.history.listen(router => {
      this.dealLocationSearchParams()
    })
  }
  componentWillUnmount() {
    UNLISTEN_SEARCHFUNC && UNLISTEN_SEARCHFUNC(); // 执行解绑
  }
  dealLocationSearchParams = () => {
    let hashString = window.location.hash, searchObj = {};
    if (hashString && hashString.indexOf('?') !== -1) {
      searchObj = queryString.parse(hashString.split("?")[1]);
    }
    if (searchObj.deskstop_search) {
      this.setState({
        textValue: searchObj.deskstop_search
      }, () => this.getAllSuggetPathList())
    }
  }
  getAllSuggetPathList = (value) => {
    const { textValue } = this.state;
    let { widgetConfig } = this.props;
    let outputData = {};
    outputData[widgetConfig.keyType] = textValue
    let { dispatch } = this.props, paramData = { ___refresh_timestamp: (new Date()).getTime() };
    Object.assign(paramData, outputData)
    dispatch({ type: 'node/updateParams', paramData: paramData });
  }
  render() {
    let { textValue, hotKeyWordsVisible, hotKeyOptions = [] } = this.state;
    let { widgetConfig } = this.props;
    return (
      <div ref={this.currentRef} style={{ width: Number(widgetConfig.complength) || 540, margin: 'auto', padding: `${Number(widgetConfig.compPadding)}px 0` || '20px 0' }} className={classNames("biz-tSearch", this.props.className)} onKeyDown={this.onKeyDown}>
        <Search
          enterButton={widgetConfig.btnText || 'search'}
          style={{ width: Number(widgetConfig.complength) || 540 }}
          value={textValue}
          ref="searchInput"
          placeholder="团队、集群、应用、事件、指标..."
          onFocus={this.focusChange}
          onChange={this.handleChange}
          onSearch={this.enterSearch}
          onPressEnter={this.enterSearch}
          size="large" />
        <div style={{ display: hotKeyWordsVisible && hotKeyOptions.length ? "" : "none", backgroundColor: 'white', width: Number(widgetConfig.complength) - 2 || 540, }} className="panel-container">
          {
            textValue && <div style={{ textAlign: "center", borderBottom: "1px dashed", width: Number(widgetConfig.complength) - 2 || 540, margin: 'auto' }}>
              <span style={{ color: "#2db7f5", fontSize: "14px" }}>✩✩前10推荐列表✩✩</span>
              <span>回车搜索更多</span>
            </div>
          }
          <div className="hotKey-panel" style={{ width: Number(widgetConfig.complength) - 2 || 540, }}>
            {
              hotKeyOptions && hotKeyOptions.length && hotKeyOptions.map((kw, index) => {
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
      </div>
    );
  }
}

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