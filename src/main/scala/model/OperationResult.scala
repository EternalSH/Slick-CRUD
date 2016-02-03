package model

import model.OperationError.ErrorType

/**
 * Pseudo-monad used to store multiple operations chain result.
 *  First error is propagated in flatMap/map operations and it stops from executing further operations.
 *
 * @tparam A  type of expected result value
 *
 * @author Amadeusz Kosik (kosikamadeusz@gmail.com)
 */
sealed trait OperationResult[+A] {
  def isSuccessful: Boolean

  /** Performs flatMap operation. */
  def flatMap[B](f: A => OperationResult[B]): OperationResult[B]
  /** Performs map operation. */
  def map[B](f: A => B): OperationResult[B]
  /** Calls onSuccess if all operations succeeded or onError with first found error. */
  def finishWith(onSuccess: A => Unit)(onError: OperationError => Unit): Unit
}

/**
 * Pseudo-modal representing result from successfully performed operation.
 * @author Amadeusz Kosik (kosikamadeusz@gmail.com)
 */
case class OperationSuccess[+A](result: A) extends OperationResult[A] {
  override val isSuccessful = true

  def flatMap[B](f: A => OperationResult[B]): OperationResult[B] =
    f(result)

  def map[B](f: A => B): OperationResult[B] =
    OperationSuccess(f(result))

  def finishWith(onSuccess: A => Unit)(onError: OperationError => Unit): Unit =
    onSuccess(result)
}

/**
 * OperationError companion object.
 *  Stores available ErrorTypes.
 *
 * @author Amadeusz Kosik (kosikamadeusz@gmail.com)
 */
object OperationError {

  /**
   * Type of the error stored in the OperationError.
   */
  sealed trait ErrorType {
    /** Name used in HTTP response body. */
    def name: String
  }

  /**
   * Entity with requested ID was not found.
   */
  case object IdNotFound extends ErrorType {
    override val name = "ID_NOT_FOUND"
  }

  /**
   * Entity validation failed (one of it's fields broke validation rules).
   */
  case object ValidationFailed extends ErrorType {
    override val name = "VALIDATION_FAILED"
  }

  /**
   * Operation has failed for an unknown reason (eg. unexpected SQL error).
   */
  case object OperationFailed extends ErrorType {
    override val name = "OPERATION_FAILED"
  }
}

/**
 * Pseudo-modal representing error from failed operation.
 * @author Amadeusz Kosik (kosikamadeusz@gmail.com)
 */
case class OperationError(errorType: ErrorType, errors: List[String] = List.empty) extends OperationResult[Nothing] {
  override val isSuccessful = false

  def flatMap[B](f: Nothing => OperationResult[B]): OperationResult[B] =
    OperationError(errorType, errors)

  def map[B](f: Nothing => B): OperationResult[B] =
    OperationError(errorType, errors)

  def finishWith(onSuccess: Nothing => Unit)(onError: OperationError => Unit): Unit =
    onError(this)
}

