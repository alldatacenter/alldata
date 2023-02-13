/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import React from 'react';
import styles from './index.module.less';

export interface ContainerProps {
  className?: string;
  style?: object;
}

// Blocking, there is only one block for a page by default
//   If you want to divide into multiple blocks, you need to manually import the component, which can be used with the antd Card component, example:
// <>
//  <Container><Card/></Container>
//  <Container><Card/></Container>
// </>
const Container: React.FC<ContainerProps> = ({ className = '', children, style }) => (
  <div className={[styles.layoutContainer, className].join(' ')} style={style}>
    {children}
  </div>
);

export default Container;
