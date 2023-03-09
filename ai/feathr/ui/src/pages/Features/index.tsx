import { useState } from 'react'

import { PageHeader } from 'antd'
import { useSearchParams } from 'react-router-dom'

import { observer, useStore } from '@/hooks'

import FeatureTable from './components/FeatureTable'
import SearchBar, { SearchValue } from './components/SearchBar'

const Feature = () => {
  const [searchParams] = useSearchParams()
  const { globalStore } = useStore()
  const { project } = globalStore

  const [search, setProject] = useState<SearchValue>({
    keyword: searchParams.get('keyword') || undefined
  })

  const onSearch = (values: SearchValue) => {
    setProject(values)
  }

  return (
    <div className="page">
      <PageHeader ghost={false} title="Features">
        <SearchBar project={project} keyword={search.keyword} onSearch={onSearch} />
        <FeatureTable project={project} keyword={search.keyword} />
      </PageHeader>
    </div>
  )
}

export default observer(Feature)
