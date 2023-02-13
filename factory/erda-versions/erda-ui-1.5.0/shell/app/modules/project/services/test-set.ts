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

import agent from 'agent';

export function createTestSet(payload: TEST_SET.CreateBody): TEST_SET.TestSet {
  return agent
    .post('/api/testsets')
    .send(payload)
    .then((response: any) => response.body);
}

// 获取测试集列表
export function getTestSetList(payload: TEST_SET.GetQuery): TEST_SET.TestSet[] {
  return agent
    .get('/api/testsets')
    .query(payload)
    .then((response: any) => response.body);
}
export function getTestSetListInTestPlan({
  testPlanID,
  parentID,
}: {
  testPlanID: number;
  parentID: number;
}): TEST_SET.TestSet[] {
  return agent
    .get(`/api/testplans/${testPlanID}/testsets`)
    .query({ parentTestSetID: parentID })
    .then((response: any) => response.body);
}

// 回收测试集
export function deleteTestSet({ testSetID }: TEST_SET.DeleteQuery) {
  return agent.post(`/api/testsets/${testSetID}/actions/recycle`).then((response: any) => response.body);
}

export function recoverTestSet({ testSetID, recoverToTestSetID }: TEST_SET.RecoverQuery) {
  return agent
    .post(`/api/testsets/${testSetID}/actions/recover-from-recycle-bin`)
    .send({ recoverToTestSetID })
    .then((response: any) => response.body);
}
// 彻底删除测试集
export function deleteTestSetEntirely({ testSetID }: TEST_SET.DeleteQuery) {
  return agent
    .delete(`/api/testsets/${testSetID}/actions/clean-from-recycle-bin`)
    .then((response: any) => response.body);
}
// 移动测试集
export function moveTestSet(payload: Omit<TEST_SET.updateBody, 'name'>) {
  return agent
    .put(`/api/testsets/${payload.testSetID}`)
    .send(payload)
    .then((response: any) => response.body);
}

// 拷贝测试集
export function copyTestSet({ testSetID, copyToTestSetID }: TEST_SET.CopyTestSet) {
  return agent
    .post(`/api/testsets/${testSetID}/actions/copy`)
    .send({ copyToTestSetID })
    .then((response: any) => response.body);
}
// 重命名测试集
export function renameTestSet(payload: Omit<TEST_SET.updateBody, 'moveToParentID'>): TEST_SET.TestSet {
  return agent
    .put(`/api/testsets/${payload.testSetID}`)
    .send(payload)
    .then((response: any) => response.body);
}
