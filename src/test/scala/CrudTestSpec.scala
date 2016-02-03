import akka.actor.{Props, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import model.MessageInsertDto
import org.scalatest.concurrent.{ScalaFutures, PatienceConfiguration, Futures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpec}
import persistence.PersistenceActor
import persistence.PersistenceActor._
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

trait CrudTestSpec extends FlatSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  val actorSystem = ActorSystem("restServiceTest")
  val persistenceActor = actorSystem.actorOf(Props(classOf[PersistenceActor], PersistenceSettings("db-test", ConfigFactory.load().getInt("application.pagination.maxCount"))), "PersistenceActor")

  implicit val timeout = Timeout(5.seconds)
  implicit val defaultPatience = PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  def sampleValidMessageInsertDto =
    MessageInsertDto("Author", "author@author.com", 0.5, "title", "content")

  def sampleInvalidMessageUpdateDto =
    MessageInsertDto("A", "author.author.com", 2.5, "title", "content")

  override def beforeAll() = {
    Await.result(persistenceActor ? DropSchema andThen { case _ => persistenceActor ? CreateSchema }, 5.seconds)
  }

  override def afterAll() = {
    actorSystem.shutdown()
  }
}
