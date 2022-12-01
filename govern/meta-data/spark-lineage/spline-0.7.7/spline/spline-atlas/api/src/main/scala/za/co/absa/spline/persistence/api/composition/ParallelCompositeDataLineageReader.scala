package za.co.absa.spline.persistence.api.composition

import java.util.UUID

import org.slf4s.Logging
import za.co.absa.spline.persistence.api.DataLineageReader.PageRequest
import za.co.absa.spline.persistence.api.{CloseableIterable, DataLineageReader}

import scala.concurrent.{ExecutionContext, Future}

/**
 * The class represents a parallel composite reader from various persistence layers for the [[DataLineage]] entity.
 *
 * @param readers a set of internal readers specific to particular persistence layers
 */
class ParallelCompositeDataLineageReader(readers: Seq[DataLineageReader]) extends DataLineageReader with Logging {
  /**
   * The method loads a particular data lineage from the persistence layer.
   *
   * @param dsId An unique identifier of a data lineage
   * @return A data lineage instance when there is a data lineage with a given id in the persistence layer, otherwise None
   */
  override def loadByDatasetId(dsId: UUID, overviewOnly: Boolean)(implicit ec: ExecutionContext): Future[Option[DataLineage]] =
    firstCompletedReader(_.loadByDatasetId(dsId, overviewOnly))

  /**
   * The method scans the persistence layer and tries to find a dataset ID for a given path and application ID.
   *
   * @param path          A path for which a dataset ID is looked for
   * @param applicationId An application for which a dataset ID is looked for
   * @return An identifier of a meta data set
   */
  override def searchDataset(path: String, applicationId: String)(implicit ec: ExecutionContext): Future[Option[UUID]] =
    firstCompletedReader(_.searchDataset(path, applicationId))

  /**
   * The method loads the latest data lineage from the persistence for a given path.
   *
   * @param path A path for which a lineage graph is looked for
   * @return The latest data lineage
   */
  override def findLatestDatasetIdsByPath(path: String)(implicit ec: ExecutionContext): Future[CloseableIterable[UUID]] = {
    firstCompletedReader(_.findLatestDatasetIdsByPath(path))
  }

  /**
   * The method loads composite operations for an input datasetId.
   *
   * @param datasetId A dataset ID for which the operation is looked for
   * @return Composite operations with dependencies satisfying the criteria
   */
  override def findByInputId(datasetId: UUID, overviewOnly: Boolean)(implicit ec: ExecutionContext): Future[CloseableIterable[DataLineage]] =
    firstCompletedReader(_.findByInputId(datasetId, overviewOnly))

  /**
   * The method gets all data lineages stored in persistence layer.
   *
   * @return Descriptors of all data lineages
   */
  override def findDatasets(text: Option[String], page: PageRequest)(implicit ec: ExecutionContext): Future[CloseableIterable[PersistedDatasetDescriptor]] =
    firstCompletedReader(_.findDatasets(text, page))

  /**
   * The method returns a dataset descriptor by its ID.
   *
   * @param id An unique identifier of a dataset
   * @return Descriptors of all data lineages
   */
  override def getDatasetDescriptor(id: UUID)(implicit ec: ExecutionContext): Future[PersistedDatasetDescriptor] =
    firstCompletedReader(_.getDatasetDescriptor(id))

  private def firstCompletedReader[T](query: DataLineageReader => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    Future.firstCompletedOf(readers.map(query))
  }
}
