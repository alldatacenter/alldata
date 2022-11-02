/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.util;

import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasElement;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SearchPredicateUtil {
    private static final Logger LOG = LoggerFactory.getLogger(SearchPredicateUtil.class);

    private static Predicate ALWAYS_FALSE = new Predicate() {
        @Override
        public boolean evaluate(final Object object) {
            return false;
        }
    };

    public static ElementAttributePredicateGenerator getLTPredicateGenerator() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getLTPredicateGenerator");
        }

        ElementAttributePredicateGenerator ret = new ElementAttributePredicateGenerator() {
            @Override
            public Predicate generatePredicate(final String attrName, final Object attrVal, final Class attrClass) {
                final Predicate ret;

                if (attrName == null || attrClass == null || attrVal == null) {
                    ret = ALWAYS_FALSE;
                } else if (Short.class.isAssignableFrom(attrClass)) {
                    ret = ShortPredicate.getLTPredicate(attrName, attrClass, (Short)attrVal);
                } else if (Integer.class.isAssignableFrom(attrClass)) {
                    ret = IntegerPredicate.getLTPredicate(attrName, attrClass, (Integer)attrVal);
                } else if (Long.class.isAssignableFrom(attrClass)) {
                    ret = LongPredicate.getLTPredicate(attrName, attrClass, (Long)attrVal);
                } else if (Float.class.isAssignableFrom(attrClass)) {
                    ret = FloatPredicate.getLTPredicate(attrName, attrClass, (Float)attrVal);
                } else if (Double.class.isAssignableFrom(attrClass)) {
                    ret = DoublePredicate.getLTPredicate(attrName, attrClass, (Double)attrVal);
                } else if (Byte.class.isAssignableFrom(attrClass)) {
                    ret = BytePredicate.getLTPredicate(attrName, attrClass, (Byte)attrVal);
                } else if (BigInteger.class.isAssignableFrom(attrClass)) {
                    ret = BigIntegerPredicate.getLTPredicate(attrName, attrClass, (BigInteger)attrVal);
                } else if (BigDecimal.class.isAssignableFrom(attrClass)) {
                    ret = BigDecimalPredicate.getLTPredicate(attrName, attrClass, (BigDecimal)attrVal);
                } else if (String.class.isAssignableFrom(attrClass)) {
                    ret = StringPredicate.getLTPredicate(attrName, attrClass, (String)attrVal);
                } else {
                    ret = ALWAYS_FALSE;
                }

                return ret;
            }
        };

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getLTPredicateGenerator");
        }

        return ret;
    }

    public static ElementAttributePredicateGenerator getGTPredicateGenerator() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getGTPredicateGenerator");
        }

        ElementAttributePredicateGenerator ret = new ElementAttributePredicateGenerator() {
            @Override
            public Predicate generatePredicate(final String attrName, final Object attrVal, final Class attrClass) {
                final Predicate ret;

                if (attrName == null || attrClass == null || attrVal == null) {
                    ret = ALWAYS_FALSE;
                } else if (Short.class.isAssignableFrom(attrClass)) {
                    ret = ShortPredicate.getGTPredicate(attrName, attrClass, (Short)attrVal);
                } else if (Integer.class.isAssignableFrom(attrClass)) {
                    ret = IntegerPredicate.getGTPredicate(attrName, attrClass, (Integer)attrVal);
                } else if (Long.class.isAssignableFrom(attrClass)) {
                    ret = LongPredicate.getGTPredicate(attrName, attrClass, (Long)attrVal);
                } else if (Float.class.isAssignableFrom(attrClass)) {
                    ret = FloatPredicate.getGTPredicate(attrName, attrClass, (Float)attrVal);
                } else if (Double.class.isAssignableFrom(attrClass)) {
                    ret = DoublePredicate.getGTPredicate(attrName, attrClass, (Double)attrVal);
                } else if (Byte.class.isAssignableFrom(attrClass)) {
                    ret = BytePredicate.getGTPredicate(attrName, attrClass, (Byte)attrVal);
                } else if (BigInteger.class.isAssignableFrom(attrClass)) {
                    ret = BigIntegerPredicate.getGTPredicate(attrName, attrClass, (BigInteger)attrVal);
                } else if (BigDecimal.class.isAssignableFrom(attrClass)) {
                    ret = BigDecimalPredicate.getGTPredicate(attrName, attrClass, (BigDecimal)attrVal);
                } else if (String.class.isAssignableFrom(attrClass)) {
                    ret = StringPredicate.getGTPredicate(attrName, attrClass, (String)attrVal);
                } else {
                    ret = ALWAYS_FALSE;
                }

                return ret;
            }
        };

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getGTPredicateGenerator");
        }
        return ret;
    }

    public static ElementAttributePredicateGenerator getLTEPredicateGenerator() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getLTEPredicateGenerator");
        }

        ElementAttributePredicateGenerator ret = new ElementAttributePredicateGenerator() {
            @Override
            public Predicate generatePredicate(final String attrName, final Object attrVal, final Class attrClass) {
                final Predicate ret;

                if (attrName == null || attrClass == null || attrVal == null) {
                    ret = ALWAYS_FALSE;
                } else if (Short.class.isAssignableFrom(attrClass)) {
                    ret = ShortPredicate.getLTEPredicate(attrName, attrClass, (Short)attrVal);
                } else if (Integer.class.isAssignableFrom(attrClass)) {
                    ret = IntegerPredicate.getLTEPredicate(attrName, attrClass, (Integer)attrVal);
                } else if (Long.class.isAssignableFrom(attrClass)) {
                    ret = LongPredicate.getLTEPredicate(attrName, attrClass, (Long)attrVal);
                } else if (Float.class.isAssignableFrom(attrClass)) {
                    ret = FloatPredicate.getLTEPredicate(attrName, attrClass, (Float)attrVal);
                } else if (Double.class.isAssignableFrom(attrClass)) {
                    ret = DoublePredicate.getLTEPredicate(attrName, attrClass, (Double)attrVal);
                } else if (Byte.class.isAssignableFrom(attrClass)) {
                    ret = BytePredicate.getLTEPredicate(attrName, attrClass, (Byte)attrVal);
                } else if (BigInteger.class.isAssignableFrom(attrClass)) {
                    ret = BigIntegerPredicate.getLTEPredicate(attrName, attrClass, (BigInteger)attrVal);
                } else if (BigDecimal.class.isAssignableFrom(attrClass)) {
                    ret = BigDecimalPredicate.getLTEPredicate(attrName, attrClass, (BigDecimal)attrVal);
                } else if (String.class.isAssignableFrom(attrClass)) {
                    ret = StringPredicate.getLTEPredicate(attrName, attrClass, (String)attrVal);
                } else {
                    ret = ALWAYS_FALSE;
                }

                return ret;
            }
        };

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getLTEPredicateGenerator");
        }

        return ret;
    }

    public static ElementAttributePredicateGenerator getInRangePredicateGenerator() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getInRangePredicateGenerator");
        }

        ElementAttributePredicateGenerator ret = new ElementAttributePredicateGenerator() {

            @Override
            public Predicate generatePredicate(String attrName, Object attrVal, Class attrClass) {
                return generatePredicate(attrName, attrVal, attrVal, attrClass);
            }

            @Override
            public Predicate generatePredicate(String attrName, Object attrVal, Object attrVal2, Class attrClass) {
                final Predicate ret;

                if (attrName == null || attrClass == null || attrVal == null || attrVal2 == null) {
                    ret = ALWAYS_FALSE;
                } else if (Long.class.isAssignableFrom(attrClass)) {
                    ret = LongPredicate.getInRangePredicate(attrName, attrClass, (Long) attrVal, (Long) attrVal2);
                } else {
                    ret = ALWAYS_FALSE;
                }
                return ret;
            }
        };

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getInRangePredicateGenerator");
        }

        return ret;
    }

    public static ElementAttributePredicateGenerator getGTEPredicateGenerator() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getGTEPredicateGenerator");
        }

        ElementAttributePredicateGenerator ret = new ElementAttributePredicateGenerator() {
            @Override
            public Predicate generatePredicate(final String attrName, final Object attrVal, final Class attrClass) {
                final Predicate ret;

                if (attrName == null || attrClass == null || attrVal == null) {
                    ret = ALWAYS_FALSE;
                } else if (Short.class.isAssignableFrom(attrClass)) {
                    ret = ShortPredicate.getGTEPredicate(attrName, attrClass, (Short)attrVal);
                } else if (Integer.class.isAssignableFrom(attrClass)) {
                    ret = IntegerPredicate.getGTEPredicate(attrName, attrClass, (Integer)attrVal);
                } else if (Long.class.isAssignableFrom(attrClass)) {
                    ret = LongPredicate.getGTEPredicate(attrName, attrClass, (Long)attrVal);
                } else if (Float.class.isAssignableFrom(attrClass)) {
                    ret = FloatPredicate.getGTEPredicate(attrName, attrClass, (Float)attrVal);
                } else if (Double.class.isAssignableFrom(attrClass)) {
                    ret = DoublePredicate.getGTEPredicate(attrName, attrClass, (Double)attrVal);
                } else if (Byte.class.isAssignableFrom(attrClass)) {
                    ret = BytePredicate.getGTEPredicate(attrName, attrClass, (Byte)attrVal);
                } else if (BigInteger.class.isAssignableFrom(attrClass)) {
                    ret = BigIntegerPredicate.getGTEPredicate(attrName, attrClass, (BigInteger)attrVal);
                } else if (BigDecimal.class.isAssignableFrom(attrClass)) {
                    ret = BigDecimalPredicate.getGTEPredicate(attrName, attrClass, (BigDecimal)attrVal);
                } else if (String.class.isAssignableFrom(attrClass)) {
                    ret = StringPredicate.getGTEPredicate(attrName, attrClass, (String)attrVal);
                } else {
                    ret = ALWAYS_FALSE;
                }

                return ret;
            }
        };

        if (LOG.isDebugEnabled()) {
            LOG.debug("<- getGTEPredicateGenerator");
        }

        return ret;
    }

    public static ElementAttributePredicateGenerator getEQPredicateGenerator() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getEQPredicateGenerator");
        }

        ElementAttributePredicateGenerator ret = new ElementAttributePredicateGenerator() {
            @Override
            public Predicate generatePredicate(final String attrName, final Object attrVal, final Class attrClass) {
                final Predicate ret;

                if (attrName == null || attrClass == null || attrVal == null) {
                    ret = ALWAYS_FALSE;
                } else if (Boolean.class.isAssignableFrom(attrClass)) {
                    ret = BooleanPredicate.getEQPredicate(attrName, attrClass, (Boolean)attrVal);
                } else if (Byte.class.isAssignableFrom(attrClass)) {
                    ret = BytePredicate.getEQPredicate(attrName, attrClass, (Byte)attrVal);
                } else if (Short.class.isAssignableFrom(attrClass)) {
                    ret = ShortPredicate.getEQPredicate(attrName, attrClass, (Short)attrVal);
                } else if (Integer.class.isAssignableFrom(attrClass)) {
                    ret = IntegerPredicate.getEQPredicate(attrName, attrClass, (Integer)attrVal);
                } else if (Long.class.isAssignableFrom(attrClass)) {
                    ret = LongPredicate.getEQPredicate(attrName, attrClass, (Long)attrVal);
                } else if (Float.class.isAssignableFrom(attrClass)) {
                    ret = FloatPredicate.getEQPredicate(attrName, attrClass, (Float)attrVal);
                } else if (Double.class.isAssignableFrom(attrClass)) {
                    ret = DoublePredicate.getEQPredicate(attrName, attrClass, (Double)attrVal);
                } else if (BigInteger.class.isAssignableFrom(attrClass)) {
                    ret = BigIntegerPredicate.getEQPredicate(attrName, attrClass, (BigInteger)attrVal);
                } else if (BigDecimal.class.isAssignableFrom(attrClass)) {
                    ret = BigDecimalPredicate.getEQPredicate(attrName, attrClass, (BigDecimal)attrVal);
                } else if (String.class.isAssignableFrom(attrClass)) {
                    ret = StringPredicate.getEQPredicate(attrName, attrClass, (String)attrVal);
                } else {
                    ret = ALWAYS_FALSE;
                }

                return ret;
            }
        };

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getEQPredicateGenerator");
        }

        return ret;
    }

    public static ElementAttributePredicateGenerator getNEQPredicateGenerator() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getNEQPredicateGenerator");
        }

        ElementAttributePredicateGenerator ret = new ElementAttributePredicateGenerator() {
            @Override
            public Predicate generatePredicate(final String attrName, final Object attrVal, final Class attrClass) {
                final Predicate ret;

                if (attrName == null || attrClass == null) {
                    ret = ALWAYS_FALSE;
                } else if (Boolean.class.isAssignableFrom(attrClass)) {
                    ret = BooleanPredicate.getNEQPredicate(attrName, attrClass, (Boolean)attrVal);
                } else if (Byte.class.isAssignableFrom(attrClass)) {
                    ret = BytePredicate.getNEQPredicate(attrName, attrClass, (Byte)attrVal);
                } else if (Short.class.isAssignableFrom(attrClass)) {
                    ret = ShortPredicate.getNEQPredicate(attrName, attrClass, (Short)attrVal);
                } else if (Integer.class.isAssignableFrom(attrClass)) {
                    ret = IntegerPredicate.getNEQPredicate(attrName, attrClass, (Integer)attrVal);
                } else if (Long.class.isAssignableFrom(attrClass)) {
                    ret = LongPredicate.getNEQPredicate(attrName, attrClass, (Long)attrVal);
                } else if (Float.class.isAssignableFrom(attrClass)) {
                    ret = FloatPredicate.getNEQPredicate(attrName, attrClass, (Float)attrVal);
                } else if (Double.class.isAssignableFrom(attrClass)) {
                    ret = DoublePredicate.getNEQPredicate(attrName, attrClass, (Double)attrVal);
                } else if (BigInteger.class.isAssignableFrom(attrClass)) {
                    ret = BigIntegerPredicate.getNEQPredicate(attrName, attrClass, (BigInteger)attrVal);
                } else if (BigDecimal.class.isAssignableFrom(attrClass)) {
                    ret = BigDecimalPredicate.getNEQPredicate(attrName, attrClass, (BigDecimal)attrVal);
                } else if (String.class.isAssignableFrom(attrClass)) {
                    ret = StringPredicate.getNEQPredicate(attrName, attrClass, (String)attrVal);
                } else {
                    ret = ALWAYS_FALSE;
                }

                return ret;
            }
        };

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getNEQPredicateGenerator");
        }

        return ret;
    }

    public static ElementAttributePredicateGenerator getContainsAnyPredicateGenerator() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getContainsAnyPredicateGenerator");
        }

        ElementAttributePredicateGenerator ret = new ElementAttributePredicateGenerator() {
            @Override
            public Predicate generatePredicate(final String attrName, final Object attrVal, final Class attrClass) {
                final Predicate ret;

                if (attrName == null || attrClass == null || attrVal == null || !isValid(attrVal, attrClass)) {
                    ret = ALWAYS_FALSE;
                } else {
                    ret = new ElementAttributePredicate(attrName, attrClass) {
                        @Override
                        public boolean compareValue(final Object vertexAttrVal) {
                            return CollectionUtils.containsAny((Collection) attrVal, (Collection) vertexAttrVal);
                        }
                    };
                }
                return ret;
            }

            private boolean isValid(final Object attrVal, final Class attrClass) {
                return attrVal instanceof Collection && Collection.class.isAssignableFrom(attrClass);
            }
        };

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getContainsAnyPredicateGenerator");
        }

        return ret;
    }

    public static ElementAttributePredicateGenerator getContainsAllPredicateGenerator() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getContainsAllPredicateGenerator");
        }

        ElementAttributePredicateGenerator ret = new ElementAttributePredicateGenerator() {
            @Override
            public Predicate generatePredicate(final String attrName, final Object attrVal, final Class attrClass) {
                final Predicate ret;

                if (attrName == null || attrClass == null || attrVal == null || !isValid(attrVal, attrClass)) {
                    ret = ALWAYS_FALSE;
                } else {
                    ret = new ElementAttributePredicate(attrName, attrClass) {
                        @Override
                        public boolean compareValue(final Object vertexAttrVal) {
                            return ((Collection) attrVal).containsAll((Collection) vertexAttrVal);
                        }
                    };
                }
                return ret;
            }

            private boolean isValid(final Object attrVal, final Class attrClass) {
                return attrVal instanceof Collection && Collection.class.isAssignableFrom(attrClass);
            }
        };

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getContainsAllPredicateGenerator");
        }

        return ret;
    }

    public static ElementAttributePredicateGenerator getINPredicateGenerator() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getINPredicateGenerator");
        }

        ElementAttributePredicateGenerator ret = new ElementAttributePredicateGenerator() {
            @Override
            public Predicate generatePredicate(final String attrName, final Object attrVal, final Class attrClass) {
                final Predicate ret;

                if (attrName == null || attrClass == null || attrVal == null || !isValid(attrVal, attrClass)) {
                    ret = ALWAYS_FALSE;
                } else {
                    ret = new ElementAttributePredicate(attrName, attrClass) {
                        @Override
                        public boolean compareValue(final Object vertexAttrVal) {
                            return ((Collection)attrVal).contains(vertexAttrVal);
                        }
                    };
                }

                return ret;
            }

            private boolean isValid(final Object attrVal, final Class attrClass) {
                return attrVal instanceof Collection;
            }
        };

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getINPredicateGenerator");
        }

        return ret;
    }

    public static ElementAttributePredicateGenerator getRegexPredicateGenerator() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getRegexPredicateGenerator");
        }

        ElementAttributePredicateGenerator ret = new ElementAttributePredicateGenerator() {
            @Override
            public Predicate generatePredicate(final String attrName, final Object attrVal, final Class attrClass) {
                final Predicate ret;

                if (attrName == null || attrClass == null || attrVal == null) {
                    ret = ALWAYS_FALSE;
                } else if (String.class.isAssignableFrom(attrClass)) {
                    ret = StringPredicate.getRegexPredicate(attrName, attrClass, (String) attrVal);
                } else {
                    ret = ALWAYS_FALSE;
                }

                return ret;
            }
        };

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getRegexPredicateGenerator");
        }

        return ret;
    }

    public static ElementAttributePredicateGenerator getLIKEPredicateGenerator() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getLIKEPredicateGenerator");
        }

        ElementAttributePredicateGenerator ret = new ElementAttributePredicateGenerator() {
            @Override
            public Predicate generatePredicate(final String attrName, final Object attrVal, final Class attrClass) {
                final Predicate ret;

                if (attrName == null || attrClass == null || attrVal == null) {
                    ret = ALWAYS_FALSE;
                } else if (String.class.isAssignableFrom(attrClass)) {
                    ret = StringPredicate.getContainsPredicate(attrName, attrClass, (String)attrVal);
                } else {
                    ret = ALWAYS_FALSE;
                }

                return ret;
            }
        };

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getLIKEPredicateGenerator");
        }

        return ret;
    }

    public static ElementAttributePredicateGenerator getStartsWithPredicateGenerator() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getStartsWithPredicateGenerator");
        }

        ElementAttributePredicateGenerator ret = new ElementAttributePredicateGenerator() {
            @Override
            public Predicate generatePredicate(final String attrName, final Object attrVal, final Class attrClass) {
                final Predicate ret;

                if (attrName == null || attrClass == null || attrVal == null) {
                    ret = ALWAYS_FALSE;
                } else if (String.class.isAssignableFrom(attrClass)) {
                    ret = StringPredicate.getStartsWithPredicate(attrName, attrClass, (String)attrVal);
                } else {
                    ret = ALWAYS_FALSE;
                }

                return ret;
            }
        };

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getStartsWithPredicateGenerator");
        }

        return ret;
    }

    public static ElementAttributePredicateGenerator getEndsWithPredicateGenerator() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getEndsWithPredicateGenerator");
        }

        ElementAttributePredicateGenerator ret = new ElementAttributePredicateGenerator() {
            @Override
            public Predicate generatePredicate(final String attrName, final Object attrVal, final Class attrClass) {
                final Predicate ret;

                if (attrName == null || attrClass == null || attrVal == null) {
                    ret = ALWAYS_FALSE;
                } else if (String.class.isAssignableFrom(attrClass)) {
                    ret = StringPredicate.getEndsWithPredicate(attrName, attrClass, (String)attrVal);
                } else {
                    ret = ALWAYS_FALSE;
                }

                return ret;
            }
        };

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getEndsWithPredicateGenerator");
        }

        return ret;
    }

    public static ElementAttributePredicateGenerator getContainsPredicateGenerator() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getContainsPredicateGenerator");
        }

        ElementAttributePredicateGenerator ret = new ElementAttributePredicateGenerator() {
            @Override
            public Predicate generatePredicate(final String attrName, final Object attrVal, final Class attrClass) {
                final Predicate ret;

                if (attrName == null || attrClass == null || attrVal == null) {
                    ret = ALWAYS_FALSE;
                } else if (String.class.isAssignableFrom(attrClass)) {
                    ret = StringPredicate.getContainsPredicate(attrName, attrClass, (String)attrVal);
                } else if (Collection.class.isAssignableFrom(attrClass)) {
                    // Check if the provided value is present in the list of stored values
                    ret = new ElementAttributePredicate(attrName, attrClass) {
                        @Override
                        protected boolean compareValue(final Object vertexAttrVal) {
                            return ((Collection) vertexAttrVal).contains(attrVal);
                        }
                    };
                } else {
                    ret = ALWAYS_FALSE;
                }

                return ret;
            }
        };

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getContainsPredicateGenerator");
        }

        return ret;
    }

    public static ElementAttributePredicateGenerator getNotContainsPredicateGenerator() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getNotContainsPredicateGenerator");
        }

        ElementAttributePredicateGenerator ret = new ElementAttributePredicateGenerator() {
            @Override
            public Predicate generatePredicate(final String attrName, final Object attrVal, final Class attrClass) {
                final Predicate ret;

                if (attrName == null || attrClass == null) {
                    ret = ALWAYS_FALSE;
                } else if (String.class.isAssignableFrom(attrClass)) {
                    ret = StringPredicate.getNotContainsPredicate(attrName, attrClass, (String) attrVal);
                } else if (Collection.class.isAssignableFrom(attrClass)) {
                    // Check if the provided value is present in the list of stored values
                    ret = new ElementAttributePredicate(attrName, attrClass,true) {
                        @Override
                        protected boolean compareValue(final Object vertexAttrVal) {
                            return vertexAttrVal == null || !((Collection) vertexAttrVal).contains(attrVal);
                        }
                    };
                } else {
                    ret = ALWAYS_FALSE;
                }

                return ret;
            }
        };

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getNotContainsPredicateGenerator");
        }

        return ret;
    }

    public static ElementAttributePredicateGenerator getIsNullPredicateGenerator() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getIsNullPredicateGenerator");
        }

        ElementAttributePredicateGenerator ret = new ElementAttributePredicateGenerator() {
            @Override
            public Predicate generatePredicate(final String attrName, final Object attrVal, final Class attrClass) {
                final Predicate ret;

                if (attrName == null || attrClass == null) {
                    ret = ALWAYS_FALSE;
                } else {
                    ret = new ElementAttributePredicate(attrName, attrClass, true) {
                        @Override
                        protected boolean compareValue(final Object vertexAttrVal) {
                            return vertexAttrVal == null;
                        }
                    };
                }

                return ret;
            }
        };

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getIsNullPredicateGenerator");
        }

        return ret;
    }

    public static ElementAttributePredicateGenerator getNotNullPredicateGenerator() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getNotNullPredicateGenerator");
        }

        ElementAttributePredicateGenerator ret = new ElementAttributePredicateGenerator() {
            @Override
            public Predicate generatePredicate(final String attrName, final Object attrVal, final Class attrClass) {
                final Predicate ret;

                if (attrName == null || attrClass == null) {
                    ret = ALWAYS_FALSE;
                } else {
                    ret = new ElementAttributePredicate(attrName, attrClass, true) {
                        @Override
                        protected boolean compareValue(final Object vertexAttrVal) {
                            return vertexAttrVal != null;
                        }
                    };
                }

                return ret;
            }
        };

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getNotNullPredicateGenerator");
        }

        return ret;
    }

    public static ElementAttributePredicateGenerator getNotEmptyPredicateGenerator() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getNotEmptyPredicateGenerator");
        }

        ElementAttributePredicateGenerator ret = new ElementAttributePredicateGenerator() {
            @Override
            public Predicate generatePredicate(final String attrName, final Object attrVal, final Class attrClass) {
                final Predicate ret;

                if (attrName == null || attrClass == null) {
                    ret = ALWAYS_FALSE;
                } else {
                    ret = new ElementAttributePredicate(attrName, attrClass, true) {
                        @Override
                        protected boolean compareValue(final Object vertexAttrVal) {
                            boolean ret = false;

                            if (vertexAttrVal != null) {
                                if (vertexAttrVal instanceof Collection) {
                                    ret = CollectionUtils.isNotEmpty((Collection) vertexAttrVal);
                                } else if (vertexAttrVal instanceof String) {
                                    ret = StringUtils.isNotEmpty((String) vertexAttrVal);
                                } else {
                                    ret = true; // for other datatypes, a non-null is treated as non-empty
                                }
                            }

                            return ret;
                        }
                    };
                }

                return ret;
            }
        };

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getNotEmptyPredicateGenerator");
        }

        return ret;
    }

    public static ElementAttributePredicateGenerator getIsNullOrEmptyPredicateGenerator() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getIsNullOrEmptyPredicateGenerator");
        }

        ElementAttributePredicateGenerator ret = new ElementAttributePredicateGenerator() {
            @Override
            public Predicate generatePredicate(final String attrName, final Object attrVal, final Class attrClass) {
                final Predicate ret;

                if (attrName == null || attrClass == null) {
                    ret = ALWAYS_FALSE;
                } else {
                    ret = new ElementAttributePredicate(attrName, attrClass, true) {
                        @Override
                        protected boolean compareValue(final Object vertexAttrVal) {
                            final boolean ret;

                            if (vertexAttrVal == null) {
                                ret = true;
                            } else if (vertexAttrVal instanceof Collection) {
                                ret = CollectionUtils.isEmpty((Collection) vertexAttrVal);
                            } else if (vertexAttrVal instanceof String) {
                                ret = StringUtils.isEmpty((String) vertexAttrVal);
                            } else {
                                ret = false;
                            }

                            return ret;
                        }
                    };
                }

                return ret;
            }
        };

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getIsNullOrEmptyPredicateGenerator");
        }

        return ret;
    }

    public interface ElementAttributePredicateGenerator {
        Predicate generatePredicate(String attrName, Object attrVal, Class attrClass);
        default Predicate generatePredicate(String attrName, Object attrVal, Object attrVal2, Class attrClass) {
            return generatePredicate(attrName, attrVal, attrClass);
        }
    }

    static abstract class ElementAttributePredicate implements Predicate {
        final String  attrName;
        final Class   attrClass;
        final boolean isNullValid;

        ElementAttributePredicate(String attrName, Class attrClass) {
            this(attrName, attrClass, false);
        }

        ElementAttributePredicate(String attrName, Class attrClass, boolean isNullValid) {
            this.attrName  = attrName;
            this.attrClass = attrClass;
            this.isNullValid = isNullValid;
        }

        @Override
        public boolean evaluate(final Object object) {
            final boolean ret;

            AtlasElement element = (object instanceof AtlasVertex || object instanceof AtlasEdge) ? (AtlasElement) object : null;

            if (element != null) {
                Object attrValue;
                if (Collection.class.isAssignableFrom(attrClass)) {
                    attrValue = element.getPropertyValues(attrName, attrClass);
                } else {
                    attrValue = AtlasGraphUtilsV2.getProperty(element, attrName, attrClass);
                }

                ret = (isNullValid || attrValue != null) && compareValue(attrValue);
            } else {
                ret = false;
            }

            return ret;
        }

        protected abstract boolean compareValue(Object vertexAttrVal);
    }

    static abstract class BooleanPredicate extends ElementAttributePredicate {
        final Boolean value;

        BooleanPredicate(String attrName, Class attrClass, Boolean value) {
            super(attrName, attrClass);

            this.value = value;
        }

        BooleanPredicate(String attrName, Class attrClass, Boolean value, boolean isNullValid) {
            super(attrName, attrClass, isNullValid);
            this.value = value;
        }

        static ElementAttributePredicate getEQPredicate(String attrName, Class attrClass, Boolean value) {
            return new SearchPredicateUtil.BooleanPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Boolean) vertexAttrVal).compareTo(value) == 0;
                }
            };
        }

        static ElementAttributePredicate getNEQPredicate(String attrName, Class attrClass, Boolean value) {
            return new SearchPredicateUtil.BooleanPredicate(attrName, attrClass, value, true) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return vertexAttrVal == null || ((Boolean) vertexAttrVal).compareTo(value) != 0;
                }
            };
        }
    }

    static abstract class ShortPredicate extends ElementAttributePredicate {
        final Short value;

        ShortPredicate(String attrName, Class attrClass, Short value) {
            super(attrName, attrClass);

            this.value = value;
        }

        ShortPredicate(String attrName, Class attrClass, Short value, boolean isNullValid) {
            super(attrName, attrClass, isNullValid);
            this.value = value;
        }

        static ElementAttributePredicate getEQPredicate(String attrName, Class attrClass, Short value) {
            return new ShortPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Short) vertexAttrVal).compareTo(value) == 0;
                }
            };
        }

        static ElementAttributePredicate getNEQPredicate(String attrName, Class attrClass, Short value) {
            return new ShortPredicate(attrName, attrClass, value, true) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return vertexAttrVal == null || ((Short) vertexAttrVal).compareTo(value) != 0;
                }
            };
        }

        static ElementAttributePredicate getLTPredicate(String attrName, Class attrClass, Short value) {
            return new ShortPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Short) vertexAttrVal).compareTo(value) < 0;
                }
            };
        }

        static ElementAttributePredicate getLTEPredicate(String attrName, Class attrClass, Short value) {
            return new ShortPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Short) vertexAttrVal).compareTo(value) <= 0;
                }
            };
        }

        static ElementAttributePredicate getGTPredicate(String attrName, Class attrClass, Short value) {
            return new ShortPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Short) vertexAttrVal).compareTo(value) > 0;
                }
            };
        }

        static ElementAttributePredicate getGTEPredicate(String attrName, Class attrClass, Short value) {
            return new ShortPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Short) vertexAttrVal).compareTo(value) >= 0;
                }
            };
        }
    }

    static abstract class IntegerPredicate extends ElementAttributePredicate {
        final Integer value;

        IntegerPredicate(String attrName, Class attrClass, Integer value) {
            super(attrName, attrClass);

            this.value = value;
        }

        IntegerPredicate(String attrName, Class attrClass, Integer value, boolean isNullValid) {
            super(attrName, attrClass, isNullValid);
            this.value = value;
        }

        static ElementAttributePredicate getEQPredicate(String attrName, Class attrClass, Integer value) {
            return new IntegerPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Integer) vertexAttrVal).compareTo(value) == 0;
                }
            };
        }

        static ElementAttributePredicate getNEQPredicate(String attrName, Class attrClass, Integer value) {
            return new IntegerPredicate(attrName, attrClass, value, true) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return vertexAttrVal == null || ((Integer) vertexAttrVal).compareTo(value) != 0;
                }
            };
        }

        static ElementAttributePredicate getLTPredicate(String attrName, Class attrClass, Integer value) {
            return new IntegerPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Integer) vertexAttrVal).compareTo(value) < 0;
                }
            };
        }

        static ElementAttributePredicate getLTEPredicate(String attrName, Class attrClass, Integer value) {
            return new IntegerPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Integer) vertexAttrVal).compareTo(value) <= 0;
                }
            };
        }

        static ElementAttributePredicate getGTPredicate(String attrName, Class attrClass, Integer value) {
            return new IntegerPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Integer) vertexAttrVal).compareTo(value) > 0;
                }
            };
        }

        static ElementAttributePredicate getGTEPredicate(String attrName, Class attrClass, Integer value) {
            return new IntegerPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Integer) vertexAttrVal).compareTo(value) >= 0;
                }
            };
        }
    }

    static abstract class LongPredicate extends ElementAttributePredicate {
        final Long value;
        final Long value2;

        LongPredicate(String attrName, Class attrClass, Long value) {
            super(attrName, attrClass);

            this.value = value;
            this.value2 = null;

        }

        LongPredicate(String attrName, Class attrClass, Long value, Long value2, boolean isNullValid) {
            super(attrName, attrClass, isNullValid);
            this.value = value;
            this.value2 = value2;

        }

        LongPredicate(String attrName, Class attrClass, Long value, boolean isNullValid) {
            super(attrName, attrClass, isNullValid);
            this.value  = value;
            this.value2 = null;
        }

        static ElementAttributePredicate getEQPredicate(String attrName, Class attrClass, Long value) {
            return new LongPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Long) vertexAttrVal).compareTo(value) == 0;
                }
            };
        }

        static ElementAttributePredicate getNEQPredicate(String attrName, Class attrClass, Long value) {
            return new LongPredicate(attrName, attrClass, value, true) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return vertexAttrVal == null || ((Long) vertexAttrVal).compareTo(value) != 0;
                }
            };
        }

        static ElementAttributePredicate getLTPredicate(String attrName, Class attrClass, Long value) {
            return new LongPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Long) vertexAttrVal).compareTo(value) < 0;
                }
            };
        }

        static ElementAttributePredicate getLTEPredicate(String attrName, Class attrClass, Long value) {
            return new LongPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Long) vertexAttrVal).compareTo(value) <= 0;
                }
            };
        }

        static ElementAttributePredicate getGTPredicate(String attrName, Class attrClass, Long value) {
            return new LongPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Long) vertexAttrVal).compareTo(value) > 0;
                }
            };
        }

        static ElementAttributePredicate getGTEPredicate(String attrName, Class attrClass, Long value) {
            return new LongPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Long) vertexAttrVal).compareTo(value) >= 0;
                }
            };
        }

        static ElementAttributePredicate getInRangePredicate(String attrName, Class attrClass, Long rangeStart, Long rangeEnd) {
            return new LongPredicate(attrName, attrClass, rangeStart, rangeEnd, false) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Long) vertexAttrVal).compareTo(value) >= 0 && ((Long) vertexAttrVal).compareTo(value2) <= 0;
                }
            };
        }
    }

    static abstract class FloatPredicate extends ElementAttributePredicate {
        final Float value;

        FloatPredicate(String attrName, Class attrClass, Float value) {
            super(attrName, attrClass);

            this.value = value;
        }

        FloatPredicate(String attrName, Class attrClass, Float value, boolean isNullValid) {
            super(attrName, attrClass, isNullValid);
            this.value = value;
        }

        static ElementAttributePredicate getEQPredicate(String attrName, Class attrClass, Float value) {
            return new FloatPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Float) vertexAttrVal).compareTo(value) == 0;
                }
            };
        }

        static ElementAttributePredicate getNEQPredicate(String attrName, Class attrClass, Float value) {
            return new FloatPredicate(attrName, attrClass, value, true) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return vertexAttrVal == null || ((Float) vertexAttrVal).compareTo(value) != 0;
                }
            };
        }

        static ElementAttributePredicate getLTPredicate(String attrName, Class attrClass, Float value) {
            return new FloatPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Float) vertexAttrVal).compareTo(value) < 0;
                }
            };
        }

        static ElementAttributePredicate getLTEPredicate(String attrName, Class attrClass, Float value) {
            return new FloatPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Float) vertexAttrVal).compareTo(value) <= 0;
                }
            };
        }

        static ElementAttributePredicate getGTPredicate(String attrName, Class attrClass, Float value) {
            return new FloatPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Float) vertexAttrVal).compareTo(value) > 0;
                }
            };
        }

        static ElementAttributePredicate getGTEPredicate(String attrName, Class attrClass, Float value) {
            return new FloatPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Float) vertexAttrVal).compareTo(value) >= 0;
                }
            };
        }
    }

    static abstract class DoublePredicate extends ElementAttributePredicate {
        final Double value;

        DoublePredicate(String attrName, Class attrClass, Double value) {
            super(attrName, attrClass);

            this.value = value;
        }

        DoublePredicate(String attrName, Class attrClass, Double value, boolean isNullValid) {
            super(attrName, attrClass, isNullValid);
            this.value = value;
        }

        static ElementAttributePredicate getEQPredicate(String attrName, Class attrClass, Double value) {
            return new DoublePredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Double) vertexAttrVal).compareTo(value) == 0;
                }
            };
        }

        static ElementAttributePredicate getNEQPredicate(String attrName, Class attrClass, Double value) {
            return new DoublePredicate(attrName, attrClass, value, true) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return vertexAttrVal == null || ((Double) vertexAttrVal).compareTo(value) != 0;
                }
            };
        }

        static ElementAttributePredicate getLTPredicate(String attrName, Class attrClass, Double value) {
            return new DoublePredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Double) vertexAttrVal).compareTo(value) < 0;
                }
            };
        }

        static ElementAttributePredicate getLTEPredicate(String attrName, Class attrClass, Double value) {
            return new DoublePredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Double) vertexAttrVal).compareTo(value) <= 0;
                }
            };
        }

        static ElementAttributePredicate getGTPredicate(String attrName, Class attrClass, Double value) {
            return new DoublePredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Double) vertexAttrVal).compareTo(value) > 0;
                }
            };
        }

        static ElementAttributePredicate getGTEPredicate(String attrName, Class attrClass, Double value) {
            return new DoublePredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Double) vertexAttrVal).compareTo(value) >= 0;
                }
            };
        }
    }

    static abstract class BytePredicate extends ElementAttributePredicate {
        final Byte value;

        BytePredicate(String attrName, Class attrClass, Byte value) {
            super(attrName, attrClass);

            this.value = value;
        }

        BytePredicate(String attrName, Class attrClass, Byte value, boolean isNullValid) {
            super(attrName, attrClass, isNullValid);
            this.value = value;
        }

        static ElementAttributePredicate getEQPredicate(String attrName, Class attrClass, Byte value) {
            return new BytePredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Byte) vertexAttrVal).compareTo(value) == 0;
                }
            };
        }

        static ElementAttributePredicate getNEQPredicate(String attrName, Class attrClass, Byte value) {
            return new BytePredicate(attrName, attrClass, value, true) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return vertexAttrVal == null || ((Byte) vertexAttrVal).compareTo(value) != 0;
                }
            };
        }

        static ElementAttributePredicate getLTPredicate(String attrName, Class attrClass, Byte value) {
            return new BytePredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Byte) vertexAttrVal).compareTo(value) < 0;
                }
            };
        }

        static ElementAttributePredicate getLTEPredicate(String attrName, Class attrClass, Byte value) {
            return new BytePredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Byte) vertexAttrVal).compareTo(value) <= 0;
                }
            };
        }

        static ElementAttributePredicate getGTPredicate(String attrName, Class attrClass, Byte value) {
            return new BytePredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Byte) vertexAttrVal).compareTo(value) > 0;
                }
            };
        }

        static ElementAttributePredicate getGTEPredicate(String attrName, Class attrClass, Byte value) {
            return new BytePredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((Byte) vertexAttrVal).compareTo(value) >= 0;
                }
            };
        }
    }

    static abstract class BigIntegerPredicate extends ElementAttributePredicate {
        final BigInteger value;

        BigIntegerPredicate(String attrName, Class attrClass, BigInteger value) {
            super(attrName, attrClass);

            this.value = value;
        }

        BigIntegerPredicate(String attrName, Class attrClass, BigInteger value, boolean isNullValid) {
            super(attrName, attrClass, isNullValid);
            this.value = value;
        }

        static ElementAttributePredicate getEQPredicate(String attrName, Class attrClass, BigInteger value) {
            return new BigIntegerPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((BigInteger) vertexAttrVal).compareTo(value) == 0;
                }
            };
        }

        static ElementAttributePredicate getNEQPredicate(String attrName, Class attrClass, BigInteger value) {
            return new BigIntegerPredicate(attrName, attrClass, value, true) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return vertexAttrVal == null || ((BigInteger) vertexAttrVal).compareTo(value) != 0;
                }
            };
        }

        static ElementAttributePredicate getLTPredicate(String attrName, Class attrClass, BigInteger value) {
            return new BigIntegerPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((BigInteger) vertexAttrVal).compareTo(value) < 0;
                }
            };
        }

        static ElementAttributePredicate getLTEPredicate(String attrName, Class attrClass, BigInteger value) {
            return new BigIntegerPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((BigInteger) vertexAttrVal).compareTo(value) <= 0;
                }
            };
        }

        static ElementAttributePredicate getGTPredicate(String attrName, Class attrClass, BigInteger value) {
            return new BigIntegerPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((BigInteger) vertexAttrVal).compareTo(value) > 0;
                }
            };
        }

        static ElementAttributePredicate getGTEPredicate(String attrName, Class attrClass, BigInteger value) {
            return new BigIntegerPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((BigInteger) vertexAttrVal).compareTo(value) >= 0;
                }
            };
        }
    }

    static abstract class BigDecimalPredicate extends ElementAttributePredicate {
        final BigDecimal value;

        BigDecimalPredicate(String attrName, Class attrClass, BigDecimal value) {
            super(attrName, attrClass);

            this.value = value;
        }

        BigDecimalPredicate(String attrName, Class attrClass, BigDecimal value, boolean isNullValid) {
            super(attrName, attrClass, true);
            this.value = value;
        }

        static ElementAttributePredicate getEQPredicate(String attrName, Class attrClass, BigDecimal value) {
            return new BigDecimalPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((BigDecimal) vertexAttrVal).compareTo(value) == 0;
                }
            };
        }

        static ElementAttributePredicate getNEQPredicate(String attrName, Class attrClass, BigDecimal value) {
            return new BigDecimalPredicate(attrName, attrClass, value, true) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return vertexAttrVal == null || ((BigDecimal) vertexAttrVal).compareTo(value) != 0;
                }
            };
        }

        static ElementAttributePredicate getLTPredicate(String attrName, Class attrClass, BigDecimal value) {
            return new BigDecimalPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((BigDecimal) vertexAttrVal).compareTo(value) < 0;
                }
            };
        }

        static ElementAttributePredicate getLTEPredicate(String attrName, Class attrClass, BigDecimal value) {
            return new BigDecimalPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((BigDecimal) vertexAttrVal).compareTo(value) <= 0;
                }
            };
        }

        static ElementAttributePredicate getGTPredicate(String attrName, Class attrClass, BigDecimal value) {
            return new BigDecimalPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((BigDecimal) vertexAttrVal).compareTo(value) > 0;
                }
            };
        }

        static ElementAttributePredicate getGTEPredicate(String attrName, Class attrClass, BigDecimal value) {
            return new BigDecimalPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((BigDecimal) vertexAttrVal).compareTo(value) >= 0;
                }
            };
        }
    }

    static abstract class StringPredicate extends ElementAttributePredicate {
        final String value;

        StringPredicate(String attrName, Class attrClass, String value) {
            super(attrName, attrClass);

            this.value = value;
        }

        StringPredicate(String attrName, Class attrClass, String value, boolean isNullValid) {
            super(attrName, attrClass, isNullValid);
            this.value = value;
        }

        static ElementAttributePredicate getEQPredicate(String attrName, Class attrClass, String value) {
            return new StringPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((String) vertexAttrVal).compareTo(value) == 0;
                }
            };
        }

        static ElementAttributePredicate getNEQPredicate(String attrName, Class attrClass, String value) {
            return new StringPredicate(attrName, attrClass, value, true) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return vertexAttrVal == null || ((String) vertexAttrVal).compareTo(value) != 0;
                }
            };
        }

        static ElementAttributePredicate getLTPredicate(String attrName, Class attrClass, String value) {
            return new StringPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((String) vertexAttrVal).compareTo(value) < 0;
                }
            };
        }

        static ElementAttributePredicate getLTEPredicate(String attrName, Class attrClass, String value) {
            return new StringPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((String) vertexAttrVal).compareTo(value) <= 0;
                }
            };
        }

        static ElementAttributePredicate getGTPredicate(String attrName, Class attrClass, String value) {
            return new StringPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((String) vertexAttrVal).compareTo(value) > 0;
                }
            };
        }

        static ElementAttributePredicate getGTEPredicate(String attrName, Class attrClass, String value) {
            return new StringPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((String) vertexAttrVal).compareTo(value) >= 0;
                }
            };
        }

        static ElementAttributePredicate getContainsPredicate(String attrName, Class attrClass, String value) {
            return new StringPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((String) vertexAttrVal).contains(value);
                }
            };
        }

        static ElementAttributePredicate getNotContainsPredicate(String attrName, Class attrClass, String value) {
            return new StringPredicate(attrName, attrClass, value, true) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return vertexAttrVal == null || !((String) vertexAttrVal).contains(value);
                }
            };
        }

        static ElementAttributePredicate getStartsWithPredicate(String attrName, Class attrClass, String value) {
            return new StringPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((String) vertexAttrVal).startsWith(value);
                }
            };
        }

        static ElementAttributePredicate getEndsWithPredicate(String attrName, Class attrClass, String value) {
            return new StringPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    return ((String) vertexAttrVal).endsWith(value);
                }
            };
        }

        static ElementAttributePredicate getRegexPredicate(String attrName, Class attrClass, String value) {
            return new StringPredicate(attrName, attrClass, value) {
                protected boolean compareValue(Object vertexAttrVal) {
                    Pattern pattern = Pattern.compile(value, Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher((String) vertexAttrVal);
                    return matcher.matches();
                }
            };
        }
    }

    public static Predicate generateIsEntityVertexPredicate(AtlasTypeRegistry typeRegistry) {
        return new IsEntityVertexPredicate(typeRegistry);
    }


    static class IsEntityVertexPredicate implements Predicate {
        final AtlasTypeRegistry typeRegistry;


        public IsEntityVertexPredicate(AtlasTypeRegistry typeRegistry) {
            this.typeRegistry = typeRegistry;
        }

        @Override
        public boolean evaluate(final Object object) {
            final boolean ret;

            AtlasVertex vertex = (object instanceof AtlasVertex) ? (AtlasVertex) object : null;

            if (vertex != null) {
                String typeName            = AtlasGraphUtilsV2.getTypeName(vertex);
                AtlasEntityType entityType = typeRegistry.getEntityTypeByName(typeName);

                ret = entityType != null && !entityType.isInternalType();
            } else {
                ret = false;
            }

            return ret;
        }

    }

}
