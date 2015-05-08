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
//import java.io.File;
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.util.Iterator;
//import java.util.Map;
//
//import org.apache.commons.lang.exception.ExceptionUtils;
//import org.capnproto.Serialize;
//import org.iq80.leveldb.DB;
//import org.iq80.leveldb.DBIterator;
//import org.iq80.leveldb.Options;
//import org.iq80.leveldb.ReadOptions;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//class LevelDBCacheSource implements Iterator<CVImageRecord> {
//  private Logger log = LoggerFactory.getLogger("LevelDBCacheSource");
//  
//  private DB mLevelDB = null;
//  private DBIterator mIterDB = null;
//  private CVImageRecord mNext = null;
//  
//  LevelDBCacheSource(File f) throws IOException {
//    checkNotNull(f);
//    mLevelDB = factory.open(f, new Options());
//    ReadOptions opts = new ReadOptions();
//    opts.fillCache(false);
//    opts.verifyChecksums(true);
//    mIterDB = mLevelDB.iterator(opts);
//  }
//  
//  protected void finalize() throws Throwable {
//    mIterDB = null;
//    if (mLevelDB != null) { mLevelDB.close(); }
//  }
//  
//  @Override
//  public boolean hasNext() {
//    if (!mIterDB.hasNext()) { return false; }
//    Map.Entry<byte[], byte[]> entry = mIterDB.peekNext();
//    
//    try {
//      mNext = CVImageRecord.fromCPMessage(
//                  Serialize.read(ByteBuffer.wrap(entry.getValue())));
//    } catch (IOException e) {
//      log.error("Failed to read message using capnp; skipping");
//      log.error(ExceptionUtils.getStackTrace(e));
//      mIterDB.next();
//      mNext = null;
//    }
//    mLevelDB.delete(entry.getKey());
//    
//    if (mNext == null) { return hasNext(); } else { return true; }
//  }
//
//  @Override
//  public CVImageRecord next() { return mNext; }
//}