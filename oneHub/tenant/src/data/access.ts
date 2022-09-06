export const accesses: Data.Access = {
  // 工作台
  dashboardCanView: false,

  // 数据业务
  businessCanManage: false,

  // 数据计算
  computeCanManage: false,

  // 数据治理
  governCanManage: false,

  // 数据运维
  devopsCanManage: false,

  // 数据集成
  integrateCanManage: false,

  // 实时数仓
  olapCanManage: false,

  // 数据智能
  intelligenceCanManage: false,

  // 性能优化
  optimizeCanManage: false,

  // 数据存储
  storageCanManage: false,

  // 数据采集
  odsCanManage: false,
}

const roleAccessMap = {
  lawyer: {
    dashboardCanView: true,
    businessCanManage: true,
  },
  operator: {
    dashboardCanView: true,
  },
  guest: {
    dashboardCanView: true,
  },
}

export function getAccessesByRole(role: Data.Role) {
  if (role === 'admin') {
    return Object.keys(accesses).reduce((acc, cur) => {
      acc[cur] = true
      return acc
    }, {} as any) as Data.Access
  }

  const roleAccesses = roleAccessMap[role]
  if (roleAccesses) {
    return { ...accesses, ...roleAccesses } as Data.Access
  }

  return accesses
}
