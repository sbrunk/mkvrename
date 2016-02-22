

package io.brunk.mkvrename

import java.nio.file.Paths
import java.nio.file.Files
import scala.collection.JavaConversions
import scala.sys.process.stringSeqToProcess

/**
 * Simple renaming/metadata tool for TV Series stored in Matroska video files.
 * Requires MKVToolNix for setting metadata https://mkvtoolnix.download/
 */
object Main {

  case class Episode(number: Int, title: String)

  def escapeChars(name: String): String =
    name.replaceAll("[., /]+", "_").stripSuffix("_")

  def formatName(seriesTitle: String, season: Int, episode: Int, episodeTitle: String): String = {
    f"${escapeChars(seriesTitle)}%s_S$season%dE$episode%02d_${escapeChars(episodeTitle)}%s.mkv"
  }

  def formatTitle(seriesTitle: String, season: Int, episode: Int, episodeTitle: String): String = {
    f"$seriesTitle%s S$season%dE$episode%02d: $episodeTitle%s"
  }

  def main(args: Array[String]): Unit = {
    val path = Paths.get(args(0)).toAbsolutePath()
    val seriesTitle = args(1)
    val season = args(2).toInt
    val episodeTitles = args.drop(3)
    
    val episodes = for ((title, index) <- episodeTitles.zipWithIndex)
      yield Episode(index + 1, title)

    import scala.collection.JavaConversions.iterableAsScalaIterable
    val oldFileNames = Files.newDirectoryStream(path).toVector.map(_.last).sorted

    val newFileNames = for ((title, index) <- episodeTitles.zipWithIndex)
      yield Paths.get(formatName(seriesTitle, season, index + 1, title))

    if (oldFileNames.length != newFileNames.length) throw new RuntimeException("Lengths don't match")
    val mapping = oldFileNames.zip(newFileNames)

    println("Files will be renamed like this:")
    for ((oldName, newName) <- mapping)
      println(s"$oldName -> $newName")

    println("Continue? y/n")
    val answer = scala.io.StdIn.readChar()
    if (answer != 'y') {
      println("Abort")
      System.exit(0)
    }

    // rename
    for ((oldName, newName) <- mapping) {
      val oldPath = path.resolve(oldName)
      val newPath = oldPath.resolveSibling(newName)
      Files.move(oldPath, newPath)
    }

    // Set title
    for ((fileName, episode) <- newFileNames.zip(episodes)) {
      val formattedTitle = formatTitle(seriesTitle, season, episode.number, episode.title)
      val filePath = path.resolve(fileName)
      import scala.sys.process._
      Seq("mkvpropedit", filePath.toString(), "--set", s"title=$formattedTitle").!
    }
    println("Done")
  }

}