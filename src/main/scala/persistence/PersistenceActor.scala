package persistence

import akka.actor._
import akka.pattern.pipe
import model.MessageFieldTypes._
import model.OperationError.{OperationFailed, IdNotFound}
import model._
import org.joda.time.DateTime
import com.github.tototoshi.slick.PostgresJodaSupport._
import persistence.PersistenceActor._
import slick.driver.PostgresDriver.api._
import utils.SampleData

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * PersistenceActor companion object.
 *  Stores actor protocol and other helper classes definitions.
 *
 * @author Amadeusz Kosik (kosikamadeusz@gmail.com)
 */
object PersistenceActor {

  /** Defines PersistenceActor protocol. */
  sealed trait PersistenceActorProtocol

  /** Create SQL tables used by the application. */
  case object CreateSchema extends PersistenceActorProtocol

  /** Drop SQL tables */
  case object DropSchema extends PersistenceActorProtocol

  /** Insert some sample data to the database. */
  case object CreateSampleData extends PersistenceActorProtocol

  /** Delete given Message. */
  case class DeleteMessage(id: MessageId) extends PersistenceActorProtocol

  /** Retrieve given Message. */
  case class GetMessage(id: MessageId) extends PersistenceActorProtocol

  /** Store given Message. */
  case class InsertMessage(messageDto: MessageInsertDto) extends PersistenceActorProtocol

  /** List given Message and requested number of Messages before and after (sorted by their rating). */
  case class ListMessages(id: MessageId, before: Option[Int], after: Option[Int]) extends PersistenceActorProtocol

  /** Update given Message. */
  case class UpdateMessage(id: MessageId, messageDto: MessageUpdateDto) extends PersistenceActorProtocol

  /** Database connection settings. */
  case class PersistenceSettings(dbConfigName: String, paginationMaxCount: Int)
}

/**
 * Performs operations on the database.
 * @param persistenceSettings database settings
 *
 * @author Amadeusz Kosik (kosikamadeusz@gmail.com)
 */
class PersistenceActor(persistenceSettings: PersistenceSettings) extends Actor with ActorLogging with SampleData {
  val dbHandle = Database.forConfig(persistenceSettings.dbConfigName)

  /** Messages table definition. */
  private class MessageTable(tag: Tag) extends Table[Message](tag, "messages") {
    def id          = column[Option[MessageId]]("id", O.PrimaryKey, O.AutoInc)
    def dateCreated = column[DateTime]("date_created")
    def dateUpdated = column[Option[DateTime]]("date_updated")
    def authorName  = column[AuthorName]("author_name")
    def authorEmail = column[AuthorEmail]("author_email")
    def rating      = column[Rating]("rating")
    def title       = column[Title]("title")
    def content     = column[Content]("content")

    def * = (id, dateCreated, dateUpdated, authorName, authorEmail, rating, title, content).shaped <> (Message.tupled, Message.unapply)
  }

  private val messages = TableQuery[MessageTable]

  override def receive = {

    case CreateSchema =>
      log.info(s"CreateSchema requested")
      dbHandle.run(DBIO.seq(messages.schema.create)) pipeTo sender()

    case DropSchema =>
      log.info(s"DropSchema requested")
      dbHandle.run(DBIO.seq(messages.schema.drop)) pipeTo sender()

    case CreateSampleData =>
      log.info(s"CreateSampleData requested")
      dbHandle.run(DBIO.sequence((1 to 100).map(_ => sampleMessage).map(messages += _)))

    case DeleteMessage(id) =>
      log.info(s"DeleteMessage requested, id = $id")
      dbHandle.run(messages.filter(_.id === id).delete) map {
        case 0 =>
          OperationError(IdNotFound)
        case 1 =>
          id

        case anyOther =>
          log.warning("Failed DELETE SQL query")
          OperationError(OperationFailed)
      } pipeTo sender()

    case GetMessage(id) =>
      log.info(s"GetMessage requested, id = $id")
      dbHandle.run(messages.filter(_.id === id).result.headOption).map {
        case Some(message)  => message
        case None           => OperationError(IdNotFound)
      }  pipeTo sender()

    case InsertMessage(messageDto) =>
      log.info(s"InsertMessage requested, message = $messageDto")

      MessageValidator.validateInsertDto(messageDto).finishWith(dto => {
          val message = dto.toMessage(DateTime.now())
          val result = dbHandle.run(DBIO.sequence(Seq((messages returning messages.map(_.id)) += message)))
          result.map(_.head.getOrElse(OperationError(IdNotFound))) pipeTo sender()
        }
      ){ sender ! _ }

    case ListMessages(id, before, after) =>
      log.info(s"ListMessages requested, id = $id; before = $before, after = $after")

      def boundPaginationCount(requestValue: Option[Int]): Int =
        math.min(persistenceSettings.paginationMaxCount, requestValue.getOrElse(0))

      val firstMessage = messages.filter(_.id === id)

      val messagesBefore = for {
        (ref, before) <- messages.filter(_.id === id) join messages on (_.rating >= _.rating) filter(_._2.id =!= id) sortBy (_._2.rating.desc) take boundPaginationCount(before)
      } yield before

      val messagesAfter = for {
        (ref, after)  <- messages.filter(_.id === id) join messages on (_.rating <= _.rating) filter(_._2.id =!= id) sortBy (_._2.rating.asc) take boundPaginationCount(after)
      } yield after

      val allMessagesResult = (messagesBefore ++ firstMessage ++ messagesAfter).sortBy(_.rating).result

      dbHandle.run(allMessagesResult).map(_.toList).map { allMessages =>
        if(allMessages.nonEmpty)
          allMessages
        else
          OperationError(IdNotFound)
      } pipeTo sender()

    case UpdateMessage(id, messageDto) =>
      log.info(s"UpdateMessage requested, id = $id; message = $messageDto")

      MessageValidator.validateUpdateDto(messageDto).finishWith(dto => dbHandle.run((for {
          message <- messages.filter(_.id === id).result.head
          updated <- messages.filter(_.id === id).update(dto.toMessage(message, Some(DateTime.now())))
        } yield message.id.getOrElse("UPDATE_FAILED")).transactionally) pipeTo sender()
      ){ sender ! _ }

    case anything =>
      sys.error(s"Unrecognized message delivered to PersistenceActor: $anything")
  }
}