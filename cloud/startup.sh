#!/bin/bash

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

set -e
set -x

# Use double quotes to have bash execute nested hostname call (if applicable)
sed -i "s/%%ip%%/localhost/g" /etc/cassandra/cassandra.yaml
cassandra -f >> /var/log/cassandra.log &

# Using localhost works well, though note that the host outside the container
# will not be able to connect to the Master properly because Spark reverse-connects
# back to the client.  The host will be able to use the WebUI, though.
/opt/spark/bin/spark-class org.apache.spark.deploy.master.Master -h localhost >> /opt/spark/.master.out 2>> /opt/spark/.master.err < /dev/null &
sleep 10 # Let the master start
/opt/spark/bin/spark-class org.apache.spark.deploy.worker.Worker -h localhost spark://localhost:7077 >> /opt/spark/.worker.out 2>> /opt/spark/.worker.err < /dev/null

sleep infinity
