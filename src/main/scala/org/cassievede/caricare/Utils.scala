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
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import org.apache.commons.io.FileUtils
import java.nio.ByteBuffer
import scala.collection.mutable.HashMap

object Utils {

  def fillData(r: HashMap[String, Any]) = {
    if ((r contains "uri") && !(r contains "data")) {
      val uri = r("uri").asInstanceOf[URI]
      r("data") = Utils.download(uri)
    }
  }

  def fillDataset(r: HashMap[String, Any], datasetid: Int) = {
    if (!(r contains "datasetid")) {
      r("datasetid") = datasetid
    }
  }

  // TODO: keep?
  def download(uri: URI) : ByteBuffer = {
    checkNotNull(uri)
    if (uri.getScheme().equals("file")) {
      return ByteBuffer.wrap(
          FileUtils.readFileToByteArray(
              new File(uri.getRawPath())))
    } else {
      // TODO: http[s], s3 ...
      throw new URISyntaxException(uri.toString(), "URI has unsupported scheme");
    }
  }

}
