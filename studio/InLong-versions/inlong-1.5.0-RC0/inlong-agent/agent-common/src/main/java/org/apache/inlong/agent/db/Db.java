/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.agent.db;

import org.apache.inlong.common.db.CommandEntity;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.io.Closeable;
import java.util.List;

/**
 * local storage for key/value.
 */
public interface Db extends Closeable {

    abstract KeyValueEntity get(String key);

    /**
     * get command by command id
     */
    CommandEntity getCommand(String commandId);

    /**
     * put command entity in db
     */
    CommandEntity putCommand(CommandEntity entity);

    /**
     * store keyValue, if key has exists, throw exception.
     *
     * @param entity key/value
     * @throws NullPointerException key should not be null
     * @throws KeyAlreadyExistsException key already exists
     */
    void set(KeyValueEntity entity);

    /**
     * store keyValue, if key has exists, overwrite it.
     *
     * @param entity key/value
     * @return null or old value which is overwritten.
     * @throws NullPointerException key should not be null.
     */
    KeyValueEntity put(KeyValueEntity entity);

    /**
     * remove keyValue by key.
     *
     * @param key key
     * @return key/value
     * @throws NullPointerException key should not be null.
     */
    KeyValueEntity remove(String key);

    /**
     * search keyValue list by search key.
     *
     * @param searchKey search keys.
     * @return key/value list
     * @throws NullPointerException search key should not be null.
     */
    List<KeyValueEntity> search(StateSearchKey searchKey);

    /**
     * search keyValue list by search key.
     *
     * @param searchKeys search keys.
     * @return key/value list
     * @throws NullPointerException search key should not be null.
     */
    List<KeyValueEntity> search(List<StateSearchKey> searchKeys);

    /**
     * search keyValue list by search key.
     *
     * @param searchKey search keys.
     * @param keyPrefix key prefix.
     * @return key/value list
     * @throws NullPointerException search key should not be null.
     */
    List<KeyValueEntity> searchWithKeyPrefix(StateSearchKey searchKey, String keyPrefix);

    /**
     * search commands using ack status
     */
    List<CommandEntity> searchCommands(boolean isAcked);

    /**
     * search one keyValue by search key
     *
     * @param searchKey search key
     * @return null or keyValue
     */
    KeyValueEntity searchOne(StateSearchKey searchKey);

    /**
     * search one keyValue by fileName
     */
    KeyValueEntity searchOne(String fileName);

    /**
     * find all by prefix key.
     *
     * @param prefix prefix string
     * @return list of k/v
     */
    List<KeyValueEntity> findAll(String prefix);
}
