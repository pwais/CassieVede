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

FROM ubuntu:trusty

RUN apt-get update

# Java
RUN apt-get install -y openjdk-7-jdk

# Cassie.  FMI see https://wiki.apache.org/cassandra/DebianPackaging
RUN echo "deb http://www.apache.org/dist/cassandra/debian 21x main" >> /etc/apt/sources.list.d/cassandra.list && \
    echo "deb-src http://www.apache.org/dist/cassandra/debian 21x main" >> /etc/apt/sources.list.d/cassandra.list && \
    gpg --keyserver pgp.mit.edu --recv-keys F758CE318D77295D && \
	gpg --export --armor F758CE318D77295D | sudo apt-key add -  && \
	gpg --keyserver pgp.mit.edu --recv-keys 2B5C1B00 && \
	gpg --export --armor 2B5C1B00 | sudo apt-key add - && \
	gpg --keyserver pgp.mit.edu --recv-keys 0353B12C && \
	gpg --export --armor 0353B12C | sudo apt-key add - && \
	apt-get update && \
	apt-get -qq -y install cassandra

ADD cassandra.yaml /etc/cassandra/cassandra.yaml

# Scala / sbt
RUN echo "deb http://dl.bintray.com/sbt/debian /" >> /etc/apt/sources.list.d/sbt.list && \
    apt-get update && \
    apt-get install --force-yes -y sbt && \
	sbt help

### Warm up sbt dependencies
#ADD CassieVedeSBTInit /opt/CassieVedeSBTInit
#RUN cd /opt/CassieVedeSBTInit && sbt update

# Spark
RUN apt-get -y install curl
WORKDIR /tmp/
RUN curl http://d3kbcqa49mib13.cloudfront.net/spark-1.2.2-bin-hadoop2.4.tgz | tar zxvf -
RUN mv spark-1.2.2-bin-hadoop2.4 /opt/spark
RUN ls -lhat /opt/spark/bin/spark-submit # TODO: better test; --version is broken


# Captain Proto build dependencies
RUN apt-get -y install pkg-config build-essential clang llvm lldb-3.4 cmake dh-autoreconf libc++abi-dev libc++-dev

## Captain Proto needs >= cmake 3.1
#RUN apt-get -y install software-properties-common python-software-properties
#RUN add-apt-repository ppa:george-edison55/cmake-3.x 
#RUN apt-get update 
#RUN apt-get -y upgrade cmake

# Utils
RUN apt-get -y install git vim wget time s3cmd iftop sshfs lsof 

# Python
RUN apt-get -y install python2.7 python-pip
RUN pip install virtualenv

ADD startup.sh /opt/CassieVedeUtil/startup.sh
RUN chmod a+x /opt/CassieVedeUtil/startup.sh
CMD /opt/CassieVedeUtil/startup.sh
