/**
 * Created by caoshuaibiao on 2019/9/18.
 */

import React from 'react';
import NoticeBoardBar from '../../components/NoticeBoardBar';
import httpClient from '../../utils/httpClient';
import properties from 'appRoot/properties';

let show = true;
class NoticeBar extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            data: null
        };
    }

    componentDidMount() {
        if (properties.envFlag === properties.ENV.Internal) {
            //临时的信息获取接口,后续需要从新站点读取
            httpClient.get("notify/announcement/listAnnouncement").then(result => {
                result.map && this.setState({
                    data: result.map(r => {
                        return {
                            name: (r.level === 2 ? "重要" : "") + "公告",
                            content: r.content
                        }
                    })
                })
            });
        }
    }

    render() {
        if (!show) return null;
        let { data } = this.state;
        if (data && data.length) {
            return (
                <div style={this.props.style || { marginTop: 1, zIndex: 1000, width: '100%' }}>
                    <NoticeBoardBar dataSource={data} onClose={() => show = false} />
                </div>
            )
        }
        return null;
    }
}

export default NoticeBar;
