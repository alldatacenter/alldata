import React from 'react';
import _ from 'lodash';
import { CalendarOutlined } from '@ant-design/icons';
import { Tabs, Radio, InputNumber, Checkbox, Select } from 'antd';
import language from './language';


const { TabPane } = Tabs;
const { Option } = Select;
const RadioGroup = Radio.Group;

class TCron extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      second: {
        cronEvery: '4',
        incrementIncrement: 5,
        incrementStart: 0,
        rangeStart: 0,
        rangeEnd: 5,
        specificSpecific: [0],
      },
      minute: {
        cronEvery: '4',
        incrementIncrement: 5,
        incrementStart: 0,
        rangeStart: 0,
        rangeEnd: 5,
        specificSpecific: [0],
      },
      hour: {
        cronEvery: '4',
        incrementIncrement: 5,
        incrementStart: 0,
        rangeStart: 0,
        rangeEnd: 5,
        specificSpecific: [0],
      },
      day: {
        cronEvery: '1',
        incrementStart: 3,
        incrementIncrement: 5,
        rangeStart: 1,
        rangeEnd: 3,
        specificSpecific: [],
        cronLastSpecificDomDay: 1,
        cronDaysBeforeEomMinus: 1,
        cronDaysNearestWeekday: 1,
      },
      week: {
        cronEvery: '1',
        incrementStart: 3,
        incrementIncrement: 5,
        specificSpecific: [],
        cronNthDayDay: 1,
        cronNthDayNth: 1,
      },
      month: {
        cronEvery: '1',
        incrementStart: 3,
        incrementIncrement: 5,
        rangeStart: 1,
        rangeEnd: 3,
        specificSpecific: [],
      },
      year: {
        cronEvery: '1',
        incrementStart: 2018,
        incrementIncrement: 1,
        rangeStart: 2018,
        rangeEnd: 2101,
        specificSpecific: [],
      },
    }
  }

  setState(nextState) {
    super.setState(nextState, () => {
      if (this.props.onChange) {
        this.props.onChange(this.cron());
      }
    });
  }

  render() {
    const text = language[this.props.i18n || 'cn'];
    const radioStyle = {
      display: 'block',
      minHeight: '40px',
      lineHeight: '40px',
    };

    const { second, minute, hour, day, week, month, year } = this.state;

    return (
      <div className="biz-tCron">
        <Tabs>
          <TabPane tab={<span><CalendarOutlined />{text.Seconds.name}</span>} key="seconds">
            <RadioGroup onChange={e => this.setState({ second: { ...second, cronEvery: e.target.value } })} value={second.cronEvery}>
              <Radio style={radioStyle} value="1">{text.Seconds.every}</Radio>
              <Radio style={radioStyle} value="2">
                {text.Seconds.interval(
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={0} max={59}
                    value={second.incrementIncrement}
                    onChange={v => this.setState({ second: { ...second, incrementIncrement: v || 0 } })} />,
                  <InputNumber size="small"
                    value={second.incrementStart}
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={0} max={59}
                    onChange={v => this.setState({ second: { ...second, incrementStart: v || 0 } })} />
                )}
              </Radio>
              <Radio style={radioStyle} value="3">
                {text.Seconds.cycle(
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={0} max={second.rangeEnd}
                    value={second.rangeStart}
                    onChange={v => this.setState({ second: { ...second, rangeStart: v || 0 } })} />,
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={second.rangeStart} max={59}
                    value={second.rangeEnd}
                    onChange={v => this.setState({ second: { ...second, rangeEnd: v || 0 } })} />
                )}
              </Radio>
              <Radio style={radioStyle} value="4">
                {text.Seconds.specific}
                <div style={{ marginLeft: 24 }}>{_.range(0, 10).map(this.renderSecondCheckbox.bind(this))}</div>
                <div style={{ marginLeft: 24 }}>{_.range(10, 20).map(this.renderSecondCheckbox.bind(this))}</div>
                <div style={{ marginLeft: 24 }}>{_.range(20, 30).map(this.renderSecondCheckbox.bind(this))}</div>
                <div style={{ marginLeft: 24 }}>{_.range(30, 40).map(this.renderSecondCheckbox.bind(this))}</div>
                <div style={{ marginLeft: 24 }}>{_.range(40, 50).map(this.renderSecondCheckbox.bind(this))}</div>
                <div style={{ marginLeft: 24 }}>{_.range(50, 60).map(this.renderSecondCheckbox.bind(this))}</div>
              </Radio>
            </RadioGroup>
          </TabPane>
          <TabPane tab={<span><CalendarOutlined />{text.Minutes.name}</span>} key="minutes">
            <RadioGroup onChange={e => this.setState({ minute: { ...minute, cronEvery: e.target.value } })} value={minute.cronEvery}>
              <Radio style={radioStyle} value="1">{text.Minutes.every}</Radio>
              <Radio style={radioStyle} value="2">
                {text.Minutes.interval(
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={0} max={59}
                    value={minute.incrementIncrement}
                    onChange={v => this.setState({ minute: { ...minute, incrementIncrement: v || 0 } })} />,
                  <InputNumber size="small"
                    value={minute.incrementStart}
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={0} max={59}
                    onChange={v => this.setState({ minute: { ...minute, incrementStart: v || 0 } })} />
                )}
              </Radio>
              <Radio style={radioStyle} value="3">
                {text.Minutes.cycle(
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={1} max={minute.rangeEnd}
                    value={minute.rangeStart}
                    onChange={v => this.setState({ minute: { ...minute, rangeStart: v || 0 } })} />,
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={minute.rangeStart} max={59}
                    value={minute.rangeEnd}
                    onChange={v => this.setState({ minute: { ...minute, rangeEnd: v || 0 } })} />
                )}
              </Radio>
              <Radio style={radioStyle} value="4">
                {text.Minutes.specific}
                <div style={{ marginLeft: 24 }}>{_.range(0, 10).map(this.renderMinuteCheckbox.bind(this))}</div>
                <div style={{ marginLeft: 24 }}>{_.range(10, 20).map(this.renderMinuteCheckbox.bind(this))}</div>
                <div style={{ marginLeft: 24 }}>{_.range(20, 30).map(this.renderMinuteCheckbox.bind(this))}</div>
                <div style={{ marginLeft: 24 }}>{_.range(30, 40).map(this.renderMinuteCheckbox.bind(this))}</div>
                <div style={{ marginLeft: 24 }}>{_.range(40, 50).map(this.renderMinuteCheckbox.bind(this))}</div>
                <div style={{ marginLeft: 24 }}>{_.range(50, 60).map(this.renderMinuteCheckbox.bind(this))}</div>
              </Radio>
            </RadioGroup>
          </TabPane>
          <TabPane tab={<span><CalendarOutlined />{text.Hours.name}</span>} key="hours">
            <RadioGroup onChange={e => this.setState({ hour: { ...hour, cronEvery: e.target.value } })} value={hour.cronEvery}>
              <Radio style={radioStyle} value="1">{text.Hours.every}</Radio>
              <Radio style={radioStyle} value="2">
                {text.Hours.interval(
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={0} max={23}
                    value={hour.incrementIncrement}
                    onChange={v => this.setState({ hour: { ...hour, incrementIncrement: v || 0 } })} />,
                  <InputNumber size="small"
                    value={hour.incrementStart}
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={0} max={23}
                    onChange={v => this.setState({ hour: { ...hour, incrementStart: v || 0 } })} />
                )}
              </Radio>
              <Radio style={radioStyle} value="3">
                {text.Hours.cycle(
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={0} max={hour.rangeEnd}
                    value={hour.rangeStart}
                    onChange={v => this.setState({ hour: { ...hour, rangeStart: v || 0 } })} />,
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={hour.rangeStart} max={23}
                    value={hour.rangeEnd}
                    onChange={v => this.setState({ hour: { ...hour, rangeEnd: v || 0 } })} />
                )}
              </Radio>
              <Radio style={radioStyle} value="4">
                {text.Hours.specific}
                <div style={{ marginLeft: 24 }}>{_.range(0, 8).map(this.renderHourCheckbox.bind(this))}</div>
                <div style={{ marginLeft: 24 }}>{_.range(8, 16).map(this.renderHourCheckbox.bind(this))}</div>
                <div style={{ marginLeft: 24 }}>{_.range(16, 24).map(this.renderHourCheckbox.bind(this))}</div>
              </Radio>
            </RadioGroup>
          </TabPane>
          <TabPane tab={<span><CalendarOutlined />{text.Day.name}</span>} key="day">
            <RadioGroup onChange={e => this.setState({ day: { ...day, cronEvery: e.target.value } })} value={day.cronEvery}>
              <Radio style={radioStyle} value="1">{text.Day.every}</Radio>
              <Radio style={radioStyle} value="2">
                {text.Day.intervalWeek(
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={1} max={7}
                    value={week.incrementIncrement}
                    onChange={v => this.setState({ week: { ...week, incrementIncrement: v || 0 } })} />,
                  <Select size="small" value={week.incrementStart} onChange={v => this.setState({ week: { ...week, incrementStart: v } })}>
                    {_.range(1, 8).map(i => <Option key={i} value={i}>{text.Week[i - 1]}</Option>)}
                  </Select>
                )}
              </Radio>
              <Radio style={radioStyle} value="3">
                {text.Day.intervalDay(
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={1} max={31}
                    value={day.incrementIncrement}
                    onChange={v => this.setState({ day: { ...day, incrementIncrement: v || 0 } })} />,
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={1} max={31}
                    value={day.incrementStart}
                    onChange={v => this.setState({ day: { ...day, incrementStart: v || 0 } })} />
                )}
              </Radio>
              <Radio style={{ ...radioStyle, height: 80 }} value="4">
                {text.Day.specificWeek}
                <div style={{ marginLeft: 24 }}>{_.range(0, 7).map(this.renderWeekCheckbox.bind(this))}</div>
              </Radio>
              <Radio style={{ ...radioStyle, height: 160 }} value="5">
                {text.Day.specificDay}
                <div style={{ marginLeft: 24 }}>{_.range(1, 12).map(this.renderDayCheckbox.bind(this))}</div>
                <div style={{ marginLeft: 24 }}>{_.range(12, 23).map(this.renderDayCheckbox.bind(this))}</div>
                <div style={{ marginLeft: 24 }}>{_.range(23, 32).map(this.renderDayCheckbox.bind(this))}</div>
              </Radio>
              <Radio style={radioStyle} value="6">{text.Day.lastDay}</Radio>
              <Radio style={radioStyle} value="7">{text.Day.lastWeekday}</Radio>
              <Radio style={radioStyle} value="8">
                {text.Day.lastWeek(
                  <Select size="small" value={day.cronLastSpecificDomDay} onChange={v => this.setState({ day: { ...day, cronLastSpecificDomDay: v } })}>
                    {_.range(1, 8).map(i => <Option key={i} value={i}>{text.Week[i - 1]}</Option>)}
                  </Select>
                )}
              </Radio>
              <Radio style={radioStyle} value="9">
                {text.Day.beforeEndMonth(
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={1} max={30}
                    value={day.cronDaysBeforeEomMinus}
                    onChange={v => this.setState({ day: { ...day, cronDaysBeforeEomMinus: v || 0 } })} />
                )}
              </Radio>
              <Radio style={radioStyle} value="10">
                {text.Day.nearestWeekday(
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={1} max={31}
                    value={day.cronDaysNearestWeekday}
                    onChange={v => this.setState({ day: { ...day, cronDaysNearestWeekday: v || 0 } })} />
                )}
              </Radio>
              <Radio style={radioStyle} value="11">
                {text.Day.someWeekday(
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={1} max={5}
                    value={week.cronNthDayNth}
                    onChange={v => this.setState({ week: { ...week, cronNthDayNth: v || 0 } })} />,
                  <Select size="small" value={week.cronNthDayDay} onChange={v => this.setState({ week: { ...week, cronNthDayDay: v } })}>
                    {_.range(1, 8).map(i => <Option key={i} value={i}>{text.Week[i - 1]}</Option>)}
                  </Select>
                )}
              </Radio>
            </RadioGroup>
          </TabPane>
          <TabPane tab={<span><CalendarOutlined />{text.Month.name}</span>} key="month">
            <RadioGroup onChange={e => this.setState({ month: { ...month, cronEvery: e.target.value } })} value={month.cronEvery}>
              <Radio style={radioStyle} value="1">{text.Month.every}</Radio>
              <Radio style={radioStyle} value="2">
                {text.Month.interval(
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={1} max={12}
                    value={month.incrementIncrement}
                    onChange={v => this.setState({ month: { ...month, incrementIncrement: v || 0 } })} />,
                  <InputNumber size="small"
                    value={month.incrementStart}
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={1} max={12}
                    onChange={v => this.setState({ month: { ...month, incrementStart: v || 0 } })} />
                )}
              </Radio>
              <Radio style={radioStyle} value="3">
                {text.Month.cycle(
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={1} max={month.rangeEnd}
                    value={month.rangeStart}
                    onChange={v => this.setState({ month: { ...month, rangeStart: v || 0 } })} />,
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={month.rangeStart} max={12}
                    value={month.rangeEnd}
                    onChange={v => this.setState({ month: { ...month, rangeEnd: v || 0 } })} />
                )}
              </Radio>
              <Radio style={radioStyle} value="4">
                {text.Month.specific}
                <div style={{ marginLeft: 24 }}>{_.range(1, 7).map(this.renderMonthCheckbox.bind(this))}</div>
                <div style={{ marginLeft: 24 }}>{_.range(7, 13).map(this.renderMonthCheckbox.bind(this))}</div>
              </Radio>
            </RadioGroup>
          </TabPane>
          <TabPane tab={<span><CalendarOutlined />{text.Year.name}</span>} key="year">
            <RadioGroup onChange={e => this.setState({ year: { ...year, cronEvery: e.target.value } })} value={year.cronEvery}>
              <Radio style={radioStyle} value="1">{text.Year.every}</Radio>
              <Radio style={radioStyle} value="2">
                {text.Year.interval(
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={1} max={100}
                    value={year.incrementIncrement}
                    onChange={v => this.setState({ year: { ...year, incrementIncrement: v || 0 } })} />,
                  <InputNumber size="small"
                    value={year.incrementStart}
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={2018} max={2101}
                    onChange={v => this.setState({ year: { ...year, incrementStart: v || 0 } })} />
                )}
              </Radio>
              <Radio style={radioStyle} value="3">
                {text.Year.cycle(
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={2018} max={year.rangeEnd}
                    value={year.rangeStart}
                    onChange={v => this.setState({ year: { ...year, rangeStart: v || 0 } })} />,
                  <InputNumber size="small"
                    style={{ marginLeft: 2, marginRight: 2 }}
                    min={year.rangeStart} max={2101}
                    value={year.rangeEnd}
                    onChange={v => this.setState({ year: { ...year, rangeEnd: v || 0 } })} />
                )}
              </Radio>
              <Radio style={radioStyle} value="4">
                {text.Year.specific}
                <div style={{ marginLeft: 24 }}>{_.range(2018, 2026).map(this.renderYearCheckbox.bind(this))}</div>
                <div style={{ marginLeft: 24 }}>{_.range(2026, 2034).map(this.renderYearCheckbox.bind(this))}</div>
              </Radio>
            </RadioGroup>
          </TabPane>
        </Tabs>
      </div>
    );
  }

  renderSecondCheckbox(sec) {
    const { second } = this.state;

    const onCheck = checked => {
      if (checked) {
        this.setState({ second: { ...second, specificSpecific: second.specificSpecific.concat([sec]), cronEvery: '4' } });
      } else {
        this.setState({ second: { ...second, specificSpecific: second.specificSpecific.filter(n => n !== sec), cronEvery: '4' } });
      }
    }

    return <Checkbox key={sec}
      checked={second.specificSpecific.indexOf(sec) !== -1}
      onChange={e => onCheck(e.target.checked)}>
      {sec < 10 ? '0' + sec : sec}
    </Checkbox>;
  }

  renderMinuteCheckbox(min) {
    const { minute } = this.state;

    const onCheck = checked => {
      if (checked) {
        this.setState({ minute: { ...minute, specificSpecific: minute.specificSpecific.concat([min]), cronEvery: '4' } });
      } else {
        this.setState({ minute: { ...minute, specificSpecific: minute.specificSpecific.filter(n => n !== min), cronEvery: '4' } });
      }
    }

    return <Checkbox key={min}
      checked={minute.specificSpecific.indexOf(min) !== -1}
      onChange={e => onCheck(e.target.checked)}>
      {min < 10 ? '0' + min : min}
    </Checkbox>;
  }

  renderHourCheckbox(h) {
    const { hour } = this.state;

    const onCheck = checked => {
      if (checked) {
        this.setState({ hour: { ...hour, specificSpecific: hour.specificSpecific.concat([h]), cronEvery: '4' } });
      } else {
        this.setState({ hour: { ...hour, specificSpecific: hour.specificSpecific.filter(n => n !== h), cronEvery: '4' } });
      }
    }

    return <Checkbox key={h}
      checked={hour.specificSpecific.indexOf(h) !== -1}
      onChange={e => onCheck(e.target.checked)}>
      {h < 10 ? '0' + h : h}
    </Checkbox>;
  }

  renderWeekCheckbox(wk) {
    const { day, week } = this.state;
    const text = language[this.props.i18n || 'cn'];

    const weekArray = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
    const onCheck = checked => {
      if (checked) {
        this.setState({ week: { ...week, specificSpecific: week.specificSpecific.concat([weekArray[wk]]) }, day: { ...day, cronEvery: '4' } });
      } else {
        this.setState({ week: { ...week, specificSpecific: week.specificSpecific.filter(n => n !== weekArray[wk]) }, day: { ...day, cronEvery: '4' } });
      }
    }

    return <Checkbox key={wk}
      checked={week.specificSpecific.indexOf(weekArray[wk]) !== -1}
      onChange={e => onCheck(e.target.checked)}>
      {text.Week[wk]}
    </Checkbox>;
  }

  renderDayCheckbox(d) {
    const { day } = this.state;

    const onCheck = checked => {
      if (checked) {
        this.setState({ day: { ...day, specificSpecific: day.specificSpecific.concat([d]), cronEvery: '5' } });
      } else {
        this.setState({ day: { ...day, specificSpecific: day.specificSpecific.filter(n => n !== d), cronEvery: '5' } });
      }
    }

    return <Checkbox key={d}
      checked={day.specificSpecific.indexOf(d) !== -1}
      onChange={e => onCheck(e.target.checked)}>
      {d < 10 ? '0' + d : d}
    </Checkbox>;
  }

  renderMonthCheckbox(mon) {
    const { month } = this.state;

    const onCheck = checked => {
      if (checked) {
        this.setState({ month: { ...month, specificSpecific: month.specificSpecific.concat([mon]), cronEvery: '4' } });
      } else {
        this.setState({ month: { ...month, specificSpecific: month.specificSpecific.filter(n => n !== mon), cronEvery: '4' } });
      }
    }

    return <Checkbox key={mon}
      checked={month.specificSpecific.indexOf(mon) !== -1}
      onChange={e => onCheck(e.target.checked)}>
      {mon < 10 ? '0' + mon : mon}
    </Checkbox>;
  }

  renderYearCheckbox(y) {
    const { year } = this.state;

    const onCheck = checked => {
      if (checked) {
        this.setState({ year: { ...year, specificSpecific: year.specificSpecific.concat([y]), cronEvery: '4' } });
      } else {
        this.setState({ year: { ...year, specificSpecific: year.specificSpecific.filter(n => n !== y), cronEvery: '4' } });
      }
    }

    return <Checkbox key={y}
      checked={year.specificSpecific.indexOf(y) !== -1}
      onChange={e => onCheck(e.target.checked)}>
      {y}
    </Checkbox>;
  }

  cron() {
    return `${this.secondsText() || '*'} ${this.minutesText() || '*'} ${this.hoursText() || '*'} ${this.daysText() || '*'} ${this.monthsText() || '*'} ${this.weeksText() || '?'} ${this.yearsText() || '*'}`
  }

  secondsText() {
    const { second } = this.state;
    let seconds = '';
    let cronEvery = second.cronEvery;
    switch (cronEvery.toString()) {
      case '1':
        seconds = '*';
        break;
      case '2':
        seconds = second.incrementStart + '/' + second.incrementIncrement;
        break;
      case '3':
        seconds = second.rangeStart + '-' + second.rangeEnd;
        break;
      case '4':
        second.specificSpecific.map(val => {
          seconds += val + ','
        });
        seconds = seconds.slice(0, -1);
        break;
    }
    return seconds;
  }

  minutesText() {
    const { minute } = this.state;
    let minutes = '';
    let cronEvery = minute.cronEvery;
    switch (cronEvery.toString()) {
      case '1':
        minutes = '*';
        break;
      case '2':
        minutes = minute.incrementStart + '/' + minute.incrementIncrement;
        break;
      case '3':
        minutes = minute.rangeStart + '-' + minute.rangeEnd;
        break;
      case '4':
        minute.specificSpecific.map(val => {
          minutes += val + ','
        });
        minutes = minutes.slice(0, -1);
        break;
    }
    return minutes;
  }

  hoursText() {
    const { hour } = this.state;
    let hours = '';
    let cronEvery = hour.cronEvery;
    switch (cronEvery.toString()) {
      case '1':
        hours = '*';
        break;
      case '2':
        hours = hour.incrementStart + '/' + hour.incrementIncrement;
        break;
      case '3':
        hours = hour.rangeStart + '-' + hour.rangeEnd;
        break;
      case '4':
        hour.specificSpecific.map(val => {
          hours += val + ','
        });
        hours = hours.slice(0, -1);
        break;
    }
    return hours;
  }

  daysText() {
    const { day } = this.state;
    let days = '';
    let cronEvery = day.cronEvery;
    switch (cronEvery.toString()) {
      case '1':
        break;
      case '2':
      case '4':
      case '8':
      case '11':
        days = '?';
        break;
      case '3':
        days = day.incrementStart + '/' + day.incrementIncrement;
        break;
      case '5':
        day.specificSpecific.map(val => {
          days += val + ','
        });
        days = days.slice(0, -1);
        break;
      case '6':
        days = "L";
        break;
      case '7':
        days = "LW";
        break;
      case '9':
        days = 'L-' + day.cronDaysBeforeEomMinus;
        break;
      case '10':
        days = day.cronDaysNearestWeekday + "W";
        break
    }
    return days;
  }

  weeksText() {
    const { day, week } = this.state;
    let weeks = '';
    let cronEvery = day.cronEvery;
    switch (cronEvery.toString()) {
      case '1':
      case '3':
      case '5':
      case '6':
      case '7':
      case '9':
      case '10':
        weeks = '?';
        break;
      case '2':
        weeks = week.incrementStart + '/' + week.incrementIncrement;
        break;
      case '4':
        week.specificSpecific.map(val => {
          weeks += val + ','
        });
        weeks = weeks.slice(0, -1) || '*';
        break;
      case '8':
        weeks = day.cronLastSpecificDomDay + 'L';
        break;
      case '11':
        weeks = week.cronNthDayDay + "#" + week.cronNthDayNth;
        break;
    }
    return weeks;
  }

  monthsText() {
    const { month } = this.state;
    let months = '';
    let cronEvery = month.cronEvery;
    switch (cronEvery.toString()) {
      case '1':
        months = '*';
        break;
      case '2':
        months = month.incrementStart + '/' + month.incrementIncrement;
        break;
      case '3':
        months = month.rangeStart + '-' + month.rangeEnd;
        break;
      case '4':
        month.specificSpecific.map(val => {
          months += val + ','
        });
        months = months.slice(0, -1);
        break;
    }
    return months;
  }

  yearsText() {
    const { year } = this.state;
    let years = '';
    let cronEvery = year.cronEvery;
    switch (cronEvery.toString()) {
      case '1':
        years = '*';
        break;
      case '2':
        years = year.incrementStart + '/' + year.incrementIncrement;
        break;
      case '3':
        years = year.rangeStart + '-' + year.rangeEnd;
        break;
      case '4':
        year.specificSpecific.map(val => {
          years += val + ','
        });
        years = years.slice(0, -1);
        break;
    }
    return years;
  }
}

export default TCron;
