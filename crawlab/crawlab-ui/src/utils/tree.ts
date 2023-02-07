const getNormalizedNodes = <T = any>(node: TreeNode<T>): T[] => {
  let nodes = [] as T[];
  nodes.push(node as T);
  node.children?.forEach((subNode: any) => {
    nodes = nodes.concat(getNormalizedNodes(subNode));
  });
  return nodes;
};

export const normalizeTree = <T = any>(nodes: TreeNode<T>[]): T[] => {
  let results = [] as T[];
  nodes.forEach(node => {
    results = results.concat(getNormalizedNodes(node));
  });
  return results;
};
