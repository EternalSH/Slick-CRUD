import akka.pattern.ask
import model.OperationError.{IdNotFound, ValidationFailed}
import model._
import persistence.PersistenceActor._

class CrudSpec extends CrudTestSpec {

  "Application" should "store Messages" in {
    whenReady(persistenceActor ? InsertMessage(sampleValidMessageInsertDto)) {
      case id: Long =>
        assert(id > 0)
    }
  }

  "Application" should "validate insert requests" in {
    whenReady(persistenceActor ? InsertMessage(sampleInvalidMessageUpdateDto)) {
      case OperationError(errorType, errors) =>
        assert(errorType == ValidationFailed)
        assert(errors.length == 3)
    }
  }

  "Application" should "retrieve single Message" in {
    val newMessage = sampleValidMessageInsertDto

    whenReady(persistenceActor ? InsertMessage(newMessage)) {
      case id: Long =>
        whenReady(persistenceActor ? GetMessage(id)) {
          case message: Message =>
            assert(message.id.contains(id))
            assert(message.authorName == newMessage.authorName)
            assert(message.title == newMessage.title)
        }
    }
  }

  "Application" should "throw 404 when Message is not found" in {
    whenReady(persistenceActor ? GetMessage(9999L)) {
      case OperationError(errorType, errors) =>
        assert(errorType == IdNotFound)
    }
  }


  "Application" should "update Message" in {
    val newMessage = sampleValidMessageInsertDto

    whenReady(persistenceActor ? InsertMessage(newMessage)) {
      case insertId: Long =>
        whenReady(persistenceActor ? UpdateMessage(insertId, MessageUpdateDto(title = Some("no-title")))) {
          case updateId: Long =>
            assert(insertId == updateId)
            whenReady(persistenceActor ? GetMessage(updateId)) {
              case message: Message =>
                assert(message.title == "no-title")
                assert(message.content == newMessage.content)
            }
        }
    }
  }

  "Application" should "validate update requests" in {
    val newMessage = sampleValidMessageInsertDto

    whenReady(persistenceActor ? InsertMessage(newMessage)) {
      case insertId: Long =>
        val updateMessageDto = MessageUpdateDto(authorName = Some("-"), authorEmail = Some("zdzisiek.neo.pl"), rating = Some(99.9))
        whenReady(persistenceActor ? UpdateMessage(insertId, updateMessageDto)) {
          case OperationError(errorType, errors) =>
            assert(errorType     == ValidationFailed)
            assert(errors.length == 3)
        }
    }
  }


  "Application" should "delete Message" in {
    val newMessage = sampleValidMessageInsertDto

    whenReady(persistenceActor ? InsertMessage(newMessage)) {
      case insertId: Long =>
        whenReady(persistenceActor ? DeleteMessage(insertId)) {
          case updateId: Long =>
            assert(insertId == updateId)
            whenReady(persistenceActor ? GetMessage(updateId)) {
              case OperationError(errorType, errors) =>
                assert(errorType == IdNotFound)
            }
        }
    }
  }

  "Application" should "validate delete Message id" in {
    whenReady(persistenceActor ? DeleteMessage(99999L)) {
      case OperationError(errorType, errors) =>
        assert(errorType == IdNotFound)
    }
  }
}
