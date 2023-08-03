import React, { forwardRef } from 'react'

import { DeleteOutlined } from '@ant-design/icons'
import { Button, Space, notification, Popconfirm, message } from 'antd'
import { useQuery } from 'react-query'
import { useNavigate } from 'react-router-dom'

import { fetchProjects, deleteEntity } from '@/api'
import ResizeTable, { ResizeColumnType } from '@/components/ResizeTable'
import { useStore } from '@/hooks'
import { Project } from '@/models/model'

export interface ProjectTableProps {
  project?: string
}
const ProjectTable = (props: ProjectTableProps, ref: any) => {
  const navigate = useNavigate()
  const { globalStore } = useStore()
  const { setProjectList } = globalStore
  const { project } = props

  const columns: ResizeColumnType<Project>[] = [
    {
      key: 'name',
      title: 'Name',
      dataIndex: 'name',
      resize: false
    },
    {
      key: 'action',
      title: 'Action',
      width: 240,
      resize: false,
      render: (record: Project) => {
        const { name } = record
        return (
          <Space size="middle">
            <Button
              ghost
              type="primary"
              onClick={() => {
                navigate(`/${name}/features`)
              }}
            >
              View Features
            </Button>
            <Button
              ghost
              type="primary"
              onClick={() => {
                navigate(`/${name}/lineage`)
              }}
            >
              View Lineage
            </Button>
            <Popconfirm
              title="Are you sure to delete this project?"
              placement="topRight"
              onConfirm={() => {
                return new Promise((resolve) => {
                  onDelete(name, resolve)
                })
              }}
            >
              <Button danger ghost type="primary" icon={<DeleteOutlined />}>
                Detete
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
  } = useQuery<Project[]>(
    ['Projects', project],
    async () => {
      const reuslt = await fetchProjects()
      setProjectList(reuslt)
      return reuslt.reduce((list, item: string) => {
        const text = project?.trim().toLocaleLowerCase()
        if (!text || item.includes(text)) {
          list.push({ name: item })
        }
        return list
      }, [] as Project[])
    },
    {
      retry: false,
      refetchOnWindowFocus: false
    }
  )

  const onDelete = async (entity: string, resolve: (value?: unknown) => void) => {
    try {
      await deleteEntity(entity)
      message.success('The project is deleted successfully.')

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
      rowKey="name"
      loading={isLoading}
      columns={columns}
      dataSource={tableData}
      scroll={{ x: '100%' }}
    />
  )
}

const ProjectTableComponent = forwardRef<unknown, ProjectTableProps>(ProjectTable)

ProjectTableComponent.displayName = 'ProjectTableComponent'

export default ProjectTableComponent
