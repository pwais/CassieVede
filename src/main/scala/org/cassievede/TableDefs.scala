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

object Defs {

  val CVKeyspaceName = "cassievede"

  val CVImagesTableName = "images"

  val Keyspace = """
CREATE KEYSPACE cassievede
WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };
"""

  val DatasetTable = """
CREATE TABLE cassievede.dataset
  (id int PRIMARY KEY,
   name text);
"""

  val ImageTable = """
CREATE TABLE cassievede.image
  (datasetid int,              // Foreign key: dataset.id
   partitionid int,            // Optionally partition a very large dataset
   id uuid,                    // Unique ID

   // Core data
   name text,                  // Name of the image, e.g. original filename
   data blob,                  // Raw image file (e.g. jpeg data)

   // Labels
   classnames list<text>,      // Label classnames (e.g. for cropped objects)
   binlabels blob,             // Binary-encoded labels (e.g. trees)
   tags list<text>,            // Attributes that don't define classes

   // Attributes
   width bigint,               // (Optional hint) known width of image
   height bigint,              // (Optional hint) known height of image
   geoid bigint,               // (Optional hint) geohash key (e.g. from EXIF)
   longitude double,           // (Optional hint) (e.g. from EXIF)
   latitude double,            // (Optional hint) (e.g. from EXIF)
   uri text,                   // (Optional) original URI source of image
   extra map<varchar, blob>,   // (Optional) K-v of Capnp-serialized data

   PRIMARY KEY ((datasetid, partitionid), id))
                               // (datasetid, partitionid) -> max 4B ids
WITH
  caching = 'keys_only' AND    // Don't cache image data!
  compression =                // TODO: Try LZ4
    {'sstable_compression': ''} AND
  compaction =                 // Optimize for reads
    { 'class' :  'LeveledCompactionStrategy'  };
"""

}
