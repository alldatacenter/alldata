
import React from 'react';

export default {
    Seconds:{
        name: 'Seconds',
        every: 'Every second',
        interval: (step, start) => <span>Every {step} second(s), starting at {start} second</span>,
        specific: 'Specific second (choose one or many)',
        cycle: (start, end) => <span>Every second between second {start} and second {end}</span>,
    },
    Minutes:{
        name: 'Minutes',
        every: 'Every minute',
        interval: (step, start) => <span>Every {step} minute(s), starting at {start} minute</span>,
        specific: 'Specific minute (choose one or many)',
        cycle: (start, end) => <span>Every minute between minute {start} and minute {end}</span>,
    },
    Hours:{
        name: 'Hours',
        every: 'Every hour',
        interval: ['Every','hour(s) starting at hour'],
        interval: (step, start) => <span>Every {step} hour(s), starting at {start} hour</span>,
        specific: 'Specific hour (choose one or many)',
        cycle: (start, end) => <span>Every minute between hour {start} and hour {end}</span>,
    },
    Day:{
        name: 'Day',
        every: 'Every day',
        intervalWeek: (step, start) => <span>Every {step} day(s) starting on {start}</span>,
        intervalDay: (step, start) => <span>Every {step} day(s) starting at the {start} of the month</span>,
        specificWeek: 'Specific day of week (choose one or many)',
        specificDay: 'Specific day of month (choose one or many)',
        lastDay: 'On the last day of the month',
        lastWeekday: 'On the last weekday of the month',
        lastWeek: day => <span>On the last {day} of the month</span>,
        beforeEndMonth: num => <span>{num} day(s) before the end of the month</span>,
        nearestWeekday: day => <span>Nearest weekday (Monday to Friday) to the {day} of the month</span>,
        someWeekday: (nth, day) => <span>On the {nth}th {day} of the month</span>,
    },
    Week:['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'],
    Month:{
        name: 'Month',
        every: 'Every month',
        interval: ['Every','month(s) starting in'],
        interval: (step, start) => <span>Every {step} month(s) starting in {start}</span>,
        specific: 'Specific month (choose one or many)',
        cycle: (start, end) => <span>Every month between {start} and {end}</span>,
    },
    Year:{
        name:'Year',
        every:'Any year',
        interval: (step, start) => <span>Every {step} year(s) starting in {start}</span>,
        specific: 'Specific year (choose one or many)',
        cycle: (start, end) => <span>Every year between {start} and {end}</span>,
    },
    Save:'Save',
    Close:'Close'
}