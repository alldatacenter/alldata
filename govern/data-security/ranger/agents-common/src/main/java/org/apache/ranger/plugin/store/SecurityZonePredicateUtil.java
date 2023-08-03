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
import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.util.SearchFilter;

import java.util.List;

public class SecurityZonePredicateUtil extends AbstractPredicateUtil {

    public SecurityZonePredicateUtil() {
        super();
    }

    @Override
    public void addPredicates(SearchFilter filter, List<Predicate> predicates) {
        //super.addPredicates(filter, predicates);

        addPredicateForServiceName(filter.getParam(SearchFilter.SERVICE_NAME), predicates);
        addPredicateForMatchingZoneId(filter.getParam(SearchFilter.ZONE_ID), predicates);
        addPredicateForNonMatchingZoneName(filter.getParam(SearchFilter.ZONE_NAME), predicates);
    }

    private Predicate addPredicateForServiceName(final String serviceName, List<Predicate> predicates) {
        if(StringUtils.isEmpty(serviceName)) {
            return null;
        }

        Predicate ret = new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                if(object == null) {
                    return false;
                }

                boolean ret = false;

                if(object instanceof RangerSecurityZone) {
                    RangerSecurityZone securityZone = (RangerSecurityZone) object;

                    ret = securityZone.getServices().get(serviceName) != null;
                }

                return ret;
            }
        };

        if(predicates != null) {
            predicates.add(ret);
        }

        return ret;
    }

    private Predicate addPredicateForMatchingZoneId(final String zoneId, List<Predicate> predicates) {
        if (StringUtils.isEmpty(zoneId)) {
            return null;
        }

        Predicate ret = new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                if(object == null) {
                    return false;
                }

                boolean ret = false;

                if(object instanceof RangerSecurityZone) {
                    RangerSecurityZone securityZone = (RangerSecurityZone) object;

                    if (StringUtils.equals(zoneId, securityZone.getId().toString())) {
                        ret = true;
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

    private Predicate addPredicateForNonMatchingZoneName(final String zoneName, List<Predicate> predicates) {

        Predicate ret = new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                if(object == null) {
                    return false;
                }

                boolean ret = false;

                if(object instanceof RangerSecurityZone) {
                    RangerSecurityZone securityZone = (RangerSecurityZone) object;

                    if (StringUtils.isEmpty(zoneName) || !StringUtils.equals(zoneName, securityZone.getName())) {
                        ret = true;
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

