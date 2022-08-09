/*
 * Copyright 2019 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.harvester

import org.apache.spark.SPARK_VERSION
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.execution.QueryExecution
import org.apache.spark.sql.util.QueryExecutionListener
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, BeforeAndAfter, Succeeded}
import org.scalatestplus.mockito.MockitoSugar
import za.co.absa.commons.io.TempFile
import za.co.absa.commons.json.DefaultJacksonJsonSerDe
import za.co.absa.commons.scalatest.ConditionalTestTags._
import za.co.absa.commons.version.Version._
import za.co.absa.spline.harvester.SparkLineageInitializer._
import za.co.absa.spline.harvester.SparkLineageInitializerSpec._
import za.co.absa.spline.agent.AgentConfig.ConfProperty
import za.co.absa.spline.harvester.conf.{SQLFailureCaptureMode, SplineMode}
import za.co.absa.spline.harvester.dispatcher.LineageDispatcher
import za.co.absa.spline.harvester.exception.SplineInitializationException
import za.co.absa.spline.harvester.listener.SplineQueryExecutionListener
import za.co.absa.spline.producer.model.{ExecutionEvent, ExecutionPlan}
import za.co.absa.spline.test.fixture.{SparkFixture, SystemFixture}

import scala.concurrent.{Future, Promise}
import scala.util.Try

class SparkLineageInitializerSpec
  extends AsyncFlatSpec
    with BeforeAndAfter
    with Matchers
    with MockitoSugar
    with SparkFixture.NewPerTest
    with SystemFixture.IsolatedSystemPropertiesPerTest {

  before {
    sys.props.put(ConfProperty.RootLineageDispatcher, TestLineageDispatcherName)
    sys.props.put(ConfProperty.dispatcherClassName(TestLineageDispatcherName), classOf[MockLineageDispatcher].getName)
    MockLineageDispatcher.reset()
  }

  behavior of "codeless initialization"

  it should "ignore subsequent programmatic init" taggedAs ignoreIf(ver"$SPARK_VERSION" < ver"2.3") in {
    sys.props.put(SparkQueryExecutionListenersKey, classOf[SplineQueryExecutionListener].getName)
    withSparkSession { session =>
      session.enableLineageTracking()
      for (_ <- runSuccessfulDummySparkJob(session)) yield {
        MockLineageDispatcher.verifyTheOnlyLineageCaptured()
        MockLineageDispatcher.instanceCount should be(1)
      }
    }
  }

  it should "propagate to child sessions" taggedAs ignoreIf(ver"$SPARK_VERSION" < ver"2.3") in {
    sys.props.put(SparkQueryExecutionListenersKey, classOf[SplineQueryExecutionListener].getName)
    withSparkSession { session =>
      val subSession = session.newSession()
      for (_ <- runSuccessfulDummySparkJob(subSession)) yield {
        MockLineageDispatcher.verifyTheOnlyLineageCaptured()
      }
    }
  }

  behavior of "enableLineageTracking()"

  it should "warn on double initialization" taggedAs ignoreIf(ver"$SPARK_VERSION" < ver"2.3") in {
    withSparkSession { session =>
      session.enableLineageTracking() // 1st is fine
      MockLineageDispatcher.instanceCount should be(1)
      session.enableLineageTracking() // 2nd should warn
      MockLineageDispatcher.instanceCount should be(1)
    }
  }

  it should "allow user to start again after error" in {
    sys.props += ConfProperty.Mode -> SplineMode.BEST_EFFORT.toString

    withSparkSession { sparkSession =>
      for {
        _ <- {
          // first attempt
          // purposely set wrong dispatcher settings, that are going to cause Spline init to fail
          sys.props += ConfProperty.RootLineageDispatcher -> "wrong"
          sys.props += ConfProperty.dispatcherClassName("wrong") -> "wrong.dispatcher.class [this is a testing error - IGNORE]"

          sparkSession.enableLineageTracking()
          runSuccessfulDummySparkJob(sparkSession)
        }
        _ <- {
          // second attempt
          // now with correct settings
          sys.props.put(ConfProperty.RootLineageDispatcher, TestLineageDispatcherName)
          sys.props.put(ConfProperty.dispatcherClassName(TestLineageDispatcherName), classOf[MockLineageDispatcher].getName)

          sparkSession.enableLineageTracking()
          runSuccessfulDummySparkJob(sparkSession)
        }
      } yield {
        // we executed two Spark jobs, the second one with correct Spline settings, so the only (the 2nd job's one) lineage should be captured
        MockLineageDispatcher.verifyTheOnlyLineageCaptured()
      }
    }
  }

  it should "return the spark session back to the caller" in {
    withSparkSession(session =>
      session.enableLineageTracking() shouldBe session
    )
  }

  behavior of "Spline modes"

  it should "disable Spline and proceed after dispatcher init failure, when mode == BEST_EFFORT" in {
    sys.props += ConfProperty.Mode -> SplineMode.BEST_EFFORT.toString

    withNewSparkSession { sparkSession =>
      MockLineageDispatcher.onConstructionThrow(new SplineInitializationException("boom"))
      sparkSession.enableLineageTracking()
      for (_ <- runSuccessfulDummySparkJob(sparkSession)) yield {
        MockLineageDispatcher.verifyNoLineageCaptured()
      }
    }
  }

  it should "abort application on Java exception, when mode == REQUIRED" in {
    sys.props += ConfProperty.Mode -> SplineMode.REQUIRED.toString
    sys.props += ConfProperty.RootLineageDispatcher -> "wrong"
    sys.props += ConfProperty.dispatcherClassName("wrong") -> "wrong.dispatcher.class"

    the[ClassNotFoundException] thrownBy {
      withSparkSession(_.enableLineageTracking())
    } should have message "wrong.dispatcher.class"
  }

  it should "abort application on Spline exception, when mode == REQUIRED" in {
    sys.props += ConfProperty.Mode -> SplineMode.REQUIRED.toString

    the[SplineInitializationException] thrownBy {
      MockLineageDispatcher.onConstructionThrow(new SplineInitializationException("boom"))
      withNewSparkSession(_.enableLineageTracking())
    } should have message "boom"
  }

  it should "not react on agent init failure, when mode == DISABLED" in {
    sys.props += ConfProperty.Mode -> SplineMode.DISABLED.toString

    withSparkSession { sparkSession =>
      MockLineageDispatcher.onConstructionThrow(new SplineInitializationException("boom"))
      sparkSession.enableLineageTracking()
      for (_ <- runSuccessfulDummySparkJob(sparkSession)) yield {
        MockLineageDispatcher.verifyNoLineageCaptured()
      }
    }
  }

  it should "have no effect, when mode == DISABLED" in {
    sys.props += ConfProperty.Mode -> SplineMode.DISABLED.toString

    withNewSparkSession { sparkSession =>
      sparkSession.enableLineageTracking()
      for (_ <- runSuccessfulDummySparkJob(sparkSession)) yield {
        MockLineageDispatcher.verifyNoLineageCaptured()
      }
    }
  }

  behavior of "SQL failure capture modes"

  for (mode <- SQLFailureCaptureMode.values) {
    it should s"capture successful execution in $mode mode" in {
      sys.props += ConfProperty.SQLFailureCaptureMode -> mode.toString

      withNewSparkSession { sparkSession =>
        sparkSession.enableLineageTracking()
        for (_ <- runSuccessfulDummySparkJob(sparkSession)) yield {
          MockLineageDispatcher.verifyTheOnlyLineageCaptured()
        }
      }
    }
  }

  for (mode <- Seq(SQLFailureCaptureMode.NON_FATAL, SQLFailureCaptureMode.ALL)) {
    it should s"capture failed execution in $mode mode" in {
      sys.props += ConfProperty.SQLFailureCaptureMode -> mode.toString

      withNewSparkSession { sparkSession =>
        sparkSession.enableLineageTracking()
        for (_ <- runFailingDummySparkJob(sparkSession)) yield {
          MockLineageDispatcher.verifyTheOnlyLineageCaptured()
        }
      }
    }
  }

  it should s"not capture failed execution in NONE mode" in {
    sys.props += ConfProperty.SQLFailureCaptureMode -> SQLFailureCaptureMode.NONE.toString

    withNewSparkSession { sparkSession =>
      sparkSession.enableLineageTracking()
      for (_ <- runFailingDummySparkJob(sparkSession)) yield {
        MockLineageDispatcher.verifyNoLineageCaptured()
      }
    }
  }
}

object SparkLineageInitializerSpec {

  private val TestLineageDispatcherName = "test"

  private class MockLineageDispatcher extends LineageDispatcher {

    MockLineageDispatcher.onConstruction()

    override def name = "Mock"

    override def send(plan: ExecutionPlan): Unit = MockLineageDispatcher.theMock.send(plan)

    override def send(event: ExecutionEvent): Unit = MockLineageDispatcher.theMock.send(event)
  }

  private object MockLineageDispatcher extends MockitoSugar with DefaultJacksonJsonSerDe {
    private val theMock: LineageDispatcher = mock[LineageDispatcher]
    private[this] var throwableOnConstruction: Option[_ <: Throwable] = None
    private[this] var _instanceCount: Int = _

    private def onConstruction(): Unit = {
      this.throwableOnConstruction.foreach(throw _)
      this._instanceCount += 1
    }

    def instanceCount: Int = this._instanceCount

    def reset(): Unit = {
      this._instanceCount = 0
      this.throwableOnConstruction = None
      Mockito.reset(theMock)
    }

    def onConstructionThrow(th: Throwable): Unit = {
      this.throwableOnConstruction = Some(th)
    }

    def verifyTheOnlyLineageCaptured(): Assertion = {
      verify(theMock, times(1)).send(any[ExecutionPlan]())
      verify(theMock, times(1)).send(any[ExecutionEvent]())
      Succeeded
    }

    def verifyNoLineageCaptured(): Assertion = {
      verify(theMock, never()).send(any[ExecutionPlan]())
      verify(theMock, never()).send(any[ExecutionEvent]())
      Succeeded
    }
  }

  private def runSuccessfulDummySparkJob(session: SparkSession): Future[Unit] = runDummySparkJob(session, succeed = true)

  private def runFailingDummySparkJob(session: SparkSession): Future[Unit] = runDummySparkJob(session, succeed = false)

  private def runDummySparkJob(session: SparkSession, succeed: Boolean): Future[Unit] = {
    val promise = Promise[Unit]

    session.listenerManager.register(new QueryExecutionListener {
      override def onSuccess(funcName: String, qe: QueryExecution, durationNs: Long): Unit = promise.success()

      override def onFailure(funcName: String, qe: QueryExecution, exception: Exception): Unit = promise.success()
    })

    import session.implicits._
    var ds = Seq(Foo(1, 2)).toDS
    if (!succeed) {
      ds = ds.map(_ => sys.error("test error"))
    }

    Try(ds.write.save(TempFile(pathOnly = true).deleteOnExit().path.toString))

    promise.future
  }

  case class Foo(a: Int, b: Int)
}
