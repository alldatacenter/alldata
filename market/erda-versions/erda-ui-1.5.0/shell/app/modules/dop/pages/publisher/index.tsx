// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import React from 'react';
import { createPublisherList, publisherTabs } from 'app/modules/publisher/pages/publisher-manage/publisher-list-v2';
import publisherStore from 'app/modules/publisher/stores/publisher';
import { useLoading } from 'core/stores/loading';
import { goTo } from 'common/utils';
import { Redirect } from 'react-router-dom';

export const RedirectTo = () => {
  return <Redirect to="./publisher/MOBILE" />;
};

const Mapper = () => {
  const [joinedArtifactsList, joinedArtifactsPaging] = publisherStore.useStore((s) => [
    s.joinedArtifactsList,
    s.joinedArtifactsPaging,
  ]);
  const [loading] = useLoading(publisherStore, ['getJoinedArtifactsList']);
  const { getJoinedArtifactsList } = publisherStore.effects;
  const { clearJoinedArtifactsList } = publisherStore.reducers;
  return {
    list: joinedArtifactsList,
    paging: joinedArtifactsPaging,
    isFetching: loading,
    getList: getJoinedArtifactsList,
    clearList: clearJoinedArtifactsList,
    onItemClick: (publisher: PUBLISHER.IPublisher) => {
      goTo(`./${publisher.id}`);
    },
  };
};
const PublisherList = createPublisherList(Mapper);

export { publisherTabs };

export default PublisherList;
