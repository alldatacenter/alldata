import React, { forwardRef } from 'react'

import { DeleteOutlined } from '@ant-design/icons'
import { Button, message, notification, Popconfirm, Space } from 'antd'
import { useQuery } from 'react-query'
import { useNavigate } from 'react-router-dom'

import { fetchDataSources, deleteEntity } from '@/api'
import ResizeTable, { ResizeColumnType } from '@/components/ResizeTable'
import { DataSource } from '@/models/model'

export interface DataSourceTableProps {
  project: string
}
const DataSourceTable = (props: DataSourceTableProps, ref: any) => {
  const navigate = useNavigate()

  const { project } = props

  const getDetialUrl = (guid: string) => {
    return `/${project}/datasources/${guid}`
  }

  const columns: ResizeColumnType<DataSource>[] = [
    {
      key: 'name',
      title: 'Name',
      ellipsis: true,
      width: 200,
      render: (record: DataSource) => {
        return (
          <Button
            type="link"
            onClick={() => {
              navigate(getDetialUrl(record.guid))
            }}
          >
            {record.displayText}
          </Button>
        )
      }
    },
    {
      key: 'type',
      title: 'Type',
      ellipsis: true,
      width: 80,
      render: (record: DataSource) => {
        return record.attributes.type
      }
    },
    {
      key: 'path',
      title: 'Path',
      width: 220,
      render: (record: DataSource) => {
        return record.attributes.path
      }
    },
    {
      key: 'preprocessing',
      title: 'Preprocessing',
      ellipsis: true,
      width: 190,
      render: (record: DataSource) => {
        return record.attributes.preprocessing
      }
    },
    {
      key: 'eventTimestampColumn',
      title: 'Event Timestamp Column',
      ellipsis: true,
      width: 190,
      render: (record: DataSource) => {
        return record.attributes.eventTimestampColumn
      }
    },
    {
      key: 'timestampFormat',
      title: 'Timestamp Format',
      ellipsis: true,
      width: 190,
      render: (record: DataSource) => {
        return record.attributes.timestampFormat
      }
    },
    {
      title: 'Action',
      fixed: 'right',
      width: 240,
      resize: false,
      render: (record: DataSource) => {
        const { guid } = record
        return (
          <Space size="middle">
            <Button
              ghost
              type="primary"
              onClick={() => {
                navigate(getDetialUrl(guid))
              }}
            >
              View Details
            </Button>
            <Popconfirm
              title="Are you sure to delete this data source?"
              placement="topRight"
              onConfirm={() => {
                return new Promise((resolve) => {
                  onDelete(guid, resolve)
                })
              }}
            >
              <Button danger ghost type="primary" icon={<DeleteOutlined />}>
                Delete
              </Button>
            </Popconfirm>
          </Space>
        )
      }
    }
  ]

  const {
    isLoading,
    data: tableData,
    refetch
  } = useQuery<DataSource[]>(
    ['dataSources', project],
    async () => {
      if (project) {
        return await fetchDataSources(project)
      } else {
        return []
      }
    },
    {
      retry: false,
      refetchOnWindowFocus: false
    }
  )

  const onDelete = async (entity: string, resolve: (value?: unknown) => void) => {
    try {
      await deleteEntity(entity)
      message.success('The date source is deleted successfully.')
      refetch()
    } catch (e: any) {
      notification.error({
        message: '',
        description: e.detail,
        placement: 'top'
      })
    } finally {
      resolve()
    }
  }
  return (
    <ResizeTable
      rowKey="guid"
      loading={isLoading}
      columns={columns}
      dataSource={tableData}
      scroll={{ x: '100%' }}
    />
  )
}

const DataSourceTableComponent = forwardRef<unknown, DataSourceTableProps>(DataSourceTable)

DataSourceTableComponent.displayName = 'DataSourceTableComponent'

export default DataSourceTableComponent
