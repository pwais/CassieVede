import collection.mutable.Stack
import org.scalatest._
import org.apache.spark.SparkContext
import org.apache.log4j.Logger
import org.apache.log4j.Level
import akka.event.Logging
import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import cassievede.TastCQLDataSet
import org.cassandraunit.CassandraCQLUnit
import org.junit.Test
import org.junit.Rule
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

class ExampleSpec extends FlatSpec with Matchers {

  "A Stack" should "pop values in last-in-first-out order" in {
    val stack = new Stack[Int]
    stack.push(1)
    stack.push(2)
    stack.pop() should be (2)
    stack.pop() should be (1)
  }

  it should "throw NoSuchElementException if an empty stack is popped" in {
    val emptyStack = new Stack[Int]
    a [NoSuchElementException] should be thrownBy {
      emptyStack.pop()
    }
  }

  it should "be a moof" in {
    val stack = new Stack[String]
	  stack.push("moof")
	  stack.pop()
	  stack.size should be (0)


    val k =
"""
CREATE KEYSPACE cassievede
WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

CREATE TABLE cassievede.dataset
  (id int PRIMARY KEY,
   name varchar);

CREATE TABLE cassievede.image
  (datasetid int,              // Foreign key: dataset.id
   partitionid varint,         // Optionally partition a very large dataset
   name varchar,               // Name of the image, e.g. original filename
   data blob,                  // Raw image file (e.g. jpeg data)
   classnames list<varchar>,   // Label classnames (e.g. for cropped objects)
   width varint,               // (Optional hint) known width of image
   height varint,              // (Optional hint) known height of image
   geoid varint,               // (Optional hint) geohash key (e.g. from EXIF)
   longitude double,           // (Optional hint) (e.g. from EXIF)
   latitude double,            // (Optional hint) (e.g. from EXIF)
   extra map<varchar, blob>,   // (Optional) K-v of serialized data

   PRIMARY KEY ((datasetid, partitionid), name))
                               // (datasetid, partitionid) -> max 4B names
WITH
  caching = 'keys_only' AND    // Don't cache image data!
  compression =                // TODO: Try LZ4
    {'sstable_compression': ''} AND
  compaction =                 // Optimize for reads
    { 'class' :  'LeveledCompactionStrategy'  };


"""
  }
}





//@RunWith(classOf[JUnit4])
//class CassaTest {
//
//  var unit = new CassandraCQLUnit(new TastCQLDataSet())
//
//  @Rule def getUnit() { unit }
//
//  @Test def basicTast() {
////    EmbeddedCassandraServerHelper.startEmbeddedCassandra()
////    val
//    val unit = getUnit()
//    val result = unit.session.execute("select * from mytable WHERE id='myKey01'");
//    val v = result.iterator().next().getString("value")
////    v should be ("myValue01")
//    assertEquals("myValue01", v)
//    println(v)
//    println("meow")
//    println("meow")
//    println("meow")
//    println("meow")
//    println("meow")
//  }
//
//}









object SparkTest extends org.scalatest.Tag("com.qf.test.tags.SparkTest")

trait SparkTestUtils extends FunSuite {
  var sc: SparkContext = _

  /**
   * convenience method for tests that use spark.  Creates a local spark context, and cleans
   * it up even if your test fails.  Also marks the test with the tag SparkTest, so you can
   * turn it off
   *
   * By default, it turn off spark logging, b/c it just clutters up the test output.  However,
   * when you are actively debugging one test, you may want to turn the logs on
   *
   * @param name the name of the test
   * @param silenceSpark true to turn off spark logging
   */
  def sparkTest(name: String, silenceSpark : Boolean = true)(body: => Unit) {
    test(name, SparkTest){
      val origLogLevels = if (silenceSpark) SparkUtil.silenceSpark() else null
      sc = new SparkContext("local[4]", name)
      try {
        body
      }
      finally {
        sc.stop
        sc = null
        // To avoid Akka rebinding to the same port, since it doesn't unbind immediately on shutdown
        System.clearProperty("spark.master.port")
//        if (silenceSpark) Logging.setLogLevels(origLogLevels)
      }
    }
  }
}

object SparkUtil {
  def silenceSpark() {
    setLogLevels(Level.WARN, Seq("spark", "org.eclipse.jetty", "akka"))
  }

  def setLogLevels(level: org.apache.log4j.Level, loggers: TraversableOnce[String]) = {
    loggers.map{
      loggerName =>
        val logger = Logger.getLogger(loggerName)
        val prevLevel = logger.getLevel()
        logger.setLevel(level)
        loggerName -> prevLevel
    }.toMap
  }

}

class OurAwesomeClassTest extends SparkTestUtils with ShouldMatchers {
  sparkTest("spark filter") {
    val data = sc.parallelize(1 to 1e6.toInt)
    data.filter{_ % 2 == 0}.count should be (5e5.toInt)
  }

  test("non-spark code") {
    val x = 17
    val y = 3
//    OurAwesomeClass.plus(x,y) should be (20)
  }
}

