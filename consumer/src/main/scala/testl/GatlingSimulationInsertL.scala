package testl

import com.datastax.driver.core._
import com.datastax.driver.core.policies.{ConstantSpeculativeExecutionPolicy, DCAwareRoundRobinPolicy, TokenAwarePolicy}
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.github.gatling.cql.Predef._

import scala.concurrent.duration.DurationInt


class GatlingSimulationInsertL extends Simulation {
  val cluster = Cluster.builder().addContactPoints("localhost","localhost","localhost")
    //.withCredentials("username", "password")
    .withPoolingOptions(new PoolingOptions().setConnectionsPerHost(HostDistance.LOCAL, 10, 10).setMaxRequestsPerConnection(HostDistance.LOCAL, 3276)
    .setConnectionsPerHost(HostDistance.REMOTE, 1, 1).setMaxRequestsPerConnection(HostDistance.REMOTE, 32768))
    .withQueryOptions(new QueryOptions().setConsistencyLevel(ConsistencyLevel.ONE).setFetchSize(500))
    .withSpeculativeExecutionPolicy(
      new ConstantSpeculativeExecutionPolicy(
        500, // delay before a new execution is launched
        2 // maximum number of executions
      ))
    //Change WithLocalDC("NY") and set the local cluster you want to read/write from
    .withLoadBalancingPolicy(new TokenAwarePolicy(DCAwareRoundRobinPolicy.builder().withLocalDc("NY").build())).build()

  val session = cluster.connect() //Your C* session
  val cqlConfig = cql.session(session) //Initialize Gatling DSL with your session

  val read_UC1 = session.prepare("""select * from poc.customer where ww_client_id =?  ;""")
  val readScnUC1 = scenario("UC1").feed(csv("read_UC1.csv")).exec(cql("UC1").execute(read_UC1).withParams("${uuid}"))

  val read_UC2 = session.prepare("""select * from poc.customer where solr_query=? limit 10 ;""")
  val readScnUC2 = scenario("UC2").feed(csv("search_UC2.csv")).exec(cql("UC2").execute(read_UC2).withParams("${solr_query}"))

  val update_UC3 = session.prepare("""select * from poc.customer where solr_query= ? limit 10 ;""")
  val readScnUC3 = scenario("UC3").feed(csv("search_UC3.csv")).exec(cql("UC3").execute(update_UC3).withParams("${solr_query}"))

  val update_UC4 = session.prepare("""select * from poc.customer where solr_query= ? limit 10 ;""")
  val readScnUC4 = scenario("UC4").feed(csv("search_UC4_1.csv")).exec(cql("UC4").execute(update_UC4).withParams("${solr_query}"))

  val update_UC5 = session.prepare("""select * from poc.customer where solr_query= ? limit 10 ;""")
  val readScnUC5 = scenario("UC5").feed(csv("search_UC5.csv")).exec(cql("UC5").execute(update_UC5).withParams("${solr_query}"))

  val update_UC8 = session.prepare("""INSERT INTO poc.customer2 (ww_client_id, addresses) VALUES ( 'CL06', {{type:'Home', line_number:2, creation_date:01012010, line_1:'Rue toto', line_2:'Paris',line_3:'19eme arrondissement', postal_code:'75019', city:'Paris',country:'France'} , {type:'Work', line_number:5, creation_date:01012009, line_1:'Rue Euryale', line_2:'Paris',line_3:'1er arron', postal_code:'75001', city:'Paris',country:'France'} });""")
  val readScnUC8 = scenario("UC8").feed(csv("read_UC8_1.csv")).exec(cql("UC8").execute(update_UC8).withParams("${uuid}"))

  val update_UC9 = session.prepare("""UPDATE poc.customer2 SET firstname_local = 'Yassine' where ww_client_id = ?;""")
  val readScnUC9 = scenario("UC9").feed(csv("update_UC9.csv")).exec(cql("UC9").execute(update_UC9).withParams("${uuid}"))

  val update_UC10 = session.prepare("""select * from poc2.transactions where ww_client_id =?;""")
  val readScnUC10 = scenario("UC10").feed(csv("read_UC10.csv")).exec(cql("UC10").execute(update_UC10).withParams("${uuid}"))

  val update_UC11 = session.prepare("""select * from poc2.products_by_customer where ww_client_id = ?;""")
  val readScnUC11 = scenario("UC11").feed(csv("read_UC11.csv")).exec(cql("UC11").execute(update_UC11).withParams("${uuid}"))

  val update_UC12 = session.prepare("""INSERT INTO test.transactions(ww_client_id ,validation_date ,ww_transaction_id ,store_code ,document_type ,transaction_type) VALUES (?,20281228,'G000FD0DD00A003590700','A0E6','1','00');""")
  val readScnUC12 = scenario("UC12").feed(csv("read_UC12_1.csv")).exec(cql("UC12").execute(update_UC12).withParams("${uuid}"))



  val readPerSecPerQuery = 1000
  val searchPerSecPerQuery = 10
  val rampupDurationSec = 100
  val testDurationSec = 100
  setUp(
    readScnUC1.inject(rampUsersPerSec(1) to readPerSecPerQuery during (rampupDurationSec seconds), rampUsersPerSec(readPerSecPerQuery) to readPerSecPerQuery during (testDurationSec seconds)) ,
    readScnUC3.inject(rampUsersPerSec(1) to searchPerSecPerQuery during (rampupDurationSec seconds), rampUsersPerSec(searchPerSecPerQuery) to searchPerSecPerQuery during(testDurationSec seconds)),
    readScnUC4.inject(rampUsersPerSec(1) to searchPerSecPerQuery during (rampupDurationSec seconds), rampUsersPerSec(searchPerSecPerQuery) to searchPerSecPerQuery during(testDurationSec seconds)),
    readScnUC5.inject(rampUsersPerSec(1) to searchPerSecPerQuery during (rampupDurationSec seconds), rampUsersPerSec(searchPerSecPerQuery) to searchPerSecPerQuery during(testDurationSec seconds)),
    readScnUC8.inject(rampUsersPerSec(1) to readPerSecPerQuery during (rampupDurationSec seconds), rampUsersPerSec(readPerSecPerQuery) to readPerSecPerQuery during (testDurationSec seconds)) ,
    readScnUC9.inject(rampUsersPerSec(1) to readPerSecPerQuery during (rampupDurationSec seconds), rampUsersPerSec(readPerSecPerQuery) to readPerSecPerQuery during (testDurationSec seconds)) ,
    readScnUC10.inject(rampUsersPerSec(1) to readPerSecPerQuery during (rampupDurationSec seconds), rampUsersPerSec(readPerSecPerQuery) to readPerSecPerQuery during (testDurationSec seconds)),
    readScnUC11.inject(rampUsersPerSec(1) to readPerSecPerQuery during (rampupDurationSec seconds), rampUsersPerSec(readPerSecPerQuery) to readPerSecPerQuery during (testDurationSec seconds)),
    readScnUC12.inject(rampUsersPerSec(1) to readPerSecPerQuery during (rampupDurationSec seconds), rampUsersPerSec(readPerSecPerQuery) to readPerSecPerQuery during (testDurationSec seconds))
  ).protocols(cqlConfig)
}

