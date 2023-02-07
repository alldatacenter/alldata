export const getFieldsFromData = (data: TableData<Result>) => {
  if (data.length === 0) {
    return [];
  }
  const keys = new Set<string>();
  data.forEach(item => {
    if (typeof item !== 'object') return;
    Object.keys(item).forEach(key => {
      keys.add(key);
    });
  });
  return Array.from(keys).map(key => {
    return {key};
  });
};
