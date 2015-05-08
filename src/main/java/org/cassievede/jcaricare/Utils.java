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
//import java.io.IOException;
//import java.net.URI;
//import java.net.URISyntaxException;
//
//import org.apache.commons.io.FileUtils;
//import org.cassievede.msg.capnpm.CVImageProtos.CVImage;
//
//class Utils {
//  
//  public static void loadCVImageData(CVImage.Builder cvImg) throws URISyntaxException, IOException {
//    checkNotNull(cvImg);
//    if (cvImg.hasData() || !cvImg.hasUri()) { return; }
//    
//    URI uri = new URI(cvImg.getUri().toString());
//    cvImg.setData(download(uri));
//  }
//  
//  public static byte[] download(URI uri) throws URISyntaxException, IOException {
//    if (uri.getScheme().equals("file:")) {
//      return FileUtils.readFileToByteArray(new File(uri.getRawPath()));
//    } else {
//      throw new URISyntaxException(uri.toString(), "URI has unsupported scheme");
//    }
//  }
//}
