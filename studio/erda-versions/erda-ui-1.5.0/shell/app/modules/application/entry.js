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

import applicationStore from './stores/application';
import buildStore from './stores/build';
import certificateStore from './stores/certificate-reference';
import dataTaskStore from './stores/dataTask';
import deployStore from './stores/deploy';
import libraryStore from './stores/library-reference';
import notifyStore from './stores/notify';
import notifyGroupStore from './stores/notify-group';
import pipelineStore from './stores/pipeline-config';
import qualityStore from './stores/quality';
import releaseStore from './stores/release';
import repoStore from './stores/repo';
import testStore from './stores/test';
import problemStore from './stores/problem';

export default (registerModule) => {
  return registerModule({
    key: 'application',
    stores: [
      applicationStore,
      buildStore,
      certificateStore,
      dataTaskStore,
      deployStore,
      libraryStore,
      notifyStore,
      notifyGroupStore,
      pipelineStore,
      qualityStore,
      releaseStore,
      repoStore,
      testStore,
      problemStore,
    ],
  });
};
