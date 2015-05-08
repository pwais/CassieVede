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
package org.cassivede.caricare

import org.scalatest._
import org.cassievede.caricare.Utils
import java.io.File
import java.net.URI
import java.io.FileNotFoundException

class UtilsSpec extends FlatSpec with Matchers {

  "Utils.download" should "throw for an invalid URI" in {
    val existe = this.getClass().getResource("/images").toURI()
    val noExiste = new URI(existe.toString() + "/moof")
    an [FileNotFoundException] should be thrownBy Utils.download(noExiste)
  }

  "Utils.download" should "download a local file" in {
    val im =
      this.getClass().getResource("/images/root/cat/390-cat-sitting-whiskers.jpg").toURI()
    val buf = Utils.download(im)
    buf should not be (null)
    buf.array().length should not be (0)
  }

}
