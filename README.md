CassieVede
==========

"Cassandra Sees."  A utility for managing datasets of images in
[Cassandra](http://cassandra.apache.org/).

Quickstart
==========

1. [Install sbt](http://www.scala-sbt.org/release/tutorial/Setup.html)
(e.g. `$ sudo port install sbt`).

2. `$ sbt eclipse with-source=true` to generate Eclipse project files
with source attachment.

Running in Docker
=================

Mac OS X / Boot2Docker
======================

Note that Cassandra and Spark require high limits on processes, number of files,
and (especially) memory locking (so that JVM calls to mmap work correctly).
If you're using Docker on Mac OS X, you'll want to modify the daemon to
increase these ulimits as follows:

1. Log in to the boot2docker machine: `mac $ boot2docker ssh`

2. Run as root `root@boot2docker# sudo su`

3. Edit docker settings as follows:
   `# vi /etc/init.d/docker`

   Before `start()`, add:
   ```
   ulimit -n 1048576
   ulimit -p 1048576
   ulimit -l unlimited
   ```

4. To check, run `mac $ bootstrap.py --indocker` and then run 
`root@caa0ab651cd9:/opt/CassieVede# ulimit -a`.  You should see:

```
...
max locked memory       (kbytes, -l) unlimited <-- Success!
open files                      (-n) 1048576   <-- Success!
max user processes              (-u) 1048576   <-- Success!
...
```


discuss decision about images and file size is ok
