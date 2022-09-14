/**
 * Created by caoshuaibiao on 2019/3/30.
 */
import React from 'react';
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { DownOutlined } from '@ant-design/icons';
import { Dropdown, Menu, Button, Modal, Tooltip, List } from 'antd';
import localeHelper from '../../utils/localeHelper';
import * as util from "../../utils/utils";
import { Link } from 'dva/router';

export default class LinksRender extends React.Component {
    //处理配置的path本来就带有一部分的固定参数等情况
    handleUrl = (url, rowData) => {
        let rowParams = {};
        if (rowData) {
            Object.keys(rowData).forEach(rk => {
                rowParams[rk] = (rowData[rk].value ? rowData[rk].value : rowData[rk])
            });
        }
        let path = url.startsWith("/") ? url.substring(1) : url, start = url.indexOf("?"), initParam = "";
        if (start > 0) {
            path = path.substring(0, start - 1);
            initParam = "&" + url.substring(start + 1);
        }
        return {
            href: path + "?" + util.objectToUrlParams(rowParams) + initParam,
            search: util.objectToUrlParams(rowParams) + initParam
        }
    };

    render() {
        let { links = [], layout, linkData } = this.props;
        if (layout === 'vertical') {
            let menuItems = [];
            links.forEach((action, index) => {
                let { icon, path, label } = action;
                let genlink = this.handleUrl(path, linkData);
                menuItems.push(
                    <Menu.Item key={index}>
                        <div>
                            {
                                path.startsWith("http") ?
                                    <a style={{ marginRight: 16 }} href={genlink.href} target="_blank">{icon ? <LegacyIcon type={icon} /> : null}{label}</a> :
                                    <Link style={{ marginRight: 16 }} to={{
                                        pathname: path,
                                        search: genlink.search
                                    }}>{icon ? <LegacyIcon type={icon} /> : null}{label}</Link>
                            }
                        </div>
                    </Menu.Item>
                );
                menuItems.push(<Menu.Divider key={index + "_Divider"} />);
            });
            if (menuItems.length > 0) menuItems.pop();
            return (
                <div>
                    <Dropdown overlay={<Menu>{menuItems}</Menu>}>
                        <a>
                            {localeHelper.get("operation", '操作')} <DownOutlined />
                        </a>
                    </Dropdown>
                </div>
            );
        }
        return (
            <div style={{ display: 'flex', flexDirection: 'row' }}>
                {
                    links.map((action, index) => {
                        let { icon, path, label } = action;
                        let genlink = this.handleUrl(path, linkData);
                        if (path.startsWith("http")) {
                            return <a style={{ marginRight: 16 }} key={index} href={genlink.href} target="_blank">{icon ? <LegacyIcon type={icon} /> : null}{label}</a>;
                        }
                        return (
                            <Link style={{ marginRight: 16 }} key={index} to={{
                                pathname: path,
                                search: genlink.search
                            }}>{icon ? <LegacyIcon type={icon} /> : null}{label}</Link>
                        );
                    })
                }
            </div>
        );
    }
}