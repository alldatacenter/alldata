import React from 'react';
import { Select, Spin } from 'antd';
import _ from 'lodash';
import api from '../api';

const Option = Select.Option;



class UserSelector extends React.Component {
    constructor(props) {
        super(props);

        this.lastFetchId = 0;

        this.fetchUser = _.debounce(this.fetchUser, 800);
        let { value, selected } = this.props;
        if (value && !Array.isArray(value)) {
            value = false
        }
        if (Array.isArray(value) && value.length && typeof value[0] === 'string') {
            value = false
        }
        this.state = {
            selected: value || selected || [],
            candidate: [],
            fetching: false
        };
    }

    componentWillMount() {
        let { empIds, value } = this.props, initEmpIds = [];
        if (value && !Array.isArray(value) && value.length > 4) {
            initEmpIds = value.split(",");
        }
        if (Array.isArray(value) && value.length && typeof value[0] === 'string') {
            initEmpIds = value;
        }
        if (empIds && empIds.length) {
            if (!Array.isArray(empIds)) {
                initEmpIds = JSON.parse(empIds);
            } else {
                initEmpIds = empIds;
            }
        }
        if (isFinite(initEmpIds)) {
            initEmpIds = [initEmpIds + ""]
        }
        initEmpIds = initEmpIds.filter(emp => emp.length > 0);
        if (initEmpIds && initEmpIds.length) {
            this.setState({ fetching: true });
            let req = [];
            initEmpIds.forEach(empId => {
                let ea = (empId + "").split("");
                //必须去除前缀的0才能工号查询
                while (ea[0] == "0") {
                    ea.shift();
                }
                req.push(this.getBucUserInfo(ea.join("")));
            });
            Promise.all(req).then(results => {
                let users = [];
                results.forEach(r => {
                    if (r.length) {
                        users.push(r[0]);
                    }
                });
                this.setState({ selected: users, fetching: false });
                this.onSelect(users);
            });
            if (req.length === 0) {
                this.setState({ fetching: false });
            }
        }

    }

    componentWillReceiveProps(nextProps) {
        if (!nextProps.value) {
            this.setState({ selected: [] })
        }
    }

    getBucUserInfo = (k) => {
        return api.getBucUserInfo(k);
    };

    fetchUser = (value) => {
        this.setState({ fetching: true });
        this.lastFetchId += 1;
        const fetchId = this.lastFetchId;
        this.getBucUserInfo(value)
            .then(users => {
                if (fetchId !== this.lastFetchId) { // for fetch callback order
                    return;
                }
                this.setState({ candidate: users || [], fetching: false });
            });
    }

    onSelect(users) {
        let { onlyEmpId } = this.props;
        if (onlyEmpId) {
            users = users.map(user => user.empId || user.emplId);
        }
        this.props.onSelect && this.props.onSelect(users);
        this.props.onChange && this.props.onChange(users);
    }

    handleChange(ss) {
        let selected = [];
        if (this.props.mode === 'single') {
            if (_.isEmpty(ss)) {
                selected = [];
            } else {
                selected = this.state.candidate.filter(u => (u.emplId || u.empId) === ss.key);
            }
        } else {
            const users = this.getSelected().concat(this.state.candidate);
            const userMap = _.zipObject(users.map(u => (u.emplId || u.empId)), users);
            selected = ss.map(s => userMap[s.key]);
        }
        if (_.isEqual(selected, this.getSelected())) return;

        this.setState({
            selected: selected,
            candidate: [],
            fetching: false,
        }, () => this.onSelect(this.state.selected));
    }

    handleDeselect(d) {
        let selected = this.getSelected().filter(u => (u.emplId || u.empId) !== d.key);

        this.setState({
            selected: selected,
            candidate: [],
            fetching: false,
        }, () => this.onSelect(this.state.selected));
    }

    createLabelRender(userRender) {
        return d => userRender ? userRender(d) : `${d.nickNameCn || d.name || d.lastName}(${d.emplId || d.empId})`;
    }

    getSelected() {
        return this.state.selected;
    }

    createOptionRender(userRender) {
        const defaultAvatarStyles = {
            height: 30,
            width: 30,
            marginRight: 5,
            borderRadius: '50%',
            verticalAlign: 'middle'
        };
        const defaultOptionRender = u => <div style={{ height: 30 }}>
            <img style={defaultAvatarStyles} src={u.avatar} />
            <span>{`${u.name}${u.nickNameCn ? '(' + u.nickNameCn + ')' : ''}-${(u.emplId || u.empId)}-${(u.deptDesc || '').split('-')[0]}`}</span>
        </div>;
        return d => userRender ? userRender(d) : defaultOptionRender(d);
    }

    render() {
        const styles = { width: '100%', minWidth: 200 };
        const { fetching, candidate, selected } = this.state;

        const selectedArray = selected;
        return (
            this.props.mode === 'single'
                ?
                <Select
                    className="biz-tUserSelector"
                    showSearch
                    labelInValue
                    allowClear
                    value={selectedArray.length ? { key: selectedArray[0].emplId || selectedArray[0].empId, label: this.createLabelRender(this.props.labelRender)(selectedArray[0]) } : undefined}
                    placeholder="Select a user"
                    optionFilterProp="children"
                    notFoundContent={fetching ? <Spin size="small" /> : null}
                    onSearch={this.fetchUser.bind(this)}
                    onChange={this.handleChange.bind(this)}
                    filterOption={false}
                    style={{ ...styles, ...this.props.style }}
                    onFocus={() => this.fetchUser('')}
                    disabled={this.props.disabled}
                >
                    {_.differenceBy(candidate, selectedArray, u => (u.emplId || u.empId))
                        .map(u => <Option key={(u.emplId || u.empId)}>{this.createOptionRender(this.props.optionRender)(u)}</Option>)}
                </Select>
                :
                <Select
                    className="biz-tUserSelector"
                    mode="multiple"
                    labelInValue
                    value={Array.isArray(selectedArray) ? selectedArray.map(u => ({ key: (u.emplId || u.empId), label: this.createLabelRender(this.props.labelRender)(u) })) : [selectedArray]}
                    placeholder={this.props.placeholder || "Select users"}
                    notFoundContent={fetching ? <Spin size="small" /> : null}
                    filterOption={false}
                    onSearch={this.fetchUser.bind(this)}
                    onChange={this.handleChange.bind(this)}
                    onDeselect={this.handleDeselect.bind(this)}
                    style={{ ...styles, ...this.props.style }}
                    onFocus={() => this.fetchUser('')}
                    disabled={this.props.disabled}
                >
                    {_.differenceBy(candidate, selectedArray, u => (u.emplId || u.empId))
                        .map(u => <Option key={(u.emplId || u.empId)}>{this.createOptionRender(this.props.optionRender)(u)}</Option>)}
                </Select>
        );
    }
}

export default UserSelector;
