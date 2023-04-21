import { useState, useCallback } from 'react'

export default function colonyModel() {
  const [colonyData, setColonyData] = useState<API.ColonyData>()
  const [actionCount, setActionCount] = useState(0)
  const setClusterId = useCallback((value: number)=>{
    setColonyData({...colonyData, clusterId: value})
  },[])

  const setActionCountModel = useCallback((value: number)=>{
    setActionCount(value)
  },[])

  return {
    colonyData,
    setClusterId,
    setColonyData,
    setActionCountModel,
    actionCount,
  }
}