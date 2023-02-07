const table: LComponentsTable = {
  columns: {
    actions: '操作',
  },
  actions: {
    editSelected: '编辑已选项',
    deleteSelected: '删除已选项',
    export: '导出',
    customizeColumns: '自定义列',
  },
  columnsTransfer: {
    title: '表格列自定义',
    titles: {
      left: '可用',
      right: '已选'
    }
  },
  header: {
    sort: {
      tooltip: {
        sort: '排序',
        sortAscending: '升序排序',
        sortDescending: '降序排序',
      },
    },
    filter: {
      tooltip: {
        filter: '筛选',
        search: '搜索',
        include: '包含',
      },
    },
    dialog: {
      sort: {
        title: '排序',
        clearSort: '清除排序',
        ascending: '升序',
        descending: '降序',
      },
      filter: {
        title: '筛选',
        search: '搜索',
      },
    },
  },
};

export default table;
