package testr

import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneOffset}

import com.datastax.driver.core._
import com.datastax.driver.core.policies.{ConstantSpeculativeExecutionPolicy, DCAwareRoundRobinPolicy, TokenAwarePolicy}
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.github.gatling.cql.Predef._

import scala.concurrent.duration.DurationInt
import scala.util.Random


class GatlingSimulationInsertR extends Simulation {
  val cluster = Cluster.builder().addContactPoints("localhost").withAuthProvider(new PlainTextAuthProvider("username", "password"))
      //.withCredentials("username", "password")
    .withPoolingOptions(new PoolingOptions().setConnectionsPerHost(HostDistance.LOCAL, 10, 20).setMaxRequestsPerConnection(HostDistance.LOCAL, 3276)
    .setConnectionsPerHost(HostDistance.REMOTE, 10, 20).setMaxRequestsPerConnection(HostDistance.REMOTE, 3276))
    .withQueryOptions(new QueryOptions().setConsistencyLevel(ConsistencyLevel.ONE).setFetchSize(500))
    .withSpeculativeExecutionPolicy(
      new ConstantSpeculativeExecutionPolicy(
        500, // delay before a new execution is launched
        2 // maximum number of executions
      ))
    //Change WithLocalDC("NY") and set the local cluster you want to read/write from
    .withLoadBalancingPolicy(new TokenAwarePolicy(DCAwareRoundRobinPolicy.builder().build())).build()

  val session = cluster.connect()
  //Your C* session
  val cqlConfig = cql.session(session) //Initialize Gatling DSL with your session

  val readOptin = session.prepare("""select * from test.optin_by_client where id_part_src = ? and cd_app_src = ? and cd_country_src = ?;""")
  val readScnOptin = scenario("optin_by_client").feed(csv("optin_by_client.csv")).exec(cql("optin_by_client").execute(readOptin).withParams("${id_part_src}", "${cd_app_src}", "${cd_country_src}"))

  val readOptin_historical = session.prepare("""select * from test.optin_by_client_historical where id_part_src = ? and cd_app_src = ? and cd_country_src = ?;""")
  val readScnOptin_historical = scenario("optin_by_client_historical").feed(csv("optin_by_client_historical.csv")).exec(cql("optin_by_client_historical").execute(readOptin_historical).withParams("${id_part_src}", "${cd_app_src}", "${cd_country_src}"))

  val insertOptin = session.prepare("""insert into test.optin_by_client (id_part_src, cd_app_src, cd_country_src, owner_alpha_code, cd_optin_type, cd_channel_comm, cd_recipient, cd_optin_owner, cd_newsletter_type, dt_tech_upd, optin_value) values ( ?, 'BP', 'FR', 'DIAC', 1, 4, 0, 2, 0, ?, 1)""")
  val insertScnOptin = scenario("insert optin_by_client")
    .feed((Iterator.continually({
      Map("id_part_src" -> Random.alphanumeric.take(10).mkString,
        "dt_tech_upd" -> Timestamp.valueOf(LocalDateTime.now()))
    }))).exec(cql("insert optin_by_client").execute(insertOptin).withParams("${id_part_src}", "${dt_tech_upd}"))

  val insertOptin_historical = session.prepare("""insert into test.optin_by_client_historical (id_part_src, cd_app_src, cd_country_src, owner_alpha_code, cd_optin_type, cd_channel_comm, cd_recipient, cd_optin_owner, cd_newsletter_type, dt_tech_upd, optin_value) values (?, 'BP', 'FR', 'DIAC', 1, 4, 0, 2, 0, ?, 1);""")
  val insertScnOptin_historical = scenario("insert optin_by_client_historical")
    .feed((Iterator.continually({
      Map("id_part_src" -> Random.alphanumeric.take(10).mkString,
        "dt_tech_upd" ->Timestamp.valueOf(LocalDateTime.now()))
    })))
    .exec(cql("insert optin_by_client_historical").execute(insertOptin_historical).withParams("${id_part_src}", "${dt_tech_upd}"))


  val readPerSecPerQuery = 1000
  val searchPerSecPerQuery = 10
  val rampupDurationSec = 100
  val testDurationSec = 100
  setUp(
    readScnOptin.inject(rampUsersPerSec(1) to readPerSecPerQuery during (rampupDurationSec seconds), rampUsersPerSec(readPerSecPerQuery) to readPerSecPerQuery during (testDurationSec seconds)),
    readScnOptin_historical.inject(rampUsersPerSec(1) to readPerSecPerQuery during (rampupDurationSec seconds), rampUsersPerSec(readPerSecPerQuery) to readPerSecPerQuery during (testDurationSec seconds)),
    insertScnOptin.inject(rampUsersPerSec(1) to searchPerSecPerQuery during (rampupDurationSec seconds), rampUsersPerSec(searchPerSecPerQuery) to searchPerSecPerQuery during (testDurationSec seconds)),
    insertScnOptin_historical.inject(rampUsersPerSec(1) to searchPerSecPerQuery during (rampupDurationSec seconds), rampUsersPerSec(searchPerSecPerQuery) to searchPerSecPerQuery during (testDurationSec seconds))
  ).protocols(cqlConfig)
}