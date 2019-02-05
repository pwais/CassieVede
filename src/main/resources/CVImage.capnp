# Copyright 2015 Maintainers of CassieVede
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

@0xa1ef564a5fdcc7a9;

using Cxx = import "/capnp/c++.capnp";
$Cxx.namespace("cvd_msg::capnpm");

using Java = import "/capnp/java.capnp";
$Java.package("org.cassievede.msg.capnpm");
$Java.outerClassname("CVImageProtos");

struct CVImage {
  
  // Image key
  datasetid @0 :Int32;
  datasetname @1 :Text;
  partitionid @2 :Int32;
  id @3 :Int64;
  
  // Core data
  name @4 :Text;
  data @5 :Data;
  
  // Labels
  classnames @6 :List(Text);
  binlabels @7 :Data;
  tags @8 :List(Text);
  
  // Attributes
  width @9 :Int64;
  height @10 :Int64;
  geoid @11 :Int64;
  longitude @12 :Float64;
  latitude @13 :Float64;
  uri @14 :Text;
  
  struct ExtraEntry {
    key @0 :Text;
    value @1 :Data; // Capnp-serialized data
  }
  extra @15 :List(ExtraEntry);
}
