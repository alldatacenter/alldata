import { accesses, getAccessesByRole } from '@/data/access'

// 权限控制
export default function access(initialState: { currentUser: Data.CurrentUser | null }) {
  const { currentUser } = initialState

  if (!currentUser) {
    return accesses
  }

  return getAccessesByRole(currentUser.role)
}
