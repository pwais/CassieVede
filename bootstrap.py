#!/usr/bin/env python

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
(e.g. for Eclipse).

Notes:
 * Requires internet access for --deps, sbt dependencies, etc.
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
  config_group.add_option(
    '--docker-name', type="string", default='cv',
    help="Give the Docker dev container instance this name [default: %default]")
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
  actions_group.add_option(
    '--docker-run', default=False, action='store_true',
    help="Start a local Spark & Cassandra instance inside the Docker "
         "container (e.g. to use for testing code built outside the "
         "container).")
  actions_group.add_option(
    '--rm-docker', default=False, action='store_true',
    help="Remove the Docker container started by --indocker")
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
          the links fail (e.g. due to permissions), you'll
          need to either set up the link yourself or
          compile capnproto-java manually
          (e.g. perhaps just `$ sbt compile`).
           
          ************************************************
          ************************************************
          """
        run_in_shell("mkdir -p /usr/local/include/capnp/")
        INCS = ("c++.capnp",
                "persistent.capnp",
                "rpc.capnp",
                "rpc-twoparty.capnp",
                "schema.capnp")
        for i in INCS:
          run_in_shell(
            "ln -s " +
            os.path.join(CAPNP_PATH, 'include/capnp', i) + " " + 
            os.path.join("/usr/local/include/capnp/", i))
      
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
    run_in_shell("sbt assembly publishLocal")



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
  
  def start_docker():
    
    # Are we already running?
    is_running = False
    try:
      subprocess.check_call(
        "docker inspect " + opts.docker_name,
        shell=True,
        stdout=open(os.devnull, 'w'),
        stderr=open(os.devnull, 'w'))
      is_running = True
    except subprocess.CalledProcessError:
      # We're not running yet!
      pass
    
    if is_running:
      log.info("Container " + opts.docker_name + " already running")
      return
    
    log.info("Starting Dockerized Spark & Cassandra ...")
    
    # Do NOT mount:
    # * deps -- they must be compiled for the container
    # * target -- should be compiled for container
    vol_maps = (
      # Allow bootstrap.py to run inside container
      (os.path.abspath('LICENSE'), '/opt/CassieVede/LICENSE'),
      (os.path.abspath('bootstrap.py'), '/opt/CassieVede/bootstrap.py'),
      
      # Mount as much of the project as needed to build it
      (os.path.abspath('src'), '/opt/CassieVede/src'),
      (os.path.abspath('build.sbt'), '/opt/CassieVede/build.sbt'),
      (os.path.abspath('project'), '/opt/CassieVede/project'),
      (os.path.abspath('.git'), '/opt/CassieVede/.git'),
      (os.path.abspath('.gitmodules'), '/opt/CassieVede/.gitmodules'),
      
      # Mount local Ivy/Maven repos to dramatically accelerate build
      (os.path.join(os.path.expanduser('~'), '.ivy2'), '/root/.ivy2'),
      (os.path.join(os.path.expanduser('~'), '.m2'), '/root/.m2'),)
    docker_cmd = (
      "docker run -d -t -p 9042:9042 -p 8080:8080 -p 8081:8081 " +
        "-w /opt/CassieVede " +
        "--cap-add SYS_ADMIN --device /dev/fuse " + # For SSHFS
        "--name " + opts.docker_name + " " +
        " ".join(("-v " + src + ":" + dst) for (src, dst) in vol_maps) + " " +
        opts.docker_tag)
    run_in_shell(docker_cmd)
  
  if opts.docker_run:
    start_docker()
    if platform.system() == 'Darwin':
      log.info(
         "We need for forward the Cassandra port on boot2docker's "
         "VirtualBox host so that we can connect to it from "
         "this machine")
      print >> sys.stderr, \
        """
        ************************************************
        ************************************************
         
        If you want to access Cassandra or the Spark 
        WebUI from this host, you need to forward ports
        on boot2docker's VirtualBox instance.  Run the
        following to open ports for Cassandra (CQL)
        and Spark WebUIs, respectively:
        
          VBoxManage controlvm "boot2docker-vm" natpf1 "tcp-port9042,tcp,,9042,,9042";
          VBoxManage controlvm "boot2docker-vm" natpf1 "tcp-port8080,tcp,,8080,,8080";
          VBoxManage controlvm "boot2docker-vm" natpf1 "tcp-port8081,tcp,,8081,,8081";
        
        Note that these settings are sticky for the 
        boot2docker VM.
        
        Once set, you should be able to:
         * Run CassieVede against the container's
           Cassandra instance (e.g. use the default
           `--cassandra localhost` setting in the CLI)
         * Access the Spark WebUI at:
            http://localhost:8080/
        
        ************************************************
        ************************************************
        """

  if opts.indocker:
    start_docker()
    docker_cmd = "docker exec -it " + opts.docker_name + " bash"
    log.info("Command: " + docker_cmd)
    os.execvp("docker", docker_cmd.split(" "))
  
  if opts.rm_docker:
    run_in_shell(
      "docker kill " + opts.docker_name + " && docker rm " + opts.docker_name)

  ##
  ## SERDE (Re-)Generation
  ##
  
  if opts.proto_capnp:
    CAPNP_COMPILER = os.path.abspath(
                        os.path.join(CAPNP_PATH, 'c++/src/capnp/capnp'))
    # Put capnp execs on the PATH.  TODO: install capnp localling in deps
    DEPS = os.path.abspath('./deps/capnproto/c++/src/capnp')
    os.environ['PATH'] = os.environ.get('PATH', '') + ':' + DEPS
    # capnp compile -I includes --src-prefix path -o capnp-java-compiler:dest MyCapnpFile.capnp
    run_in_shell(
      CAPNP_COMPILER + " " +
      "compile --verbose " +
      "-I " + os.path.join(CAPNP_JAVA_PATH, 'compiler/src/main/schema/') + " " +
      "-I " + os.path.join(CAPNP_PATH, 'include/capnp') + " " +
      "--src-prefix src/main/resources/ " + 
      "-o " + os.path.join(CAPNP_JAVA_PATH, 'capnpc-java') + ":src/main/java/org/cassievede/msg/capnpm " +
      "src/main/resources/CVImage.capnp")
