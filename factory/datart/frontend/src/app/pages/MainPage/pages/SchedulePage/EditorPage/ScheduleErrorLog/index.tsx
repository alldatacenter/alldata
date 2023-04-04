import { Card, Table, TableColumnsType } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { FC, useEffect, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components';
import { BORDER_RADIUS } from 'styles/StyleConstants';
import { LogStatus, LOG_STATUS_TEXT } from '../../constants';
import { useScheduleSlice } from '../../slice';
import {
  selectScheduleLogs,
  selectScheduleLogsLoading,
} from '../../slice/selectors';
import { getScheduleErrorLogs } from '../../slice/thunks';
import { ErrorLog } from '../../slice/types';

interface ScheduleErrorLogProps {
  scheduleId: string;
}
export const ScheduleErrorLog: FC<ScheduleErrorLogProps> = ({ scheduleId }) => {
  const dispatch = useDispatch();
  const logs = useSelector(selectScheduleLogs),
    loading = useSelector(selectScheduleLogsLoading);
  const { actions } = useScheduleSlice();
  const t = useI18NPrefix('schedule.editor.scheduleErrorLog.index');
  useEffect(() => {
    if (scheduleId) {
      dispatch(getScheduleErrorLogs({ scheduleId, count: 100 }));
    }
    return () => {
      dispatch(actions.clearLogs);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [scheduleId, dispatch]);
  const columns: TableColumnsType<ErrorLog> = useMemo(() => {
    return [
      { title: t('startTime'), dataIndex: 'start', key: 'start' },
      { title: t('endTime'), dataIndex: 'end', key: 'end' },
      {
        title: t('logPhase'),
        dataIndex: 'status',
        key: 'status',
        render(status: LogStatus) {
          return LOG_STATUS_TEXT[status];
        },
      },
      {
        title: t('executionInformation'),
        dataIndex: 'message',
        key: 'message',
        render(text, record) {
          const isSuccess = record?.status === LogStatus.S15;
          return isSuccess ? t('success') : text;
        },
      },
    ];
  }, [t]);

  if (logs?.length > 0) {
    return (
      <FormCard title={t('log')}>
        <FormWrapper>
          <Table
            rowKey="id"
            loading={loading}
            columns={columns}
            dataSource={logs || []}
            size="small"
            scroll={{ y: 400 }}
            pagination={false}
          />
        </FormWrapper>
      </FormCard>
    );
  } else {
    return <></>;
  }
};

const FormCard = styled(Card)`
  &.ant-card {
    background-color: ${p => p.theme.componentBackground};
    border-radius: ${BORDER_RADIUS};
    box-shadow: ${p => p.theme.shadowBlock};
  }
`;
const FormWrapper = styled.div`
  width: 860px;
`;
