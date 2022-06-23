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

package za.co.absa.spline.admin

import ch.qos.logback.classic.{Level, Logger}
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory
import scopt.{OptionDef, OptionParser}
import za.co.absa.spline.admin.AdminCLI.AdminCLIConfig
import za.co.absa.spline.common.ConsoleUtils._
import za.co.absa.spline.common.SplineBuildInfo
import za.co.absa.spline.common.scala13.Option
import za.co.absa.spline.common.security.TLSUtils
import za.co.absa.spline.persistence.AuxiliaryDBAction._
import za.co.absa.spline.persistence.OnDBExistsAction.{Drop, Fail, Skip}
import za.co.absa.spline.persistence.{ArangoConnectionURL, ArangoManagerFactory, ArangoManagerFactoryImpl}

import scala.concurrent.Await
import scala.concurrent.duration._

object AdminCLI extends App {

  import scala.concurrent.ExecutionContext.Implicits._

  case class AdminCLIConfig(
    cmd: Command = null,
    logLevel: Level = Level.INFO,
    disableSslValidation: Boolean = false,
  )

  implicit class OptionParserOps(val p: OptionParser[AdminCLIConfig]) extends AnyVal {
    def placeNewLine(): Unit = p.note("")

    def dbCommandOptions: Seq[OptionDef[_, AdminCLIConfig]] = Seq(
      p.arg[String]("<db_url>")
        required()
        text s"ArangoDB connection string in the format: ${ArangoConnectionURL.HumanReadableFormat}"
        action { case (url, c@AdminCLIConfig(cmd: DBCommand, _, _)) => c.copy(cmd.dbUrl = ArangoConnectionURL(url)) }
    )
  }

  private val dbManagerFactoryImpl = new ArangoManagerFactoryImpl()
  private val maybeConsole = InputConsole.systemConsoleIfAvailable()

  val dbManagerFactory = maybeConsole
    .map(console => new InteractiveArangoManagerFactoryProxy(dbManagerFactoryImpl, new UserInteractor(console)))
    .getOrElse(dbManagerFactoryImpl)

  new AdminCLI(dbManagerFactory).exec(args)
}

class AdminCLI(dbManagerFactory: ArangoManagerFactory) {

  def exec(args: Array[String]): Unit = {

    val cliParser: OptionParser[AdminCLIConfig] = new OptionParser[AdminCLIConfig](AppConfig.Spline.CLI.Executable) {

      import AdminCLI._

      head(
        s"""
           |Spline Admin Tool
           |Version: ${SplineBuildInfo.Version} (rev. ${SplineBuildInfo.Revision})
           |""".stripMargin
      )

      help("help").text("Print this usage text.")
      version('v', "version").text("Print version info.")

      {
        val logLevels = classOf[Level].getFields.collect { case f if f.getType == f.getDeclaringClass => f.getName }
        val logLevelsString = logLevels.mkString(", ")

        (opt[String]('l', "log-level")
          text s"Log level ($logLevelsString). Default is ${AdminCLIConfig().logLevel}."
          validate (l => if (logLevels.contains(l.toUpperCase)) success else failure(s"<log-level> should be one of: $logLevelsString"))
          action ((str, conf) => conf.copy(logLevel = Level.valueOf(str))))
      }

      // FIXME: Deprecated since Spline 0.6.1. To be removed in Spline 1.0.0 - https://github.com/AbsaOSS/spline/issues/906
      (opt[Unit]('k', "insecure")
        text s"Deprecated. See --disable-ssl-validation"
        action { case (_, conf) => conf.copy(disableSslValidation = true) })

      (opt[Unit]("disable-ssl-validation")
        text s"Disable validation of self-signed SSL certificates. (Don't use on production)."
        action { case (_, conf) => conf.copy(disableSslValidation = true) })

      this.placeNewLine()

      (cmd("db-init")
        action ((_, c) => c.copy(cmd = DBInit()))
        text "Initialize Spline database"
        children(
        opt[Unit]('f', "force")
          text "Re-create the database if one already exists."
          action { case (_, c@AdminCLIConfig(cmd: DBInit, _, _)) => c.copy(cmd.copy(force = true)) },
        opt[Unit]('s', "skip")
          text "Skip existing database. Don't throw error, just end."
          action { case (_, c@AdminCLIConfig(cmd: DBInit, _, _)) => c.copy(cmd.copy(skip = true)) })
        children (this.dbCommandOptions: _*)
        )

      this.placeNewLine()

      (cmd("db-upgrade")
        action ((_, c) => c.copy(cmd = DBUpgrade()))
        text "Upgrade Spline database"
        children (this.dbCommandOptions: _*))

      this.placeNewLine()

      (cmd("db-exec")
        action ((_, c) => c.copy(cmd = DBExec()))
        text "Auxiliary actions mainly intended for development, testing etc."
        children(
        opt[Unit]("check-access")
          text "Check access to the database"
          action { case (_, c@AdminCLIConfig(cmd: DBExec, _, _)) => c.copy(cmd.addAction(CheckDBAccess)) },
        opt[Unit]("foxx-reinstall")
          text "Reinstall Foxx services"
          action { case (_, c@AdminCLIConfig(cmd: DBExec, _, _)) => c.copy(cmd.addAction(FoxxReinstall)) },
        opt[Unit]("indices-delete")
          text "Delete indices"
          action { case (_, c@AdminCLIConfig(cmd: DBExec, _, _)) => c.copy(cmd.addAction(IndicesDelete)) },
        opt[Unit]("indices-create")
          text "Create indices"
          action { case (_, c@AdminCLIConfig(cmd: DBExec, _, _)) => c.copy(cmd.addAction(IndicesCreate)) },
        opt[Unit]("views-delete")
          text "Delete views"
          action { case (_, c@AdminCLIConfig(cmd: DBExec, _, _)) => c.copy(cmd.addAction(ViewsDelete)) },
        opt[Unit]("views-create")
          text "Create views"
          action { case (_, c@AdminCLIConfig(cmd: DBExec, _, _)) => c.copy(cmd.addAction(ViewsCreate)) })
        children (this.dbCommandOptions: _*)
        )

      checkConfig {
        case AdminCLIConfig(null, _, _) =>
          failure("No command given")
        case AdminCLIConfig(cmd: DBCommand, _, _) if cmd.dbUrl == null =>
          failure("DB connection string is required")
        case AdminCLIConfig(cmd: DBInit, _, _) if cmd.force && cmd.skip =>
          failure("Options '--force' and '--skip' cannot be used together")
        case _ =>
          success
      }
    }

    val conf = cliParser
      .parse(args, AdminCLIConfig())
      .getOrElse(sys.exit(1))

    LoggerFactory
      .getLogger(ROOT_LOGGER_NAME)
      .asInstanceOf[Logger]
      .setLevel(conf.logLevel)

    val sslCtxOpt = Option.when(conf.disableSslValidation)(TLSUtils.TrustingAllSSLContext)

    conf.cmd match {
      case DBInit(url, force, skip) =>
        val onExistsAction = (force, skip) match {
          case (true, false) => Drop
          case (false, true) => Skip
          case (false, false) => Fail
        }
        val dbManager = dbManagerFactory.create(url, sslCtxOpt)
        val wasInitialized = Await.result(dbManager.initialize(onExistsAction), Duration.Inf)
        if (!wasInitialized) println(ansi"%yellow{Skipped. DB is already initialized}")

      case DBUpgrade(url) =>
        val dbManager = dbManagerFactory.create(url, sslCtxOpt)
        Await.result(dbManager.upgrade(), Duration.Inf)

      case DBExec(url, actions) =>
        val dbManager = dbManagerFactory.create(url, sslCtxOpt)
        Await.result(dbManager.execute(actions: _*), Duration.Inf)
    }

    println(ansi"%green{DONE}")
  }
}
