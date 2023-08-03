import { useContext } from 'react'

import { observer } from 'mobx-react'

import { storesContext } from '../store'

export { observer }

export const useStore = () => {
  return useContext(storesContext)
}
