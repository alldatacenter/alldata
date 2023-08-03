import React from 'react'

import { PageHeader } from 'antd'

import { observer, useStore } from '@/hooks'

import FeatureForm from './components/FeatureForm'

const NewFeature = () => {
  const { globalStore } = useStore()
  const { project } = globalStore

  return (
    <div className="page">
      <PageHeader title="Create Feature" ghost={false}>
        <FeatureForm project={project} />
      </PageHeader>
    </div>
  )
}

export default observer(NewFeature)
