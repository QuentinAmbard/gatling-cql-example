import java.time.{LocalDateTime, ZoneId}
import java.util.{Calendar, Date, UUID}

import com.datastax.driver.core._
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.github.gatling.cql.Predef._

import scala.collection.mutable
import scala.concurrent.duration.DurationInt
import scala.util.Random


class GatlingSimulationInsert extends Simulation {
  val cluster = Cluster.builder().addContactPoint("10.240.0.2") // .addContactPoints("10.240.0.24","10.240.0.23","10.240.0.25")//.addContactPoint("127.0.0.1")
    .withPoolingOptions(new PoolingOptions().setConnectionsPerHost(HostDistance.LOCAL, 10, 10).setMaxRequestsPerConnection(HostDistance.LOCAL, 3276)
    .setConnectionsPerHost(HostDistance.REMOTE, 1, 1).setMaxRequestsPerConnection(HostDistance.REMOTE, 32768))
    .withQueryOptions(new QueryOptions().setConsistencyLevel(ConsistencyLevel.ONE).setFetchSize(500))
    .build()
  val session = cluster.connect() //Your C* session

  val cqlConfig = cql.session(session) //Initialize Gatling DSL with your session

  //Prepare your statement, we want to be effective, right?
  val readAlert1 = session.prepare("""INSERT INTO umbrella.sensor_data (sensor_id , bucket_ts , time , hive , latlong , level , sensor_type , value , vendor , wing ) VALUES ( ?,?,?,?,?,?,?,?,?,?)""")

  val random = new util.Random

  def randomDate(from: LocalDateTime, to: LocalDateTime): Date = {
    val secondsInYear = 360 * 24 * 60 * 60
    val random = new Random(System.nanoTime)
    // You may want a different seed
    val ldt = from.plusSeconds(random.nextInt(secondsInYear))
    Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
  }

  val dates = mutable.Map[UUID, Int]()
  var sensoruuids = (1 to 14800).map(_ => {val uuid = UUID.randomUUID(); dates(uuid) = 0; uuid })

  val readScn1 = scenario("geospacial search with facet").repeat(1) {
    feed(Iterator.continually({
      val r = Random.nextInt(sensoruuids.size)
      val vendor = r % 10
      val hive = r % 4
      val lat = r % 87
      val lat_d = r % 56
      val long = r % 87
      val long_d = r % 46
      val sensorUuid: UUID = sensoruuids(Random.nextInt(sensoruuids.size))
      val count = dates(sensorUuid)
      dates(sensorUuid) = count + 1
      Map(
        "sensor_id" -> sensorUuid,
        "bucket_ts" -> new java.util.Date((count / 20000).toLong*1000),
        "time" -> new java.util.Date((count).toLong*1000),
        "hive" -> hive,
        "latlong" -> s"$lat.$lat_d, $long.$long_d",
        "level" -> vendor,
        "sensor_type" -> s"vendor-$vendor",
        "value" -> 1.03f,
        "vendor" -> s"vendor-$vendor",
        "wing" -> s"wing-$vendor"
      )
    })).exec(cql("prepared SELECT geospacial search with facet").execute(readAlert1)
      .withParams("${sensor_id}","${bucket_ts}","${time}","${hive}","${latlong}","${level}","${sensor_type}","${value}","${vendor}","${wing}")



      .consistencyLevel(ConsistencyLevel.ONE)
    )
  }


  val readPerSecPerQuery = 15000
  val testDurationSec = 100
  setUp(readScn1.inject(rampUsersPerSec(1) to readPerSecPerQuery during (20 seconds), rampUsersPerSec(readPerSecPerQuery) to readPerSecPerQuery during (testDurationSec seconds)) //,
    //readScn2.inject(rampUsersPerSec(1) to readPerSecPerQuery during (20 seconds), rampUsersPerSec(readPerSecPerQuery) to readPerSecPerQuery during(testDurationSec seconds))//,
    //readScn2.inject(rampUsersPerSec(10) to readPerSecPerQuery during (20 seconds), constantUsersPerSec(readPerSecPerQuery) during(testDurationSec seconds)),
    //readScn3.inject(rampUsersPerSec(10) to readPerSecPerQuery during (20 seconds), constantUsersPerSec(readPerSecPerQuery) during(testDurationSec seconds)),
    //readScn4.inject(rampUsersPerSec(10) to readPerSecPerQuery during (20 seconds), constantUsersPerSec(readPerSecPerQuery) during(testDurationSec seconds))
  ).protocols(cqlConfig)
}

