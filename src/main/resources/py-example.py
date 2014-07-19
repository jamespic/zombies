from zombie import Player, Move, PlayerRegistry

class PyExample(Player):
    def doTurn(self, context):
        return Move.STAY

PlayerRegistry.registerPlayer("py-example", PyExample())