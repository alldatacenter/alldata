/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

describe('AddRepositoryModal Service', function () {
  var AddRepositoryModal, $modal;
  
  beforeEach(module('ambariAdminConsole', function($provide){
  }));
  
  beforeEach(inject(function (_AddRepositoryModal_, _$modal_) {
    AddRepositoryModal = _AddRepositoryModal_;
    $modal = _$modal_;

    spyOn($modal, 'open').and.returnValue({
      result: {
        then: function() {
        }
      }
    });
  }));

  it('should open modal window', function () {
    AddRepositoryModal.show();
    expect($modal.open).toHaveBeenCalled();
  });

  it('should check if repo exists', function () {
    var existingRepos = [
      {
        Repositories: {
          repo_id: 'repo1'
        }
      }
    ];
    expect(AddRepositoryModal.repoExists(existingRepos, 'repo1')).toBe(true);
    expect(AddRepositoryModal.repoExists(existingRepos, 'repo2')).toBe(false);
  });

  it('should get repositories for selected OS', function () {
    var os1Repos = [
      {
        Repositories: {
          os_type: 'os1',
          repo_id: 'repo1'
        }
      }, {
        Repositories: {
          os_type: 'os1',
          repo_id: 'repo2'
        }
      }
    ];
    var os2Repos = [
      {
        Repositories: {
          os_type: 'os2',
          repo_id: 'repo1'
        }
      },{
        Repositories: {
          os_type: 'os2',
          repo_id: 'repo2'
        }
      }
    ];

    var osList = [
      {
        OperatingSystems: {
          os_type: 'os1'
        },
        repositories: os1Repos
      }, {
        OperatingSystems: {
          os_type: 'os2'
        },
        repositories: os2Repos
      }
    ];
    expect(AddRepositoryModal.getRepositoriesForOS(osList, 'os1')).toEqual(os1Repos);
    expect(AddRepositoryModal.getRepositoriesForOS(osList, 'os2')).toEqual(os2Repos);
    expect(AddRepositoryModal.getRepositoriesForOS(osList, 'os3')).toBe(null);
  });
});
