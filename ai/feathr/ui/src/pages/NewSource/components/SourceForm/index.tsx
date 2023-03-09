import React, { forwardRef } from 'react'

import { Button, Col, Divider, Form, Input, Row, Select } from 'antd'

import AddTags from '@/components/AddTags'

import { useForm, SourceTypeEnum } from './useForm'

export interface SourceFormProps {
  project?: string
}

const { Item } = Form
const { TextArea } = Input

const formItemLayout = {
  labelCol: {
    xs: { span: 24 },
    sm: { span: 8 }
  },
  wrapperCol: {
    xs: { span: 24 },
    sm: { span: 16 }
  }
}

const SourceForm = (props: SourceFormProps, ref: any) => {
  const { project } = props
  const [form] = Form.useForm()

  const { createLoading, sourceTypeOptions, jdbcAuthOptions, type, onTabsChange, onFinish } =
    useForm(form, project)

  return (
    <>
      <Form
        {...formItemLayout}
        labelWrap
        style={{ margin: '0 auto', maxWidth: 700 }}
        form={form}
        onFinish={onFinish}
      >
        <Item label="Project" name="project">
          <Input disabled />
        </Item>
        <Item name="name" label="Name" rules={[{ required: true }]}>
          <Input maxLength={200} />
        </Item>
        <Item label="Type" name="type" rules={[{ required: true }]}>
          <Select options={sourceTypeOptions} />
        </Item>
        <Divider orientation="left">Datasource Attributes</Divider>
        {(type === SourceTypeEnum.HDFS || type === SourceTypeEnum.SNOWFLAKE) && (
          <Item name="path" label="Path" rules={[{ required: true }]}>
            <TextArea autoSize={{ maxRows: 3, minRows: 3 }} />
          </Item>
        )}
        {type === SourceTypeEnum.JDBC && (
          <>
            <Item name="url" label="Url" rules={[{ required: true }]}>
              <Input />
            </Item>
            <Form.Item
              name={[SourceTypeEnum.JDBC, 'value']}
              label="Type"
              rules={[{ required: true, message: 'This is required' }]}
            >
              <Input
                addonBefore={
                  <Item noStyle name={[SourceTypeEnum.JDBC, 'type']} rules={[{ required: true }]}>
                    <Select style={{ width: 100 }}>
                      <Select.Option value="dbtable">DBTABLE</Select.Option>
                      <Select.Option value="query">QUERY</Select.Option>
                    </Select>
                  </Item>
                }
              />
            </Form.Item>
            <Item name="auth" label="Auth" rules={[{ required: true }]}>
              <Select options={jdbcAuthOptions} />
            </Item>
          </>
        )}
        {type === SourceTypeEnum.COSMOSDB && (
          <>
            <Item name="endpoint" label="Endpoint" rules={[{ required: true }]}>
              <Input />
            </Item>
            <Item name="dbtable" label="Database Table" rules={[{ required: true }]}>
              <Input />
            </Item>
            <Item name="container" label="Container" rules={[{ required: true }]}>
              <Input />
            </Item>
          </>
        )}
        {type === SourceTypeEnum.SPARKSQL && (
          <>
            <Form.Item
              name={[SourceTypeEnum.SPARKSQL, 'value']}
              label="Type"
              rules={[{ required: true, message: 'This is required' }]}
            >
              <Input
                addonBefore={
                  <Item
                    noStyle
                    name={[SourceTypeEnum.SPARKSQL, 'type']}
                    rules={[{ required: true }]}
                  >
                    <Select style={{ width: 100 }}>
                      <Select.Option value="sql">SQL</Select.Option>
                      <Select.Option value="table">TABLE</Select.Option>
                    </Select>
                  </Item>
                }
              />
            </Form.Item>
          </>
        )}

        <Item name="preprocessing" label="Preprocessing">
          <TextArea autoSize={{ maxRows: 3, minRows: 3 }} />
        </Item>
        <Item name="eventTimestampColumn" label="Event Timestamp Column">
          <Input />
        </Item>
        <Item name="timestampFormat" label="Timestamp Format">
          <Input />
        </Item>
        <Divider orientation="left">Datasource Tags</Divider>
        <Row>
          <Col xs={24} sm={{ span: 22, offset: 2 }}>
            <AddTags onChange={onTabsChange} />
          </Col>
        </Row>
        <Divider />
        <Item label=" " colon={false}>
          <Button
            type="primary"
            htmlType="submit"
            title="submit and go back to list"
            loading={createLoading}
          >
            Submit
          </Button>
        </Item>
      </Form>
    </>
  )
}

const SourceFormComponent = forwardRef<unknown, SourceFormProps>(SourceForm)

SourceFormComponent.displayName = 'SourceFormComponent'

export default SourceFormComponent
