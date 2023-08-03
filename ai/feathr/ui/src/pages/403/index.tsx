import React from 'react'

import { Result, Button } from 'antd'
import { useNavigate, useLocation } from 'react-router-dom'

const Page403 = () => {
  const navigate = useNavigate()
  const location = useLocation()

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
      status="403"
      subTitle={`Sorry,  ${location.state || 'you are not authorized to access this page.'}`}
      title="403"
    />
  )
}

export default Page403
