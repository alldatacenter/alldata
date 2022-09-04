import React, { Component } from "react";
import { DatePicker } from "antd";
import _ from "lodash";
import moment from "moment";


const { RangePicker } = DatePicker;
class DatePickerWrapper extends Component {
  constructor(props) {
    super(props);
    this.state = {
      value: this.props.value
    }
  }

  onChange = (date, dateString) => {
    this.setState({
      value: date
    })
    this.props.onChange(date);
  };

  disabledDate(isNeedDisableDate, current, latestTime) {
    if (isNeedDisableDate) {
      return current && current > moment(latestTime);
    } else {
      return false;
    }
  }

  renderExtraFooter = (timeQuickers, relativeStartTime) => {
    let { type } = this.props;
    return <div>
      {timeQuickers.map(item => {
        if (type === 'range') {
          return <a onClick={() => this.onChange([moment(relativeStartTime).subtract(item.value[0], item.value[1]), moment(relativeStartTime)])}>{item.label} </a>
        }
        return <a onClick={() => this.onChange(moment(relativeStartTime).subtract(item.value[0], item.value[1]))}>{item.label} </a>
      })}
    </div>
  };

  render() {
    let { type } = this.props;
    let isNeedDisableDate = _.get(this.props, "item.defModel.isNeedDisableDate", false);
    let disableMaxTime = _.parseInt(_.get(this.props, "item.defModel.disableMaxTime", moment().unix()));
    let relativeStartTime = _.parseInt(_.get(this.props, "item.defModel.relativeStartTime", moment().unix()));
    let timeQuickers = _.get(this.props, "item.defModel.timeQuickers", []);
    if (type === 'range') {
      return (
        <RangePicker onChange={this.onChange}
          value={this.state.value}
          style={{ width: '100%' }} placeholder={["", ""]}
          format={window.APPLICATION_DATE_FORMAT || "YYYY-MM-DD HH:mm"} showTime={{ format: 'HH:mm' }}
          renderExtraFooter={() => this.renderExtraFooter(timeQuickers, relativeStartTime * 1000)}
        />
      );
    }
    return (
      <DatePicker
        showTime
        value={this.state.value}
        onChange={this.onChange}
        renderExtraFooter={() => this.renderExtraFooter(timeQuickers, relativeStartTime * 1000)}
        format={window.APPLICATION_DATE_FORMAT || "YYYY-MM-DD HH:mm"}
        disabledDate={current => this.disabledDate(isNeedDisableDate, current, disableMaxTime * 1000)}
        {...this.props}
      />
    );
  }
}

export default DatePickerWrapper;
