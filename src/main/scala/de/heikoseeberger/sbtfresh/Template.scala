/*
 * Copyright 2016 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.heikoseeberger.sbtfresh

import de.heikoseeberger.sbtfresh.license.License
import java.time.LocalDate.now

private object Template {

  private val year = now().getYear

  def buildProperties: String =
    """|sbt.version = 1.3.13
       |""".stripMargin

  def buildSbt(
      organization: String,
      name: String,
      packageSegments: Vector[String],
      author: String,
      license: Option[License],
      setUpTravis: Boolean,
      setUpWartremover: Boolean
  ): String = {
    val nameIdentifier = if (name.segments.mkString == name) name else s"`$name`"

    val licenseSettings = {
      def settings(license: License) = {
        val License(_, name, url) = license
        s"""|
            |    licenses += ("$name", url("$url")),""".stripMargin
      }
      license.map(settings).getOrElse("")
    }

    val wartremoverSettings =
      if (setUpWartremover)
        """|,
           |    Compile / compile / wartremoverWarnings ++= Warts.unsafe""".stripMargin
      else
        ""

    val scalaVersion =
      if (setUpTravis)
        """|// scalaVersion from .travis.yml via sbt-travisci
           |    // scalaVersion := "2.13.3",""".stripMargin
      else
        """scalaVersion := "2.13.3","""

    s"""|// *****************************************************************************
        |// Projects
        |// *****************************************************************************
        |
        |lazy val $nameIdentifier =
        |  project
        |    .in(file("."))
        |    .enablePlugins(AutomateHeaderPlugin)
        |    .settings(commonSettings)
        |    .settings(
        |      libraryDependencies ++= Seq(
        |        library.munit           % Test,
        |        library.munitScalaCheck % Test,
        |      ),
        |    )
        |
        |// *****************************************************************************
        |// Library dependencies
        |// *****************************************************************************
        |
        |lazy val library =
        |  new {
        |    object Version {
        |      val munit = "0.7.12"
        |    }
        |    val munit           = "org.scalameta" %% "munit"            % Version.munit
        |    val munitScalaCheck = "org.scalameta" %% "munit-scalacheck" % Version.munit
        |  }
        |
        |// *****************************************************************************
        |// Settings
        |// *****************************************************************************
        |
        |lazy val commonSettings =
        |  Seq(
        |    $scalaVersion
        |    organization := "$organization",
        |    organizationName := "$author",
        |    startYear := Some($year),$licenseSettings
        |    scalacOptions ++= Seq(
        |      "-unchecked",
        |      "-deprecation",
        |      "-language:_",
        |      "-encoding", "UTF-8",
        |      "-Ywarn-unused:imports",
        |    ),
        |    testFrameworks += new TestFramework("munit.Framework"),
        |    scalafmtOnCompile := true$wartremoverSettings,
        |)
        |""".stripMargin
  }

  def gitignore: String =
    """|# sbt
       |lib_managed
       |project/project
       |target
       |
       |# Worksheets (Eclipse or IntelliJ)
       |*.sc
       |
       |# Eclipse
       |.cache*
       |.classpath
       |.project
       |.scala_dependencies
       |.settings
       |.target
       |.worksheet
       |
       |# IntelliJ
       |.idea
       |
       |# ENSIME
       |.ensime
       |.ensime_lucene
       |.ensime_cache
       |
       |# Mac
       |.DS_Store
       |
       |# Akka
       |ddata*
       |journal
       |snapshots
       |
       |# Log files
       |*.log
       |
       |# jenv
       |.java-version
       |
       |# Metals
       |.metals
       |.bloop
       |""".stripMargin

  def notice(author: String): String =
    s"""|Copyright $year $author
        |""".stripMargin

  def plugins(setUpTravis: Boolean, setUpWartremover: Boolean): String = {
    val travisPlugin =
      if (setUpTravis)
        """|
           |addSbtPlugin("com.dwijnand"      % "sbt-travisci" % "1.2.0")""".stripMargin
      else
        ""
    val wartRemoverPlugin =
      if (setUpWartremover)
        """|
           |addSbtPlugin("org.wartremover"   % "sbt-wartremover" % "2.4.9")""".stripMargin
      else
        ""

    s"""|addSbtPlugin("com.dwijnand"      % "sbt-dynver"   % "4.1.1")
        |addSbtPlugin("de.heikoseeberger" % "sbt-header"   % "5.6.0")
        |addSbtPlugin("org.scalameta"     % "sbt-scalafmt" % "2.4.2")${travisPlugin}${wartRemoverPlugin}
        |""".stripMargin
  }

  def readme(name: String, license: Option[License]): String = {
    val licenseText = {
      def text(license: License) = {
        val License(_, name, url) = license
        s"""|## License ##
            |
            |This code is open source software licensed under the
            |[$name]($url) license.""".stripMargin
      }
      license.map(text).getOrElse("")
    }
    s"""|# $name #
        |
        |Welcome to $name!
        |
        |## Contribution policy ##
        |
        |Contributions via GitHub pull requests are gladly accepted from their original author. Along with
        |any pull requests, please state that the contribution is your original work and that you license
        |the work to the project under the project's open source license. Whether or not you state this
        |explicitly, by submitting any copyrighted material via pull request, email, or other means you
        |agree to license the material under the project's open source license and warrant that you have the
        |legal authority to do so.
        |
        |$licenseText
        |""".stripMargin
  }

  def scalafmtConf: String =
    """|version = "2.7.1"
       |
       |preset = "defaultWithAlign"
       |
       |maxColumn                  = 100
       |indentOperator.preset      = "spray"
       |unindentTopLevelOperators  = true
       |spaces.inImportCurlyBraces = true
       |rewrite.rules              = ["AsciiSortImports", "RedundantBraces", "RedundantParens"]
       |""".stripMargin

  def travisYml: String =
    """|language: scala
       |
       |scala:
       |  - 2.13.3
       |
       |jdk:
       |  - openjdk8
       |""".stripMargin
}
