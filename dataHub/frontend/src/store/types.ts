export type Resources = ('Firefox' | 'Safari' | 'Ubuntu' | 'Chrome')[]
export type Os = 'centos' | 'debian' | 'suse' | 'ubuntu' | 'windows'

export interface AddResources {
  id: number // agent id
  data: Resources // 表示要添加的资源
}

export interface DeleteResources {
  id: number // agent id
  data: number // agent id 对应的 resource index
}

export interface Agent {
  name: string
  os: Os
  status: string
  type: string
  ip: string
  location: string
  resources: Resources
  id: number
}

export type Agents = Agent[]
export interface Notice {
  loading: undefined | boolean
  newResourceAgentId: undefined | number // 当前打正在添加 resource 的 agent ID
}

export interface State {
  agents: Agents
  notice: Notice
}

export type Action =
  | { type: 'AGENTS_FETCH' } // API 获取 agents 数据
  | { type: 'AGENTS_INIT'; payload: Agent[] }
  | { type: 'RESOURCES_ADD'; payload: AddResources }
  | { type: 'RESOURCES_DELETE'; payload: DeleteResources }
  | { type: 'NOTICE_RESOURCE_CLOSE' }
  | { type: 'NOTICE_RESOURCE_NEW'; payload: number }
  | { type: 'NOTICE_START' }
  | { type: 'NOTICE_STOP' }
  | { type: 'TEST'; payload: { str: string } }

export type Reducer = (state: State, action: Action) => State

export interface ProviderProps {
  children?: JSX.Element[] | JSX.Element | React.ReactNode
  className?: string
}
