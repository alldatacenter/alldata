import { List } from 'antd';
import { ListItem } from 'app/components';
import { selectOrgId } from 'app/pages/MainPage/slice/selectors';
import { memo, useCallback, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory } from 'react-router';
import { JobTypes, JOB_TYPE_TEXT } from '../constants';
import { selectArchivedListLoading } from '../slice/selectors';
import { getArchivedSchedules } from '../slice/thunks';
import { Schedule } from '../slice/types';

interface RecycleProps {
  scheduleId?: string;
  list?: Schedule[];
}

export const Recycle = memo(({ scheduleId, list }: RecycleProps) => {
  const dispatch = useDispatch();
  const history = useHistory();
  const loading = useSelector(selectArchivedListLoading);
  const orgId = useSelector(selectOrgId);

  useEffect(() => {
    dispatch(getArchivedSchedules(orgId));
  }, [dispatch, orgId]);

  const toDetail = useCallback(
    id => () => {
      history.push(`/organizations/${orgId}/schedules/${id}`);
    },
    [history, orgId],
  );

  return (
    <List
      dataSource={list}
      loading={loading}
      renderItem={({ id, name, type }) => (
        <ListItem
          key={name}
          selected={scheduleId === id}
          className="recycle"
          onClick={toDetail(id)}
        >
          <List.Item.Meta
            title={name}
            description={JOB_TYPE_TEXT[type as JobTypes]}
          />
        </ListItem>
      )}
    ></List>
  );
});
