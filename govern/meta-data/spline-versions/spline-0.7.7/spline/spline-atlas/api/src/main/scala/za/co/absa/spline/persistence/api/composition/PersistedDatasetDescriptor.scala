package za.co.absa.spline.persistence.api.composition

import java.util.UUID

/**
  * The case class represents a basic descriptor containing all necessary information to identify a persisted dataset.
  *
  * @param datasetId An unique identifier of the dataset
  * @param appId     An ID of the Spark application that produced this dataset
  * @param appName   A name of the Spark application that produced this dataset
  * @param path      Persisted dataset (file) URL
  * @param timestamp UNIX timestamp (in millis) when the dataset has been produced
  */
case class PersistedDatasetDescriptor
(
  datasetId: UUID,
  appId: String,
  appName: String,
  path: String,
  timestamp: Long
)
