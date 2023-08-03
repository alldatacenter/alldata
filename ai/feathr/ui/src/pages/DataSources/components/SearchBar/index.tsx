import React from 'react'

import { Button } from 'antd'
import { useNavigate } from 'react-router-dom'

export interface SearchBarProps {
  project: string
}

const SearchBar = (props: SearchBarProps) => {
  const navigate = useNavigate()

  const { project } = props

  const onNavigateNewSource = () => {
    navigate(`/${project}/datasources/new`)
  }

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'space-between',
        marginBottom: 16
      }}
    >
      <span></span>
      <Button type="primary" onClick={onNavigateNewSource}>
        + Create Source
      </Button>
    </div>
  )
}

export default SearchBar
