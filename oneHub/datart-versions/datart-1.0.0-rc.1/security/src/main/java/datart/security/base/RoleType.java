/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.security.base;

public enum RoleType {

    /**
     * 组织拥有者,拥有组织最高权限
     */
    ORG_OWNER,

    /**
     * 用户附属角色，每个用户独有的角色
     */
    PER_USER,

    /**
     * 普通角色,由用户自由创建
     */
    NORMAL,

}
