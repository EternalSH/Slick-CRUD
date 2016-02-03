import persistence.PersistenceActor._
import akka.actor.{Props, ActorSystem}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import controller.RestServiceActor
import persistence._
import spray.can.Http
import scala.concurrent.duration._

/**
 * CRUD Application in Spray/Slick.
 * @author Amadeusz Kosik (kosikamadeusz@gmail.com)
 */
object Boot extends App {

  implicit val timeout = Timeout(5.seconds)
  implicit val actorSystem = ActorSystem("restService")

  val config = ConfigFactory.load()

  val persistenceSettings = PersistenceSettings("db", config.getInt("application.pagination.maxCount"))
  val persistenceActor = actorSystem.actorOf(Props(classOf[PersistenceActor], persistenceSettings), "PersistenceActor")
  val restServiceActor = actorSystem.actorOf(Props(classOf[RestServiceActor], persistenceActor), "RestService")

  if(args.contains("--create-schema"))
    persistenceActor ! CreateSchema

  if(args.contains("--create-sample-data"))
    persistenceActor ! CreateSampleData

  IO(Http) ? Http.Bind(restServiceActor, interface = "localhost", port = 8080)
}
