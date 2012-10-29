import java.io.File

import sbt._
import Keys._

import com.mojolly.scalate.ScalatePlugin._
import com.mojolly.scalate.ScalatePlugin.ScalateKeys._

object BuildSettings {
  val buildOrganization = "dev.example"
  val buildVersion      = "0.1-SNAPSHOT"
  val buildScalaVersion = "2.9.2"

  val buildSettings = Defaults.defaultSettings ++ Seq (
    organization := buildOrganization,
    version      := buildVersion,
    scalaVersion := buildScalaVersion
  )
}

object TemplateSettings {
  val templateSettings = scalateSettings ++ Seq(
    scalateTemplateDirectory.in(Compile) := new File("src/main/webapp/WEB-INF")
  )
}

object Resolvers {
  val typesafeRepo  = "Typesafe Repo"   at "http://repo.typesafe.com/typesafe/releases/"
}

object Versions {
  val Akka   = "2.0.3"
  val Jersey = "1.9.1"
  val Jetty  = "8.0.4.v20111024"
  val Spring = "3.1.0.RELEASE"
}

object Dependencies {
  import Versions._

  // compile dependencies
  lazy val akkaActor    = "com.typesafe.akka"          % "akka-actor"        % Akka           % "compile"
  lazy val jsr311       = "javax.ws.rs"                % "jsr311-api"        % "1.1.1"        % "compile"
  lazy val jerseyCore   = "com.sun.jersey"             % "jersey-core"       % Jersey         % "compile"
  lazy val jerseyJson   = "com.sun.jersey"             % "jersey-json"       % Jersey         % "compile"
  lazy val jerseyServer = "com.sun.jersey"             % "jersey-server"     % Jersey         % "compile"
  lazy val jerseySpring = "com.sun.jersey.contribs"    % "jersey-spring"     % Jersey         % "compile"
  lazy val eventsourced = "org.eligosource"           %% "eventsourced"      % "0.4-SNAPSHOT" % "compile"
  lazy val scalate      = "org.fusesource.scalate"     % "scalate-core"      % "1.5.2"        % "compile"
  lazy val scalaStm     = "org.scala-tools"           %% "scala-stm"         % "0.5"          % "compile"
  lazy val scalaz       = "org.scalaz"                %% "scalaz-core"       % "6.0.4"        % "compile"
  lazy val springWeb    = "org.springframework"        % "spring-web"        % Spring         % "compile"

  // container dependencies TODO: switch from "compile" to "container" when using xsbt-web-plugin
  lazy val jettyServer  = "org.eclipse.jetty"          % "jetty-server"      % Jetty   % "compile"
  lazy val jettyServlet = "org.eclipse.jetty"          % "jetty-servlet"     % Jetty   % "compile"
  lazy val jettyWebapp  = "org.eclipse.jetty"          % "jetty-webapp"      % Jetty   % "compile"

  // runtime dependencies
  lazy val configgy  = "net.lag" % "configgy" % "2.0.0" % "runtime"

  // test dependencies
  lazy val scalatest = "org.scalatest" %% "scalatest" % "1.8" % "test"
}

object ExampleBuild extends Build {
  import BuildSettings._
  import TemplateSettings._
  import Resolvers._
  import Dependencies._

  lazy val example = Project (
    "eventsourced-example",
    file("."),
    settings = buildSettings ++ templateSettings ++ Seq (
      resolvers            := Seq (typesafeRepo),
      // compile dependencies (backend)
      libraryDependencies ++= Seq (akkaActor, scalaStm, eventsourced, scalaz),
      // compile dependencies (frontend)
      libraryDependencies ++= Seq (jsr311, jerseyCore, jerseyJson, jerseyServer, jerseySpring, springWeb, scalate),
      // container dependencies
      libraryDependencies ++= Seq (jettyServer, jettyServlet, jettyWebapp),
      // runtime dependencies
      libraryDependencies ++= Seq (configgy),
      // test dependencies
      libraryDependencies ++= Seq (scalatest),
      // tasks with custom boot classpath
      mainRunNobootcpSetting,
      testRunNobootcpSetting
    )
  )

  val runNobootcp =
    InputKey[Unit]("run-nobootcp", "Runs main classes without Scala library on the boot classpath")

  val mainRunNobootcpSetting = runNobootcp <<= runNobootcpInputTask(Runtime)
  val testRunNobootcpSetting = runNobootcp <<= runNobootcpInputTask(Test)

  def runNobootcpInputTask(configuration: Configuration) = inputTask {
    (argTask: TaskKey[Seq[String]]) => (argTask, streams, fullClasspath in configuration) map { (at, st, cp) =>
      val runCp = cp.map(_.data).mkString(java.io.File.pathSeparator)
      val runOpts = Seq("-classpath", runCp) ++ at
      val result = Fork.java.fork(None, runOpts, None, Map(), false, StdoutOutput).exitValue()
      if (result != 0) sys.error("Run failed")
    }
  }
}