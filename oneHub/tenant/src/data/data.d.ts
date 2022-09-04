declare namespace Data {
  export type Role = 'guest' | 'operator' | 'lawyer' | 'admin'

  export interface CurrentUser {
    id: string
    username: string
    name: string
    email: string
    role: Role
  }

  export interface Access {
    dashboardCanView: boolean
    businessCanManage: boolean
  }
}
