/*
 * Copyright 2022 ABSA Group Limited
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

package za.co.absa.spline.agent

import org.apache.commons.configuration.{BaseConfiguration, Configuration, SubsetConfiguration}
import org.apache.spark.sql.SparkSession
import org.scalatest.OptionValues._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.commons.{HierarchicalObjectFactory, NamedEntity}
import za.co.absa.spline.agent.AgentBOMSpec.{MockLineageDispatcher, MockPostProcessingFilter, SimpleConfig, createBOM}
import za.co.absa.spline.agent.AgentConfig.ConfProperty
import za.co.absa.spline.harvester.conf.{SQLFailureCaptureMode, SplineMode}
import za.co.absa.spline.harvester.dispatcher.{CompositeLineageDispatcher, KafkaLineageDispatcher, LineageDispatcher}
import za.co.absa.spline.harvester.postprocessing.{AbstractPostProcessingFilter, CompositePostProcessingFilter}
import za.co.absa.spline.producer.model.{ExecutionEvent, ExecutionPlan}

class AgentBOMSpec
  extends AnyFlatSpec
    with Matchers {

  behavior of "AgentBOM"

  behavior of "splineMode"

  it should "fail on missing properties" in {
    val emptyBOM = createBOM(new SimpleConfig(), Seq(new SimpleConfig()))
    the[NoSuchElementException] thrownBy emptyBOM.splineMode should have message "Missing configuration property spline.mode"
    the[NoSuchElementException] thrownBy emptyBOM.execPlanUUIDVersion should have message "Missing configuration property spline.internal.execPlan.uuid.version"
    the[NoSuchElementException] thrownBy emptyBOM.sqlFailureCaptureMode should have message "Missing configuration property spline.sql.failure.capture"
    the[NoSuchElementException] thrownBy emptyBOM.iwdStrategy should have message "Missing configuration property spline.IWDStrategy"
    the[NoSuchElementException] thrownBy emptyBOM.lineageDispatcher should have message "Missing configuration property spline.lineageDispatcher"
  }

  it should "fail on missing config properties for wrapped dispatcher" in {
    val bom = createBOM(new SimpleConfig(), Seq(new SimpleConfig(
      ConfProperty.RootLineageDispatcher -> "composite",
      s"${ConfProperty.RootLineageDispatcher}.composite.className" -> classOf[CompositeLineageDispatcher].getName,
      s"${ConfProperty.RootLineageDispatcher}.composite.dispatchers" -> "myKafka",
      s"${ConfProperty.RootLineageDispatcher}.myKafka.className" -> classOf[KafkaLineageDispatcher].getName
    )))

    (the[NoSuchElementException] thrownBy bom.lineageDispatcher).getMessage should include("Missing configuration property spline.lineageDispatcher.myKafka")
  }

  it should "for non-composable components, return the first found value from the config with the highest precedence" in {
    val bom = createBOM(new SimpleConfig(), Seq(
      new SimpleConfig(ConfProperty.Mode -> "BEST_EFFORT", ConfProperty.ExecPlanUUIDVersion -> "17"),
      new SimpleConfig(ConfProperty.Mode -> "DISABLED", ConfProperty.SQLFailureCaptureMode -> "ALL")
    ))
    bom.splineMode shouldBe SplineMode.BEST_EFFORT
    bom.sqlFailureCaptureMode shouldBe SQLFailureCaptureMode.ALL
    bom.execPlanUUIDVersion shouldBe 17
  }

  it should "for composable components, return a composite of all components aligned according to configs' precedence" in {
    val bom = createBOM(
      // Default config - should be ignored since other values are provided
      new SimpleConfig(
        ConfProperty.RootPostProcessingFilter -> "defaultPP",
        s"${ConfProperty.RootPostProcessingFilter}.defaultPP.className" -> classOf[CompositePostProcessingFilter].getName
      ),
      Seq(
        // Higher precedence config
        new SimpleConfig(
          // Alter dispatcher "user"
          s"${ConfProperty.RootLineageDispatcher}.user.propFoo" -> "overridden",
          // Define filter "admin"
          ConfProperty.RootPostProcessingFilter -> "admin",
          s"${ConfProperty.RootPostProcessingFilter}.admin.className" -> classOf[CompositePostProcessingFilter].getName,
          s"${ConfProperty.RootPostProcessingFilter}.admin.filters" -> "a,b",
          s"${ConfProperty.RootPostProcessingFilter}.a.className" -> classOf[MockPostProcessingFilter].getName,
          s"${ConfProperty.RootPostProcessingFilter}.b.className" -> classOf[MockPostProcessingFilter].getName
        ),

        // Lower precedence config
        new SimpleConfig(
          // Define dispatcher "user"
          ConfProperty.RootLineageDispatcher -> "user",
          s"${ConfProperty.RootLineageDispatcher}.user.className" -> classOf[MockLineageDispatcher].getName,
          s"${ConfProperty.RootLineageDispatcher}.user.propFoo" -> "original",
          // Define filter "user"
          ConfProperty.RootPostProcessingFilter -> "user",
          s"${ConfProperty.RootPostProcessingFilter}.user.className" -> classOf[MockPostProcessingFilter].getName
        )
      )
    )

    // Assert dispatcher chain
    bom.lineageDispatcher.name shouldBe "user"
    bom.lineageDispatcher shouldBe a[CompositeLineageDispatcher]
    bom.lineageDispatcher.asInstanceOf[CompositeLineageDispatcher].delegatees should have size 1
    bom.lineageDispatcher.asInstanceOf[CompositeLineageDispatcher].delegatees(0).name shouldBe "user"
    bom.lineageDispatcher.asInstanceOf[CompositeLineageDispatcher].delegatees(0) shouldBe a[MockLineageDispatcher]
    bom.lineageDispatcher.asInstanceOf[CompositeLineageDispatcher].delegatees(0).asInstanceOf[MockLineageDispatcher].foo shouldBe "overridden"

    // Assert filter chain
    bom.postProcessingFilter.value.name shouldBe "a, b, user"
    bom.postProcessingFilter.value shouldBe a[CompositePostProcessingFilter]
    bom.postProcessingFilter.value.asInstanceOf[CompositePostProcessingFilter].delegatees should have size 2
    bom.postProcessingFilter.value.asInstanceOf[CompositePostProcessingFilter].delegatees(0).name shouldBe "a, b"
    bom.postProcessingFilter.value.asInstanceOf[CompositePostProcessingFilter].delegatees(0) shouldBe a[CompositePostProcessingFilter]
    bom.postProcessingFilter.value.asInstanceOf[CompositePostProcessingFilter].delegatees(0).asInstanceOf[CompositePostProcessingFilter].delegatees should have size 2
    bom.postProcessingFilter.value.asInstanceOf[CompositePostProcessingFilter].delegatees(0).asInstanceOf[CompositePostProcessingFilter].delegatees(0).name shouldEqual "a"
    bom.postProcessingFilter.value.asInstanceOf[CompositePostProcessingFilter].delegatees(0).asInstanceOf[CompositePostProcessingFilter].delegatees(1).name shouldEqual "b"
    bom.postProcessingFilter.value.asInstanceOf[CompositePostProcessingFilter].delegatees(1).name shouldBe "user"
    bom.postProcessingFilter.value.asInstanceOf[CompositePostProcessingFilter].delegatees(1) shouldBe a[MockPostProcessingFilter]
  }

  it should "use default dispatcher when there is no other configured" in {
    val bom = createBOM(
      // Default config
      new SimpleConfig(
        ConfProperty.RootLineageDispatcher -> "defaultDispatcher",
        s"${ConfProperty.RootLineageDispatcher}.defaultDispatcher.className" -> classOf[MockLineageDispatcher].getName
      ),
      Seq.empty
    )

    // Assert dispatcher chain
    bom.lineageDispatcher.name shouldBe "defaultDispatcher"
    bom.lineageDispatcher shouldBe a[CompositeLineageDispatcher]
    bom.lineageDispatcher.asInstanceOf[CompositeLineageDispatcher].delegatees should have size 1
    bom.lineageDispatcher.asInstanceOf[CompositeLineageDispatcher].delegatees(0).name shouldBe "defaultDispatcher"
    bom.lineageDispatcher.asInstanceOf[CompositeLineageDispatcher].delegatees(0) shouldBe a[MockLineageDispatcher]
  }

  it should "not use default dispatcher when there is other configured" in {
    val bom = createBOM(
      // Default config
      new SimpleConfig(
        ConfProperty.RootLineageDispatcher -> "defaultDispatcher",
        s"${ConfProperty.RootLineageDispatcher}.defaultDispatcher.className" -> classOf[MockLineageDispatcher].getName
      ),
      Seq(
        new SimpleConfig(
          ConfProperty.RootLineageDispatcher -> "otherDispatcher",
          s"${ConfProperty.RootLineageDispatcher}.otherDispatcher.className" -> classOf[MockLineageDispatcher].getName
        )
      )
    )
    // Assert dispatcher chain
    bom.lineageDispatcher.name shouldBe "otherDispatcher"
    bom.lineageDispatcher shouldBe a[CompositeLineageDispatcher]
    bom.lineageDispatcher.asInstanceOf[CompositeLineageDispatcher].delegatees should have size 1
    bom.lineageDispatcher.asInstanceOf[CompositeLineageDispatcher].delegatees(0).name shouldBe "otherDispatcher"
    bom.lineageDispatcher.asInstanceOf[CompositeLineageDispatcher].delegatees(0) shouldBe a[MockLineageDispatcher]
  }

}

object AgentBOMSpec {

  import org.scalatestplus.mockito.MockitoSugar._

  def createBOM(defaultConf: Configuration, confs: Seq[Configuration]): AgentBOM =
    AgentBOM.createFrom(defaultConf, confs, mock[SparkSession])

  class SimpleConfig(entries: (String, Any)*) extends BaseConfiguration {
    entries.foreach { case (k, v) => setProperty(k, v) }
  }

  trait NamedByConfigPrefix extends NamedEntity {
    this: {val hof: HierarchicalObjectFactory} =>
    override val name: String = hof.configuration.asInstanceOf[SubsetConfiguration].getPrefix.split('.').last
  }

  class MockPostProcessingFilter(val hof: HierarchicalObjectFactory)
    extends AbstractPostProcessingFilter(null)
      with NamedByConfigPrefix

  class MockLineageDispatcher(val hof: HierarchicalObjectFactory)
    extends LineageDispatcher
      with NamedByConfigPrefix {

    val foo: String = hof.configuration.getString("propFoo")

    override def send(plan: ExecutionPlan): Unit = ()

    override def send(event: ExecutionEvent): Unit = ()
  }
}
