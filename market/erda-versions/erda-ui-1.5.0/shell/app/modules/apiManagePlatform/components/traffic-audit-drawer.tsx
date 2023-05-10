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
import { Drawer } from 'antd';
import CommonChart from 'apiManagePlatform/components/chart';
import i18n from 'i18n';

interface IProps {
  queries: {
    endpoint: string;
    client: string;
    workspace: string;
    projectID: number;
  };
  visible: boolean;
  onClose: () => void;
}

const TrafficAuditDrawer = ({ visible, onClose, queries }: IProps) => {
  const [query, setQuery] = React.useState({});
  React.useEffect(() => {
    if (visible) {
      setQuery(queries);
    }
  }, [queries, visible]);
  const handleClose = () => {
    onClose();
  };

  return (
    <Drawer title={i18n.t('traffic audit')} visible={visible} width={1000} onClose={handleClose} destroyOnClose>
      <CommonChart type="apim_client" extraQuery={query} />
    </Drawer>
  );
};

export default TrafficAuditDrawer;
