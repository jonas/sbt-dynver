package sbtdynver

import java.util._

import sbt._, Keys._

object DynverPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger  = allRequirements

  private val dynver = DynVer(None, RealClock)

  override def buildSettings = Seq(
       version := dynver.version(),
    isSnapshot := dynver.isSnapshot()
  )
}

final case class DynVer(wd: Option[File], clock: Clock) {
  def version(): String = {
    def overrideVersion = sys.props get "project.version"
    def   dynverVersion = Some(makeDynVer())
    def    datedVersion = s"HEAD+$currentYearMonthDay"

    Seq(overrideVersion, dynverVersion) reduce (_ orElse _) getOrElse datedVersion
  }

  def isSnapshot(): Boolean = isDirty() || hasNoTags()

  def currentYearMonthDay(): String = "%1$tY%1$tm%1$td" format new Date

  def makeDynVer(): String = {
    Process(s"""git describe --abbrev=8 --match v[0-9].* --always --dirty=+$currentYearMonthDay""", wd).!!.init
      .replaceAll("^v", "")
      .replaceAll("-([0-9]+)-g([0-9a-f]{8})", "+$1-$2")
  }

  def isDirty(): Boolean = Process("git status --untracked-files=no --porcelain", wd).!!.nonEmpty

  def hasNoTags(): Boolean = Process("git for-each-ref --format %(objecttype) refs/tags/", wd).!!
    .linesIterator.forall(_ startsWith "commit")
}

abstract class Clock private[sbtdynver]() {
  def now(): Date
}

object RealClock extends Clock {
  def now(): Date = new Date()
}
