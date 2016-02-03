import akka.pattern.ask
import com.typesafe.config.ConfigFactory
import model._
import persistence.PersistenceActor._

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class PaginationSpec extends CrudTestSpec {

  "Application" should "paginate Messages" in {
    val newMessages = (1 to 20).map(i => sampleValidMessageInsertDto.copy(rating = 0.04 * i))
    val idsFuture = Future.sequence(newMessages.map(persistenceActor ? InsertMessage(_)))

    whenReady(idsFuture) {
      case ids: Vector[Long] =>
        val maxMessagesCount = ConfigFactory.load().getInt("application.pagination.maxCount")

        whenReady(persistenceActor ? ListMessages((ids.sum / ids.length).toInt, None, None)) {
          case messages: List[Message] => assert(messages.length == 1)
        }

        whenReady(persistenceActor ? ListMessages((ids.sum / ids.length).toInt, Some(2), Some(2))) {
          case messages: List[Message] => assert(messages.length == 5)
        }

        whenReady(persistenceActor ? ListMessages((ids.sum / ids.length).toInt, Some(100), None)) {
          case messages: List[Message] => assert(messages.length == maxMessagesCount + 1)
        }

        whenReady(persistenceActor ? ListMessages((ids.sum / ids.length).toInt, None, Some(100))) {
          case messages: List[Message] => assert(messages.length == maxMessagesCount + 1)
        }

        whenReady(persistenceActor ? ListMessages(4, Some(99), Some(99))) {
          case messages: List[Message] => assert(messages.length == 1 + maxMessagesCount * 2)
        }
    }
  }
}
