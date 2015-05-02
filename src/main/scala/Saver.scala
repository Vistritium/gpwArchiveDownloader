import java.io.{File, PrintWriter}

import akka.actor.Actor
import akka.actor.Actor.Receive
import scala.collection.mutable._

class Saver(pathToSave: String, dates: List[String]) extends Actor {
  val file = new File(pathToSave)
  if (file.exists()) file.delete()
  file.createNewFile()
  require(file.canWrite, "File must be writable")


  val dateToIndex = dates.zipWithIndex.toMap

  val nameToArray = Map[String, Array[String]]();

  def getNewArray = {
    Array.fill[String](dates.length)("")
  }


  override def receive: Receive = {
    case SaveEntity(date, name, value) => {


      def updateArray(array: Array[String]): Unit = {
        val index = dateToIndex(date)
        array.update(index, value)
      }

      nameToArray.get(name) match {
        case Some(x) => updateArray(x)
        case None => {
          val newArray = getNewArray
          nameToArray.update(name, newArray)
          updateArray(newArray)
        }
      }

    }

    case SaveNow => {

      def convertToCsvLine(list: List[String]) = {
        list.map('"' + _ + '"').mkString(",") + "\n"
      }

      val writer = new PrintWriter(file)

      def convertAndPrintList(list: List[String]): Unit ={
        writer.print(convertToCsvLine(list))
        writer.flush()
      }

      val fromMap = nameToArray.toList

      val names = fromMap.map(_._1)

      convertAndPrintList("date\\name" :: names)

      dates.foreach(date => {
        val index = dateToIndex(date)
        val nameToCurrentDate = nameToArray.map{
          case (name, array) => {
            array(index)
          }
        }.toList

        convertAndPrintList(date :: nameToCurrentDate)
      })

      writer.flush()
      writer.close()

      System.exit(0)

    }
  }


}

case class SaveEntity(date: String, name: String, value: String)

case object SaveNow
