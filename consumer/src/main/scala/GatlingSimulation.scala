package cassandra

import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import com.datastax.driver.core._
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.github.gatling.cql.Predef._

import scala.concurrent.duration.DurationInt
import scala.util.Random


class GatlingSimulation extends Simulation {
  val cluster = Cluster.builder().addContactPoint("10.240.0.2") // .addContactPoints("10.240.0.24","10.240.0.23","10.240.0.25")//.addContactPoint("127.0.0.1")
    .withPoolingOptions(new PoolingOptions().setConnectionsPerHost(HostDistance.LOCAL, 1, 1).setMaxRequestsPerConnection(HostDistance.LOCAL, 32768)
    .setConnectionsPerHost(HostDistance.REMOTE, 1, 1).setMaxRequestsPerConnection(HostDistance.REMOTE, 32768))
    .withQueryOptions(new QueryOptions().setConsistencyLevel(ConsistencyLevel.ONE).setFetchSize(500))
    .build()
    val session = cluster.connect() //Your C* session

  val cqlConfig = cql.session(session) //Initialize Gatling DSL with your session

  //Prepare your statement, we want to be effective, right?
  val readAlert1 = session.prepare("""select * from umbrella.sensor_data where solr_query= ? ;""")

  val random = new util.Random

  def randomDate(from: LocalDateTime, to: LocalDateTime): Date = {
    val secondsInYear = 360 * 24 * 60 * 60
    val random = new Random(System.nanoTime)
    // You may want a different seed
    val ldt = from.plusSeconds(random.nextInt(secondsInYear))
    Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
  }

  //select * from umbrella.sensor_data where solr_query='{"q":"*:*", "facet":{"field":"level"}, "fq":"+{!geofilt pt="6.477, 9.789" sfield=latlong d=2}"}'

  var count = 0

  val readScn1 = scenario("geospacial search with facet").repeat(1) {
    feed(Iterator.continually({
      val query = random.nextInt(10) match {
        case i if i < 4 => s"""{"q":"*:*", "facet":{"field":"wing"}, "fq":"+{!geofilt pt=\\"${random.nextInt(10)}.${random.nextInt(1000)}, ${random.nextInt(100)}.${random.nextInt(1000)}\\" sfield=latlong d=${random.nextInt(6)}}"}"""
        case i if i < 8 => s"""{"q":"*:*", "facet":{"field":"level"}, "fq":"+{!geofilt pt=\\"${random.nextInt(10)}.${random.nextInt(1000)}, ${random.nextInt(100)}.${random.nextInt(1000)}\\" sfield=latlong d=${random.nextInt(6)}}"}"""
        case _ => s"""{"q":"*:*", "facet":{"field":"hive"}}"""
      }
      println(query)
      Map(
        "solr_query" -> query
      )
    })).exec(cql("prepared SELECT geospacial search with facet").execute(readAlert1)
      .withParams("${solr_query}")
      .consistencyLevel(ConsistencyLevel.ONE)
    )
  }

  val readScn2 = scenario("geospacial search").repeat(1) {
    feed(Iterator.continually({
      Map(
        "solr_query" -> s"""{"q":"*:*", "fq":"+{!geofilt pt=\\"${random.nextInt(10)}.${random.nextInt(1000)}, ${random.nextInt(100)}.${random.nextInt(1000)}\\" sfield=latlong d=${random.nextInt(6)}}"}"""
      )
    })).exec(cql("prepared SELECT geospacial search").execute(readAlert1)
      .withParams("${solr_query}")
      .consistencyLevel(ConsistencyLevel.ONE)
    )
  }


  val readPerSecPerQuery = 10
  val testDurationSec = 100
  setUp(readScn1.inject(rampUsersPerSec(1) to readPerSecPerQuery during (20 seconds), rampUsersPerSec(readPerSecPerQuery) to readPerSecPerQuery during (testDurationSec seconds)) ,
    readScn2.inject(rampUsersPerSec(1) to readPerSecPerQuery during (20 seconds), rampUsersPerSec(readPerSecPerQuery) to readPerSecPerQuery during(testDurationSec seconds))//,
    //readScn2.inject(rampUsersPerSec(10) to readPerSecPerQuery during (20 seconds), constantUsersPerSec(readPerSecPerQuery) during(testDurationSec seconds)),
    //readScn3.inject(rampUsersPerSec(10) to readPerSecPerQuery during (20 seconds), constantUsersPerSec(readPerSecPerQuery) during(testDurationSec seconds)),
    //readScn4.inject(rampUsersPerSec(10) to readPerSecPerQuery during (20 seconds), constantUsersPerSec(readPerSecPerQuery) during(testDurationSec seconds))
  ).protocols(cqlConfig)
}

