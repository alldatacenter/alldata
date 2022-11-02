package za.co.absa.spline.persistence.api.composition

import java.util.UUID

/**
  * The case class represents a data set descriptor
  * @param id An unique identifier
  * @param schema A data set schema
  */
case class MetaDataset(id: UUID, schema: Schema)
