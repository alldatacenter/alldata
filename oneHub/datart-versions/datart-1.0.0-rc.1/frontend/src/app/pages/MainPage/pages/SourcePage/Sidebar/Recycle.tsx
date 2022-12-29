/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { selectOrgId } from 'app/pages/MainPage/slice/selectors';
import classnames from 'classnames';
import { memo, useCallback, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory } from 'react-router';
import { List, ListItem } from '.';
import { getArchivedSources } from '../slice/thunks';
import { Source } from '../slice/types';

interface RecycleProps {
  sourceId?: string;
  list?: Source[];
}

export const Recycle = memo(({ sourceId, list }: RecycleProps) => {
  const dispatch = useDispatch();
  const history = useHistory();
  const orgId = useSelector(selectOrgId);

  useEffect(() => {
    dispatch(getArchivedSources(orgId));
  }, [dispatch, orgId]);

  const toDetail = useCallback(
    id => () => {
      history.push(`/organizations/${orgId}/sources/${id}`);
    },
    [history, orgId],
  );

  return (
    <List>
      {list?.map(({ id, name, type, config }) => {
        return (
          <ListItem
            key={name}
            className={classnames({ recycle: true, selected: id === sourceId })}
            onClick={toDetail(id)}
          >
            <header>
              <h4>{name}</h4>
              <span>{type}</span>
            </header>
          </ListItem>
        );
      })}
    </List>
  );
});
