/*
 * Copyright 2015 Maintainers of CassieVede
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
package org.cassievede

import org.slf4j._
import java.io.File
import scala.collection.immutable.HashSet
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Session
import org.cassievede.caricare.datastream.DataStreamFactory
import org.cassievede.caricare.LoaderQueue

case class CVSessionConfig(
    stdinCVImage: Boolean = false,
    cnameDirs: Seq[File] = Seq(),
    cnameDirExts: Seq[String] = Constants.imageExtensions.toSeq,
    dataset: String = null,

    cache: Boolean = false,
    resumeCache: Boolean = false,
    cacheDir: File =
      new File(System.getProperty("java.io.tmpdir"), "CassieVedeCache"),

    doCreateKeyspace: Boolean = false,
    doCreateDataset: Boolean = false,
    doLoad: Boolean = false,
    doDropDataset: Boolean = false,

    sparkChunkSize: Long = -1,
    sparkQDepth: Long = -1,

    localDir: Boolean = false,
    cassandra: String = "")

//    foo: Int = -1, out: File = new File("."), xyz: Boolean = false,
//  libName: String = "", maxCount: Int = -1, verbose: Boolean = false, debug: Boolean = false,
//  mode: String = "", files: Seq[File] = Seq(), keepalive: Boolean = false,
//  jars: Seq[File] = Seq(), kwargs: Map[String,String] = Map())

object CVMain {

  val log: Logger = LoggerFactory.getLogger("CVMain")

  def createCluster(conf: CVSessionConfig) : Cluster = {
    val toks = conf.cassandra.split(":")
    val host = if (!toks(0).isEmpty()) { toks(0) } else { "127.0.0.1" }
    val port = if (toks.size > 1) { toks(0).toInt } else { 9042 }
    return Cluster.builder().addContactPoint(host).withPort(port).build()
  }

  def safeExec(conf: CVSessionConfig, f: DBUtil => Unit) = {
    val c = createCluster(conf)
    try {
      val d = new DBUtil(c.newSession())
      f(d)
    } finally {
      c.close()
    }
  }

  def doLoad(conf: CVSessionConfig) = {
    val stream = DataStreamFactory.createStream(conf)
    val checkpointer = DataStreamFactory.createCheckpointer(conf, stream)
    val q = new LoaderQueue(conf, stream, checkpointer)
    q.run()
  }

  def doCreateDataset(conf: CVSessionConfig) = {
    safeExec(
      conf,
      (d: DBUtil) => {
        d.createDataset(conf.dataset)
        log.info("Created dataset: " + conf.dataset)
      })
  }

  def doDropDataset(conf: CVSessionConfig) = {
    safeExec(
      conf,
      (d: DBUtil) => {
        d.dropDataset(conf.dataset)
        log.info("Deleted all data for dataset: " + conf.dataset)
      })
  }

  def doCreateKS(conf: CVSessionConfig) = {
    safeExec(
      conf,
      (d: DBUtil) => {
        d.createTables()
        log.info("Created Cassie Keyspace and Tables")
      })
  }

  def main(args: Array[String]) = {

    val parser = new scopt.OptionParser[CVSessionConfig]("CassieVedeCLI") {
      head("CassieVedeCLI: A utility for image data loading with CassieVede")
      help("help") text("Show this usage message")

      ///
      /// Input sources
      ///
      opt[Unit]("stdin-cvimage") action { (x, c) => c.copy(stdinCVImage = true)
      } text("Read a stream of CVImages from standard in")
      opt[Seq[File]]("cname-dirs") valueName("<dir1>, <dir2> ...") action {
        (x, c) => c.copy(cnameDirs = x)
      } text("Walk these directories of class/name/file.ext")
      opt[Seq[String]]("cname-exts") valueName("<ext1>, <ext2> ...") action {
        (x, c) => c.copy(cnameDirExts = x)
      } text(
          "With --cname-dirs, accept only files with the given extensions. " +
          "Use the empty string to accept all files.  Default accepted " +
          "extensions: " + Constants.imageExtensions.toSeq)

      ///
      /// Cache
      ///
      opt[Unit]("cache") action { (x, c) => c.copy(cache = true)
      } text(
          "When importing files, cache the import job before " +
          "executing in case the import job fails")
      opt[Unit]("resume-cache") action { (x, c) => c.copy(resumeCache = true)
      } text("Resume an import job from a local cache.")
      opt[File]("cached-dir") valueName("<dir>") action {
        (x, c) => c.copy(cacheDir = x)
      } text(
          "Save / read from this directory of cache files " +
          "(default: java.io.tmpdir/CassieVedeCache)")

      ///
      /// Loading config
      ///
      opt[Int]("spark-chunk-size") action {
        (x, c) => c.copy(sparkChunkSize = x)
      } text(
          "Size of a chunk in Megabytes.  By default we allocate 1/2 " +
          "of the JVM heap to chunks, with each chunk using " +
          "1 / --spark-queue-depth of the reserved memory.")
      opt[Int]("spark-queue-depth") action {
        (x, c) => c.copy(sparkQDepth = x)
      } text(
          "Number of chunks to load concurrently. By default, " +
          "we choose a number equal to the number of Spark workers.")

      ///
      /// Other Config
      ///
      opt[String]("dataset") action {
        (x, c) => c.copy(dataset = x)
      } text("Use this dataset")
      opt[String]("cassandra") action {
        (x, c) => c.copy(cassandra = x)
      } text("Specify the Cassandra host[:port]")

      ///
      /// Actions
      ///
      opt[Unit]("create-keyspace") action { (x, c) => c.copy(doCreateKeyspace = true)
      } text("Create the CassieVede keyspace")
      opt[Unit]("load") action { (x, c) => c.copy(doLoad = true)
      } text("Load data from a source")
      opt[Unit]("create") action { (x, c) => c.copy(doCreateDataset = true)
      } text("Create the given dataset")
      opt[Unit]("drop") action { (x, c) => c.copy(doDropDataset = true)
      } text("Drop the given dataset")


    }

//      opt[Int]('f', "foo") action { (x, c) =>
//        c.copy(foo = x) } text("foo is an integer property")
//      opt[File]('o', "out") required() valueName("<file>") action { (x, c) =>
//        c.copy(out = x) } text("out is a required file property")
//      opt[(String, Int)]("max") action { case ((k, v), c) =>
//        c.copy(libName = k, maxCount = v) } validate { x =>
//        if (x._2 > 0) success else failure("Value <max> must be >0")
//      } keyValueName("<libname>", "<max>") text("maximum count for <libname>")
//      opt[Seq[File]]('j', "jars") valueName("<jar1>,<jar2>...") action { (x,c) =>
//        c.copy(jars = x) } text("jars to include")
//      opt[Map[String,String]]("kwargs") valueName("k1=v1,k2=v2...") action { (x, c) =>
//        c.copy(kwargs = x) } text("other arguments")
//      opt[Unit]("verbose") action { (_, c) =>
//        c.copy(verbose = true) } text("verbose is a flag")
//      opt[Unit]("debug") hidden() action { (_, c) =>
//        c.copy(debug = true) } text("this option is hidden in the usage text")
//      note("some notes.\n")
//
//      arg[File]("<file>...") unbounded() optional() action { (x, c) =>
//        c.copy(files = c.files :+ x) } text("optional unbounded args")
//      cmd("update") action { (_, c) =>
//        c.copy(mode = "update") } text("update is a command.") children(
//        opt[Unit]("not-keepalive") abbr("nk") action { (_, c) =>
//          c.copy(keepalive = false) } text("disable keepalive"),
//        opt[Boolean]("xyz") action { (x, c) =>
//          c.copy(xyz = x) } text("xyz is a boolean property"),
//        checkConfig { c =>
//          if (c.keepalive && c.xyz) failure("xyz cannot keep alive") else success }
//      )
//    }

    // parser.parse returns Option[CVSessionConfig]
    val mconf = parser.parse(args, CVSessionConfig())
    if (!mconf.isDefined) sys.exit(1)

    val conf = mconf.get
    if (conf.doCreateKeyspace) {
      doCreateKS(conf)
    } else if (conf.doCreateDataset) {
      doCreateDataset(conf)
    } else if (conf.doDropDataset) {
      doDropDataset(conf)
    } else if (conf.doLoad) {
      doLoad(conf)
    } else {
      log.warn("Nothing to do!")
    }
  }
}
