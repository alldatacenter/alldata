import React from 'react'

import { PageHeader } from 'antd'

import { observer, useStore } from '@/hooks'

import ScourceForm from './components/SourceForm'

const NewFeature = () => {
  const { globalStore } = useStore()
  const { project } = globalStore

  return (
    <div className="page">
      <PageHeader title="Create Data Source" ghost={false}>
        <ScourceForm project={project} />
      </PageHeader>
    </div>
  )
}

export default observer(NewFeature)
