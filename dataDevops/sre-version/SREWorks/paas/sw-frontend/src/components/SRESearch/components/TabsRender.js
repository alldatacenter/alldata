/**
 * Created by xuwei on 18/3/7.
 */
import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { Card, Tabs } from 'antd';
import _ from 'lodash';
import SearchService from '../services/service';

import TabContentListRender from './TabContentListRender';

const TabPane = Tabs.TabPane;

class TabsRender extends Component {

    constructor(props, context) {
        super(props, context);
        this.state = {
            loading: false,
            tabList: [],
            sreworksSearchPath: props.sreworksSearchPath,
            search_content: props.search_content,
            category: props.category,
            moreLinkPrefix: props.moreLinkPrefix,
            requestId: '',

        }
        this.getTabList = this.getTabList.bind(this)

    }

    getTabList(userEmpId, sreworksSearchPath, category, search_content) {
        if (search_content !== '') {
            this.setState({
                loading: true
            })
            SearchService.searchTypeList(userEmpId, sreworksSearchPath, category, search_content)
                .then(res => {
                    this.setState({
                        tabList: res,
                        search_content: search_content,
                        loading: false,
                        // requestId: res,
                    });
                }).catch(err => {
                    // handleError(err);
                    this.setState({
                        loading: false,
                    });
                })

        }

    }


    componentWillMount() {
        this.getTabList(this.props.userEmpId, this.props.sreworksSearchPath, this.props.category, this.props.search_content)
    }

    componentWillReceiveProps(nextProps, nextContext) {
        if (!_.isEqual(this.props.search_content, nextProps.search_content) || nextProps.is_force) {
            this.getTabList(nextProps.userEmpId, nextProps.sreworksSearchPath, nextProps.category, nextProps.search_content)
        }
        this.setState({
            isVisible: nextProps.isVisible
        })
    }


    render() {
        return (
            <Card className="panel-container-card" loading={this.state.loading}>
                {
                    this.state.tabList.length !== 0 ?
                        <Tabs tabPosition={'left'} style={{ height: 392 }}>
                            {
                                this.state.tabList.map(item =>
                                    <TabPane tab={item.type} key={`${item.type}${item.count}`}>
                                        <TabContentListRender
                                            key={`${item.type}${item.count}`}
                                            userEmpId={this.props.userEmpId}
                                            sreworksSearchPath={this.state.sreworksSearchPath}
                                            category={this.state.category}
                                            moreLinkPrefix={this.state.moreLinkPrefix}
                                            search_content={this.state.search_content}
                                            type={item.type}
                                            count={item.count}
                                            requestId={this.state.requestId}
                                        />

                                    </TabPane>
                                )
                            }
                        </Tabs> : (
                            <span style={{ marginLeft: '10px' }}>无匹配数据</span>
                        )
                }

            </Card>
        );
    }
}

TabsRender.propTypes = {
    userEmpId: PropTypes.string,
    is_force: PropTypes.bool,
    category: PropTypes.string,
    sreworksSearchPath: PropTypes.string,
    search_content: PropTypes.string,
    moreLinkPrefix: PropTypes.string,
};
TabsRender.defaultProps = {};

export default TabsRender;
