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
//
//import java.io.File;
//import java.nio.file.Path;
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//
//import com.google.common.base.Joiner;
//import com.google.common.base.Splitter;
//import com.google.common.collect.Lists;
//import com.google.common.io.Files;
//
//public class ClassnameMappingDir implements Iterator<CVImageRecord> {
//  
//  private File mRootDir = null;
//  private Iterator<File> mIterFiles = null;
//  
//  ClassnameMappingDir(File root) {
//    checkNotNull(root);
//    this.mRootDir = root;
//    mIterFiles = Files.fileTreeTraverser().preOrderTraversal(root).iterator();
//  }
//  
//  public static String extractFileName(Path path) {
//    return path.getFileName().toString();
//  }
//  
//  public static String extractClassname(Path path) {
//    ArrayList<String> toks = Lists.newArrayList(
//                                Splitter
//                                  .on(System.getProperty("file.separator"))
//                                  .trimResults()
//                                  .omitEmptyStrings()
//                                  .split(path.toString()));
//    List<String> classnameToks = toks.subList(0, toks.size() - 1);
//    return Joiner.on(".").skipNulls().join(classnameToks);
//  }
//  
//  @Override
//  public boolean hasNext() { return mIterFiles.hasNext(); }
//
//  @Override
//  public CVImageRecord next() {
//    File f = mIterFiles.next();
//    Path path = mRootDir.toPath().getParent().relativize(f.toPath());
//    
//    CVImageRecord r = new CVImageRecord();
//    r.name = extractFileName(path);
//    r.classnames = Lists.newArrayList(extractClassname(path));
//    r.uri = f.toURI();
//    return r;
//  }
//}
