val globalSettings = Seq(
  version := "0.1",
  scalaVersion := "2.11.8"
)

val akkaVersion = "2.3.12"
val sparkVersion = "1.6.1"
val sparkCassandraConnectorVersion = "1.6.0-M2"
val cassandraDriverVersion = "2.1.7.1"
val kafkaVersion = "0.9.0.1"


lazy val consumer = (project in file("consumer"))
  .settings(name := "consumer")
  .settings(globalSettings:_*)
  .settings(libraryDependencies ++= consumerDeps)
  .settings(resolvers += "Job Server Bintray" at "https://dl.bintray.com/spark-jobserver/maven")

lazy val consumerDeps = Seq(
  "net.liftweb" %% "lift-json" % "2.6.3",
  "io.gatling" % "gatling-core" % "2.2.3",
  "spark.jobserver" %% "job-server-api" % "0.6.2" ,//% "provided",
  "com.datastax.spark" % "spark-cassandra-connector_2.10" % sparkCassandraConnectorVersion,
  "org.apache.spark"  %% "spark-mllib"           % sparkVersion ,//% "provided",
  "org.apache.spark"  %% "spark-graphx"          % sparkVersion ,//% "provided",
  "org.apache.spark"  %% "spark-sql"             % sparkVersion ,//% "provided",
  "org.apache.spark"  %% "spark-streaming"       % sparkVersion ,//% "provided",
  "org.apache.spark"  %% "spark-streaming-kafka" % sparkVersion ,//% "provided",
  "com.databricks"    %% "spark-csv"             % "1.2.0",
  "au.com.bytecode" % "opencsv" % "2.4"
)

resolvers += "Job Server Bintray" at "https://dl.bintray.com/spark-jobserver/maven"
