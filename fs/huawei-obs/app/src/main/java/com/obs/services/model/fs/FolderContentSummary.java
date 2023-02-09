package com.obs.services.model.fs;

import java.util.ArrayList;
import java.util.List;

public class FolderContentSummary {
    private String dir;
    private long dirHeight;
    private List<LayerSummary> layerSummaries;

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public long getDirHeight() {
        return dirHeight;
    }

    public void setDirHeight(long dirHeight) {
        this.dirHeight = dirHeight;
    }

    public List<LayerSummary> getLayerSummaries() {
        if (layerSummaries == null) {
            this.layerSummaries = new ArrayList<LayerSummary>();
        }
        return layerSummaries;
    }

    @Override
    public String toString() {
        return "FolderContentSummary{" + "dir='" + dir + '\'' + ", dirHeight=" + dirHeight + ", LayerSummaries="
                + layerSummaries + '}';
    }

    static public class LayerSummary {
        private long summaryHeight;
        private long dirCount;
        private long fileCount;
        private long fileSize;

        public long getSummaryHeight() {
            return summaryHeight;
        }

        public void setSummaryHeight(long summaryHeight) {
            this.summaryHeight = summaryHeight;
        }

        public long getDirCount() {
            return dirCount;
        }

        public void setDirCount(long dirCount) {
            this.dirCount = dirCount;
        }

        public long getFileCount() {
            return fileCount;
        }

        public void setFileCount(long fileCount) {
            this.fileCount = fileCount;
        }

        public long getFileSize() {
            return fileSize;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }

        @Override
        public String toString() {
            return "LayerSummary{" + "summaryHeight=" + summaryHeight + ", dirCount=" + dirCount + ", fileCount="
                    + fileCount + ", fileSize=" + fileSize + '}';
        }
    }
}
