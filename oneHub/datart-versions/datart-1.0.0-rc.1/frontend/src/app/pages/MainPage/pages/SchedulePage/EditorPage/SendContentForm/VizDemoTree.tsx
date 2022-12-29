import { FilePptOutlined } from '@ant-design/icons';
import { Tree } from 'app/components';
import {
  selectStoryboardListLoading,
  selectStoryboards,
} from 'app/pages/MainPage/pages/VizPage/slice/selectors';
import { Storyboard } from 'app/pages/MainPage/pages/VizPage/slice/types';
import { FC, useCallback, useMemo } from 'react';
import { useSelector } from 'react-redux';
import { VizTypes } from '../../constants';
import { VizContentsItem } from '../../slice/types';

interface VizDemoTreeProps {
  value?: VizContentsItem[];
  onChange?: (v: VizContentsItem[]) => void;
}
export const VizDemoTree: FC<VizDemoTreeProps> = ({ value, onChange }) => {
  const demos = useSelector(selectStoryboards);
  const loading = useSelector(selectStoryboardListLoading);
  const treeData = useMemo(() => {
    return demos?.length > 0
      ? demos.map(v => Object.assign({}, v, { title: v?.name, key: v?.id }))
      : [];
  }, [demos]);

  const onCheck = useCallback(
    (_, { checkedNodes }) => {
      const items: VizContentsItem[] = (checkedNodes as Storyboard[]).map(
        v => ({
          vizId: v?.id,
          vizType: VizTypes.StoryBoard,
        }),
      );
      onChange?.(items);
    },
    [onChange],
  );
  const checkedKeys = useMemo(() => {
    return value ? value.map(v => v?.vizId) : [];
  }, [value]);
  return (
    <Tree
      loading={loading}
      treeData={treeData}
      checkable
      checkedKeys={checkedKeys}
      onCheck={onCheck}
      defaultExpandAll
      icon={<FilePptOutlined />}
    />
  );
};
