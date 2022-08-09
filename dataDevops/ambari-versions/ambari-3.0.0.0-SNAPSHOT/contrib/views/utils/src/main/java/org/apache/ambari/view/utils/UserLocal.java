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

package org.apache.ambari.view.utils;

import org.apache.ambari.view.ViewContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages end user specific objects.
 * Ensures that the instance of specific class is exists only in one instance for
 * specific user of specific instance.
 * @param <T> user-local class
 */
public class UserLocal<T> {
  private final static Logger LOG =
    LoggerFactory.getLogger(UserLocal.class);

  private final static Map<Class, Map<String, Object>> viewSingletonObjects = new ConcurrentHashMap<>();
  private final Class<? extends T> tClass;
  private final static Map<String,ReentrantLock> locks = new HashMap<>();

  public UserLocal(Class<? extends T> tClass) {
    this.tClass =tClass;
  }

  /**
   * Initial value of user-local class. Value can be set either by initialValue() on first get() call,
   * or directly by calling set() method. Default initial value is null, it can be changed by overriding
   * this method.
   * @param context initial value usually based on user properties provided by View Context
   * @return initial value of user-local variable
   */
  protected synchronized T initialValue(ViewContext context) {
    return null;
  }

  /**
   * gives the lock object for the specified key. creates if not yet created.
   * @param key : the key for which the lock is required.
   * @return : ReentrantLock for that key
   */
  private static ReentrantLock getLockFor(String key){
    LOG.info("Finding lock for : {}", key);
    if( null == locks.get(key)){
      LOG.info("Lock not found for {} ",key);
      synchronized (locks){
        if(null == locks.get(key)){
          LOG.info("Creating lock for {} ", key);
          locks.put(key,new ReentrantLock());
        }
      }
    }

    return locks.get(key);
  }

  /**
   * Returns user-local instance.
   * If instance of class is not present yet for user, calls initialValue to create it.
   * @param context View context that provides instance and user names.
   * @return instance
   */
  public T get(ViewContext context) {
    if (!viewSingletonObjects.containsKey(tClass)) {
      synchronized (viewSingletonObjects) {
        if (!viewSingletonObjects.containsKey(tClass)) {
          viewSingletonObjects.put(tClass, new ConcurrentHashMap<String, Object>());
        }
      }
    }

    Map<String, Object> instances = viewSingletonObjects.get(tClass);

    String key = getTagName(context);

    LOG.debug("looking for key : {}", key);
    if (!instances.containsKey(key)) {
      String lockKey = tClass.getName() + "_" + key;
      LOG.info("key {} not found. getting lock for {}", key,lockKey);

      ReentrantLock lock = getLockFor(lockKey);
      boolean gotLock = lock.tryLock();

      if( !gotLock ){
        LOG.error("Lock could not be obtained for {}. Throwing exception.",lockKey);
        throw new RuntimeException(String.format("Failed to initialize %s for %s. Try Again.", tClass.getName(), key));
      }
      else {
        try {
          T initValue = initialValue(context);
          LOG.info("Obtained initial value : {} for key : {}",initValue,key);
          instances.put(key, initValue);
        }finally{
          lock.unlock();
        }
      }
    }

    return (T) instances.get(key);
  }

  /**
   * Method for directly setting user-local singleton instances.
   * @param obj new variable value for current user
   * @param context ViewContext that provides username and instance name
   */
  public void set(T obj, ViewContext context) {
    if (!viewSingletonObjects.containsKey(tClass)) {
      synchronized (viewSingletonObjects) {
        if (!viewSingletonObjects.containsKey(tClass)) {
          viewSingletonObjects.put(tClass, new ConcurrentHashMap<String, Object>());
        }
      }
    }
    String key = getTagName(context);
    LOG.info("setting key : value {} : {}", key,obj );
    Map<String, Object> instances = viewSingletonObjects.get(tClass);
    instances.put(key, obj);
  }

  /**
   * Remove instance if it's already exists.
   * @param context ViewContext that provides username and instance name
   */
  public void remove(ViewContext context) {
    String key = getTagName(context);
    LOG.info("removing key : {}", key);
    Map<String, Object> instances = viewSingletonObjects.get(tClass);
    if( null != instances ){
      instances.remove(key);
    }
  }

  /**
   * Returns unique key for Map to store a user-local variable.
   * @param context ViewContext
   * @return Unique identifier of pair instance-user.
   */
  private String getTagName(ViewContext context) {
    if (context == null) {
      return "<null>";
    }
    return String.format("%s:%s", context.getInstanceName(), context.getUsername());
  }

  /**
   * Method for testing purposes, intended to clear the cached user-local instances.
   * Method should not normally be called from production code.
   * @param tClass classname instances of which should be dropped
   */
  public static void dropAllConnections(Class tClass) {
    LOG.info("removing all {} " ,tClass.getName());
    Map<String, Object> instances = viewSingletonObjects.get(tClass);
    if (instances != null) {
      instances.clear();
    }
  }

  /**
   * Method for testing purposes, intended to clear the cached user-local instances.
   * Drops all classes of user-local variables.
   * Method should not normally be called from production code.
   */
  public static void dropAllConnections() {
      LOG.info("clearing all viewSingletonObjects.");
      viewSingletonObjects.clear();
  }

  /**
   *
   * Drops all objects for give instance name.
   *
   * @param instanceName : the name of the view instance for which the keys needs to be dropped.
   */
  public static void dropInstanceCache(String instanceName){
    LOG.info("removing all the keys for instanceName : {}", instanceName);
    for(Map<String,Object> cache : viewSingletonObjects.values()){
      for(Iterator<Map.Entry<String, Object>> it = cache.entrySet().iterator();it.hasNext();){
        Map.Entry<String, Object> entry = it.next();
        if(entry.getKey().startsWith(instanceName+":")){
          LOG.debug("removing key : {} ",entry.getKey());
          it.remove();
        }
      }
    }
  }
}
