//package cassievede;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import org.cassandraunit.dataset.CQLDataSet;
//
//public class TastCQLDataSet implements CQLDataSet {
//
//  @Override
//  public List<String> getCQLStatements() {
//    ArrayList<String> s = new ArrayList<String>();
//    s.add(
//        "CREATE TABLE myTable(" +
//        "id varchar," +
//        "value varchar," +
//        "PRIMARY KEY(id));");
//    
//    s.add("INSERT INTO myTable(id, value) values('myKey01','myValue01');");
//    s.add("INSERT INTO myTable(id, value) values('myKey02','myValue02');");
//    return s;
//  }
//
//  @Override
//  public String getKeyspaceName() {
//    return "tastKeyspaceName";
//  }
//
//  @Override
//  public boolean isKeyspaceCreation() {
//    return true;
//  }
//
//  @Override
//  public boolean isKeyspaceDeletion() {
//    return false;
//  }
//
//}
