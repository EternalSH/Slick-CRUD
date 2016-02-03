version       := "0.0.1"

scalaVersion  := "2.11.6"

scalacOptions := Seq("-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.3"
  Seq(
    "io.spray"                      %%  "spray-can"         % sprayV,
    "io.spray"                      %%  "spray-routing"     % sprayV,
    "io.spray"                      %%  "spray-testkit"     % sprayV              % "test",
    "io.spray"                      %%  "spray-json"        % "1.3.1",
    "com.typesafe.akka"             %%  "akka-actor"        % akkaV,
    "com.typesafe.akka"             %%  "akka-testkit"      % akkaV               % "test",
    "org.scalatest"                 %% "scalatest"          % "2.2.4"             % "test",
    "org.scalatest"                 %   "scalatest_2.11"    % "2.2.1"             % "test",
    "com.typesafe.slick"            %%  "slick"             % "3.0.2",
    "com.typesafe"                  %   "config"            % "1.2.1",
    "org.postgresql"                %   "postgresql"        % "9.3-1100-jdbc41",
    "com.typesafe.scala-logging"    %%  "scala-logging"     % "3.1.0",
    "org.slf4j"                     %   "slf4j-nop"         % "1.6.4",
    "com.zaxxer"                    %   "HikariCP-java6"    % "2.3.6",
    "joda-time"                     %   "joda-time"         % "2.8.2",
    "org.joda"                      %   "joda-convert"      % "1.7",
    "com.github.tototoshi"          %%  "slick-joda-mapper" % "2.0.0"
  )
}
