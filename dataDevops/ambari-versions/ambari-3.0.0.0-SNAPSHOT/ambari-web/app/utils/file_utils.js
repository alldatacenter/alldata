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

  fileTypeMap: {
    csv: 'text/csv',
    json: 'application/json'
  },

  /**
   * download text file
   * @param data {String}
   * @param fileType {String}
   * @param fileName {String}
   */
  downloadTextFile: function (data, fileType, fileName) {
    if ($.browser.msie && $.browser.version < 10) {
      this.openInfoInNewTab(data);
    } else if (typeof safari !== 'undefined') {
      this.safariDownload(data, fileType, fileName);
    } else {
      try {
        var blob = new Blob([data], {
          type: (this.fileTypeMap[fileType] || 'text/' + fileType) + ';charset=utf-8;'
        });
        saveAs(blob, fileName);
      } catch (e) {
        this.openInfoInNewTab(data);
      }
    }
  },

  /**
   * download multiple files archived in ZIP
   * @param {object[]} files
   */
  downloadFilesInZip: function(files) {
    const zip = new JSZip();
    for (const file of files) {
      const blob = new Blob([file.data], {
        type: (this.fileTypeMap[file.type] || 'text/' + file.type) + ';charset=utf-8;'
      });
      zip.file(file.name, blob);
    }
    zip.generateAsync({type:"blob"})
      .then(function(content) {
        saveAs(content, "blueprint.zip");
      });
  },

  /**
   * open content of text file in new window
   * @param data {String}
   */
  openInfoInNewTab: function (data) {
    var newWindow = window.open('');
    var newDocument = newWindow.document;
    newDocument.write(data);
    newWindow.focus();
  },

  /**
   * Hack to dowload text data in Safari
   * @param data {String}
   * @param fileType {String}
   * @param fileName {String}
   */
  safariDownload: function (data, fileType, fileName) {
    var file = 'data:attachment/' + fileType + ';charset=utf-8,' + encodeURI(data);
    var linkEl = document.createElement("a");
    linkEl.href = file;
    linkEl.download = fileName;

    document.body.appendChild(linkEl);
    linkEl.click();
    document.body.removeChild(linkEl);
  },

  fileNameFromPath: function(path) {
    return path.split('/').slice(-1);
  }

};
