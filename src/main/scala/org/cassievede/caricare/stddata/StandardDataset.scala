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
package org.cassievede.caricare.stddata

import scala.collection.mutable.HashMap
import org.cassievede.caricare.datastream.Datastream
import org.cassievede.CVSessionConfig

trait StandardDataset {

  // Return a list of dataset name(s) that this factory creates
  def datasetNames(conf: CVSessionConfig) : List[String]

  // Stream records from this dataset
  def stream(conf: CVSessionConfig) : Datastream
}
