import {useState, useEffect} from 'react'
import Monaco from 'react-monaco-editor'

const Page = () => {
  return (
    <div>
      sql
      <Monaco 
      height={700}
      width={300}
      />
    </div>
  )
}
export default Page