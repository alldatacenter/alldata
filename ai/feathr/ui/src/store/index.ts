import { createContext } from 'react'

import globalStore from './globalStore'

export { globalStore }

export const storesContext = createContext({
  globalStore
})
