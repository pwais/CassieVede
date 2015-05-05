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

# Based on Oarphkit bootstrap.py

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

Bootstrap CassieVede and run common development actions. 
Run in the root of the CassieVede repository.  In a fresh
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
  log = logging.getLogger("cv.bootstrap")
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
    '--parallel', type="string", default=str(multiprocessing.cpu_count()),
    help="Use this build parallelism [default %default]")
  config_group.add_option(
    '--docker-tag', type="string", default='cassievedebox',
    help="Give the Docker dev container this tag [default: %default]")
  option_parser.add_option_group(config_group)
  
  actions_group = OptionGroup(
                    option_parser,
                    "Actions",
                    "Prepare/execute common build actions")
  actions_group.add_option(
    '--all', default=False, action='store_true',
    help="Equivalent to --deps --build --test")
  actions_group.add_option(
    '--clean', default=False, action='store_true',
    help="Clear dirs: --deps-dir")
  actions_group.add_option(
    '--deps', default=False, action='store_true',
    help="Download, build, and test all dependencies to --deps-dir")
  actions_group.add_option(
    '--build', default=False, action='store_true',
    help="Build using local dependencies")
  actions_group.add_option(
    '--test', default=False, action='store_true',
    help="Run unit tests")
  actions_group.add_option(
    '--install-local', default=False, action='store_true',
    help="Publish to local Maven / Ivy repo")
  
  actions_group.add_option(
    '--eclipse', default=False, action='store_true',
    help="Generate Eclipse project")
  
  actions_group.add_option(
    '--proto-capnp', default=False, action='store_true',
    help="Regenerate Captain Proto code")
  
  actions_group.add_option(
    '--build-docker', default=False, action='store_true',
    help="Build the dev machine docker container.")
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
    sys.exit(0)
  
  if opts.all:
    for flagname in ('deps', 'build', 'test', 'install_local'):
      setattr(opts, flagname, True)
  
  MAKE_J_PARALLEL = "make -j" + opts.parallel
  
  ##
  ## Dependencies
  ##
  
  PROTOBUF_PATH = os.path.abspath(os.path.join(opts.deps_dir, 'protobuf'))
  CAPNP_JAVA_PATH = os.path.abspath(os.path.join(opts.deps_dir, 'capnproto-java'))
  CAPNP_PATH = os.path.abspath(os.path.join(opts.deps_dir, 'capnproto'))
  if opts.deps:     
    run_in_shell("git submodule update --init")
    
    # == capnp c++ ==
    if not os.path.exists(os.path.join(CAPNP_PATH, 'lib/libkj.a')):
      run_in_shell(
        "cd " + os.path.join(CAPNP_PATH, 'c++') + " && " +
        "cmake -DCAPNP_INCLUDE_DIR=" + os.path.join(CAPNP_PATH, 'include/') + " -DCMAKE_VERBOSE_MAKEFILE=ON -DCMAKE_CXX_COMPILER=clang++ -DCMAKE_INSTALL_PREFIX:PATH=" + CAPNP_PATH + " . && " +
        MAKE_J_PARALLEL + " && " +
        "make install && " +
        # Now test capnp
        "./src/capnp/capnp-tests && ./src/capnp/capnp --help")
    
    # == capnp java ==
    if not os.path.exists(os.path.join(CAPNP_JAVA_PATH, 'runtime/target/runtime-0.1.0-SNAPSHOT.jar')):
      
      if not os.path.exists("/usr/local/include/capnp"):
        print >> sys.stderr, \
          """
          ************************************************
          ************************************************
           
          Warn: capnp includes not found (e.g. c++.capnp).
          Unfortunately the capnproto-java depends on
          system-installed base *.capnp includes. We'll
          try to symlink the includes into place, but if
          link fails (e.g. due to permissions), you'll
          need to either set up the link yourself or
          compile capnproto-java manually
          (e.g. perhaps just `$ sbt compile`).
           
          ************************************************
          ************************************************
          """
        run_shell(
          "ln -s " + os.path.join(CAPNP_PATH, 'include/capnp') + " /usr/local/include/capnp")
      
      # Help capnproto-java / sbt find the capnp compiler
      os.environ['PKG_CONFIG_PATH'] = (
         os.environ.get('PKG_CONFIG_PATH', '') + ':' + os.path.join(CAPNP_PATH, 'c++/'))
      os.environ['PATH'] = (
         os.environ.get('PATH', '') + ':' + os.path.join(CAPNP_PATH, 'bin/'))
      run_in_shell(
        "cd " + CAPNP_JAVA_PATH + " && "
        "make && sbt compile test package publishLocal")
    
  
  
  
   ##
   ## Building
   ##

  if opts.build:
    log.info("Building in " + opts.build_dir)
    run_in_shell("sbt compile")
    
  if opts.test:
    run_in_shell("sbt test")
  
  if opts.install_local:
    run_in_shell("sbt package publishLocal")



  ##
  ## IDE Support
  ##

  if opts.eclipse:
    run_in_shell("sbt eclipse with-source=true")

    print >> sys.stderr, \
      """
      ************************************************
      ************************************************
       
      To use the generated Eclipse Project, go to
      Import > Existing Project, and then choose
      the repo root as the project directory
        %s
       
      ************************************************
      ************************************************
      """ % (os.path.abspath('.'),)
  


  ##
  ## Docker
  ##

  if opts.build_docker:
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
  
  if opts.indocker:  
    log.info("Dropping into Dockerized bash ...")
    # Only mount the source, not e.g. build or deps, which can't
    # be shared.
    vol_maps = (
      (os.path.abspath('src'), '/opt/CassieVede/src'),
  		(os.path.abspath('build.sbt'), '/opt/CassieVede/build.sbt'),
  		(os.path.abspath('LICENSE'), '/opt/CassieVede/LICENSE'),
  		(os.path.abspath('project'), '/opt/CassieVede/project'),
      (os.path.abspath('bootstrap.py'), '/opt/CassieVede/bootstrap.py'),
      (os.path.abspath('.git'), '/opt/CassieVede/.git'),
      (os.path.abspath('.gitmodules'), '/opt/CassieVede/.gitmodules'),)
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
    os.environ['PATH'] = os.environ.get('PATH', '') + ':' + DEPS
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
