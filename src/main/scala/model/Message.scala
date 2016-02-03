package model

import model.MessageFieldTypes._
import model.OperationError.ValidationFailed
import org.joda.time.DateTime

/**
 * Companion object for Message entity class.
 *  Stores definitions of entity fields types
 */
object MessageFieldTypes {
  type MessageId = Long
  type AuthorName = String
  type AuthorEmail = String
  type Rating = Double
  type Title = String
  type Content = String
}

/**
 * Message - sample application entity
 *
 * @param id            DB generated ID
 * @param dateCreated   creation date
 * @param dateUpdated   last update date
 * @param authorName    author nickname
 * @param authorEmail   author email
 * @param rating        message rating
 * @param title         message title
 * @param content       message content
 *
 * @author Amadeusz Kosik (kosikamadeusz@gmail.com)
 */
case class Message(
  id:          Option[MessageId],
  dateCreated: DateTime,
  dateUpdated: Option[DateTime],
  authorName:  AuthorName,
  authorEmail: AuthorEmail,
  rating:      Rating,
  title:       Title,
  content:     Content)

/**
 * MessageDTO used in client's create requests.
 *
 * @param authorName    author nickname
 * @param authorEmail   author email
 * @param rating        message rating
 * @param title         message title
 * @param content       message content
 *
 * @author Amadeusz Kosik (kosikamadeusz@gmail.com)
 */
case class MessageInsertDto(
  authorName:  AuthorName,
  authorEmail: AuthorEmail,
  rating:      Rating,
  title:       Title,
  content:     Content) {

  def toMessage(createDate: DateTime): Message =
    Message(None, createDate, None, authorName, authorEmail, rating, title, content)
}

/**
 * MessageDTO used in client's update requests.
 *
 * @param authorName    author nickname
 * @param authorEmail   author email
 * @param rating        message rating
 * @param title         message title
 * @param content       message content
 *
 * @author Amadeusz Kosik (kosikamadeusz@gmail.com)
 */
case class MessageUpdateDto(
  authorName:  Option[AuthorName] = None,
  authorEmail: Option[AuthorEmail] = None,
  rating:      Option[Rating] = None,
  title:       Option[Title] = None,
  content:     Option[Content] = None) {

  def toMessage(originalMessage: Message, dateUpdated: Option[DateTime]): Message =
    Message(
      originalMessage.id,
      originalMessage.dateCreated,
      dateUpdated,
      authorName.getOrElse(originalMessage.authorName),
      authorEmail.getOrElse(originalMessage.authorEmail),
      rating.getOrElse(originalMessage.rating),
      title.getOrElse(originalMessage.title),
      content.getOrElse(originalMessage.content))
}

/**
 * Performs input (DTO) validation.
 * @author Amadeusz Kosik (kosikamadeusz@gmail.com)
 */
object MessageValidator {

  /**
   * Single validation rule for single field.
   *
   * @param errorName error name (sent to the client when the validation failed)
   * @param validator validation function (returns FALSE if validation failed)
   * @tparam FieldType type of the entity field
   */
  case class ValidationRule[FieldType](errorName: String, validator: FieldType => Boolean)

  /**
   * Basic Messages entity validators.
   */
  private object ValidationRules {
    val authorName = ValidationRule[AuthorName]("AUTHOR_NAME_TOO_SHORT", _.length > 2)
    val authorEmail = ValidationRule[AuthorEmail]("AUTHOR_EMAIL_INCORRECT", """\w+@\w+\.\w{2,3}""".r.findFirstIn(_).isDefined)
    val rating = ValidationRule[Rating]("RATING_OUT_OF_RANGE", rating => rating <= 1.0 && rating >= 0.0)
  }

  /**
   * Creates validator callback
   * @param rule            field validation rule
   * @param fieldExtractor  extracts Option[Field Type] from the EntityType
   * @tparam EntityType     type of the entity
   * @tparam FieldType      type of the field
   * @return                field validator function
   */
  private def validateFactory[EntityType, FieldType](rule: ValidationRule[FieldType], fieldExtractor: EntityType => Option[FieldType]): EntityType => Option[String] = {
    entity => {
      val fieldValue = fieldExtractor(entity)

      if(fieldValue.map(rule.validator).getOrElse(true))
        None
      else
        Some(rule.errorName)
    }
  }

  /**
   * Performs validation of the entity against given validation rules.
   *
   * @param entity        entity to validate
   * @param validators    validators to validate with
   * @tparam EntityType   type of the entity
   * @return              result of the validation operation
   */
  private def validate[EntityType](entity: EntityType, validators: List[EntityType => Option[String]]) = {
    val errors = validators.flatMap(_.apply(entity))

    if(errors.isEmpty)
      OperationSuccess(entity)
    else
      OperationError(ValidationFailed, errors)
  }

  /**
   * Checks input data and returns list of validation errors.
   *  List is empty when input data is valid.
   *
   * @param messageInsertDto  input value to validate
   * @return                  list of validation errors
   */
  def validateInsertDto(messageInsertDto: MessageInsertDto) = {
    val validators = List(
      validateFactory[MessageInsertDto, AuthorName](ValidationRules.authorName, msg => Some(msg.authorName)),
      validateFactory[MessageInsertDto, AuthorEmail](ValidationRules.authorEmail, msg => Some(msg.authorEmail)),
      validateFactory[MessageInsertDto, Rating](ValidationRules.rating, msg => Some(msg.rating))
    )

    validate(messageInsertDto, validators)
  }

  /**
   * Checks input data and returns list of validation errors.
   *  List is empty when input data is valid.
   *
   * @param messageUpdateDto  input value to validate
   * @return                  list of validation errors
   */
  def validateUpdateDto(messageUpdateDto: MessageUpdateDto) = {
    val validators = List(
      validateFactory[MessageUpdateDto, AuthorName](ValidationRules.authorName, _.authorName),
      validateFactory[MessageUpdateDto, AuthorEmail](ValidationRules.authorEmail, _.authorEmail),
      validateFactory[MessageUpdateDto, Rating](ValidationRules.rating, _.rating)
    )

    validate(messageUpdateDto, validators)
  }
}