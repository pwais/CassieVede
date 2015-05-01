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

//import java.io.File

case class CVSessionConfig(
    stdin: Boolean = false,
    localDir: Boolean = false,
    cassandra: String = "")
    
//    foo: Int = -1, out: File = new File("."), xyz: Boolean = false,
//  libName: String = "", maxCount: Int = -1, verbose: Boolean = false, debug: Boolean = false,
//  mode: String = "", files: Seq[File] = Seq(), keepalive: Boolean = false,
//  jars: Seq[File] = Seq(), kwargs: Map[String,String] = Map())

object CVMain {
  def main(args: Array[String]) : Unit = {
    
    val parser = new scopt.OptionParser[CVSessionConfig]("CassieVedeCLI") {
      head("CassieVedeCLI: A utility for image data loading with CassieVede")
      help("help") text("Show this usage message")
      opt[Boolean]("stdin") action { (s, c) => c.copy(stdin = s)
      } text("Read from standard in")
      opt[Boolean]("local") action { (path, c) => c.copy(localDir = path)
      } text("Read from the given directory path")
      opt[String]('c', "cassandra") action {
        (cass, c) => c.copy(cassandra = cass)
      } text("Specify the Cassandra host")
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

    // parser.parse returns Option[C]
    val conf = parser.parse(args, CVSessionConfig())
    if (!conf.isDefined) return
    
    println("meow!")  
  }
}
