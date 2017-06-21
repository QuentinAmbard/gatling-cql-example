package testn

import com.datastax.driver.core._
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.github.gatling.cql.Predef._

import scala.concurrent.duration.DurationInt


class TestNGatlingSimulation extends Simulation {
  val codecRegistry = new CodecRegistry()
  codecRegistry.register(TimeUUIDCodec.instance)
  codecRegistry.register(TimestampCodec.instance)
  codecRegistry.register(BooleanCodec.instance)


  val cluster = Cluster.builder().addContactPoint("127.0.0.1")
    .withPoolingOptions(new PoolingOptions().setConnectionsPerHost(HostDistance.LOCAL, 1, 1).setMaxRequestsPerConnection(HostDistance.LOCAL, 32768)
      .setConnectionsPerHost(HostDistance.REMOTE, 1, 1).setMaxRequestsPerConnection(HostDistance.REMOTE, 32768))
    .withQueryOptions(new QueryOptions().setConsistencyLevel(ConsistencyLevel.ONE).setFetchSize(500))
    .withCodecRegistry(codecRegistry)
    .withCredentials("cassandra", "cassandra")
    .build()

  val session = cluster.connect()
  val cqlConfig = cql.session(session)

  val writeEvents = session.prepare(
    """insert into test.events ( event_id, object_parent_id, event_originator , event_type , event_code , event_date_time, event_location ,
                                      object_parent_status_code , notified , event_meta_data , event_details , deleted, owner , channel) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?);""")

  val writeEventScn = scenario("Write events").feed(csv("test-events.csv")).repeat(10) {
    exec(cql("Write events").execute(writeEvents)
      .withParams("${event_id}", "${object_parent_id}", "${event_originator}", "${event_type}", "${event_code}", "${event_date_time}", "${event_location}", "${object_parent_status_code}"
        , "${notified}", "${event_meta_data}", "${event_details}", "${deleted}", "${owner}", "${channel}"))
  }

  val readEvents = session.prepare(
    """select * from test.events where event_id = (?);""")
  val readEventScn = scenario("Read events").feed(csv("test-events.csv")).repeat(10) {
    exec(cql("Read events").execute(writeEvents)
      .withParams("${event_id}"))
  }



  val writePerSecPerQuery = 1000
  val readPerSecPerQuery = 3000
  val testDurationSec = 100
  setUp(writeEventScn.inject(rampUsersPerSec(1) to writePerSecPerQuery during (20 seconds), rampUsersPerSec(writePerSecPerQuery) to writePerSecPerQuery during (testDurationSec seconds)),
    readEventScn.inject(rampUsersPerSec(1) to readPerSecPerQuery during (20 seconds), rampUsersPerSec(readPerSecPerQuery) to readPerSecPerQuery during (testDurationSec seconds))

  ).protocols(cqlConfig)
}

