// Copyright 2015 Maintainers of CassieVede
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

name := "cassievede"
version := "0.0.1"
scalaVersion := "2.10.4"

// Allow local repo use
resolvers += Resolver.mavenLocal

// Scala Unit Testing
libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

// Java Unit Testing
libraryDependencies += "junit" % "junit" % "4.12" % "test"
libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test

// CLI
libraryDependencies += "com.github.scopt" %% "scopt" % "3.3.0"
// resolvers += Resolver.sonatypeRepo("public") still need?

// Spark
libraryDependencies += "org.apache.spark" %% "spark-core" % "1.3.1"

// Cassandra
libraryDependencies += "com.datastax.cassandra"  % "cassandra-driver-core" % "2.1.5"

// Cassandra Test
libraryDependencies += "org.cassandraunit" % "cassandra-unit" % "2.0.2.2" % "test"
