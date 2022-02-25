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

module.exports = {
  /**
   * Divide source array in chunks, then push each chunk into destination array
   * with delay time one by one. So then destination array gets more and more items
   * till all items are loaded.
   * @param options
   * options.initSize - number of items which will be pushed in array immediately
   * options.chunkSize - number of items which will be pushed after defined delay
   * options.delay - interval between each chunk push
   * options.destination - array where items will be pushed
   * options.source - source of items
   * options.context - the object that should know when data is completely loaded,
   * lazy loading will define "isLoaded" property for context object and update it
   * @return {Object} - instance of lazy loading run
   */
  run: function (options) {
    var initSize = options.initSize || 25,
      chunkSize = options.chunkSize || 50,
      delay = options.delay || 300,
      destination = options.destination,
      source = options.source,
      self = this,
      instance = {
        context: options.context,
        timeoutRef: null
      },
      chunks;
    if (Array.isArray(destination) && Array.isArray(source)) {
      destination.pushObjects(source.slice(0, initSize));
      if(source.length > initSize) {
        chunks = this.divideIntoChunks(source.slice(initSize, source.length), chunkSize);
        this.pushChunk(chunks, 0, delay, destination, instance);
      } else {
        instance.context.set('isLoaded', true);
      }
    }
    return instance;
  },

  /**
   * push chunks into destination array in delay time
   * @param chunks
   * @param index
   * @param delay
   * @param destination
   * @param instance
   */
  pushChunk: function (chunks, index, delay, destination, instance) {
    var self = this;
    instance.timeoutRef = setTimeout(function () {
      destination.pushObjects(chunks[index]);
      if (chunks.length === (++index)) {
        instance.context.set('isLoaded', true);
      } else {
        self.pushChunk(chunks, index, delay, destination, instance);
      }
    }, delay);
  },

  /**
   * divide source array into chunks
   * @param source
   * @param chunkSize
   * @return {Array}
   */
  divideIntoChunks: function (source, chunkSize) {
    var chunk = [];
    var chunks = [];
    var counter = 0;
    source.forEach(function (item) {
      counter++;
      chunk.push(item);
      if (counter === chunkSize) {
        chunks.push(chunk);
        chunk = [];
        counter = 0;
      }
    });
    if (chunk.length > 0) {
      chunks.push(chunk);
    }
    return chunks;
  },
  /**
   * terminate lazy loading run
   * @param instance - lazy loading instance
   */
  terminate: function (instance) {
    clearTimeout(instance.timeoutRef);
  }
};
