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
//package org.cassievede.caricare.stddata
//
//object TinyImageNet200 extends StandardDataset {
//
//  val fileUri = "http://cs231n.stanford.edu/tiny-imagenet-200.zip"
//
//  val streamable = false; // We have to do two passes; one to extract labels
//
//  /**
//   * FMI: https://tinyimagenet.herokuapp.com/ (Online at time of writing)
//   *
//   * The dataset has the (unpublished) layout:
//   *   200 classes in all:
//   *     tiny-imagenet-200/words.txt:
//   *       A TSV file of (wnid, description [csv]) tuples
//   *     tiny-imagenet-200/wnids.txt:
//   *       A text file listing wnids
//   *
//   *   Train data (labeled, 500 per class):
//   *     tiny-imagenet-200/train/(wnid)/images/(wnid)_(imnumber).JPEG
//   *     tiny-imagenet-200/train/(wnid)/(wnid)_boxes.txt:
//   *       A TSV file of (fname, bbox)
//   *
//   *   Validation data (labeled, 50 per class):
//   *   tiny-imagenet-200/val/images/val_(imnumber).JPEG
//   *   tiny-imagenet-200/val/val_annotations.txt:
//   *     A TSV file of (filename, wnid, bbox) tuples
//   *
//   *   Test data (unlabeled, 50 total):
//   *     tiny-imagenet-200/test/images/test_1860.JPEG
//   *
//   *
//   *     TODO: this will be complicated. we'll want to do one pass to grab
//   *     the labels and a second pass to generate the image records
//   */
//
//
//}
