/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.common.util

import java.io.{File, FileInputStream, InputStreamReader, IOException}
import java.lang.management.ManagementFactory
import java.math.{MathContext, RoundingMode}
import java.net._
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util
import java.util.{Locale, Properties, Random, UUID}
import java.util.concurrent.{Callable, ConcurrentHashMap, ThreadPoolExecutor, TimeoutException, TimeUnit}

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.util.Try
import scala.util.control.{ControlThrowable, NonFatal}

import com.google.common.net.InetAddresses
import com.google.protobuf.{ByteString, GeneratedMessageV3}
import io.netty.channel.unix.Errors.NativeIoException
import org.apache.commons.lang3.SystemUtils
import org.roaringbitmap.RoaringBitmap

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.exception.CelebornException
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.meta.{DiskStatus, FileInfo, WorkerInfo}
import org.apache.celeborn.common.network.protocol.TransportMessage
import org.apache.celeborn.common.network.util.TransportConf
import org.apache.celeborn.common.protocol.{PartitionLocation, PartitionSplitMode, PartitionType, TransportModuleConstants}
import org.apache.celeborn.common.protocol.message.{ControlMessages, Message, StatusCode}
import org.apache.celeborn.common.protocol.message.ControlMessages.WorkerResource

object Utils extends Logging {

  def createDateFormat: SimpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US)

  def stringToSeq(str: String): Seq[String] = {
    str.split(",").map(_.trim()).filter(_.nonEmpty)
  }

  def getSystemProperties: Map[String, String] = {
    System.getProperties.stringPropertyNames().asScala
      .map(key => (key, System.getProperty(key))).toMap
  }

  def timeStringAsMs(str: String): Long = {
    JavaUtils.timeStringAsMs(str)
  }

  def timeStringAsSeconds(str: String): Long = {
    JavaUtils.timeStringAsSec(str)
  }

  def byteStringAsBytes(str: String): Long = {
    JavaUtils.byteStringAsBytes(str)
  }

  def byteStringAsKb(str: String): Long = {
    JavaUtils.byteStringAsKb(str)
  }

  def byteStringAsMb(str: String): Long = {
    JavaUtils.byteStringAsMb(str)
  }

  def byteStringAsGb(str: String): Long = {
    JavaUtils.byteStringAsGb(str)
  }

  def memoryStringToMb(str: String): Int = {
    // Convert to bytes, rather than directly to MB, because when no units are specified the unit
    // is assumed to be bytes
    (JavaUtils.byteStringAsBytes(str) / 1024 / 1024).toInt
  }

  def bytesToString(size: Long): String = bytesToString(BigInt(size))

  def bytesToString(size: BigInt): String = {
    val EB = 1L << 60
    val PB = 1L << 50
    val TB = 1L << 40
    val GB = 1L << 30
    val MB = 1L << 20
    val KB = 1L << 10

    if (size >= BigInt(1L << 11) * EB) {
      // The number is too large, show it in scientific notation.
      BigDecimal(size, new MathContext(3, RoundingMode.HALF_UP)).toString() + " B"
    } else {
      val (value, unit) = {
        if (size >= 2 * EB) {
          (BigDecimal(size) / EB, "EB")
        } else if (size >= 2 * PB) {
          (BigDecimal(size) / PB, "PB")
        } else if (size >= 2 * TB) {
          (BigDecimal(size) / TB, "TB")
        } else if (size >= 2 * GB) {
          (BigDecimal(size) / GB, "GB")
        } else if (size >= 2 * MB) {
          (BigDecimal(size) / MB, "MB")
        } else if (size >= 2 * KB) {
          (BigDecimal(size) / KB, "KB")
        } else {
          (BigDecimal(size), "B")
        }
      }
      "%.1f %s".formatLocal(Locale.US, value, unit)
    }
  }

  def megabytesToString(megabytes: Long): String = {
    bytesToString(megabytes * 1024L * 1024L)
  }

  /**
   * Returns a human-readable string representing a duration such as "35ms"
   */
  def msDurationToString(ms: Long): String = {
    val second = 1000
    val minute = 60 * second
    val hour = 60 * minute
    val locale = Locale.US

    ms match {
      case t if t < second =>
        "%d ms".formatLocal(locale, t)
      case t if t < minute =>
        "%.1f s".formatLocal(locale, t.toFloat / second)
      case t if t < hour =>
        "%.1f m".formatLocal(locale, t.toFloat / minute)
      case t =>
        "%.2f h".formatLocal(locale, t.toFloat / hour)
    }
  }

  @throws(classOf[CelebornException])
  def extractHostPortFromRssUrl(essUrl: String): (String, Int) = {
    try {
      val uri = new java.net.URI(essUrl)
      val host = uri.getHost
      val port = uri.getPort
      if (uri.getScheme != "rss" ||
        host == null ||
        port < 0 ||
        (uri.getPath != null && !uri.getPath.isEmpty) || // uri.getPath returns "" instead of null
        uri.getFragment != null ||
        uri.getQuery != null ||
        uri.getUserInfo != null) {
        throw new CelebornException("Invalid master URL: " + essUrl)
      }
      (host, port)
    } catch {
      case e: java.net.URISyntaxException =>
        throw new CelebornException("Invalid master URL: " + essUrl, e)
    }
  }

  def tryOrIOException[T](block: => T): T = {
    try {
      block
    } catch {
      case e: IOException =>
        logError("Exception encountered", e)
        throw e
      case NonFatal(e) =>
        logError("Exception encountered", e)
        throw new IOException(e)
    }
  }

  def tryLogNonFatalError(block: => Unit) {
    try {
      block
    } catch {
      case NonFatal(t) =>
        logError(s"Uncaught exception in thread ${Thread.currentThread().getName}", t)
    }
  }

  def tryOrExit(block: => Unit) {
    try {
      block
    } catch {
      case e: ControlThrowable => throw e
      case t: Throwable => throw t
    }
  }

  def tryWithSafeFinallyAndFailureCallbacks[T](block: => T)(
      catchBlock: => Unit = (),
      finallyBlock: => Unit = ()): T = {
    var originalThrowable: Throwable = null
    try {
      block
    } catch {
      case cause: Throwable =>
        // Purposefully not using NonFatal, because even fatal exceptions
        // we don't want to have our finallyBlock suppress
        originalThrowable = cause
        try {
          logError("Aborting task", originalThrowable)
          //          TaskContext.get().markTaskFailed(originalThrowable)
          catchBlock
        } catch {
          case t: Throwable =>
            if (originalThrowable != t) {
              originalThrowable.addSuppressed(t)
              logWarning(s"Suppressing exception in catch: ${t.getMessage}", t)
            }
        }
        throw originalThrowable
    } finally {
      try {
        finallyBlock
      } catch {
        case t: Throwable if (originalThrowable != null && originalThrowable != t) =>
          originalThrowable.addSuppressed(t)
          logWarning(s"Suppressing exception in finally: ${t.getMessage}", t)
          throw originalThrowable
      }
    }
  }

  def startServiceOnPort[T](
      startPort: Int,
      startService: Int => (T, Int),
      conf: CelebornConf,
      serviceName: String = ""): (T, Int) = {

    require(
      startPort == 0 || (1024 <= startPort && startPort < 65536),
      "startPort should be between 1024 and 65535 (inclusive), or 0 for a random free port.")

    val serviceString = if (serviceName.isEmpty) "" else s" '$serviceName'"
    val maxRetries = conf.portMaxRetries
    for (offset <- 0 to maxRetries) {
      // Do not increment port if startPort is 0, which is treated as a special port
      val tryPort =
        if (startPort == 0) {
          startPort
        } else {
          userPort(startPort, offset)
        }
      try {
        val (service, port) = startService(tryPort)
        logInfo(s"Successfully started service$serviceString on port $port.")
        return (service, port)
      } catch {
        case e: Exception if isBindCollision(e) =>
          if (offset >= maxRetries) {
            val exceptionMessage =
              if (startPort == 0) {
                s"${e.getMessage}: Service$serviceString failed after " +
                  s"$maxRetries retries (on a random free port)! " +
                  s"Consider explicitly setting the appropriate binding address for " +
                  s"the service$serviceString (for example spark.driver.bindAddress " +
                  s"for SparkDriver) to the correct binding address."
              } else {
                s"${e.getMessage}: Service$serviceString failed after " +
                  s"$maxRetries retries (starting from $startPort)! Consider explicitly setting " +
                  s"the appropriate port for the service$serviceString (for example spark.ui.port " +
                  s"for SparkUI) to an available port or increasing spark.port.maxRetries."
              }
            val exception = new BindException(exceptionMessage)
            // restore original stack trace
            exception.setStackTrace(e.getStackTrace)
            throw exception
          }
          if (startPort == 0) {
            // As startPort 0 is for a random free port, it is most possibly binding address is
            // not correct.
            logWarning(s"Service$serviceString could not bind on a random free port. " +
              "You may check whether configuring an appropriate binding address.")
          } else {
            logWarning(s"Service$serviceString could not bind on port $tryPort. " +
              s"Attempting port ${tryPort + 1}.")
          }
      }
    }
    // Should never happen
    throw new CelebornException(s"Failed to start service$serviceString on port $startPort")
  }

  def userPort(base: Int, offset: Int): Int = {
    (base + offset - 1024) % (65536 - 1024) + 1024
  }

  def isBindCollision(exception: Throwable): Boolean = {
    exception match {
      case e: BindException =>
        if (e.getMessage != null) {
          return true
        }
        isBindCollision(e.getCause)
      //      case e: MultiException =>
      //        e.getThrowables.asScala.exists(isBindCollision)
      case e: NativeIoException =>
        (e.getMessage != null && e.getMessage.startsWith("bind() failed: ")) ||
          isBindCollision(e.getCause)
      case e: Exception => isBindCollision(e.getCause)
      case _ => false
    }
  }

  def encodeFileNameToURIRawPath(fileName: String): String = {
    require(!fileName.contains("/") && !fileName.contains("\\"))
    // `file` and `localhost` are not used. Just to prevent URI from parsing `fileName` as
    // scheme or host. The prefix "/" is required because URI doesn't accept a relative path.
    // We should remove it after we get the raw path.
    new URI("file", null, "localhost", -1, "/" + fileName, null, null).getRawPath.substring(1)
  }

  /**
   * Shuffle the elements of a collection into a random order, returning the
   * result in a new collection. Unlike scala.util.Random.shuffle, this method
   * uses a local random number generator, avoiding inter-thread contention.
   */
  def randomize[T: ClassTag](seq: TraversableOnce[T]): Seq[T] = {
    randomizeInPlace(seq.toArray)
  }

  /**
   * Shuffle the elements of an array into a random order, modifying the
   * original array. Returns the original array.
   */
  def randomizeInPlace[T](arr: Array[T], rand: Random = new Random): Array[T] = {
    for (i <- (arr.length - 1) to 1 by -1) {
      val j = rand.nextInt(i + 1)
      val tmp = arr(j)
      arr(j) = arr(i)
      arr(i) = tmp
    }
    arr
  }

  val isWindows: Boolean = SystemUtils.IS_OS_WINDOWS

  val isMac: Boolean = SystemUtils.IS_OS_MAC_OSX

  val isMacOnAppleSilicon: Boolean =
    SystemUtils.IS_OS_MAC_OSX && SystemUtils.OS_ARCH.equals("aarch64")

  private lazy val localIpAddress: InetAddress = findLocalInetAddress()

  private def findLocalInetAddress(): InetAddress = {
    val defaultIpOverride = System.getenv("CELEBORN_LOCAL_IP")
    if (defaultIpOverride != null) {
      InetAddress.getByName(defaultIpOverride)
    } else {
      val address = InetAddress.getLocalHost
      if (address.isLoopbackAddress) {
        // Address resolves to something like 127.0.1.1, which happens on Debian; try to find
        // a better address using the local network interfaces
        // getNetworkInterfaces returns ifs in reverse order compared to ifconfig output order
        // on unix-like system. On windows, it returns in index order.
        // It's more proper to pick ip address following system output order.
        val activeNetworkIFs = NetworkInterface.getNetworkInterfaces.asScala.toSeq
        val reOrderedNetworkIFs = if (isWindows) activeNetworkIFs else activeNetworkIFs.reverse

        for (ni <- reOrderedNetworkIFs) {
          val addresses = ni.getInetAddresses.asScala
            .filterNot(addr => addr.isLinkLocalAddress || addr.isLoopbackAddress).toSeq
          if (addresses.nonEmpty) {
            val addr = addresses.find(_.isInstanceOf[Inet4Address]).getOrElse(addresses.head)
            // because of Inet6Address.toHostName may add interface at the end if it knows about it
            val strippedAddress = InetAddress.getByAddress(addr.getAddress)
            // We've found an address that looks reasonable!
            logWarning("Your hostname, " + InetAddress.getLocalHost.getHostName + " resolves to" +
              " a loopback address: " + address.getHostAddress + "; using " +
              strippedAddress.getHostAddress + " instead (on interface " + ni.getName + ")")
            logWarning("Set CELEBORN_LOCAL_IP if you need to bind to another address")
            return strippedAddress
          }
        }
        logWarning("Your hostname, " + InetAddress.getLocalHost.getHostName + " resolves to" +
          " a loopback address: " + address.getHostAddress + ", but we couldn't find any" +
          " external IP address!")
        logWarning("Set CELEBORN_LOCAL_IP if you need to bind to another address")
      }
      address
    }
  }

  private var customHostname: Option[String] = sys.env.get("CELEBORN_LOCAL_HOSTNAME")

  def setCustomHostname(hostname: String) {
    // DEBUG code
    Utils.checkHost(hostname)
    customHostname = Some(hostname)
  }

  def localCanonicalHostName: String = {
    customHostname.getOrElse(localIpAddress.getCanonicalHostName)
  }

  def localHostName: String = {
    customHostname.getOrElse(localIpAddress.getHostAddress)
  }

  def localHostNameForURI: String = {
    customHostname.getOrElse(InetAddresses.toUriString(localIpAddress))
  }

  /**
   * Checks if the host contains only valid hostname/ip without port
   * NOTE: Incase of IPV6 ip it should be enclosed inside []
   */
  def checkHost(host: String): Unit = {
    if (host != null && host.split(":").length > 2) {
      assert(
        host.startsWith("[") && host.endsWith("]"),
        s"Expected hostname or IPv6 IP enclosed in [] but got $host")
    } else {
      assert(host != null && host.indexOf(':') == -1, s"Expected hostname or IP but got $host")
    }
  }

  def checkHostPort(hostPort: String): Unit = {
    if (hostPort != null && hostPort.split(":").length > 2) {
      assert(
        hostPort != null && hostPort.indexOf("]:") != -1,
        s"Expected host and port but got $hostPort")
    } else {
      assert(
        hostPort != null && hostPort.indexOf(':') != -1,
        s"Expected host and port but got $hostPort")
    }
  }

  // Typically, this will be of order of number of nodes in cluster
  // If not, we should change it to LRUCache or something.
  private val hostPortParseResults = new ConcurrentHashMap[String, (String, Int)]()

  def parseHostPort(hostPort: String): (String, Int) = {
    // Check cache first.
    val cached = hostPortParseResults.get(hostPort)
    if (cached != null) {
      return cached
    }

    def setDefaultPortValue(): (String, Int) = {
      val retval = (hostPort, 0)
      hostPortParseResults.put(hostPort, retval)
      retval
    }
    // checks if the host:port contains IPV6 ip and parses the host, port
    if (hostPort != null && hostPort.split(":").length > 2) {
      val index: Int = hostPort.lastIndexOf("]:")
      if (-1 == index) {
        return setDefaultPortValue()
      }
      val port = hostPort.substring(index + 2).trim()
      val retVal = (hostPort.substring(0, index + 1).trim(), if (port.isEmpty) 0 else port.toInt)
      hostPortParseResults.putIfAbsent(hostPort, retVal)
    } else {
      val index: Int = hostPort.lastIndexOf(':')
      if (-1 == index) {
        return setDefaultPortValue()
      }
      val port = hostPort.substring(index + 1).trim()
      val retVal = (hostPort.substring(0, index).trim(), if (port.isEmpty) 0 else port.toInt)
      hostPortParseResults.putIfAbsent(hostPort, retVal)
    }

    hostPortParseResults.get(hostPort)
  }

  private val MAX_DEFAULT_NETTY_THREADS = 64

  def fromCelebornConf(
      _conf: CelebornConf,
      module: String,
      numUsableCores: Int = 0): TransportConf = {
    val conf = _conf.clone

    // Specify thread configuration based on our JVM's allocation of cores (rather than necessarily
    // assuming we have all the machine's cores).
    // NB: Only set if serverThreads/clientThreads not already set.
    val numThreads = defaultNumThreads(numUsableCores)
    conf.setIfMissing(s"celeborn.$module.io.serverThreads", numThreads.toString)
    conf.setIfMissing(s"celeborn.$module.io.clientThreads", numThreads.toString)
    if (TransportModuleConstants.PUSH_MODULE == module) {
      conf.setIfMissing(s"celeborn.$module.io.numConnectionsPerPeer", numThreads.toString)
    }
    // TODO remove after releasing 0.2.0
    conf.setIfMissing(s"rss.$module.io.serverThreads", numThreads.toString)
    conf.setIfMissing(s"rss.$module.io.clientThreads", numThreads.toString)

    new TransportConf(module, conf)
  }

  private def defaultNumThreads(numUsableCores: Int): Int = {
    val availableCores =
      if (numUsableCores > 0) numUsableCores else Runtime.getRuntime.availableProcessors()
    math.min(availableCores, MAX_DEFAULT_NETTY_THREADS)
  }

  def getClassLoader: ClassLoader = getClass.getClassLoader

  def getContextOrClassLoader: ClassLoader =
    Option(Thread.currentThread().getContextClassLoader).getOrElse(getClassLoader)

  def classIsLoadable(clazz: String): Boolean = {
    Try {
      // scalastyle:off classforname
      Class.forName(clazz, false, getContextOrClassLoader)
      // scalastyle:on classforname
    }.isSuccess
  }

  /** Preferred alternative to Class.forName(className) */
  def classForName(className: String): Class[_] = {
    // scalastyle:off classforname
    Class.forName(className, true, getContextOrClassLoader)
    // scalastyle:on classforname
  }

  def getCodeSourceLocation(clazz: Class[_]): String = {
    new File(clazz.getProtectionDomain.getCodeSource.getLocation.toURI).getPath
  }

  def loadDefaultRssProperties(conf: CelebornConf, filePath: String = null): String = {
    val path = Option(filePath).getOrElse(getDefaultPropertiesFile())
    Option(path).foreach { confFile =>
      getPropertiesFromFile(confFile).filter { case (k, v) =>
        k.startsWith("celeborn.") || k.startsWith("rss.")
      }.foreach { case (k, v) =>
        conf.setIfMissing(k, v)
        sys.props.getOrElseUpdate(k, v)
      }
    }
    path
  }

  def getDefaultPropertiesFile(env: Map[String, String] = sys.env): String = {
    env.get("CELEBORN_CONF_DIR")
      .orElse(env.get("CELEBORN_HOME").map { t => s"$t${File.separator}conf" })
      .map { t => new File(s"$t${File.separator}celeborn-defaults.conf") }
      .filter(_.isFile)
      .map(_.getAbsolutePath)
      .orNull
  }

  def getDefaultQuotaConfigurationFile(env: Map[String, String] = sys.env): String = {
    env.get("CELEBORN_CONF_DIR")
      .orElse(env.get("CELEBORN_HOME").map { t => s"$t${File.separator}conf" })
      .map { t => new File(s"$t${File.separator}quota.yaml") }
      .filter(_.isFile)
      .map(_.getAbsolutePath)
      .orNull
  }

  private[util] def trimExceptCRLF(str: String): String = {
    val nonSpaceOrNaturalLineDelimiter: Char => Boolean = { ch =>
      ch > ' ' || ch == '\r' || ch == '\n'
    }

    val firstPos = str.indexWhere(nonSpaceOrNaturalLineDelimiter)
    val lastPos = str.lastIndexWhere(nonSpaceOrNaturalLineDelimiter)
    if (firstPos >= 0 && lastPos >= 0) {
      str.substring(firstPos, lastPos + 1)
    } else {
      ""
    }
  }

  def getPropertiesFromFile(filename: String): Map[String, String] = {
    val file = new File(filename)
    require(file.exists(), s"Properties file $file does not exist")
    require(file.isFile(), s"Properties file $file is not a normal file")

    val inReader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)
    try {
      val properties = new Properties()
      properties.load(inReader)
      properties.stringPropertyNames().asScala
        .map { k => (k, trimExceptCRLF(properties.getProperty(k))) }
        .toMap

    } catch {
      case e: IOException =>
        throw new CelebornException(s"Failed when loading RSS properties from $filename", e)
    } finally {
      inReader.close()
    }
  }

  def makeShuffleKey(applicationId: String, shuffleId: Int): String = {
    s"$applicationId-$shuffleId"
  }

  def splitShuffleKey(shuffleKey: String): (String, Int) = {
    val splits = shuffleKey.split("-")
    val appId = splits.dropRight(1).mkString("-")
    val shuffleId = splits.last.toInt
    (appId, shuffleId)
  }

  def splitPartitionLocationUniqueId(uniqueId: String): (Int, Int) = {
    val splits = uniqueId.split("-")
    val partitionId = splits.dropRight(1).mkString("-").toInt
    val epoch = splits.last.toInt
    (partitionId, epoch)
  }

  def makeReducerKey(applicationId: String, shuffleId: Int, partitionId: Int): String = {
    s"$applicationId-$shuffleId-$partitionId"
  }

  def makeMapKey(applicationId: String, shuffleId: Int, mapId: Int, attemptId: Int): String = {
    s"$applicationId-$shuffleId-$mapId-$attemptId"
  }

  def makeMapKey(shuffleId: Int, mapId: Int, attemptId: Int): String = {
    s"$shuffleId-$mapId-$attemptId"
  }

  def shuffleKeyPrefix(shuffleKey: String): String = {
    shuffleKey + "-"
  }

  def bytesToInt(bytes: Array[Byte], bigEndian: Boolean = true): Int = {
    if (bigEndian) {
      bytes(0) << 24 | bytes(1) << 16 | bytes(2) << 8 | bytes(3)
    } else {
      bytes(3) << 24 | bytes(2) << 16 | bytes(1) << 8 | bytes(0)
    }
  }

  def timeIt(f: => Unit): Long = {
    val start = System.currentTimeMillis
    f
    System.currentTimeMillis - start
  }

  def getThreadDump(): String = {
    val runtimeMXBean = ManagementFactory.getRuntimeMXBean
    val pid = runtimeMXBean.getName.split("@")(0)
    runCommand(s"jstack -l ${pid}")
  }

  private def readProcessStdout(process: Process): String = {
    val stream = process.getInputStream
    val sb = new StringBuilder
    var res = stream.read()
    while (res != -1) {
      sb.append(res.toChar)
      res = stream.read()
    }
    sb.toString()
  }

  def runCommand(cmd: String): String = {
    val process = Runtime.getRuntime.exec(cmd)
    readProcessStdout(process)
  }

  def runCommandComplex(cmd: String): String = {
    val cmds = Array("/bin/sh", "-c", cmd)
    val process = Runtime.getRuntime.exec(cmds)
    readProcessStdout(process)
  }

  /**
   * Create a directory inside the given parent directory. The directory is guaranteed to be
   * newly created, and is not marked for automatic deletion.
   */
  def createDirectory(root: String, namePrefix: String = "rss"): File = {
    var attempts = 0
    val maxAttempts = 10
    var dir: File = null
    while (dir == null) {
      attempts += 1
      if (attempts > maxAttempts) {
        throw new IOException("Failed to create a temp directory (under " + root + ") after " +
          maxAttempts + " attempts!")
      }
      try {
        dir = new File(root, namePrefix + "-" + UUID.randomUUID.toString)
        if (dir.exists() || !dir.mkdirs()) {
          dir = null
        }
      } catch { case _: SecurityException => dir = null; }
    }

    dir.getCanonicalFile
  }

  /**
   * Create a temporary directory inside the given parent directory. The directory will be
   * automatically deleted when the VM shuts down.
   */
  def createTempDir(
      root: String = System.getProperty("java.io.tmpdir"),
      namePrefix: String = "celeborn"): File = {
    val dir = createDirectory(root, namePrefix)
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      override def run(): Unit = {
        if (dir != null) {
          JavaUtils.deleteRecursively(dir)
        }
      }
    }))
    dir
  }

  def mkString(args: Seq[String], sep: String = ","): String = {
    args.mkString(sep)
  }

  /**
   * @param slots
   * @return return a worker related slots usage by disk
   */
  def getSlotsPerDisk(
      slots: WorkerResource): util.Map[WorkerInfo, util.Map[String, Integer]] = {
    val workerSlotsDistribution = new util.HashMap[WorkerInfo, util.Map[String, Integer]]()
    slots.asScala.foreach { case (workerInfo, (masterPartitionLoc, slavePartitionLoc)) =>
      val diskSlotsMap = new util.HashMap[String, Integer]()

      def countSlotsByDisk(location: util.List[PartitionLocation]): Unit = {
        location.asScala.foreach(item => {
          val mountPoint = item.getStorageInfo.getMountPoint
          if (diskSlotsMap.containsKey(mountPoint)) {
            diskSlotsMap.put(mountPoint, 1 + diskSlotsMap.get(mountPoint))
          } else {
            diskSlotsMap.put(mountPoint, 1)
          }
        })
      }

      countSlotsByDisk(masterPartitionLoc)
      countSlotsByDisk(slavePartitionLoc)
      workerSlotsDistribution.put(workerInfo, diskSlotsMap)
    }
    workerSlotsDistribution
  }

  def getSlotsPerDisk(
      masterLocations: util.List[PartitionLocation],
      workerLocations: util.List[PartitionLocation]): util.Map[String, Integer] = {
    val slotDistributions = new util.HashMap[String, Integer]()
    (masterLocations.asScala ++ workerLocations.asScala)
      .foreach {
        case location =>
          val mountPoint = location.getStorageInfo.getMountPoint
          if (slotDistributions.containsKey(mountPoint)) {
            slotDistributions.put(mountPoint, slotDistributions.get(mountPoint) + 1)
          } else {
            slotDistributions.put(mountPoint, 1)
          }
      }
    logDebug(s"locations to distribution ," +
      s" ${masterLocations.asScala.map(_.toString).mkString(",")} " +
      s"${workerLocations.asScala.map(_.toString).mkString(",")} " +
      s"to ${slotDistributions} ")
    slotDistributions
  }

  /**
   * if the action is timeout, will return the callback result
   * if other exception will be thrown directly
   * @param block the normal action block
   * @param callback callback if timeout
   * @param timeoutInSeconds timeout limit value in seconds
   * @tparam T result type
   * @return result
   */
  def tryWithTimeoutAndCallback[T](block: => T)(callback: => T)(
      threadPool: ThreadPoolExecutor,
      timeoutInSeconds: Long = 10,
      errorMessage: String = "none"): T = {
    val futureTask = new Callable[T] {
      override def call(): T = {
        block
      }
    }

    var future: java.util.concurrent.Future[T] = null
    try {
      future = threadPool.submit(futureTask)
      future.get(timeoutInSeconds, TimeUnit.SECONDS)
    } catch {
      case _: TimeoutException =>
        logError(s"TimeoutException in thread ${Thread.currentThread().getName}," +
          s" error message: $errorMessage")
        callback
      case throwable: Throwable =>
        throw throwable
    } finally {
      if (null != future && !future.isCancelled) {
        future.cancel(true)
      }
    }
  }

  def toTransportMessage(message: Any): Any = {
    message match {
      case legacy: Message =>
        ControlMessages.toTransportMessage(legacy)
      case pb: GeneratedMessageV3 =>
        ControlMessages.toTransportMessage(pb)
      case _ =>
        message
    }
  }

  def fromTransportMessage(message: Any): Any = {
    message match {
      case transportMessage: TransportMessage =>
        ControlMessages.fromTransportMessage(transportMessage)
      case _ => message
    }
  }

  def toStatusCode(status: Int): StatusCode = {
    status match {
      case 0 =>
        StatusCode.SUCCESS
      case 1 =>
        StatusCode.PARTIAL_SUCCESS
      case 2 =>
        StatusCode.REQUEST_FAILED
      case 3 =>
        StatusCode.SHUFFLE_ALREADY_REGISTERED
      case 4 =>
        StatusCode.SHUFFLE_NOT_REGISTERED
      case 5 =>
        StatusCode.RESERVE_SLOTS_FAILED
      case 6 =>
        StatusCode.SLOT_NOT_AVAILABLE
      case 7 =>
        StatusCode.WORKER_NOT_FOUND
      case 8 =>
        StatusCode.PARTITION_NOT_FOUND
      case 9 =>
        StatusCode.SLAVE_PARTITION_NOT_FOUND
      case 10 =>
        StatusCode.DELETE_FILES_FAILED
      case 11 =>
        StatusCode.PARTITION_EXISTS
      case 12 =>
        StatusCode.REVIVE_FAILED
      case 13 =>
        StatusCode.REPLICATE_DATA_FAILED
      case 14 =>
        StatusCode.NUM_MAPPER_ZERO
      case 15 =>
        StatusCode.MAP_ENDED
      case 16 =>
        StatusCode.STAGE_ENDED
      case 17 =>
        StatusCode.PUSH_DATA_FAIL_NON_CRITICAL_CAUSE
      case 18 =>
        StatusCode.PUSH_DATA_WRITE_FAIL_SLAVE
      case 19 =>
        StatusCode.PUSH_DATA_WRITE_FAIL_MASTER
      case 20 =>
        StatusCode.PUSH_DATA_FAIL_PARTITION_NOT_FOUND
      case 21 =>
        StatusCode.HARD_SPLIT
      case 22 =>
        StatusCode.SOFT_SPLIT
      case 23 =>
        StatusCode.STAGE_END_TIME_OUT
      case 24 =>
        StatusCode.SHUFFLE_DATA_LOST
      case 25 =>
        StatusCode.WORKER_SHUTDOWN
      case 26 =>
        StatusCode.NO_AVAILABLE_WORKING_DIR
      case 27 =>
        StatusCode.WORKER_IN_BLACKLIST
      case 28 =>
        StatusCode.UNKNOWN_WORKER
      case 30 =>
        StatusCode.PUSH_DATA_SUCCESS_MASTER_CONGESTED
      case 31 =>
        StatusCode.PUSH_DATA_SUCCESS_SLAVE_CONGESTED
      case 38 =>
        StatusCode.PUSH_DATA_CREATE_CONNECTION_FAIL_MASTER
      case 39 =>
        StatusCode.PUSH_DATA_CREATE_CONNECTION_FAIL_SLAVE
      case 40 =>
        StatusCode.PUSH_DATA_CONNECTION_EXCEPTION_MASTER
      case 41 =>
        StatusCode.PUSH_DATA_CONNECTION_EXCEPTION_SLAVE
      case 42 =>
        StatusCode.PUSH_DATA_TIMEOUT_MASTER
      case 43 =>
        StatusCode.PUSH_DATA_TIMEOUT_SLAVE
      case _ =>
        null
    }
  }

  def toShuffleSplitMode(mode: Int): PartitionSplitMode = {
    mode match {
      case 0 => PartitionSplitMode.SOFT
      case 1 => PartitionSplitMode.HARD
      case _ =>
        logWarning(s"invalid shuffle mode $mode, fallback to soft")
        PartitionSplitMode.SOFT
    }
  }

  def toPartitionType(value: Int): PartitionType = {
    value match {
      case 0 => PartitionType.REDUCE
      case 1 => PartitionType.MAP
      case 2 => PartitionType.MAPGROUP
      case _ =>
        logWarning(s"invalid partitionType $value, fallback to ReducePartition")
        PartitionType.REDUCE
    }
  }

  def toDiskStatus(value: Int): DiskStatus = {
    value match {
      case 0 => DiskStatus.HEALTHY
      case 1 => DiskStatus.READ_OR_WRITE_FAILURE
      case 2 => DiskStatus.IO_HANG
      case 3 => DiskStatus.HIGH_DISK_USAGE
      case 4 => DiskStatus.CRITICAL_ERROR
      case _ => null
    }
  }

  def getPeerPath(path: String): String = {
    if (path.endsWith("0")) {
      path.substring(0, path.length - 1) + "1"
    } else {
      path.substring(0, path.length - 1) + "0"
    }
  }

  val SORTED_SUFFIX = ".sorted"
  val INDEX_SUFFIX = ".index"
  val SUFFIX_HDFS_WRITE_SUCCESS = ".success"

  def isHdfsPath(path: String): Boolean = {
    path.startsWith("hdfs://")
  }

  def getSortedFilePath(path: String): String = {
    path + SORTED_SUFFIX
  }

  def getIndexFilePath(path: String): String = {
    path + INDEX_SUFFIX
  }

  def getWriteSuccessFilePath(path: String): String = {
    path + SUFFIX_HDFS_WRITE_SUCCESS
  }

  def roaringBitmapToByteString(roaringBitMap: RoaringBitmap): ByteString = {
    if (roaringBitMap != null && !roaringBitMap.isEmpty) {
      val buf = ByteBuffer.allocate(roaringBitMap.serializedSizeInBytes())
      roaringBitMap.serialize(buf)
      buf.rewind()
      ByteString.copyFrom(buf)
    } else {
      ByteString.EMPTY
    }
  }

  def byteStringToRoaringBitmap(bytes: ByteString): RoaringBitmap = {
    if (!bytes.isEmpty) {
      val roaringBitmap = new RoaringBitmap()
      val buf = bytes.asReadOnlyByteBuffer()
      buf.rewind()
      roaringBitmap.deserialize(buf)
      roaringBitmap
    } else {
      null
    }
  }

  def checkedDownCast(value: Long): Int = {
    val downCast = value.toInt
    if (downCast.toLong != value) {
      throw new IllegalArgumentException("Cannot downcast long value " + value + " to integer.")
    }
    downCast
  }

  @throws[IOException]
  def checkFileIntegrity(fileChannel: FileChannel, length: Int): Unit = {
    val remainingBytes = fileChannel.size - fileChannel.position
    if (remainingBytes < length) {
      logError(
        s"File remaining bytes not not enough, remaining: ${remainingBytes}, wanted: ${length}.")
      throw new RuntimeException(s"File is corrupted ${fileChannel}")
    }
  }

  def getShortFormattedFileName(fileInfo: FileInfo): String = {
    val parentFile = fileInfo.getFile.getParent
    parentFile.substring(
      parentFile.lastIndexOf("/"),
      parentFile.length) + "/" + fileInfo.getFile.getName
  }
}
