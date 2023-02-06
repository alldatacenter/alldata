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
import FileContainer from 'application/common/components/file-container';
import { IF, FileEditor } from 'common';
import releaseStore from 'app/modules/application/stores/release';

interface IProps {
  releaseId: string;
}

const ReleaseDetailYml = ({ releaseId }: IProps) => {
  const [yml, setYml] = React.useState('');

  React.useEffect(() => {
    const { getDiceYml } = releaseStore.effects;
    getDiceYml(releaseId).then((diceYml) => {
      setYml(diceYml);
    });
  }, [releaseId]);

  return (
    <div className="release-detail-page">
      <IF check={yml}>
        <FileContainer className="mt-3" name="dice.yml">
          <FileEditor name="dice.yml" fileExtension="yml" value={yml} readOnly />
        </FileContainer>
      </IF>
    </div>
  );
};

export default ReleaseDetailYml;
