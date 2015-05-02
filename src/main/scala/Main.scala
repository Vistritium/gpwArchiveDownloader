import java.io.{PrintWriter, File}
import java.text.MessageFormat

import akka.actor.{ActorRef, Props, ActorSystem}
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.{HttpClients, DefaultHttpClient}
import org.apache.http.util.EntityUtils
import org.htmlcleaner.HtmlCleaner
import scala.collection.JavaConversions._
import scala.io.Source

object Main {

  val client = HttpClients.createDefault()
  val cleaner = new HtmlCleaner()

  val actorSystem = ActorSystem()

  val url = """http://www.gpw.pl/notowania_archiwalne_full?type=10&date={0}"""
  val fileName: String = "D:\\gpw.csv"
  val errorFileName: String = "D:\\error.txt"

  def main(args: Array[String]) {

    val lines = Source.fromFile(getClass.getResource("/dates").toURI).getLines().toList

    val max = lines.length
    var current = 0;

    val saver = actorSystem.actorOf(Props(classOf[Saver], fileName, lines))

    val errorPrintWriter = {
      val errorFile = new File(errorFileName)
      if (errorFile.exists()) errorFile.delete()
      errorFile.createNewFile()
      require(errorFile.canWrite, "File must be writable")

      new PrintWriter(errorFile)
    }


    lines.par.foreach(line => {

      try {
        parsePage(MessageFormat.format(url, line), line, saver)
      }
      catch {
        case e: Exception => {
          e.printStackTrace()
          System.err.println("Error for date " + line)
          errorPrintWriter.println(line)
        }
      } finally {
        current = current + 1;
        val currentPercent = (current / max.toFloat) * 100;
        println(s"Done $currentPercent%  $current out of $max")
      }
    })

    errorPrintWriter.flush()
    errorPrintWriter.close()

    saver ! SaveNow


  }

  def parsePage(url: String, date: String, saver: ActorRef) = {
    val get = new HttpGet(url)

    val closeableResponse = client.execute(get)

    val entity = closeableResponse.getEntity

    val stream = entity.getContent

    val cleanedHtml = cleaner.clean(stream)

    EntityUtils.consume(entity)

    val foundTableClass = cleanedHtml.findElementByAttValue("class", "tab03", true, true)
    val tbody = foundTableClass.getAllElementsList(false).get(0)
    val tbodyChildren = tbody.getAllElementsList(false)

    def getElem(column: Int, row: Int): String = {
      val tr = tbodyChildren.get(row)
      val tds = tr.getAllElementsList(false)
      val elem = tds.get(column)
      elem.getText.toString
    }

    (1 until tbodyChildren.size()) foreach {
      row => {
       val name = getElem(0, row)
       val value = getElem(7, row)
       saver ! SaveEntity(date, name, value)
      }
    }

  }

}
