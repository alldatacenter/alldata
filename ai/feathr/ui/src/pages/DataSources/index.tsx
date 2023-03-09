import { PageHeader } from 'antd'

import { observer, useStore } from '@/hooks'

import DataSourceTable from './components/DataSourceTable'
import SearchBar from './components/SearchBar'

const DataSources = () => {
  const { globalStore } = useStore()
  const { project } = globalStore

  return (
    <div className="page">
      <PageHeader ghost={false} title="Data Sources">
        <SearchBar project={project} />
        <DataSourceTable project={project} />
      </PageHeader>
    </div>
  )
}

export default observer(DataSources)
