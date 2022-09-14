/**
 * Created by xuwei on 18/3/7.
 */
import React, { Component } from "react";
import PropTypes from "prop-types";
import { List, Spin, Divider, Tag } from "antd";
import _ from "lodash";
import SearchService from "../services/service";
import InfiniteScroll from "react-infinite-scroller";
import WrapLog from "./WrapLogForComponent";


class TabContentListRender extends Component {

  constructor(props, context) {
    super(props, context);
    this.state = {
      page: 0,


      data: [],
      loading: false,
      hasMore: true,
    };
    this.handleInfiniteOnLoad = this.handleInfiniteOnLoad.bind(this);
  }


  componentWillMount() {
    this.handleInfiniteOnLoad(1);

  }


  handleInfiniteOnLoad = (page) => {
    let data = this.state.data;
    this.setState({
      loading: true,
    });
    SearchService.searchTypeElements(this.props.userEmpId, this.props.sreworksSearchPath, this.props.category, this.props.search_content, this.props.type, page, 10)
      .then(res => {
        if (res.length === 0) {
          this.setState({
            hasMore: false,
            loading: false,
          });
        } else {
          data = data.concat(res);
          this.setState({
            data,
            loading: false,
          });
        }

      }).catch(err => {
        this.setState({
          loading: false,
        });

      });

  };

  render() {

    const WrapLogA = WrapLog("a", this.props.sreworksSearchPath);
    const search_content = this.props.search_content;

    RegExp.quote = function (str) {
      return str.replace(/([.?*+^$[\]\\(){}|-])/g, "\\$1");
    };
    return (
      <div>
        <div className="panel-container-infinite-container">
          <InfiniteScroll
            initialLoad={false}
            pageStart={1}
            loadMore={this.handleInfiniteOnLoad}
            hasMore={!this.state.loading && this.state.hasMore}
            threshold={300}
            useWindow={false}
          >
            <List
              size="small"
              itemLayout="horizontal"
              dataSource={this.state.data}
              renderItem={item => {
                let matchFirstKey = _.head(_.filter(Object.keys(item["content"]["kv"]), function (key) {
                  return (typeof item["content"]["kv"][key]) === "string" && key !== "_title" && item["content"]["kv"][key].search(new RegExp(search_content, "i")) !== -1;
                }));
                return (
                  <List.Item>
                    <List.Item.Meta
                      title={
                        <span style={{ fontSize: "16px" }}>{item["content"]["kv"]["_title"]}
                          {item["content"]["kv"]["precise"] ? (
                            <Tag style={{ float: "right" }} color="blue">精准匹配</Tag>) : ("")}
                        </span>
                      }
                      description={

                        <div>
                          {
                            matchFirstKey ?
                              <div>
                                <span>{matchFirstKey}:&nbsp;</span><span
                                  dangerouslySetInnerHTML={{ __html: item["content"]["kv"][matchFirstKey].replace(new RegExp(RegExp.quote(search_content), "igm"), "<span style=\"color: red\">" + item["content"]["kv"][matchFirstKey].match(new RegExp(RegExp.quote(search_content), "i")) + "</span>") }}></span>
                              </div>
                              : ""
                          }

                          {Object.keys(item["exlink"]).map((key, index) =>
                            <WrapLogA
                              logParams={
                                {
                                  requestId: this.props.requestId,
                                  userEmpId: this.props.userEmpId,
                                  href: `${item["exlink"][key]}`,
                                  aName: `${key}`,
                                }
                              }
                              key={key} href={item["exlink"][key]}
                              target="_blank">
                              {key}
                              {index !== (_.size(item["exlink"]) - 1) ? (
                                <Divider type="vertical" />) : ("")}
                            </WrapLogA>,
                          )
                          }
                        </div>
                      }
                    />
                  </List.Item>
                );

              }}
            >
              {this.state.loading && this.state.hasMore && <Spin className="panel-container-infinite-loading" />}
            </List>
          </InfiniteScroll>
        </div>
        <Divider style={{ marginTop: "5px", marginBottom: "10px" }} />
        <div style={{ textAlign: "right" }}>
          <span>总数: <span>{this.props.count}&nbsp;&nbsp;&nbsp;</span></span>
          <a style={{ marginRight: "15px" }}
            href={`${this.props.moreLinkPrefix}/${this.props.category}/${this.props.search_content}`}
            target="_blank">查看更多</a>
        </div>
      </div>

    );
  }
}

TabContentListRender.propTypes = {
  userEmpId: PropTypes.string,
  sreworksSearchPath: PropTypes.string,
  category: PropTypes.string,
  moreLinkPrefix: PropTypes.string,
  search_content: PropTypes.string,
  type: PropTypes.string,
  count: PropTypes.number,
  requestId: PropTypes.string,
};
TabContentListRender.defaultProps = {};

export default TabContentListRender;
