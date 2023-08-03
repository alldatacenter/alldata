import React, { useEffect } from 'react'

import { Result, Button } from 'antd'
import { useLocation, useNavigate } from 'react-router-dom'

import { useStore } from '@/hooks'

const Page404 = () => {
  const { globalStore } = useStore()
  const navigate = useNavigate()

  const location = useLocation()

  useEffect(() => {
    if (location.state) {
      globalStore.changeProject('')
    }
  }, [location.state])

  return (
    <Result
      extra={
        <Button
          type="primary"
          onClick={() => {
            navigate('/')
          }}
        >
          Back Home
        </Button>
      }
      status="404"
      subTitle={`Sorry, ${location.state || 'the page you visited does not exist.'} `}
      title="404"
    />
  )
}

export default Page404
