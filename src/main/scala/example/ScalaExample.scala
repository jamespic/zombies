package example
import zombie._

class ScalaExample extends Player {
    override def doTurn(context: PlayerContext) = Move.STAY
}
