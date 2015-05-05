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
  datasetid @0 :Int32;
  datasetname @1 :Text;
  partitionid @2 :UInt64;
  name @3 :Text;
  data @4 :Data;
  classnames @5 :List(Text);
  width @6 :UInt64;
  height @7 :UInt64;
  geoid @8 :UInt64;
  longitude @9 :Float64;
  latitude @10 :Float64;
  
  struct ExtraEntry {
    key @0 :Text;
    value @1 :Data;
  }
  extra @11 :List(ExtraEntry);
}
