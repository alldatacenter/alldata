import React from 'react';

export default {
    Seconds: {
        name: '秒',
        every: '每一秒鐘',
        interval: (step, start) => <span>從第{start}秒鐘開始，每隔{step}秒鐘</span>,
        specific: '具體秒數(可多選)',
        cycle: (start, end) => <span>從第{start}秒鐘到第{end}秒鐘</span>,
    },
    Minutes: {
        name: '分',
        every: '每一分鐘',
        interval: (step, start) => <span>從第{start}分鐘開始，每隔{step}分鐘</span>,
        specific: '具體分鐘數(可多選)',
        cycle: (start, end) => <span>從第{start}分鐘到第{end}分鐘</span>,
    },
    Hours: {
        name: '時',
        every: '每一小時',
        interval: (step, start) => <span>從第{start}小時開始，每隔{step}小時</span>,
        specific: '具體小時數(可多選)',
        cycle: (start, end) => <span>從第{start}小時到第{end}小時</span>,
    },
    Day: {
        name: '天',
        every: '每一天',
        intervalWeek: (step, start) => <span>從{start}開始，每隔{step}天</span>,
        intervalDay: (step, start) => <span>從第{start}天開始，每隔{step}天</span>,
        specificWeek: '具體星期幾(可多選)',
        specificDay: '具體天數(可多選)',
        lastDay: '每個月的最後一天',
        lastWeekday: '每個月的最後一個工作日',
        lastWeek: day => <span>每個月的最後一個{day}</span>,
        beforeEndMonth: num => <span>在每個月底前{num}天</span>,
        nearestWeekday: day => <span>離每個月{day}號最近的工作日</span>,
        someWeekday: (nth, day) => <span>每個月的第{nth}個{day}</span>,
    },
    Week: ['天','一','二','三','四','五','六'].map(val => '星期' + val),
    Month: {
        name: '月',
        every: '每一月',
        interval: (step, start) => <span>從{start}月開始，每隔{step}個月</span>,
        specific: '具體月數(可多選)',
        cycle: (start, end) => <span>從{start}月到{end}月</span>,
    },
    Year: {
        name:'年',
        every:'每一年',
        interval: (step, start) => <span>從{start}年開始，每隔{step}年</span>,
        specific:'具體年份(可多選)',
        cycle: (start, end) => <span>從{start}年到{end}年</span>,
    },
    Save:'保存',
    Close:'關閉'
}