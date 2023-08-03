import React, { useEffect, useRef, useState } from 'react'

import { Modal, Typography, List, Input, Space } from 'antd'
import VirtualList from 'rc-virtual-list'
import { useNavigate } from 'react-router-dom'

import { observer, useStore } from '@/hooks'
import { Project } from '@/models/model'

const { Search } = Input

const { Link } = Typography

const ContainerHeight = 400

const SwitchProjectModal = () => {
  const navigate = useNavigate()

  const { globalStore } = useStore()
  const { projectList, routeName, openSwitchProjectModal, setSwitchProjecModalOpen } = globalStore

  const searchRef = useRef('')

  const [data, setData] = useState<Project[]>([])

  const onSearch = (value: string) => {
    searchRef.current = value
    const list = projectList.filter((item) => item.name.includes(value))
    setData(list)
  }

  const onSelected = (name: string) => {
    navigate(`/${name}/${routeName}`)
    setSwitchProjecModalOpen(false)
  }

  const onCancel = () => {
    setSwitchProjecModalOpen(false)
  }

  useEffect(() => {
    if (openSwitchProjectModal) {
      onSearch(searchRef.current)
    }
  }, [openSwitchProjectModal, projectList])

  return (
    <Modal title="Switch Project" open={openSwitchProjectModal} footer={null} onCancel={onCancel}>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Search placeholder="Search" onSearch={onSearch} />
        <List bordered size="small">
          <VirtualList data={data} height={ContainerHeight} itemHeight={41} itemKey="name">
            {(item: Project) => (
              <List.Item key={item.name}>
                <List.Item.Meta
                  description={
                    <Link
                      onClick={() => {
                        onSelected(item.name)
                      }}
                    >
                      {item.name}
                    </Link>
                  }
                />
              </List.Item>
            )}
          </VirtualList>
        </List>
      </Space>
    </Modal>
  )
}

export default observer(SwitchProjectModal)
