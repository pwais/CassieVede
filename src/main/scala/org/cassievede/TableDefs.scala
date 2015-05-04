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

object Defs {

  val CVKeyspaceName = "cassievede"

  val Keyspace = """
CREATE KEYSPACE cassievede
WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };
"""

  val DatasetTable = """
CREATE TABLE cassievede.dataset
  (id int PRIMARY KEY,
   name varchar);
"""

  val ImageTable = """
CREATE TABLE cassievede.image
  (datasetid int,              // Foreign key: dataset.id
   partitionid varint,         // Optionally partition a very large dataset
   name varchar,               // Name of the image, e.g. original filename
   data blob,                  // Raw image file (e.g. jpeg data)
   classnames list<varchar>,   // Label classnames (e.g. for cropped objects)
   width varint,               // (Optional hint) known width of image
   height varint,              // (Optional hint) known height of image
   geoid varint,               // (Optional hint) geohash key (e.g. from EXIF)
   longitude double,           // (Optional hint) (e.g. from EXIF)
   latitude double,            // (Optional hint) (e.g. from EXIF)
   extra map<varchar, blob>,   // (Optional) K-v of serialized data

   PRIMARY KEY ((datasetid, partitionid), name))
                               // (datasetid, partitionid) -> max 4B names
WITH
  caching = 'keys_only' AND    // Don't cache image data!
  compression =                // TODO: Try LZ4
    {'sstable_compression': ''} AND
  compaction =                 // Optimize for reads
    { 'class' :  'LeveledCompactionStrategy'  };
"""

}
