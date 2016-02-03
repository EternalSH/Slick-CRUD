package utils

import model.Message
import org.joda.time.DateTime

/**
 * Sample data generator.
 * @author Amadeusz Kosik (kosikamadeusz@gmail.com)
 */
trait SampleData {

  def sampleMessage: Message = {
    import util.Random._
    def sampleEmail = nextString(32) + "@" + nextString(32) + ".com"

    Message(None, DateTime.now(), None, nextString(16), sampleEmail, nextDouble(), nextString(16), nextString(64))
  }
}
