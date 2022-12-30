/**
 * Created by xuwei on 18/3/7.
 */
import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { Card, message, Tabs, Divider, Tooltip } from 'antd';
import { MoreOutlined } from '@ant-design/icons';
import _ from 'lodash';
import SearchService from '../services/service';
import appService from '../../../core/services/appService';

import TabContentListRender from './TabContentListRender';
import properties from '../../../properties';

class FlatList extends Component {

    constructor(props) {
        super(props);
        this.state = {
            loading: false,
            suggestList: [],
            sreworksSearchPath: props.sreworksSearchPath,
            search_content: props.search_content,
            category: props.category,
            moreLinkPrefix: props.moreLinkPrefix,
            requestId: '',

        }

    }
    componentWillReceiveProps(nextProps, nextContext) {
        if (!_.isEqual(this.props.search_content, nextProps.search_content) || nextProps.is_force) {
        }
        this.setState({
            isVisible: nextProps.isVisible
        })
    }
    turnToNewPath = (path) => {
        let fullpath = '';
        if (path) {
            fullpath = properties.baseUrl + "#/" + path
            window.open(fullpath)
        }
    }
    searchMore = () => {
        let fullpath = '';
        const { search_content } = this.props;
        if (search_content) {
            fullpath = properties.baseUrl + `#/search/main?deskstop_search=${search_content}`
        } else {
            fullpath = properties.baseUrl + `#/search/main`
        }
        window.open(fullpath)
    }
    render() {
        const { suggestList } = this.props;
        return (
            <Card className="flat-list-pane" loading={this.state.loading}>
                {
                    suggestList.length !== 0 ?
                        (<div className="flat-list">
                            {
                                suggestList.map((item, index) => {
                                    return <div key={index} className="suggest-list-cell" onClick={() => this.turnToNewPath(item.url)} style={{ height: '30px',paddingRight:20}}>
                                        <span>{item.type}</span>
                                        <Divider type="vertical"/>
                                        <Tooltip title={item.title}>
                                        <span style={{wordBreak:"break-all"}}>{item.title}</span>
                                        </Tooltip>
                                    </div>
                                })
                            }
                            {
                                suggestList.length > 49 && <div onClick={this.searchMore} className='more-btn'><MoreOutlined />查看更多</div>
                            }
                        </div>)
                        : (
                            <span style={{ marginLeft: '10px' }}>无匹配数据</span>
                        )
                }

            </Card>
        );
    }
}

FlatList.propTypes = {
    userEmpId: PropTypes.string,
    is_force: PropTypes.bool,
    category: PropTypes.string,
    sreworksSearchPath: PropTypes.string,
    search_content: PropTypes.string,
};
FlatList.defaultProps = {};

export default FlatList;
