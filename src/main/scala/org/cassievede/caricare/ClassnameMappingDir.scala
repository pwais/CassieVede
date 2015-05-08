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

package org.cassievede.caricare

import com.google.common.base.Preconditions.checkNotNull
import java.io.File
import java.nio.file.Path
import java.util.ArrayList
import com.google.common.base.Joiner
import com.google.common.base.Splitter
import com.google.common.collect.Lists
import com.google.common.io.Files
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import org.cassievede.Constants
import org.apache.commons.io.FilenameUtils

object ClassnameMappingDir {

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
class ClassnameMappingDir(rootDir: File) extends Iterator[HashMap[String, Any]] {
  lazy val iterFiles = {
    checkNotNull(rootDir)
    Files.fileTreeTraverser().preOrderTraversal(rootDir).iterator()
  }
  val accpetedExtensions: HashSet[String] = new HashSet[String]
  private var mNext: HashMap[String, Any] = null


  // Filter results by extension
  def setAcceptAll() = accpetedExtensions.clear()
  def setAcceptImages() = accpetedExtensions ++ Constants.imageExtensions
  def addExtension(ext: String) = accpetedExtensions.add(ext)


  def hasNext() : Boolean = {
    if (!iterFiles.hasNext()) { return false }

    // Try to build the next record
    val f = iterFiles.next()
    if (!(f.exists() && !f.isDirectory() && f.canRead())) { return hasNext() }
    val path = rootDir.toPath().getParent().relativize(f.toPath())

    val ext = FilenameUtils.getExtension(path.toString())
    if (!(accpetedExtensions.isEmpty || accpetedExtensions.contains(ext))) {
      mNext = null
      return hasNext()
    }

    // We can build a record .. do it
    val r = new HashMap[String, Any]
    r("name") = ClassnameMappingDir.extractFileName(path)
    r("classnames") = List(ClassnameMappingDir.extractClassname(path))
    r("uri") = f.toURI()
    mNext = r

    return true
  }

  def next() : HashMap[String, Any] = mNext
}
