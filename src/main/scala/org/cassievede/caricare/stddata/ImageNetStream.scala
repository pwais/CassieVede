///*
// * Copyright 2015 Maintainers of CassieVede
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package org.cassievede.caricare.stddata
//
//import com.google.common.base.Preconditions.checkNotNull
//import org.cassievede.caricare.datastream.Datastream
//import scala.collection.mutable.HashMap
//import java.io.File
//import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
//import java.io.FileInputStream
//
///**
// * Generate a Datastream from a copy of the ImageNet tarball (~1.2TB)
// *
// * The ImageNet tarball has the following structure:
// *   * Each file is a tarball named (wnid).tar.  This inner tarball
// *       contains image files of the form (wnid)_(img number).JPEG
// *   * There are 21841 total inner tarballs
// */
//class ImageNetStream(rootTar: File) extends Datastream {
//
//  private var mNext: HashMap[String, Any] = null
//
//  var rootPos :Long = 0
//
//  lazy val rootTarStream = {
//    checkNotNull(rootTar)
//    new TarArchiveInputStream(new FileInputStream(rootTar))
//  }
//
//  def hasNext(): Boolean = {
//
//    /*
//     * transient pos that will get reset to 0 after deser
//     * non-transient `read` pos that will get serialized
//     * hasNext() should skip up to read if not
//     *
//     *
//     *
//     * want a list of datasets that the stream induces, so main can
//     *   create them if not exist; maybe make create require an 'already exist!'
//     * want child entry -> record helper
//     *
//     */
//
//    return false
//  }
//
//  def next() : HashMap[String, Any] = mNext
//}
