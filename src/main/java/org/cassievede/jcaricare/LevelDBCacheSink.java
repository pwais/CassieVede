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
//
//package org.cassievede.jcaricare;
//
//import static com.google.common.base.Preconditions.checkNotNull;
//import static org.fusesource.leveldbjni.JniDBFactory.factory;
//
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.nio.channels.Channels;
//
//import org.capnproto.Serialize;
//import org.iq80.leveldb.DB;
//import org.iq80.leveldb.Options;
//
//class LevelDBCacheSink {
//  
//  private DB mLevelDB = null;
//  private long mNumWritten = 0;
//  
//  LevelDBCacheSink(File f) throws IOException {
//    checkNotNull(f);
//    Options oOpts = new Options();
//    oOpts.createIfMissing(true);
//    oOpts.errorIfExists(true);
//    mLevelDB = factory.open(f, oOpts);
//  }
//  
//  protected void finalize() throws Throwable {
//    if (mLevelDB != null) { mLevelDB.close(); }
//  }
//  
//  public void consume(CVImageRecord r) throws IOException {
//    ByteArrayOutputStream s = new ByteArrayOutputStream();
//    Serialize.write(Channels.newChannel(s), CVImageRecord.toCPMessage(r));
//    mLevelDB.put(
//      ByteBuffer.allocate(8).putLong(mNumWritten).array(),
//      s.toByteArray());
//    ++mNumWritten;
//  }
//}