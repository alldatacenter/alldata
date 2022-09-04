/**
 * Created by caoshuaibiao on 2019/5/14.
 * 时间范围选择
 */
import * as React from 'react';
import moment from 'moment';
import localeHelper from '../../utils/localeHelper';
import { DatePicker, Radio, Button } from 'antd';
import _ from 'lodash';


const RangePicker = DatePicker.RangePicker;

let timeFormat = window.APPLICATION_DATE_FORMAT || "YYYY-MM-DD HH:mm:ss";

/**
 * 控件使用说明:
 *   defaultMode:默认视图定义,可选值 'day'/'month'/'year' 默认为day
 *   noFuture:控制是否能选当前以前时间 true/false
 *   dSampleQuick:天视图快捷范围定义
 *   mSampleQuick:月视图快捷范围定义
 *   ySampleQuick:年视图快捷范围定义
 *
 * SampleQuick使用说明
 * 格式: prefix+Number+suffix
 *      prefix: l/p, l代表最近 p过去
 *      Number: 整数
 *      suffix: h:hour,d:days,m:month,y:year,w:week
 * 例子: l3d 代表最近3天  p3d代表过去三天
 *       ['l3d','l6m','p6m']
 *
 */
const viewDef = {
    'day': {
        mode: ['date', 'date'],
        sampleQuick: ['l1h', 'l3h', 'l6h', 'l12h'],
        sample: []
    },
    'month': {
        mode: ['month', 'month'],
        sampleQuick: ['p1m', 'l3m', 'p3m'],
        sample: []
    },
    'year': {
        mode: ['year', 'year'],
        sampleQuick: ['p1y', 'l3y', 'p3y'],
        sample: []
    },
};

const labelMapping = {
    l: localeHelper.get('lately', '最近'), p: localeHelper.get('formerly', '过去'), M: localeHelper.get('minute', '分钟'),
    h: localeHelper.get('hour', '小时'), d: localeHelper.get('day', '天'), m: localeHelper.get('month', '月'),
    w: localeHelper.get('week', '周'), y: localeHelper.get('year', '年')
};
const typeMapping = { M: "minutes", h: 'hour', d: 'days', m: 'month', w: 'week', y: 'year' };



export default class DateRange extends React.Component {
    constructor(props) {
        super(props);
        let modeType = this.props.defaultMode || 'day';
        this.state = {
            times: props.initTime || [moment(), moment()],
            openTimepicker: false,
            modeType: modeType,
            selectedDisabledTime: [], // 待选日期是 意思就是在日历面板中点击的日期
            referDate: null

        };
        this.quickViewDef = JSON.parse(JSON.stringify(viewDef));
        let dayDef = this.quickViewDef.day, monthDef = this.quickViewDef.month, yearDef = this.quickViewDef.year;
        let dSampleQuick = this.props.dSampleQuick || dayDef.sampleQuick;
        let mSampleQuick = this.props.mSampleQuick || monthDef.sampleQuick;
        let ySampleQuick = this.props.ySampleQuick || yearDef.sampleQuick;
        function genQuick(quick, def) {
            def.sample = [];
            quick.forEach(item => {
                let arr = item.split(""), suffix, prefix, num;
                suffix = arr.pop();
                prefix = arr.shift();
                num = parseInt(arr.join(""));
                def.sample.push({
                    label: labelMapping[prefix] + " " + num + " " + (labelMapping[suffix]),
                    type: typeMapping[suffix],
                    value: num,
                    prefix: prefix
                });
            })
        }
        dayDef.sampleQuick = dSampleQuick;
        monthDef.sampleQuick = mSampleQuick;
        yearDef.sampleQuick = ySampleQuick;
        genQuick(dSampleQuick, dayDef);
        genQuick(mSampleQuick, monthDef);
        genQuick(ySampleQuick, yearDef);
        timeFormat = window.APPLICATION_DATE_FORMAT || "YYYY-MM-DD HH:mm:ss";
    }

    componentWillReceiveProps(nextProps) {
        //外界触发了初始值重设定
        let { initTime } = nextProps, preTime = this.props.initTime || [moment(), moment()];
        if (initTime[0].valueOf() !== preTime[0].valueOf() || initTime[1].valueOf() !== preTime[1].valueOf()) {
            this.setState({
                times: initTime
            });
        }
    }

    handleDisabledDate = current => {
        const { referDate } = this.state;
        const { limitRang } = this.props;
        let arr = limitRang.split(""), suffix, num;
        suffix = arr.pop();
        num = parseInt(arr.join(""));
        if (!referDate) {
            return false;
        }
        let referTime = referDate.valueOf(), year = referDate.year(), month = moment().month();
        //财年/年只支持一个控制
        if (suffix === 'f') {
            //本财年
            if (month > 2) {
                return current > moment((year + 1) + '-04-01 00:00:00') ||
                    current < moment(year + '-03-31 23:59:59')
            } else {//上财年
                return current > moment(year + '-04-01 00:00:00') ||
                    current < moment((year - 1) + '-03-31 23:59:59')
            }
        } else if (suffix === 'y') {
            return (
                current < moment(year + '-01-01 00:00:00') ||
                current > moment(year + '-12-31 23:59:59')
            )
        } else if (suffix === 'm') {
            return (
                current < moment(referTime).subtract(num, 'months') ||
                current > moment(referTime).add(num, 'months')
            )
        } else if (suffix === 'w') {
            return (
                current < moment(referTime).subtract(num, 'weeks') ||
                current > moment(referTime).add(num, 'weeks')
            )
        } else if (suffix === 'd') {
            return (
                current < moment(referTime).subtract(num, 'days') ||
                current > moment(referTime).add(num, 'days')
            )
        }
    };

    // 待选日期的变化回调
    handldOnCalendarChange = dates => {
        this.setState({ referDate: dates[0] });
        if (dates[1]) {
            this.setState({ referDate: null })
        }
    };

    // 小时
    range = (start, end) => {
        const result = [];
        for (let i = start; i < end; i++) {
            result.push(i);
        }
        return result;
    };

    render() {
        const { future, format, limitRang, ...rest } = this.props;

        if (future !== true) {
            rest.disabledDate = (current) => current && current > moment().endOf('day');
        }
        if (format) {
            timeFormat = format;
        }
        let timeRange = [];
        let typeDef = this.quickViewDef[this.state.modeType];
        typeDef.sample.forEach((value, index) => {
            timeRange.push(<a key={index} onClick={this.sampleQuickRanges.bind(this, value)}>{value.label}</a>);
        });
        return (
            <RangePicker
                mode={viewDef[this.state.modeType].mode}
                disabledDate={limitRang ? this.handleDisabledDate : () => { }}
                // disabledTime={this.disabledRangeTime}
                onCalendarChange={limitRang ? this.handldOnCalendarChange : () => { }}
                ref="my_range"
                allowClear={false}
                showTime
                onOk={this.onOk.bind(this)}
                value={this.state.times}
                //open={this.state.openTimepicker}
                // onOpenChange={this.handleEndOpenChange}
                renderExtraFooter={() => (
                    <div className="ant-calendar-range-quick-selector">
                        {timeRange}
                        <div style={{ float: 'right' }} onChange={this.handleViewChange}>
                            <Radio.Group value={this.state.modeType} size="small">
                                <Radio.Button value='year'>{localeHelper.get('year', '年')}</Radio.Button>
                                <Radio.Button value='month'>{localeHelper.get('month', '月')}</Radio.Button>
                                <Radio.Button value='day'>{localeHelper.get('day', '天')}</Radio.Button>
                            </Radio.Group>
                        </div>
                    </div>)
                }
                onPanelChange={this.handleSelectChange}
                onChange={this.handleChange}
                format={timeFormat}
            />
        );
    }
    handleChange = value => {
        this.setState({ times: value });
    };

    onOk = (value) => {
        this.setState({
            times: value,
            openTimepicker: false,
            referDate: null
        });
        if (this.props.onChange) this.props.onChange(value);
    };

    getSelectedRange = (type, value) => {
        let genValues = value;
        if (type === 'month') {
            genValues = [
                moment(genValues[0].format('YYYY-MM') + "-01 00:00:00"),
                moment(genValues[1].format('YYYY-MM') + "-" + genValues[1].daysInMonth() + " 23:59:59")
            ];
        } else if (type === 'year') {
            genValues = [
                moment(genValues[0].format('YYYY') + "-01-01 00:00:00"),
                moment(genValues[1].format('YYYY') + "-12-31 23:59:59")
            ];
        }
        //如果开始时间大于当前时间,取本月初,如果结束时间大于当前时间,取当前时间
        if (this.props.noFuture === true) {
            if (genValues[0].isAfter(moment())) genValues[0] = moment().startOf('month');
            if (genValues[1].isAfter(moment())) genValues[1] = moment();
        }
        return genValues;
    };

    handleSelectChange = (value, mode) => {
        //console.log("mode---->",mode);
        let genValues = this.getSelectedRange(this.state.modeType, value);
        this.handleChange(genValues);
    };
    sampleQuickRanges = (item) => {
        let start, end, endCopy;
        //最近
        if (item.prefix === 'l') {
            end = moment(), endCopy = moment();
        } else if (item.prefix === 'p') {//过去
            end = moment().startOf(item.type).subtract(1, 'day').endOf('day');
            endCopy = moment().startOf(item.type).subtract(1, 'day').endOf('day');
        }
        start = endCopy.subtract(item.value, item.type).add(1, 'seconds');
        this.onOk([start, end]);
    };

    handleViewChange = (e) => {
        //更改一次视图清空一次操作事务,月视图的话默认范围为本月第一天0点到本月最后一天23:59:59
        //typeClick=0;
        let type = e.target.value;
        let genValues = this.getSelectedRange(type, this.state.times);
        this.setState({ modeType: type, times: genValues });
    }
}
