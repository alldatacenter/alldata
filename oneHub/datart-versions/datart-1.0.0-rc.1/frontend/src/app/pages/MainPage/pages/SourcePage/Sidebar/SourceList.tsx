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
import { errorHandle } from 'utils/utils';
import { List, ListItem } from '.';
import { getSources } from '../slice/thunks';
import { Source } from '../slice/types';

interface SourceListProps {
  sourceId?: string;
  list?: Source[];
}

export const SourceList = memo(({ sourceId, list }: SourceListProps) => {
  const dispatch = useDispatch();
  const history = useHistory();
  const orgId = useSelector(selectOrgId);

  useEffect(() => {
    dispatch(getSources(orgId));
  }, [dispatch, orgId]);

  const toDetail = useCallback(
    id => () => {
      history.push(`/organizations/${orgId}/sources/${id}`);
    },
    [history, orgId],
  );

  const getLabel = useCallback((type: string, config: string) => {
    if (type === 'JDBC') {
      let desc = '';
      try {
        const { dbType } = JSON.parse(config);
        desc = dbType;
      } catch (error) {
        errorHandle(error);
        throw error;
      }
      return desc;
    } else {
      return type;
    }
  }, []);

  return (
    <List>
      {list?.map(({ id, name, type, config }) => {
        const itemClass = classnames({ selected: id === sourceId });
        return (
          <ListItem key={name} className={itemClass} onClick={toDetail(id)}>
            <header>
              <h4>{name}</h4>
              <span>{getLabel(type, config)}</span>
            </header>
          </ListItem>
        );
      })}
    </List>
  );
});
