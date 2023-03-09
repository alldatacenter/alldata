/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.job.factory;

import static org.apache.griffin.core.exception.GriffinExceptionMessage.PREDICATE_TYPE_NOT_FOUND;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.griffin.core.exception.GriffinException;
import org.apache.griffin.core.job.FileExistPredicator;
import org.apache.griffin.core.job.Predicator;
import org.apache.griffin.core.job.entity.SegmentPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PredicatorFactory {
    private static final Logger LOGGER = LoggerFactory
        .getLogger(PredicatorFactory.class);

    public static Predicator newPredicateInstance(SegmentPredicate segPredicate) {
        Predicator predicate;
        switch (segPredicate.getType()) {
            case "file.exist":
                predicate = new FileExistPredicator(segPredicate);
                break;
            case "custom":
                predicate = getPredicateBean(segPredicate);
                break;
            default:
                throw new GriffinException.NotFoundException(PREDICATE_TYPE_NOT_FOUND);
        }
        return predicate;
    }

    private static Predicator getPredicateBean(SegmentPredicate segmentPredicate) {
        Predicator predicate;
        String predicateClassName = (String) segmentPredicate.getConfigMap().get("class");
        try {
            Class clazz = Class.forName(predicateClassName);
            Constructor<Predicator> constructor = clazz.getConstructor(SegmentPredicate.class);
            predicate = constructor.newInstance(segmentPredicate);
        } catch (ClassNotFoundException e) {
            String message = "There is no predicate type that you input.";
            LOGGER.error(message, e);
            throw new GriffinException.ServiceException(message, e);
        } catch (NoSuchMethodException e) {
            String message = "For predicate with type " + predicateClassName +
                " constructor with parameter of type " + SegmentPredicate.class.getName() + " not found";
            LOGGER.error(message, e);
            throw new GriffinException.ServiceException(message, e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            String message = "Error creating predicate bean";
            LOGGER.error(message, e);
            throw new GriffinException.ServiceException(message, e);
        }
        return predicate;
    }
}
