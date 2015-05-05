#!/usr/bin/python

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

import logging
import multiprocessing
import os
import platform
import sys
import subprocess
from optparse import OptionParser
from optparse import OptionGroup

USAGE = (
"""%prog [options]

TODO TODO TODO

Bootstrap OarphKit and run common development actions. 
Run in the root of the Oarphkit repository.  In a fresh
checkout, run:
  $ ./bootstrap.py --all

To start fresh, run:
  $ ./bootstrap.py --clean

Look for messages in stderr about project file locations
(e.g. for Eclipse CDT).

Notes:
 * Requires internet access for --deps.
 * Why not bash scripts? Python is easier to maintain today.
    This script should not be hard to port (if desired) and is
    adequately cross-platform at the moment.
 * Tested in Python 2.7
""")

LOG_FORMAT = "%(asctime)s\t%(name)-4s %(process)d : %(message)s"

if __name__ == '__main__':
  
  # Direct all script output to a log
  log = logging.getLogger("ok.bootstrap")
  log.setLevel(logging.INFO)
  console_handler = logging.StreamHandler(stream=sys.stderr)
  console_handler.setFormatter(logging.Formatter(LOG_FORMAT))
  log.addHandler(console_handler)
  
  def run_in_shell(cmd):
    log.info("Running %s ..." % cmd)
    subprocess.check_call(cmd, shell=True)
    log.info("... done")
  
  
  
  option_parser = OptionParser(usage=USAGE)
  
  config_group = OptionGroup(
                    option_parser,
                    "Config",
                    "Configuration")
  config_group.add_option(
    '--deps-dir', default=os.path.abspath('deps'),
    help="Download all dependencies to this path [default %default]")
  config_group.add_option(
    '--build-dir', default=os.path.abspath('build'),
    help="Place all build resources here [default %default]")
  config_group.add_option(
    '--parallel', type="string", default=str(multiprocessing.cpu_count()),
    help="Use this build parallelism [default %default]")
  config_group.add_option(
    '--build-type', type="string", default='Debug',
    help="Build this build type (e.g. Debug or Release)")
  config_group.add_option(
    '--proj-dir', type="string", default='projects',
    help="Generate IDE Project files here [default %default]")
  config_group.add_option(
    '--docker-tag', type="string", default='cassievedebox',
    help="Give Docker containers this tag")
  option_parser.add_option_group(config_group)
  
  actions_group = OptionGroup(
                    option_parser,
                    "Actions",
                    "Prepare/execute common build actions")
  actions_group.add_option(
    '--all', default=False, action='store_true',
    help="Equivalent to --deps --build --test --install-local")
  actions_group.add_option(
    '--clean', default=False, action='store_true',
    help="Clear dirs: --build-dir --deps-dir")
  actions_group.add_option(
    '--deps', default=False, action='store_true',
    help="Download, build, and test all dependencies to --deps-dir")
  actions_group.add_option(
    '--build', default=False, action='store_true',
    help="Build using local dependencies")
  actions_group.add_option(
    '--build-system', default=False, action='store_true',
    help="Build using system dependencies (can skip --deps)")
  actions_group.add_option(
    '--test', default=False, action='store_true',
    help="Run unit tests")
  actions_group.add_option(
    '--install-local', default=False, action='store_true',
    help="Install to --build-dir")
  
  actions_group.add_option(
    '--eclipse', default=False, action='store_true',
    help="Generate Eclipse project")
  actions_group.add_option(
    '--xcode', default=False, action='store_true',
    help="Generate XCode project")
  
  actions_group.add_option(
    '--proto-pb', default=False, action='store_true',
    help="Regenerate Protobuf code")
  actions_group.add_option(
    '--proto-capnp', default=False, action='store_true',
    help="Regenerate Captain Proto code")
  
  actions_group.add_option(
    '--indocker', default=False, action='store_true',
    help="Drop into a Dockerized bash shell with the current project's "
         "source mounted inside.  Then, for example, try "
         "bootstrapy.py --all to build inside Docker.")
  option_parser.add_option_group(actions_group)
  
  opts, args = option_parser.parse_args()


  
  assert os.path.exists('LICENSE'), "Please run from root of OarphKit repo"
  ROOT = os.path.abspath('.')


  
  if opts.clean:
    run_in_shell('rm -rf ' + os.path.join(opts.deps_dir, '*'))
    run_in_shell('rm -rf ' + opts.build_dir)
    run_in_shell('rm -rf ' + opts.proj_dir)
    sys.exit(0)
  
  if opts.all:
    for flagname in ('deps', 'build', 'test', 'install_local'):
      setattr(opts, flagname, True)
  
  MAKE_J_PARALLEL = "make -j" + opts.parallel
  
  ##
  ## Dependencies
  ##
  
  GTEST_PATH = os.path.abspath(os.path.join(opts.deps_dir, 'gtest'))
  PROTOBUF_PATH = os.path.abspath(os.path.join(opts.deps_dir, 'protobuf'))
  CAPNP_PATH = os.path.abspath(os.path.join(opts.deps_dir, 'capnproto'))
  if opts.deps:     
    run_in_shell("git submodule update --init")
    
    # == gtest ==
    # First build gtest
    if not os.path.exists(os.path.join(GTEST_PATH, 'libgtest.a')):
      if platform.system() == "Darwin":
        GTEST_CMAKE_OPTS = "-Dgtest_build_tests=ON -DGTEST_USE_OWN_TR1_TUPLE=1"
      else:
        GTEST_CMAKE_OPTS = ("-Dgtest_build_tests=ON " +
          # Force linux build to use C++11/libc++ 
          '-DCMAKE_CXX_COMPILER="clang++" -DCMAKE_CXX_FLAGS="-fPIC -std=c++11 -stdlib=libc++ -DGTEST_USE_OWN_TR1_TUPLE=1"')      
      run_in_shell(
        "cd " + GTEST_PATH + " && " +
        "cmake " + GTEST_CMAKE_OPTS + " . && " + MAKE_J_PARALLEL + " && " +
        # Now test gtest
        "make test")
    
    # == protobuf ==
    if not os.path.exists(os.path.join(PROTOBUF_PATH, 'bin/protoc')):
      # Configure for C++11 support! And install locally!
      # https://code.google.com/p/protobuf/issues/detail?id=567
      PROTOBUF_CONFIGURE = (
        './configure --prefix=$(pwd)/$TARGET ' +
        'CC=clang CXX=clang++ ' +
        'CXXFLAGS="-fPIC -std=c++11 -stdlib=libc++ -O3 -g -DGTEST_USE_OWN_TR1_TUPLE=1" ' +
        'LDFLAGS="-lc++ -stdlib=libc++"')
      if platform.system() == "Darwin":
        PROTOBUF_CONFIGURE += ' LIBS="-lc++ -lc++abi"'
        
      run_in_shell(
        "cd deps/protobuf && " +
        "./autogen.sh && " +
        PROTOBUF_CONFIGURE + " && " +
        # Note that Google doesn't distro gtest correctly :P
        # so we don't run tests for now :(
        # https://github.com/google/protobuf/issues/119
        MAKE_J_PARALLEL + " && make install && ./bin/protoc -h")
    
    # == capnp ==
    if not os.path.exists(os.path.join(CAPNP_PATH, 'lib/libkj.a')):
      run_in_shell(
        "cd " + os.path.join(CAPNP_PATH, 'c++') + " && " +
        'cmake -DCMAKE_CXX_FLAGS="-fPIC -std=c++11 -stdlib=libc++" -DCMAKE_VERBOSE_MAKEFILE=ON -DCMAKE_CXX_COMPILER=clang++ -DCMAKE_INSTALL_PREFIX:PATH=' + CAPNP_PATH + " . && " +
        MAKE_J_PARALLEL + " && " +
        "make install && " +
        # Now test capnp
        "./src/capnp/capnp-tests && ./src/capnp/capnp --help")
    
    # == cmake ==
    try:
      run_in_shell("cmake --version")
    except Exception:
      print >> sys.stderr, \
        """
        ************************************************
        ************************************************
         
        Cmake not found! on Mac, install Macports:
         https://www.macports.org/
        and then run
         $ sudo port install cmake
         $ apt-get -y install cmake
         
        ************************************************
        ************************************************
        """
      sys.exit(-1)
  
  
  
  ##
  ## Building
  ##

  CMAKE_CMD = (
      "cmake -DCMAKE_BUILD_TYPE=" + opts.build_type + " " + 
      "-DCMAKE_INSTALL_PREFIX:PATH=" + opts.build_dir + " " + 
      "-DCMAKE_VERBOSE_MAKEFILE=ON ")
    
  # We currently want to force clang as the compiler
  # TODO(for android) support gcc
  CMAKE_CMD += (
    "-DCMAKE_C_COMPILER=clang " +
    "-DCMAKE_CXX_COMPILER=clang++ ")
    
  if not opts.build_system:
    # Tell cmake to use dependencies in /deps
    CMAKE_CMD += (
      "-DGTEST_ROOT=" + GTEST_PATH +" " +
      
      "-DPROTOBUF_LIBRARY=" + os.path.join(PROTOBUF_PATH, "lib/libprotobuf.a") + " " +
      "-DPROTOBUF_INCLUDE_DIR=" + os.path.join(PROTOBUF_PATH, "include") + " " +
      
      "-DCAPNP_INCLUDE_DIRS=" + os.path.join(CAPNP_PATH, "include") + " " +
      "-DCAPNP_LIB_KJ=" + os.path.join(CAPNP_PATH, "lib/libkj.a") + " " +
      "-DCAPNP_LIB_CAPNP=" + os.path.join(CAPNP_PATH, "lib/libcapnp.a") + " " +
      # For non-lite build; needed for dynamic
      "-DCAPNP_LIB_KJ-ASYNC=" + os.path.join(CAPNP_PATH, "lib/libkj-async.a") + " " + 
      "-DCAPNP_LIB_CAPNP-RPC=" + os.path.join(CAPNP_PATH, "lib/libcapnp-rpc.a") + " " +
      "-DCAPNP_LIB_CAPNPC=" + os.path.join(CAPNP_PATH, "lib/libcapnpc.a"))

  if opts.build or opts.build_system:
    log.info("Building in " + opts.build_dir)
    run_in_shell(
      "mkdir -p " + opts.build_dir + " && " +
      "cd " + opts.build_dir + " && " +
      CMAKE_CMD + " " + os.path.relpath(ROOT, opts.build_dir) + " && " +
      MAKE_J_PARALLEL)
    
  if opts.install_local:
    run_in_shell("cd " + opts.build_dir + " && make install")
  
  if opts.test:
    run_in_shell("cd " + opts.build_dir + " && ./oarphkit_test 2> /dev/null")



  ##
  ## IDE Support
  ##

  if opts.eclipse:
    ECLIPSE_DIR = os.path.join(opts.proj_dir, 'eclipse')
    run_in_shell(
      "mkdir -p " + ECLIPSE_DIR + " && " +
      "cd " + ECLIPSE_DIR + " && " +
      CMAKE_CMD + " -G'Eclipse CDT4 - Unix Makefiles' " + os.path.relpath(ROOT, ECLIPSE_DIR))

    # An easy way to make Eclipse see the real source directories
    # and build a directory tree in the project GUI:
    # we simply symlink the source into the Eclipse project root
    OARPHKIT_SRC = os.path.abspath('oarphkit')
    OARPHKIT_TEST_SRC = os.path.abspath('oarphkit_test')
    if not os.path.exists(os.path.join(ECLIPSE_DIR, 'oarphkit_src')):
      run_in_shell(
        "ln -s " + OARPHKIT_SRC + " " + 
        os.path.join(ECLIPSE_DIR, 'oarphkit_src'))
    if not os.path.exists(os.path.join(ECLIPSE_DIR, 'oarphkit_test_src')):
      run_in_shell(
        "ln -s " + OARPHKIT_TEST_SRC + " " + 
        os.path.join(ECLIPSE_DIR, 'oarphkit_test_src'))

    print >> sys.stderr, \
      """
      ************************************************
      ************************************************
       
      To use the generated Eclipse Project, go to
      Import > Existing Project, and then choose
      the directory
        %s
       
      ************************************************
      ************************************************
      """ % (ECLIPSE_DIR,)
  
  if opts.xcode:
    # Discussion: While cmake generates a useful XCode project, we'll
    # probably eventually want to manually create an iOS Framework and
    # CocoaPod for the library.  FWIW, we tried GYP and generated an even
    # worse XCode project than cmake.  Moreover, XCode setting specification
    # is rather awkward in GYP (and, honestly, in any tool outside of
    # XCode itself).  Git merge conflicts on XCode project files are
    # a horrible time suck, but at the time of writing there's no 
    # obviously better solution.  We might want to look at Facebook Buck
    # or Google Bazel
    
    XCODE_DIR = os.path.join(opts.proj_dir, 'xcode')
    run_in_shell(
      "mkdir -p " + XCODE_DIR + " && " +
      "cd " + XCODE_DIR + " && " +
      CMAKE_CMD + " -G'Xcode' " + os.path.relpath(ROOT, XCODE_DIR))

    print >> sys.stderr, \
      """
      ************************************************
      ************************************************
       
      To use the generated XCode Project, open
      the project file at:
        %s
       
      ************************************************
      ************************************************
      """ % (os.path.join(XCODE_DIR, 'OarphKit.xcodeproj'),)

  ##
  ## Docker
  ##

  if opts.indocker:
    log.info("Building docker image")
    
    # Copy our project's build settings into the image
    # so that sbt can pre-fetch dependencies and include
    # them in the Docker image.  This step reduces
    # sbt startup time (for development) considerably
    # (at the cost of making the image large ... )
    run_in_shell(
      "mkdir -p cloud/CassieVedeSBTInit/ && "
      "mkdir -p cloud/CassieVedeSBTInit/project && "
      "cp build.sbt cloud/CassieVedeSBTInit/ && "
      "cp project/plugins.sbt cloud/CassieVedeSBTInit/project/")
    
    run_in_shell(
      "cd cloud && docker build -t " + opts.docker_tag + " .")
    
    log.info("Dropping into Dockerized bash ...")
    # Only mount the source, not e.g. build or deps, which can't
    # be shared.
    vol_maps = (
      (os.path.abspath('src'), '/opt/CassieVede/src'),
  		(os.path.abspath('build.sbt'), '/opt/CassieVede/build.sbt'),
  		(os.path.abspath('LICENSE'), '/opt/CassieVede/LICENSE'),
  		(os.path.abspath('project'), '/opt/CassieVede/project'))
    docker_cmd = (
      "docker run -it " +
        "-w /opt/CassieVede " +
        "--name cassievedebox " +
        " ".join(("-v " + src + ":" + dst) for (src, dst) in vol_maps) + " " +
        opts.docker_tag + " bash")
    log.info("command: " + docker_cmd)
    os.execvp("docker", docker_cmd.split(" "))

  ##
  ## SERDE (Re-)Generation
  ##
  
  if opts.proto_capnp:
    # TODO: eventually we may merge this into CMake
    CAPNP_COMPILER = os.path.abspath(
                          os.path.join(CAPNP_PATH, 'c++/src/capnp/capnp'))
    # Put capnp execs on the PATH.  TODO: install capnp localling in deps
    DEPS = os.path.abspath('./deps/capnproto/c++/src/capnp')
    os.environ['PATH'] = os.environ['PATH'] + ':' + DEPS
    run_in_shell(
      CAPNP_COMPILER + " " +
      "-I ./deps/capnproto/c++/src/ " +
      "compile ./oarphkit/ok/SVMap/SVMapData.capnp " +
      " --src-prefix oarphkit/ok/SVMap " + 
      " -oc++:./oarphkit/ok_msg/")
    run_in_shell(
      CAPNP_COMPILER + " " +
      "-I ./deps/capnproto/c++/src/ " +
      "compile ./oarphkit/ok/fli/FLiSpec.capnp " +
      " --src-prefix oarphkit/ok/fli " + 
      " -oc++:./oarphkit/ok_msg/")
    run_in_shell(
      CAPNP_COMPILER + " " +
      "-I ./deps/capnproto/c++/src/ " +
      "compile ./oarphkit_test/ok_test/TestMessage.capnp " +
      " --src-prefix oarphkit_test/ok_test " + 
      " -oc++:./oarphkit_test/ok_test_msg/")

  if opts.proto_pb:
    # TODO: eventually we may merge this into CMake
    PROTO_COMPILER = os.path.join(PROTOBUF_PATH, 'bin/protoc')
    run_in_shell(
      PROTO_COMPILER + " " +
      "-Ioarphkit/ok/SVMap/ " +
      "-Ioarphkit/ok/SerializationUtils/DynamicProto/ " +
      "-Ioarphkit/ok/fli/Runtime/ " +
      "--cpp_out=oarphkit/ok_msg/ " +
      "oarphkit/ok/SerializationUtils/DynamicProto/DynamicProto.proto " +
      "oarphkit/ok/SVMap/SVMapData.proto " +
      "oarphkit/ok/fli/Runtime/FLiSpec.proto ")
    run_in_shell(
      PROTO_COMPILER + " " +
      "-Ioarphkit_test/ok_test/ " +
      "--cpp_out=oarphkit_test/ok_test_msg/ " +
      "oarphkit_test/ok_test/TestMessage.proto ")

