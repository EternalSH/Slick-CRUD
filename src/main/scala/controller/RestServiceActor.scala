package controller

import model.MessageFieldTypes.MessageId
import persistence.PersistenceActor._
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import model._
import spray.http.MediaTypes
import spray.http.StatusCodes
import spray.routing._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Success

/**
 * REST controller service actor
 * @param persistenceActor reference to the PersistenceActor
 *
 * @author Amadeusz Kosik (kosikamadeusz@gmail.com)
 */
class RestServiceActor(persistenceActor: ActorRef) extends Actor with ActorLogging with HttpService {

  implicit val timeout = Timeout(3.seconds)

  import JsonProtocol._
  import spray.httpx.SprayJsonSupport._

  /** Sends HTTP 403 response with formatted OperationError. */
  def completeBadRequest(error: OperationError) =
    respondWithMediaType(MediaTypes.`application/json`) { complete(StatusCodes.BadRequest, error) }

  /** Sends HTTP 404 response with formatted OperationError. */
  def completeNotFound(error: OperationError) =
    respondWithMediaType(MediaTypes.`application/json`) { complete(StatusCodes.NotFound, error) }

  /* ****************** */
  /* ROUTES DEFINITIONS */
  /* ****************** */

  /** Delete message */
  def routeDelete = path(IntNumber) { messageId => delete {
    onComplete(persistenceActor ? DeleteMessage(messageId)) {
      case Success(id: MessageId) =>
        log.info(s"Message $messageId deleted successfully")
        complete(s"$id")

      case Success(error: OperationError) =>
        log.info(s"Failed to delete Message $messageId")
        completeBadRequest(error)

      case anyOther =>
        sys.error(s"Unexpected result: $anyOther")
    }
  }}

  /** Get single message */
  def routeGetById = path(IntNumber) { messageId => get {
    onComplete(persistenceActor ? GetMessage(messageId)) {
      case Success(message: Message) =>
        log.info(s"Message $messageId retrieved successfully")
        complete(message)

      case Success(error: OperationError) =>
        log.info(s"Message $messageId not found")
        completeNotFound(error)

      case anyOther =>
        sys.error(s"Unexpected result: $anyOther")
    }
  }}

  /** Store message */
  def routeInsert = post { entity(as[MessageInsertDto]) { newMessage =>
    onComplete(persistenceActor ? InsertMessage(newMessage)) {
      case Success(id: MessageId) =>
        log.info(s"Message $id saved successfully")
        complete(s"$id")

      case Success(error: OperationError) =>
        log.info(s"Failed to save new message")
        completeBadRequest(error)

      case anyOther =>
        sys.error(s"Unexpected result: $anyOther")
    }
  }}

  /** List messages (paginate) */
  def routeList = get { parameters('el.as[MessageId], 'before.as[Int].?, 'after.as[Int].?) { (messageId, before, after) =>
    onComplete(persistenceActor ? ListMessages(messageId, before, after)) {
      case Success(messages: List[Message]) =>
        log.info(s"Successfully retrieved ${messages.length} messages")
        complete(messages)

      case Success(error: OperationError) =>
        log.info(s"Message $messageId not found")
        completeNotFound(error)

      case anyOther =>
        sys.error(s"Unexpected result: $anyOther")
    }
  }}

  /** Update message */
  def routeUpdate = path(IntNumber) { messageId => patch { entity(as[MessageUpdateDto]) { updatedMessage =>
    onComplete(persistenceActor ? UpdateMessage(messageId, updatedMessage)) {
      case Success(id: MessageId) =>
        log.info(s"Message $messageId updated successfully")
        complete(s"$id")

      case Success(error: OperationError) =>
        log.info(s"Failed to update Message $messageId")
        completeBadRequest(error)

      case anyOther =>
        sys.error(s"Unexpected result: $anyOther")
    }
  }}}

  override def actorRefFactory = context

  override def receive =
    runRoute(path("list") {
      routeInsert ~ routeList
    } ~ pathPrefix("list") {
      routeDelete ~ routeGetById ~ routeUpdate
    })
}
