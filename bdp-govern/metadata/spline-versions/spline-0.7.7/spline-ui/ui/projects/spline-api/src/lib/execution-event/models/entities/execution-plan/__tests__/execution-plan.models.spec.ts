/*
 * Copyright 2020 ABSA Group Limited
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

import { operationIdToExecutionPlanId } from '../execution-plan.models'


describe('ExecutionPlanModels', () => {

    describe('operationIdToExecutionPlanId', () => {

        test('convert valid values', () => {
            const dummyValues = [
                {
                    value: 'someExecutionPlanId:0',
                    expectedResult: 'someExecutionPlanId',
                },
                {
                    value: 'someExecutionPlanId:0:1',
                    expectedResult: 'someExecutionPlanId:0',
                }
            ]

            dummyValues.forEach(testCase => {
                const executionPlanId = operationIdToExecutionPlanId(testCase.value)
                expect(executionPlanId).toEqual(testCase.expectedResult)
            })

        })

        test('throw an error on invalid values', () => {
            const dummyValues = [
                'someExecutionPlanId'
            ]

            dummyValues.forEach(testCase => {
                expect(() => operationIdToExecutionPlanId(testCase)).toThrowError(testCase)
            })

        })
    })

})
