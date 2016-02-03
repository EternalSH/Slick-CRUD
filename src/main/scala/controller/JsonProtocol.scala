package controller

import model._
import org.joda.time.DateTime
import spray.json._

/**
 * Application-specific JSON formatters.
 * @author Amadeusz Kosik (kosikamadeusz@gmail.com)
 */
object JsonProtocol extends DefaultJsonProtocol {

  /** JSON formatter for org.joda.time.DateTime. */
  implicit object DateTimeFormat extends JsonFormat[DateTime] {
    def read(value: JsValue): DateTime = value match {
      case JsString(timestamp) =>
        DateTime.parse(timestamp)

      case _ =>
        deserializationError(s"Failed to parse dateTime from $value")
    }

    def write(dateTime: DateTime): JsValue =
      JsString(dateTime.toString)
  }

  /** JSON formatter for MessageInsert DTO. */
  implicit val MessageInsertDtoFormat  = jsonFormat5(MessageInsertDto)
  /** JSON formatter for MessageUpdate DTO. */
  implicit val MessageUpdateDtoFormat  = jsonFormat5(MessageUpdateDto)
  /** JSON formatter for Message entity. */
  implicit val MessageFormat     = jsonFormat8(Message)

  /** JSON formatter for OperationError object. */
  implicit object OperationErrorFormat extends RootJsonFormat[OperationError] {

    // OperationError is not supposed to be sent from client to server.
    override def read(js: JsValue) =
      deserializationError("OperationError is not expected as input...")

    override def write(error: OperationError) = {
      val errors = error.errors.map(error => JsString(error)).toVector

      JsObject(Map(
        "isSuccessful"  -> JsBoolean(error.isSuccessful),
        "errorType"     -> JsString(error.errorType.name),
        "errors"        -> JsArray(errors)
      ))
    }
  }
}
