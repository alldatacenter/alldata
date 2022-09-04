import React from 'react';

export default {
    Seconds: {
        name: '秒',
        every: '每一秒钟',
        interval: (step, start) => <span>从第{start}秒钟开始，每隔{step}秒钟</span>,
        specific: '具体秒数(可多选)',
        cycle: (start, end) => <span>从第{start}秒钟到第{end}秒钟</span>,
    },
    Minutes: {
        name: '分',
        every: '每一分钟',
        interval: (step, start) => <span>从第{start}分钟开始，每隔{step}分钟</span>,
        specific: '具体分钟数(可多选)',
        cycle: (start, end) => <span>从第{start}分钟到第{end}分钟</span>,
    },
    Hours: {
        name: '时',
        every: '每一小时',
        interval: (step, start) => <span>从第{start}小时开始，每隔{step}小时</span>,
        specific: '具体小时数(可多选)',
        cycle: (start, end) => <span>从第{start}小时到第{end}小时</span>,
    },
    Day: {
        name: '天',
        every: '每一天',
        intervalWeek: (step, start) => <span>从{start}开始，每隔{step}天</span>,
        intervalDay: (step, start) => <span>从第{start}天开始，每隔{step}天</span>,
        specificWeek: '具体星期几(可多选)',
        specificDay: '具体天数(可多选)',
        lastDay: '每个月的最后一天',
        lastWeekday: '每个月的最后一个工作日',
        lastWeek: day => <span>每个月的最后一个{day}</span>,
        beforeEndMonth: num => <span>在每个月底前{num}天</span>,
        nearestWeekday: day => <span>离每个月{day}号最近的工作日</span>,
        someWeekday: (nth, day) => <span>每个月的第{nth}个{day}</span>,
    },
    Week: ['天','一','二','三','四','五','六'].map(val => '星期' + val),
    Month: {
        name: '月',
        every: '每一月',
        interval: (step, start) => <span>从{start}月开始，每隔{step}个月</span>,
        specific: '具体月数(可多选)',
        cycle: (start, end) => <span>从{start}月到{end}月</span>,
    },
    Year: {
        name:'年',
        every:'每一年',
        interval: (step, start) => <span>从{start}年开始，每隔{step}年</span>,
        specific:'具体年份(可多选)',
        cycle: (start, end) => <span>从{start}年到{end}年</span>,
    },
    Save:'保存',
    Close:'关闭'
}