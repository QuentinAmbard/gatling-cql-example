package testn

import java.util.{Date, UUID}

import com.datastax.driver.core._
import com.datastax.driver.core.utils.UUIDs
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.github.gatling.cql.Predef._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.DurationInt
import scala.util.Random


class TestNGatlingSimulationNoCSV extends Simulation {
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
  val random = new Random()

  def getRandomStr(lenght: Int) = Random.alphanumeric take lenght mkString ("")

  def getRandom(list: Array[String]): String = list(random.nextInt(list.length))

  def getRandomDate(): Date = new Date(Math.abs(random.nextInt(999999999)))

  //Should be changed to blob
  val documentAsStr = scala.io.Source.fromFile("/home/quentin/Downloads/example.pdf").mkString


  val event_originator: Array[String] = (0 to 10).map(i => getRandomStr(20).toString).toArray
  val event_type: Array[String] = (0 to 10).map(i => getRandomStr(20).toString).toArray
  val event_code: Array[String] = (0 to 100).map(i => getRandomStr(20).toString).toArray
  val object_parent_status_code: Array[String] = (0 to 100).map(i => getRandomStr(20).toString).toArray

  val uuids: Array[UUID] = (0 to 1000000).map(i => UUIDs.timeBased()).toArray

  val writeEvents = session.prepare(
    """insert into test.events ( event_id, object_parent_id, event_originator , event_type , event_code , event_date_time, event_location ,
                                      object_parent_status_code , notified , event_meta_data , event_details , deleted, owner , channel) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?);""")


  val writeEventScn = scenario("Write events").feed((Iterator.continually({
    Map(
      "event_id" -> uuids(random.nextInt(uuids.length)),
      "object_parent_id" -> uuids(random.nextInt(uuids.length)),
      "event_originator" -> getRandom(event_originator),
      "event_type" -> getRandom(event_type),
      "event_code" -> getRandom(event_code),
      "event_date_time" -> getRandomDate(),
      "event_location" -> getRandomStr(10),
      "object_parent_status_code" -> getRandom(object_parent_status_code),
      "notified" -> random.nextBoolean().toString,
      "event_meta_data" -> getRandomStr(2000),
      "event_details" -> getRandomStr(100),
      "deleted" -> random.nextBoolean().toString,
      "owner" -> getRandomStr(20),
      "channel" -> getRandomStr(20))
  }))).exec(cql("write events").execute(writeEvents).withParams("${event_id}", "${object_parent_id}", "${event_originator}", "${event_type}", "${event_code}", "${event_date_time}", "${event_location}", "${object_parent_status_code}"
    , "${notified}", "${event_meta_data}", "${event_details}", "${deleted}", "${owner}", "${channel}"))


  val primaryIds = ArrayBuffer[String]("sdfsdf", "azdfsd")

  val readEvents = session.prepare("""select * from test.events where event_id = ? limit 1 ;""")
  val readEventScn = scenario("Read events").feed((Iterator.continually({
    Map("event_id" -> uuids(random.nextInt(uuids.length)))
  }))).exec(cql("Read events").execute(readEvents).withParams("${event_id}"))


  /** DOCUMENTS **/
  val document_object_type: Array[String] = (0 to 10).map(i => getRandomStr(20).toString).toArray
  val document_type: Array[String] = (0 to 10).map(i => getRandomStr(20).toString).toArray

  val writeDocuments = session.prepare(
    """insert into test.documents ( document_id, document_data_id, document_object_type, document_primary_id, document_secondary_id, document_tertiary_id,
      document_quaternary_id, document, document_creation_date_time, document_update_date_time, document_type, document_template, document_meta_data, document_description, deleted)
      values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);""")

  val writeDocumentScn = scenario("Write documents").feed((Iterator.continually({
    val primaryId = getRandomStr(20)
    primaryIds.append(primaryId)
    Map(
      "document_id" -> uuids(random.nextInt(uuids.length-1)),
      "document_data_id" -> getRandomStr(20),
      "document_object_type" -> getRandom(document_object_type),
      "document_primary_id" -> primaryId,
      "document_secondary_id" -> getRandomStr(20),
      "document_tertiary_id" -> getRandomStr(20),
      "document_quaternary_id" -> getRandomStr(20),
      "document" -> documentAsStr,
      "document_creation_date_time" -> getRandomDate(),
      "document_update_date_time" -> getRandomDate(),
      "document_type" -> getRandom(document_type),
      "document_template" -> random.nextBoolean(),
      "document_meta_data" -> getRandomStr(2000),
      "document_description" -> getRandomStr(200),
      "deleted" -> random.nextBoolean())
  }))).exec(cql("write documents").execute(writeDocuments).withParams("${document_id}", "${document_data_id}", "${document_object_type}", "${document_primary_id}", "${document_secondary_id}",
    "${document_tertiary_id}", "${document_quaternary_id}", "${document}", "${document_creation_date_time}", "${document_update_date_time}", "${document_type}", "${document_template}",
    "${document_meta_data}", "${document_description}", "${deleted}"))

  val readDocuments = session.prepare("""select * from test.documents where document_id = ? limit 1 ;""")
  val readDocumentScn = scenario("Read documents").feed((Iterator.continually({
    Map("document_id" -> s"""{"q":"primary_id:${uuids(random.nextInt(uuids.length))}"} """)
  }))).exec(cql("Read documents").execute(readDocuments).withParams("${document_id}"))



  val writeEventsPerSecPerQuery = 100
  val readEventsPerSecPerQuery = 300

  val writeDocumentsPerSecPerQuery = 100
  val readDocumentsPerSecPerQuery = 300

  val testDurationSec = 500
  setUp(writeEventScn.inject(rampUsersPerSec(1) to writeEventsPerSecPerQuery during (20 seconds), rampUsersPerSec(writeEventsPerSecPerQuery) to writeEventsPerSecPerQuery during (testDurationSec seconds))
    ,readEventScn.inject(rampUsersPerSec(1) to readEventsPerSecPerQuery during (20 seconds), rampUsersPerSec(readEventsPerSecPerQuery) to readEventsPerSecPerQuery during (testDurationSec seconds))

    ,writeDocumentScn.inject(rampUsersPerSec(1) to writeDocumentsPerSecPerQuery during (20 seconds), rampUsersPerSec(writeDocumentsPerSecPerQuery) to writeDocumentsPerSecPerQuery during (testDurationSec seconds))
    ,readDocumentScn.inject(rampUsersPerSec(1) to readDocumentsPerSecPerQuery during (20 seconds), rampUsersPerSec(readDocumentsPerSecPerQuery) to readDocumentsPerSecPerQuery during (testDurationSec seconds))

  ).protocols(cqlConfig)
}

