package za.co.absa.spline.persistence.api.composition

import org.slf4s.Logging
import za.co.absa.spline.persistence.api.DataLineageWriter

import scala.concurrent.{ExecutionContext, Future}

/**
  * The class represents a parallel composite writer to various persistence layers for the DataLineage entity.
  *
  * @param writers a set of internal writers specific to particular  persistence layers
  */
class ParallelCompositeDataLineageWriter(writers: Seq[DataLineageWriter]) extends DataLineageWriter with Logging {

  /**
    * The method stores a particular data lineage to the underlying persistence layers.
    *
    * @param lineage A data lineage that will be stored
    */
  override def store(lineage: DataLineage)(implicit ec: ExecutionContext): Future[Unit] = {
    log debug s"Calling underlying writers (${writers.length})"
    val futures = for (w <- writers) yield w.store(lineage)
    Future.sequence(futures).map(_ => Unit)
  }

}
