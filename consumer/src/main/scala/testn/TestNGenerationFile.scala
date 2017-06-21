package testn

import java.io.{File, PrintWriter}
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset}

import com.datastax.driver.core.utils.UUIDs

import scala.util.Random


object TestNGenerationFile extends App {
  val random = new Random()

  val formatter = DateTimeFormatter.ofPattern("yyyy-mm-dd HH:mm:ss")

  def getRandomStr(lenght: Int) = Random.alphanumeric take lenght mkString ("")

  def getRandom(list: List[String]): String = list(random.nextInt(list.length))

  def getRandomDate(): String = formatter.format(LocalDateTime.ofEpochSecond(Math.abs(random.nextInt(999999999)), 0, ZoneOffset.UTC))


  generateEvents(100000)
  generateDocuments(10000)

  def generateEvents(numLines: Int) = {

    val event_originator: List[String] = (0 to 10).map(i => getRandomStr(20).toString).toList
    val event_type: List[String] = (0 to 10).map(i => getRandomStr(20).toString).toList
    val event_code: List[String] = (0 to 100).map(i => getRandomStr(20).toString).toList
    val object_parent_status_code: List[String] = (0 to 100).map(i => getRandomStr(20).toString).toList


    val csvFields = Array("event_id", "object_parent_id", "event_originator", "event_type", "event_code", "event_date_time", "event_location",
      "object_parent_status_code", "notified", "event_meta_data", "event_details", "deleted", "owner", "channel")

    val writer = new PrintWriter(new File("/home/quentin/tools/gatling-charts-highcharts-bundle-2.2.2/user-files/data/test-events.csv"))
    try {
      writer.write(csvFields.mkString(","))
      for (i <- 0 to numLines) {
        val arr = Array(UUIDs.timeBased().toString, UUIDs.timeBased().toString, getRandom(event_originator), getRandom(event_type), getRandom(event_code), getRandomDate(), getRandomStr(10),
          getRandom(object_parent_status_code), random.nextBoolean().toString, getRandomStr(2000), getRandomStr(100), random.nextBoolean().toString, getRandomStr(20),
          getRandomStr(20))
        writer.write(arr.mkString(","))
      }
    } finally {
      writer.close()
    }
  }


  def generateDocuments(numLines: Int) = {
    val document_object_type: List[String] = (0 to 10).map(i => getRandomStr(20).toString).toList

    val csvFields = Array("document_id", "document_data_id", "document_object_type", "document_primary_id", "document_secondary_id", "document_tertiary_id", "document_quaternary_id",
      "document", "document_creation_date_time", "document_update_date_time", "document_type", "document_template", "document_meta_data", "document_description", "deleted")

    val writer = new PrintWriter(new File("/home/quentin/tools/gatling-charts-highcharts-bundle-2.2.2/user-files/data/test-documents.csv"))
    try {
      writer.write(csvFields.mkString(","))
      for (i <- 0 to numLines) {
        val arr = Array(UUIDs.timeBased().toString, getRandomStr(20), getRandom(document_object_type), getRandomStr(20), getRandomStr(20), getRandomStr(20), getRandomStr(20)
          , getRandomStr(20000), getRandomDate(), getRandomDate(), getRandomStr(20), random.nextBoolean().toString, getRandomStr(2000), getRandomStr(200), random.nextBoolean().toString)
        writer.write(arr.mkString(","))
      }
    } finally {
      writer.close()
    }
  }
}