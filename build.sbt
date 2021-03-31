name := "cateffay"
organization := "satorg"

scalaVersion := "2.13.5"

scalacOptions ++= Seq("-encoding", "utf-8") ++ Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-explaintypes",
  "-language:higherKinds",
  "-Xlint",
  "-Wdead-code",
  "-Werror"
)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect-std" % Versions.CatsEffect,
  "org.typelevel" %% "cats-effect" % Versions.CatsEffect % Test
)
