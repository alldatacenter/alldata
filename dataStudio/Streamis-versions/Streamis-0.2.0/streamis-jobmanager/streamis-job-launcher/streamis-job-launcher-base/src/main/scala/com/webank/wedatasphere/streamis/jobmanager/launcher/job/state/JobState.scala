package com.webank.wedatasphere.streamis.jobmanager.launcher.job.state

import java.net.URI

/**
 * Job state
 */
trait JobState {

    /**
     * Job state id
     * @return
     */
    def getId: String

    /**
     * location
     * @return
     */
    def getLocation: URI

    /**
     * Metadata info
     * @return
     */
    def getMetadataInfo: Any

    /**
     * Timestamp to save the state
     * @return
     */
    def getTimestamp: Long
}
