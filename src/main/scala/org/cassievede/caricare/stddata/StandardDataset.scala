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
//package org.cassievede.caricare.stddata
//
//import scala.collection.mutable.HashMap
//
//trait StandardDataset {
//
//  // TODO: make the factory method do everything, including cache to disk
//
////  // The dataset is a single file served from the given URI
////  val fileUri: String = null
////
////  // User can stream the dataset without writing to the filesystem
////  val streamable: Boolean = false
//
//  def iterItems() : Iterator[HashMap[String, Any]]
//}