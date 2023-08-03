import React, { forwardRef, Fragment } from 'react'

import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons'
import { Button, Col, Divider, Form, Input, Radio, Row, Select, Space } from 'antd'

import AddTags from '@/components/AddTags'

import { useForm, FeatureEnum, TransformationTypeEnum } from './useForm'

export interface FeatureFormProps {
  project?: string
}

const { Item } = Form

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

const FeatureForm = (props: FeatureFormProps, ref: any) => {
  const { project } = props
  const [form] = Form.useForm()

  const {
    createLoading,
    loading,
    featureType,
    selectTransformationType,
    anchorOptions,
    anchorFeatureOptions,
    derivedFeatureOptions,
    valueOptions,
    tensorOptions,
    typeOptions,
    onTabsChange,
    onFinish
  } = useForm(form, project)

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
        <Item label="Select Feature Type" name="featureType">
          <Radio.Group>
            <Radio value={FeatureEnum.Anchor}>Anchor Feature</Radio>
            <Radio value={FeatureEnum.Derived}>Derived Feature</Radio>
          </Radio.Group>
        </Item>
        {featureType === FeatureEnum.Anchor ? (
          <>
            <Item
              rules={[
                {
                  required: true
                }
              ]}
              label="Select Anchor"
              name="anchor"
            >
              <Select options={anchorOptions} loading={loading} />
            </Item>
          </>
        ) : (
          <>
            <Item label="Select Input Anchor Features" name="anchorFeatures">
              <Select showArrow mode="multiple" options={anchorFeatureOptions} loading={loading} />
            </Item>
            <Item label="Select Input Derived Features" name="derivedFeatures">
              <Select showArrow mode="multiple" options={derivedFeatureOptions} loading={loading} />
            </Item>
          </>
        )}
        <Divider orientation="left">Feature Tags</Divider>
        <Row>
          <Col xs={24} sm={{ span: 22, offset: 2 }}>
            <AddTags onChange={onTabsChange} />
          </Col>
        </Row>
        <Divider orientation="left">Feature Keys</Divider>
        <Form.List name="keys">
          {(fields, { add, remove }) => (
            <>
              {fields.map(({ key, name }, index) => (
                <Fragment key={key}>
                  <Item
                    label="Key Column"
                    name={[name, 'keyColumn']}
                    rules={[{ required: true, message: 'Missing key column' }]}
                  >
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'baseline',
                        gap: 8
                      }}
                    >
                      <Form.Item noStyle style={{ flex: 1 }}>
                        <Input maxLength={50} />
                      </Form.Item>
                      <MinusCircleOutlined onClick={() => remove(name)} />
                    </div>
                  </Item>
                  <Item
                    rules={[
                      {
                        required: true,
                        message: 'Missing key column type'
                      }
                    ]}
                    label="Key Column Type"
                    name={[name, 'keyColumnType']}
                  >
                    <Select options={valueOptions} />
                  </Item>
                  <Form.Item label="Key Full Name" name={[name, 'fullName']}>
                    <Input />
                  </Form.Item>
                  <Form.Item label="Key Column Alias" name={[name, 'keyColumnAlias']}>
                    <Input />
                  </Form.Item>
                  <Form.Item label="Description" name={[name, 'description']}>
                    <Input />
                  </Form.Item>
                  <Divider dashed></Divider>
                </Fragment>
              ))}
              <Form.Item label=" " colon={false}>
                <Button block type="dashed" icon={<PlusOutlined />} onClick={() => add()}>
                  Add keys
                </Button>
              </Form.Item>
            </>
          )}
        </Form.List>
        <Divider orientation="left">Feature Type</Divider>
        <Item name="type" label="Type" rules={[{ required: true }]}>
          <Select options={typeOptions}></Select>
        </Item>
        <Item name="tensorCategory" label="Tensor Category" rules={[{ required: true }]}>
          <Select options={tensorOptions}></Select>
        </Item>
        <Item name="dimensionType" label="Dimension Type" rules={[{ required: true }]}>
          <Select mode="multiple" options={valueOptions} />
        </Item>
        <Item name="valType" label="Value Type" rules={[{ required: true }]}>
          <Select options={valueOptions} />
        </Item>
        <Divider orientation="left">Transformation</Divider>
        <Item label="Select Transformation Type" name="selectTransformationType">
          <Radio.Group>
            <Space direction="vertical">
              <Radio value={TransformationTypeEnum.Expression}>Expression Transformation</Radio>
              <Radio value={TransformationTypeEnum.Window}>Window Transformation</Radio>
              <Radio value={TransformationTypeEnum.UDF}>UDF Transformation</Radio>
            </Space>
          </Radio.Group>
        </Item>
        {selectTransformationType === TransformationTypeEnum.Expression && (
          <Item
            rules={[
              {
                required: true
              }
            ]}
            name="transformExpr"
            label="Expression Transformation"
          >
            <Input />
          </Item>
        )}
        {selectTransformationType === TransformationTypeEnum.Window && (
          <>
            <Item
              rules={[
                {
                  required: true
                }
              ]}
              name="defExpr"
              label="Definition Expression"
            >
              <Input />
            </Item>
            <Item name="aggFunc" label="Aggregation Function">
              <Input />
            </Item>
            <Item name="window" label="Window">
              <Input />
            </Item>
            <Item name="groupBy" label="Group By">
              <Input />
            </Item>
            <Item name="filter" label="Filter">
              <Input />
            </Item>
            <Item name="limit" label="Limit">
              <Input />
            </Item>
          </>
        )}
        {selectTransformationType === TransformationTypeEnum.UDF && (
          <Item name="udfExpr" label="UDF Transformation" rules={[{ required: true }]}>
            <Input />
          </Item>
        )}

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

const FeatureFormComponent = forwardRef<unknown, FeatureFormProps>(FeatureForm)

FeatureFormComponent.displayName = 'FeatureFormComponent'

export default FeatureFormComponent
