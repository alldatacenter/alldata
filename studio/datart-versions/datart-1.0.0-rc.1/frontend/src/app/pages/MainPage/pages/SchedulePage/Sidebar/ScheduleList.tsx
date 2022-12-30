import {
  CaretRightOutlined,
  LoadingOutlined,
  PauseOutlined,
  SendOutlined,
} from '@ant-design/icons';
import { ButtonProps, List, message, Popconfirm, Space, Tooltip } from 'antd';
import { IW, ListItem, ToolbarButton } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import classnames from 'classnames';
import { FC, memo, ReactNode, useCallback, useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { FONT_SIZE_HEADING } from 'styles/StyleConstants';
import { stopPPG } from 'utils/utils';
import { JobTypes, JOB_TYPE_TEXT } from '../constants';
import { useToScheduleDetails } from '../hooks';
import { executeSchedule, startSchedule, stopSchedule } from '../services';
import { useScheduleSlice } from '../slice';
import {
  selectEditingSchedule,
  selectScheduleListLoading,
} from '../slice/selectors';
import { getSchedules } from '../slice/thunks';
import { Schedule } from '../slice/types';

interface OperationButtonProps extends ButtonProps {
  tooltipTitle?: ReactNode;
}
const OperationButton: FC<OperationButtonProps> = ({
  tooltipTitle,
  children,
  ...restProps
}) => {
  return (
    <Tooltip title={tooltipTitle} placement="bottom">
      <IW fontSize={FONT_SIZE_HEADING}>
        <ToolbarButton {...restProps} />
      </IW>
    </Tooltip>
  );
};
interface OperationsProps {
  active?: boolean;
  scheduleId: string;
  editingId?: string;
  onUpdateScheduleList: () => void;
}
const Operations: FC<OperationsProps> = ({
  active,
  scheduleId,
  onUpdateScheduleList,
  editingId,
}) => {
  const [startLoading, setStartLoading] = useState(false);
  const [stopLoading, setStopLoading] = useState(false);
  const [executeLoading, setExecuteLoading] = useState(false);
  const { actions } = useScheduleSlice();
  const dispatch = useDispatch();
  const t = useI18NPrefix('main.pages.schedulePage.sidebar.scheduleList');

  const updateScheduleActive = useCallback(
    (active: boolean) => {
      dispatch(actions.setEditingScheduleActive(active));
    },
    [actions, dispatch],
  );
  const handleStartSchedule = useCallback(() => {
    setStartLoading(true);
    startSchedule(scheduleId)
      .then(res => {
        if (!!res) {
          message.success(t('successStarted'));
          onUpdateScheduleList();
          if (editingId === scheduleId) {
            updateScheduleActive(true);
          }
        }
      })
      .finally(() => {
        setStartLoading(false);
      });
  }, [scheduleId, editingId, onUpdateScheduleList, updateScheduleActive, t]);
  const handleStopSchedule = useCallback(() => {
    setStopLoading(true);
    stopSchedule(scheduleId)
      .then(res => {
        if (!!res) {
          message.success(t('successStop'));
          onUpdateScheduleList();
          if (editingId === scheduleId) {
            updateScheduleActive(false);
          }
        }
      })
      .finally(() => {
        setStopLoading(false);
      });
  }, [scheduleId, onUpdateScheduleList, editingId, updateScheduleActive, t]);
  const handleExecuteSchedule = useCallback(() => {
    setExecuteLoading(true);
    executeSchedule(scheduleId)
      .then(res => {
        if (!!res) {
          message.success(t('successImmediately'));
          onUpdateScheduleList();
        }
      })
      .finally(() => {
        setExecuteLoading(false);
      });
  }, [scheduleId, onUpdateScheduleList, t]);

  return (
    <Space onClick={stopPPG}>
      {!active ? (
        <OperationButton
          tooltipTitle={t('start')}
          loading={startLoading}
          icon={<CaretRightOutlined />}
          onClick={handleStartSchedule}
        />
      ) : null}
      {active ? (
        <OperationButton
          tooltipTitle={t('stop')}
          loading={stopLoading}
          icon={<PauseOutlined />}
          onClick={handleStopSchedule}
        />
      ) : null}

      <Popconfirm
        title={t('executeItNow')}
        okButtonProps={{ loading: executeLoading }}
        onConfirm={handleExecuteSchedule}
      >
        <OperationButton
          loading={executeLoading}
          tooltipTitle={t('executeImmediately')}
          icon={
            <SendOutlined
              rotate={-45}
              css={`
                position: relative;
                top: -2px;
              `}
            />
          }
        />
      </Popconfirm>
    </Space>
  );
};

export const ScheduleList: FC<{
  orgId: string;
  scheduleId?: string;
  list?: Schedule[];
}> = memo(({ orgId, scheduleId, list }) => {
  const dispatch = useDispatch();
  const editingSchedule = useSelector(selectEditingSchedule);
  const loading = useSelector(selectScheduleListLoading);
  const { toDetails } = useToScheduleDetails();

  const onUpdateScheduleList = useCallback(() => {
    dispatch(getSchedules(orgId as string));
  }, [dispatch, orgId]);

  useEffect(() => {
    onUpdateScheduleList();
  }, [onUpdateScheduleList]);

  const onClockListItem = useCallback(
    id => () => {
      toDetails(orgId, id);
    },
    [toDetails, orgId],
  );

  return (
    <List
      dataSource={list}
      loading={loading && { indicator: <LoadingOutlined /> }}
      renderItem={({ id, name, type, active }) => (
        <ListItem
          key={name}
          actions={[
            <Operations
              active={active}
              scheduleId={id}
              onUpdateScheduleList={onUpdateScheduleList}
              editingId={editingSchedule?.id}
            />,
          ]}
          className={classnames({
            schedule: true,
            selected: id === scheduleId,
          })}
          onClick={onClockListItem(id)}
        >
          <List.Item.Meta
            title={name}
            description={JOB_TYPE_TEXT[type as JobTypes]}
          />
        </ListItem>
      )}
    />
  );
});
