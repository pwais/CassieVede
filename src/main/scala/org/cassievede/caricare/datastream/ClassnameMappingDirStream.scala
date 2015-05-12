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

package org.cassievede.caricare.datastream

import com.google.common.base.Preconditions.checkNotNull
import java.io.File
import java.nio.file.Path
import com.google.common.base.Joiner
import com.google.common.base.Splitter
import com.google.common.collect.Lists
import com.google.common.io.Files
import scala.collection.mutable.HashMap
import scala.collection.immutable.HashSet
import org.cassievede.Constants
import org.apache.commons.io.FilenameUtils
import scala.collection.mutable.Queue
import scala.collection.JavaConversions
import org.cassievede.caricare.Utils
import org.apache.commons.io.FileUtils

object ClassnameMappingDirStream {

  def extractFileName(path :Path) : String = {
    path.getFileName().toString()
  }

  def extractClassname(path :Path) : String = {
    val toks = Lists.newArrayList(
                                Splitter
                                  .on(System.getProperty("file.separator"))
                                  .trimResults()
                                  .omitEmptyStrings()
                                  .split(path.toString()))
    if (toks.isEmpty()) { return "" }
    val classnameToks = toks.subList(0, toks.size() - 1)
    return Joiner.on(".").skipNulls().join(classnameToks)
  }
}

/**
 * Generate a sequence of Map records following the schema of
 * the cassievede.image table.
 */
class ClassnameMappingDirStream(
    rootDir: File,
    accpetedExtensions: HashSet[String] = Constants.imageExtensions) extends Datastream {

  private var mNext: HashMap[String, Any] = null

  lazy val allFilepaths: Queue[File] = {
    checkNotNull(rootDir)
    val q = new Queue[File]
    if (rootDir.exists()) { // make tests faster
      q ++=
        JavaConversions.asScalaIterator(
            Files.fileTreeTraverser().preOrderTraversal(rootDir).iterator())
    }
    q
  }

  def hasNext(): Boolean = {
    if (allFilepaths.isEmpty) { return false }

    // Try to build the next record
    val f = allFilepaths.front
    if (!(f.exists() && !f.isDirectory() && f.canRead())) {
      mNext = null
      allFilepaths.dequeue()
      return hasNext()
    }
    val path = rootDir.toPath().getParent().relativize(f.toPath())

    val ext = FilenameUtils.getExtension(path.toString())
    if (!(accpetedExtensions.isEmpty || accpetedExtensions.contains(ext))) {
      mNext = null
      allFilepaths.dequeue()
      return hasNext()
    }

    // We can build a record .. do it!
    val r = new HashMap[String, Any]
    r("name") = ClassnameMappingDirStream.extractFileName(path)
    r("classnames") = List(ClassnameMappingDirStream.extractClassname(path))
    r("uri") = f.toURI()
    r("data") = FileUtils.readFileToByteArray(f)
    mNext = r

    // Don't generate this record again
    allFilepaths.dequeue()

    return true
  }

  def next() : HashMap[String, Any] = mNext
}
