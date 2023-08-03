/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.store;

import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerRole;
import org.apache.ranger.plugin.util.SearchFilter;

import java.util.List;

public class RolePredicateUtil extends AbstractPredicateUtil {

    public RolePredicateUtil() {
        super();
    }

    @Override
    public void addPredicates(SearchFilter filter, List<Predicate> predicates) {
        addPredicateForRoleName(filter.getParam(SearchFilter.ROLE_NAME), predicates);
        addPredicateForRoleId(filter.getParam(SearchFilter.ROLE_ID), predicates);
        addPredicateForGroupName(filter.getParam(SearchFilter.GROUP_NAME), predicates);
        addPredicateForUserName(filter.getParam(SearchFilter.USER_NAME), predicates);

        addPredicateForPartialRoleName(filter.getParam(SearchFilter.ROLE_NAME_PARTIAL), predicates);
        addPredicateForPartialGroupName(filter.getParam(SearchFilter.GROUP_NAME_PARTIAL), predicates);
        addPredicateForPartialUserName(filter.getParam(SearchFilter.USER_NAME_PARTIAL), predicates);
    }

    private Predicate addPredicateForRoleName(final String roleName, List<Predicate> predicates) {
        if(StringUtils.isEmpty(roleName)) {
            return null;
        }

        Predicate ret = new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                if(object == null) {
                    return false;
                }

                boolean ret = false;

                if(object instanceof RangerRole) {
                    RangerRole role = (RangerRole) object;

                    ret = StringUtils.equals(role.getName(), roleName);

                    if (!ret) {
                        List<RangerRole.RoleMember> roles = role.getRoles();

                        for (RangerRole.RoleMember member : roles) {
                            ret = StringUtils.equals(role.getName(), roleName);

                            if (ret) {
                                break;
                            }
                        }
                    }
                }
                return ret;
            }
        };

        if(predicates != null) {
            predicates.add(ret);
        }

        return ret;
    }

    private Predicate addPredicateForPartialRoleName(final String roleNamePartial, List<Predicate> predicates) {
        if(StringUtils.isEmpty(roleNamePartial)) {
            return null;
        }

        Predicate ret = new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                if(object == null) {
                    return false;
                }

                boolean ret = false;

                if(object instanceof RangerRole) {
                    RangerRole role = (RangerRole) object;

                    ret = StringUtils.containsIgnoreCase(role.getName(), roleNamePartial);

                    if (!ret) {
                        List<RangerRole.RoleMember> roles = role.getRoles();

                        for (RangerRole.RoleMember member : roles) {
                            ret = StringUtils.containsIgnoreCase(role.getName(), roleNamePartial);

                            if (ret) {
                                break;
                            }
                        }
                    }
                }
                return ret;
            }
        };

        if(predicates != null) {
            predicates.add(ret);
        }

        return ret;
    }

    private Predicate addPredicateForRoleId(final String roleId, List<Predicate> predicates) {
        if(StringUtils.isEmpty(roleId)) {
            return null;
        }

        Predicate ret = new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                if(object == null) {
                    return false;
                }

                boolean ret = false;

                if(object instanceof RangerRole) {
                    RangerRole role = (RangerRole) object;

                    ret = StringUtils.equals(roleId, role.getId().toString());
                }

                return ret;
            }
        };

        if(predicates != null) {
            predicates.add(ret);
        }

        return ret;
    }

    private Predicate addPredicateForGroupName(final String groupName, List<Predicate> predicates) {
        if(StringUtils.isEmpty(groupName)) {
            return null;
        }

        Predicate ret = new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                if(object == null) {
                    return false;
                }

                boolean ret = false;

                if(object instanceof RangerRole) {
                    RangerRole role = (RangerRole) object;

                    List<RangerRole.RoleMember> groups = role.getGroups();

                    for (RangerRole.RoleMember member : groups) {
                        ret = StringUtils.equals(member.getName(), groupName);

                        if (ret) {
                            break;
                        }
                    }
                }
                return ret;
            }
        };

        if(predicates != null) {
            predicates.add(ret);
        }

        return ret;
    }

    private Predicate addPredicateForPartialGroupName(final String groupNamePartial, List<Predicate> predicates) {
        if(StringUtils.isEmpty(groupNamePartial)) {
            return null;
        }

        Predicate ret = new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                if(object == null) {
                    return false;
                }

                boolean ret = false;

                if(object instanceof RangerRole) {
                    RangerRole role = (RangerRole) object;

                    List<RangerRole.RoleMember> groups = role.getGroups();

                    for (RangerRole.RoleMember member : groups) {
                        ret = StringUtils.containsIgnoreCase(member.getName(), groupNamePartial);

                        if (ret) {
                            break;
                        }
                    }
                }
                return ret;
            }
        };

        if(predicates != null) {
            predicates.add(ret);
        }

        return ret;
    }

    private Predicate addPredicateForUserName(final String userName, List<Predicate> predicates) {
        if(StringUtils.isEmpty(userName)) {
            return null;
        }

        Predicate ret = new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                if(object == null) {
                    return false;
                }

                boolean ret = false;

                if(object instanceof RangerRole) {
                    RangerRole role = (RangerRole) object;

                    List<RangerRole.RoleMember> users = role.getUsers();

                    for (RangerRole.RoleMember member : users) {
                        ret = StringUtils.equals(member.getName(), userName);

                        if (ret) {
                            break;
                        }
                    }
                }
                return ret;
            }
        };

        if(predicates != null) {
            predicates.add(ret);
        }

        return ret;
    }

    private Predicate addPredicateForPartialUserName(final String userNamePartial, List<Predicate> predicates) {
        if(StringUtils.isEmpty(userNamePartial)) {
            return null;
        }

        Predicate ret = new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                if(object == null) {
                    return false;
                }

                boolean ret = false;

                if(object instanceof RangerRole) {
                    RangerRole role = (RangerRole) object;

                    List<RangerRole.RoleMember> users = role.getUsers();

                    for (RangerRole.RoleMember member : users) {
                        ret = StringUtils.containsIgnoreCase(member.getName(), userNamePartial);

                        if (ret) {
                            break;
                        }
                    }
                }
                return ret;
            }
        };

        if(predicates != null) {
            predicates.add(ret);
        }

        return ret;
    }

}

