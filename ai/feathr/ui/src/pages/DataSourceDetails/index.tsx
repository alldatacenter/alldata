import React from 'react'

import { LoadingOutlined } from '@ant-design/icons'
import { Alert, Space, Breadcrumb, PageHeader, Spin, Button } from 'antd'
import { AxiosError } from 'axios'
import { useQuery } from 'react-query'
import { useNavigate, useParams, Link } from 'react-router-dom'

import { fetchDataSource } from '@/api'
import CardDescriptions from '@/components/CardDescriptions'
import { observer, useStore } from '@/hooks'
import { DataSource } from '@/models/model'
import { SourceAttributesMap } from '@/utils/attributesMapping'

const DataSourceDetails = () => {
  const navigate = useNavigate()
  const { globalStore } = useStore()
  const { project } = globalStore
  const { id = '' } = useParams()

  const {
    isLoading,
    error,
    data = { attributes: {} } as DataSource
  } = useQuery<DataSource, AxiosError>(['dataSourceId', id], () => fetchDataSource(project, id), {
    retry: false,
    refetchOnWindowFocus: false
  })

  const { attributes } = data

  return (
    <div className="page">
      <PageHeader
        breadcrumb={
          <Breadcrumb>
            <Breadcrumb.Item>
              <Link to={`/${project}/datasources`}>Data Sources</Link>
            </Breadcrumb.Item>
            <Breadcrumb.Item>Data Source Attributes</Breadcrumb.Item>
          </Breadcrumb>
        }
        extra={[
          <Button
            key="1"
            ghost
            type="primary"
            onClick={() => {
              navigate(`/${project}/features`)
            }}
          >
            View Features
          </Button>,
          <Button
            key="2"
            type="primary"
            onClick={() => {
              navigate(`/${project}/lineage`)
            }}
          >
            View Lineage
          </Button>
        ]}
        ghost={false}
        title="Data Source Attributes"
      >
        <Spin spinning={isLoading} indicator={<LoadingOutlined spin style={{ fontSize: 24 }} />}>
          <Space className="display-flex" direction="vertical" size="middle">
            {error && <Alert showIcon message={error} type="error" />}
            <CardDescriptions mapping={SourceAttributesMap} descriptions={attributes} />
          </Space>
        </Spin>
      </PageHeader>
    </div>
  )
}

export default observer(DataSourceDetails)
