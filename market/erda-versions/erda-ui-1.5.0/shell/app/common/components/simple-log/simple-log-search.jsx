// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import React from 'react';
import { isEmpty } from 'lodash';
import { Form, Input, Button } from 'antd';
import { ErdaIcon } from 'common';
import './simple-log.scss';
import i18n from 'i18n';

const FormItem = Form.Item;

class LogSearchForm extends React.Component {
  formRef = React.createRef();

  componentDidMount = () => {
    this.setForm(this.props.formData);
  };

  UNSAFE_componentWillReceiveProps({ formData }) {
    if (formData !== this.props.formData) {
      this.setForm(formData);
    }
  }

  setForm = (formData) => {
    if (!isEmpty(formData)) {
      this.formRef.current.setFieldsValue(formData);
      // this.handleSubmit();
    }
  };

  handleSubmit = (values) => {
    const { setSearch } = this.props;
    setSearch(this.formRef.current.getFieldsValue());
  };

  render() {
    return (
      <div className="log-search">
        <Form ref={this.formRef} onFinish={this.handleSubmit}>
          <FormItem
            name="requestId"
            className="log-search-logId"
            rules={[{ required: true, message: `${i18n.t('common:please fill out')}request id` }]}
          >
            <Input placeholder={`${i18n.t('please enter')}request id${i18n.t('search')}`} />
          </FormItem>
          <Button
            className="log-search-btn"
            type="primary"
            htmlType="submit"
            icon={<ErdaIcon type="search1" />}
          >
            {i18n.t('common:search for')}
          </Button>
        </Form>
      </div>
    );
  }
}
export default LogSearchForm;
