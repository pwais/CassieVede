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
//package org.cassievede.jcaricare;
//
//import java.net.URI;
//import java.nio.ByteBuffer;
//import java.util.List;
//
//import org.capnproto.MessageBuilder;
//import org.capnproto.MessageReader;
//import org.capnproto.Text;
//import org.capnproto.TextList;
//import org.cassievede.msg.capnpm.CVImageProtos.CVImage;
//
//class CVImageRecord {
//  public int datasetid = 0;
//  public long partitionid = 0;
//  public String name = null;
//  public ByteBuffer data = null;
//  public List<String> classnames = null;
//  // TODO others
//  public URI uri = null;
//  
//  
//  /**
//   * we need a case class to do the stuff that spark-cassandra wants
//   * we want to be careful about how it does byte buffer copies?
//   * 
//   * http://docs.datastax.com/en/developer/java-driver/1.0/java-driver/reference/javaClass2Cql3Datatypes_r.html
//   * 
//   * want to be careful here about what JNI / DIJ will think of our scala class
//   * 
//   * 
//   * so cassie jrows are just list(colnames), list(byte[])
//   * cassie srows dont even have type info 
//   * 
//   * so capnp should be the same!
//   * also should be the intermediary record!
//   * 
//   *    *** scala row can take map as ctor!! ***
//   *    
//   *    
//   * advantages of having a python tool:
//   *  * tarfile util might be better (want to test, does it do streams?)
//   *  * scrapy is in python
//   *  * python startup seems faster than jvm ... python is easier to debug
//   *  * python has better plotting libs?  
//   *  * scala doesnt have as rich ndarray?  numpy is useful for prototyping
//   *  * coroutines
//   *  
//   * advantages of scala/java-only tool:
//   *  * code re-use
//   *  * better integration with native code? we could write a djinni row type for cassie
//   *  * 
//   *  
//   * priority q!!
//   *  - so dij is gonna malloc for each type :( but user record types will probably help
//   *  
//
//here:
// * cassie rows are dyn attr maps.  Spark SQL rows prolly the same; Spark RDD
// entries often the same
// * Guava has a mutable typed dict thing
// * java stuff easily serializable -- easy to use for leveldb cache
// * dij user types will allow us to transfer dyn attr map across
// jni
// 
// so:
//  * our data reading stuff should work with dyn attr maps, i.e. python dicts. 
// (that's a job for python, eh?)
//  * the java-jni layer should have dyn attr maps in mind.  
//
//SVMap should have POD option ..
//
//pq:
// * read a dir of files
// * unit test it
// * lazy load file from disk
// * unit test it
// * load that into spark RDD and then into cassie
// * unit test it
// * add cache
// * unit test it
// * add async spark parallelize() and save() with a queue of futures,
// max queue length from command line (default to num spark machines)
// * manual test it
// * add ssh tunel
// * manual test it
// * read a tar file
// * unit test it  
// * test loading imagenet tiny into gce
//
// 
//   */
//  
//  public static MessageBuilder toCPMessage(CVImageRecord r) {
//    
//    CassandraRow r = null;
//    
//    MessageBuilder message = new MessageBuilder();
//    CVImage.Builder cvImg = message.initRoot(CVImage.factory);
//    cvImg.setName(r.name);
//    if (r.uri != null) { cvImg.setUri(r.uri.toString()); }
//    if (r.classnames != null) {
//      TextList.Builder cClassnames = cvImg.initClassnames(r.classnames.size());
//      for (int i = 0; i < r.classnames.size(); ++i) {
//        cClassnames.set(i, new Text.Reader(r.classnames.get(i)));
//      }
//    }
//   
//    // TODO: more boilerplate ... 
//    
//    return message;
//  }
//  
//  public static CVImageRecord fromCPMessage(MessageReader message) {
//    CVImageRecord r = new CVImageRecord();
//    CVImage.Reader cvImg = message.getRoot(CVImage.factory);
//    r.name = cvImg.getName().toString(); // needs hasname...
//    // TODO more boilerplate ...
//    return r;
//  }
//}
