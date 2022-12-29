import {
  DownloadListPopup,
  OnLoadTasksType,
} from 'app/pages/MainPage/Navbar/DownloadListPopup';
import { FC, useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import { SPACE_MD } from 'styles/StyleConstants';
import { DownloadTask } from '../../MainPage/slice/types';
import { useShareSlice } from '../slice';
import { selectShareDownloadPolling } from '../slice/selectors';
const SHARE_HEADER_HEIGHT = 50;

interface DownloadTaskContainerProps {
  onLoadTasks: OnLoadTasksType;
  onDownloadFile: (item: DownloadTask) => void;
}
export const DownloadTaskContainer: FC<DownloadTaskContainerProps> = ({
  children,
  ...restProps
}) => {
  const sharePolling = useSelector(selectShareDownloadPolling);
  const { shareActions } = useShareSlice();
  const dispatch = useDispatch();
  const onSetSharePolling = useCallback(
    (polling: boolean) => {
      dispatch(shareActions.setShareDownloadPolling(polling));
    },
    [dispatch, shareActions],
  );
  return (
    <>
      <HeaderArea>
        <DownloadListPopup
          polling={sharePolling}
          setPolling={onSetSharePolling}
          {...restProps}
        />
      </HeaderArea>
      <Content>{children}</Content>
    </>
  );
};
const HeaderArea = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-end;
  height: ${SHARE_HEADER_HEIGHT}px;
  padding: ${SPACE_MD};
`;
const Content = styled.div`
  height: calc(100% - ${SHARE_HEADER_HEIGHT}px);
`;
