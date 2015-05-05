//package cassievede;
//
//import static org.junit.Assert.assertEquals;
//
//import org.cassandraunit.CassandraCQLUnit;
//import org.junit.ClassRule;
//import org.junit.Test;
//
//import com.datastax.driver.core.Cluster;
//import com.datastax.driver.core.ResultSet;
//
//public class TastCassieTast {
//  @ClassRule
//  public static CassandraCQLUnit unit =
//      new CassandraCQLUnit(new TastCQLDataSet());
//  
////  @BeforeClass
////  public static void before() throws Exception {
////    EmbeddedCassandraServerHelper.startEmbeddedCassandra();
////  }
////  
////  @After
////  public void after() throws Exception {
////    EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
////  }
//  
//  @Test
//  public void basicTest() {
//    ResultSet result =
//        unit.session.execute("select * from mytable WHERE id='myKey01'");
//    String v = result.iterator().next().getString("value");
//    assertEquals("myValue01", v);
//    System.out.println(v);
//    System.out.println(v);
//    System.out.println(v);
//    
//    System.out.println(
//        Cluster.builder().addContactPoint("127.0.0.1").withPort(9142).build()
//        .connect().execute("describe tastKeyspaceName"));
//  }
//}
