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

package org.cassivede.caricare.datastream

import org.scalatest._
import org.cassievede.caricare.datastream.ClassnameMappingDir
import java.io.File
import scala.collection.mutable.HashMap
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.net.URI

class ClassnameMappingDirSpec extends FlatSpec with Matchers {

  "A ClassnameMappingDirSpec" should "yield nothing for non-existent dirs" in {
    val existe = this.getClass().getResource("/images").toURI()
    val noExiste = new URI(existe.toString() + "/moof")
    val f = new File(noExiste.getPath())
    f.exists() should be (false)
    val iter = new ClassnameMappingDir(f)
    iter.hasNext() should be (false)
  }

  it should "extract classnames" in {
    val toPath = (s: String) => { (new File(s)).toPath() }
    val toClassname = (s: String) => {
      ClassnameMappingDir.extractClassname(toPath(s))
    }

    // Windows? Sorry for you mang
    System.getProperty("file.separator") should be ("/")

    toClassname("") should be ("")
    toClassname("moof.jpg") should be ("")
    toClassname("qux/moof.jpg") should be ("qux")
    toClassname("foo/bar/qux/moof.jpg") should be ("foo.bar.qux")
    toClassname("foo/bar/qux") should be ("foo.bar")
  }

  it should "extract filenames" in {
    val toPath = (s: String) => { (new File(s)).toPath() }
    val toFileName = (s: String) => {
      ClassnameMappingDir.extractFileName(toPath(s))
    }

    // Windows? Sorry for you mang
    System.getProperty("file.separator") should be ("/")

    toFileName("") should be ("")
    toFileName("moof.jpg") should be ("moof.jpg")
    if (System.getProperty("file.separator").equals("/")) {
      toFileName("qux/moof.jpg") should be ("moof.jpg")
      toFileName("foo/bar/qux/moof.jpg") should be ("moof.jpg")
    } else if (System.getProperty("file.separator").equals("\\")) {
      toFileName("qux\\moof.jpg") should be ("moof.jpg")
      toFileName("foo\\bar\\qux\\moof.jpg") should be ("moof.jpg")
    } else {
      false should be ("Mang what platform u on?")
    }
  }

  val kExpected = scala.collection.immutable.HashMap(
    "390-cat-sitting-whiskers.jpg" -> List("root.cat"),
    "461-cat-black-sitting.jpg" -> List("root.cat"),
    "63-puppy-cute-black.jpg" -> List("root.dog"),
    "mia-1330984054TBZ.jpg" -> List("root.dog"),
    "314-single-onion-vegetable.jpg" -> List("root.onion"),
    "8-barren-tree.jpg" -> List("root.tree"))

  it should "iterate over a test directory" in {
    val root = new File(this.getClass().getResource("/images/root").toURI())
    root.exists() should be (true)

    val expected = HashMap() ++= kExpected
    val iter = new ClassnameMappingDir(root)
    for (r <- iter) {
      r contains "name" should be (true)
      r contains "classnames" should be (true)
      r contains "uri" should be (true)
      r contains "data" should be (true)

      val k = r("name").asInstanceOf[String]
      expected contains k should be (true)
      expected(k) should equal (r("classnames"))
      expected -= k
    }
    expected shouldBe empty
  }

  it should "filter files by extensions" in {
    val root = new File(this.getClass().getResource("/images").toURI())
    root.exists() should be (true)

    val iter = new ClassnameMappingDir(root)
    for (r <- iter) {
      r contains "name" should be (true)
      val k = r("name").asInstanceOf[String]
      k.indexOf("md") should be (-1)
    }
  }

  it should "serialize and resume" in {
    val root = new File(this.getClass().getResource("/images/root").toURI())
    root.exists() should be (true)

    val serDeser = (i: ClassnameMappingDir) => {
      val bo = new ByteArrayOutputStream()
      val so = new ObjectOutputStream(bo)
      so.writeObject(i)
      so.flush()
      val bi = new ByteArrayInputStream(bo.toByteArray())
      val si = new ObjectInputStream(bi)
      si.readObject().asInstanceOf[ClassnameMappingDir]
    }

    val expected = HashMap() ++= kExpected
    val checkRecord = (r: HashMap[String, Any]) => {
      val k = r("name").asInstanceOf[String]
      expected contains k should be (true)
      expected(k) should equal (r("classnames"))
      expected -= k
    }

    var iter = new ClassnameMappingDir(root)
    iter = serDeser(iter)
    while (iter.hasNext()) {
      iter = serDeser(iter)
      checkRecord(iter.next())
      iter = serDeser(iter)
    }
    expected shouldBe empty
  }
}
