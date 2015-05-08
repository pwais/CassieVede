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

case class CVSessionConfig(
    stdinCVImage: Boolean = false,
    cnameDirs: Seq[File] = Seq(),
    dataset: String = "",

    cache: Boolean = false,
    resumeCache: Boolean = false,
    cacheDir: File =
      new File(System.getProperty("java.io.tmpdir"), "CassieVedeCache"),

    doCreateKeyspace: Boolean = false,
    doLoad: Boolean = false,
    doDrop: Boolean = false,


    localDir: Boolean = false,
    cassandra: String = "")

//    foo: Int = -1, out: File = new File("."), xyz: Boolean = false,
//  libName: String = "", maxCount: Int = -1, verbose: Boolean = false, debug: Boolean = false,
//  mode: String = "", files: Seq[File] = Seq(), keepalive: Boolean = false,
//  jars: Seq[File] = Seq(), kwargs: Map[String,String] = Map())

object CVMain {

  val log:Logger = LoggerFactory.getLogger("CVMain")

  def doLoad(conf: CVSessionConfig) : Unit = {

  }

  def doDrop(conf: CVSessionConfig) : Unit = {

  }

  def doCreateKS(conf: CVSessionConfig) : Unit = {
    val toks = conf.cassandra.split(":")
    val host = if (!toks(0).isEmpty()) { toks(0) } else { "127.0.0.1" }
    val port = if (toks.size > 1) { toks(0).toInt } else { 9142 }

    log.info("Creating Cassie Keyspace and Tables")
    DBUtil.CreateCassie(host, port)
  }

  def main(args: Array[String]) : Unit = {

    val parser = new scopt.OptionParser[CVSessionConfig]("CassieVedeCLI") {
      head("CassieVedeCLI: A utility for image data loading with CassieVede")
      help("help") text("Show this usage message")

      ///
      /// Input sources
      ///
      opt[Boolean]("stdin-cvimage") action { (s, c) => c.copy(stdinCVImage = s)
      } text("Read a stream of CVImages from standard in")
      opt[Seq[File]]("cname-dirs") valueName("<dir1>,<dir2>...") action {
        (x, c) => c.copy(cnameDirs = x)
      } text("Walk these directories of class/name/file.ext")

      ///
      /// Cache
      ///
      opt[Boolean]("cache") action { (s, c) => c.copy(cache = s)
      } text(
          "When importing files, cache the import job before " +
          "executing in case the import job fails")
      opt[Boolean]("resume-cache") action { (s, c) => c.copy(resumeCache = s)
      } text("Resume an import job from a local cache.")
      opt[File]("cached-dir") valueName("<dir>") action {
        (x, c) => c.copy(cacheDir = x)
      } text(
          "Save / read from this directory of cache files " +
          "(default: java.io.tmpdir/CassieVedeCache)")

      ///
      /// Other Config
      ///
      opt[String]("dataset") action {
        (cass, c) => c.copy(dataset = cass)
      } text("Use this dataset")
      opt[String]("cassandra") action {
        (cass, c) => c.copy(cassandra = cass)
      } text("Specify the Cassandra host[:port]")

      ///
      /// Actions
      ///
      opt[Boolean]("create-keyspace") action { (s, c) => c.copy(doCreateKeyspace = s)
      } text("Create the CassieVede keyspace")
      opt[Boolean]("load") action { (s, c) => c.copy(doLoad = s)
      } text("Load data from a source")
      opt[Boolean]("drop") action { (s, c) => c.copy(doDrop = s)
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
    if (!mconf.isDefined) return

    val conf = mconf.get
    if (conf.doCreateKeyspace) {
      doCreateKS(conf)
    } else if (conf.doDrop) {
      doDrop(conf)
    } else if (conf.doLoad) {
      doLoad(conf)
    } else {
      log.warn("Nothing to do!")
    }
  }
}
