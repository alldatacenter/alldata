package za.co.absa.spline.persistence.atlas.conversion

import java.util.UUID

import org.apache.atlas.v1.model.instance.Id
import za.co.absa.spline.persistence.api.{composition => splineModel}
import za.co.absa.spline.persistence.atlas.{model => atlasModel}

/**
 * The object is responsible for conversion of [[Attribute Spline attributes]] to [[za.co.absa.spline.persistence.atlas.model.Attribute Atlas attributes]].
 */
object AttributeConverter {

  /**
   * The method converts [[Attribute Spline attributes]] to [[za.co.absa.spline.persistence.atlas.model.Attribute Atlas attributes]].
   * @param splineAttributes Spline attributes that will be converted
   * @param dataTypeIdAnNameMap A mapping from Spline data type ids to ids assigned by Atlas API and attribute names.
   * @return Atlas attributes
   */
  def convert(splineAttributes : Seq[splineModel.Attribute], dataTypeIdAnNameMap: Map[UUID, (Id, String)]): Seq[atlasModel.Attribute] = splineAttributes.map{
    case splineModel.Attribute(id, name, dataTypeId) => new atlasModel.Attribute(name, id, dataTypeIdAnNameMap(dataTypeId))
  }
}