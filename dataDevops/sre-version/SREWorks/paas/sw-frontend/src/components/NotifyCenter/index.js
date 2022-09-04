/**
 * Created by caoshuaibiao on 2020/4/13.
 * 待办通知中心
 */
import React from 'react';
import PagingTable from '../PagingTable';
import  localeHelper from '../../utils/localeHelper';
import * as util from "../../utils/utils";
import  httpClient from '../../utils/httpClient';
import { ScheduleOutlined } from '@ant-design/icons';
import { Modal, Divider, Radio, Badge, Tooltip, Popover } from 'antd';

const orderUrl="gateway/v2/foundation/order/list";
const QUERY_STATUS="INIT,PENDING,RUNNING,REOPEN";

export default class NotifyCenter extends React.Component  {

    constructor(props) {
        super(props);
        this.state = {todoCount:0,data:[]};
    }

    componentWillMount() {
        const {empId}=this.props;
        httpClient.post(orderUrl,{currentEmpId:empId,status:QUERY_STATUS}).then((result)=>{
            this.setState({
                data:result.items,
                todoCount:result.total
            });
        })
    }

    render() {
        const {empId,app}=this.props,{todoCount,data}=this.state;
        let columns = [{
            title: localeHelper.get("name",'名称'),
            dataIndex: 'name',
            key: 'name',
            width:150,
            sorter:false
        }, {
            dataIndex: "creator",
            title: "提单人",
            filterDropdown: false,
            sorter:false
        },{
            title: localeHelper.get("Type",'类型'),
            dataIndex: 'category',
            key: 'category',
            filterDropdown: false,
            sorter:false,
            dict: {
                "standChg": {
                    "color": "#FF9900",
                    "label": "标准变更"
                },
                "request": {
                    "color": "#0066CC",
                    "label": "请求"
                },
                "normalChg": {
                    "color": "#FF9900",
                    "label": "普通变更"
                },
                "issue": {
                    "color": "#FF3333",
                    "label": "问题"
                },
                "criticalChg": {
                    "color": "#FF9900",
                    "label": "紧急变更"
                },
                "changefree": {
                    "color": "#FF9900",
                    "label": "变更"
                },
                "event": {
                    "color": "#006633",
                    "label": "事件"
                }
            }
        }, {
            title: localeHelper.get("operation",'处理'),
            key: 'operation',
            filterDropdown: false,
            sorter:false,
            render:(text , record)=> {
                if(record.source==='workOrder'){
                    return <div><a href={`#/${app}/portal/ticket/detail/changeTicket?requireId=${record.instanceId}`}>{localeHelper.get("operation",'处理')}</a></div>;
                }
                return <div><a href={`#/${app}/portal/ticket/detail/do?id=${record.id}`}>{localeHelper.get("operation",'处理')}</a></div>;
            }
        }
        ];
        return (
            <div>
                <Tooltip title={localeHelper.get("common.todo.workorder.list",'待办工单')}>
                    <Badge count={todoCount} style={{fontSize:12}} overflowCount={99} className="small-ant-badge-count">
                        <Popover trigger="click" content={
                            <div className="oam">
                              <PagingTable className="min-table" data={data}
                                           params={{currentEmpId:empId,status:QUERY_STATUS}}
                                           columns={columns}
                                           pagination={false}
                              />
                                <Divider style={{ margin: "8px 0px" }}/>
                                <div style={{ marginLeft: 120 }}>
                                    <Badge count={todoCount} overflowCount={99} className="small-ant-badge-count">
                                        <a href={`#/${app}/portal/ticket/processing/list`} style={{fontSize:12}}>查看所有待办</a>
                                    </Badge>
                                </div>

                            </div>
                        } placement="bottomRight" overlayStyle={{width:380}} arrowPointAtCenter>
                            <a><ScheduleOutlined style={{ fontSize: 18 }} /></a>
                        </Popover>
                    </Badge>
                </Tooltip>
            </div>
        );
    }
}