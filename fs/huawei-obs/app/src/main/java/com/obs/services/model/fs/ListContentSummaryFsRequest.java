/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model.fs;

import com.obs.services.model.GenericRequest;

import java.util.List;

/**
 * Parameters in a request for obtaining fs folder contentSummary
 *
 */
public class ListContentSummaryFsRequest extends GenericRequest {
    private int maxKeys;

    private List<DirLayer> dirLayers;

    public int getMaxKeys() {
        return maxKeys;
    }

    public void setMaxKeys(int maxKeys) {
        this.maxKeys = maxKeys;
    }

    public List<DirLayer> getDirLayers() {
        return dirLayers;
    }

    public void setDirLayers(List<DirLayer> dirLayers) {
        this.dirLayers = dirLayers;
    }

    static public class DirLayer {
        private String key;
        private String marker;
        private long inode;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getMarker() {
            return marker;
        }

        public void setMarker(String marker) {
            this.marker = marker;
        }

        public long getInode() {
            return inode;
        }

        public void setInode(long inode) {
            this.inode = inode;
        }

        @Override
        public String toString() {
            return "DirLayer{" +
                    "key='" + key + '\'' +
                    ", marker='" + marker + '\'' +
                    ", inode=" + inode +
                    '}';
        }
    }
}
