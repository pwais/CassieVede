CassieVede
==========

"Cassandra Sees."  A utility for managing datasets of images in
[Cassandra](http:cassandra.apache.org/).

Quickstart
==========

1. [Install sbt](http:www.scala-sbt.org/release/tutorial/Setup.html)
(e.g. `$ sudo port install sbt`).

2. `$ ./bootstrap.py --all` to run a complete build; see `--help` FMI.

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

In the Cassandra log, you'll see something like: 
```
INFO CLibrary: JNA mlockall successful
```
instead of:
```
CLibrary: Unable to lock JVM memory (ENOMEM). This can result in part of the JVM being swapped out, especially with mmapped I/O enabled. ...
```

TODO
====

docs: discuss decision about images and file size is ok

pq:
 x read a dir of files
 x unit test it
 x lazy load file from disk
 x unit test it 
 * load that into spark RDD and then into cassie
 * unit test it
 x add cache
 * unit test it
 x add async spark parallelize() and save() with a queue of futures,
 max queue length from command line (default to num spark machines)
 * manual test it
 * add ssh tunel
 * manual test it
 * read a tar file
 * unit test it  
 * test loading imagenet tiny into gce
 * zip and tarfile stuff
 * tiny imagenet stuff
 * sweep commented crap
 * squash 


