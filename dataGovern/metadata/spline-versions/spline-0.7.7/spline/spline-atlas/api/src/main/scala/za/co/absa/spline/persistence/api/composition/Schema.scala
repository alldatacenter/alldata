package za.co.absa.spline.persistence.api.composition

import java.util.UUID

/**
  * The case class represents a collection of attributes. See Attribute
  *
  * @param attrs An internal sequence of attributes (referred by attribute ID)
  */
case class Schema(attrs: Seq[UUID])
