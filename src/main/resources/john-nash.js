var Constants = Packages.zombie.Constants
var Shoot = Packages.zombie.Shoot
var Move = Packages.zombie.Move
var Player = Packages.zombie.Player
var PlayerRegistry = Packages.zombie.PlayerRegistry

function mkSet() {
    var s = {}
    for (var i = 0; i < arguments.length; i++) {
        s[arguments[i]] = true
    }
    return s
}

var chumps = mkSet(
                "GordonFreeman",
                "HideyTwitchy",
                "Gunner",
                "MoveRandomly",
                "StandStill",
                "ThePriest",
                "Vortigaunt",
                "ZombieHater",
                "ZombieRightsActivist",
                "Bee",
                "Zombie",
                "SuperCoward"
              )

function dist(x, y) {
    return Math.max(Math.abs(x - Constants.CENTRE_OF_VISION), Math.abs(y - Constants.CENTRE_OF_VISION))
}

function range(width, offset) {
    var x = []
    for (var i = -width; i <= width; i++) {
        for (var j = -width; j <= width; j++) {
            if (i != 0 || j != 0) x.push([i + offset,j + offset])
        }
    }
    return x
}

function JohnNash() {
    var looted = {}
    this.doTurn = function(context) {
        var field = context.getPlayField()
        // Save looted bodies
        range(1, Constants.CENTRE_OF_VISION).forEach(function(p) {
            var x = p[0], y = p[1]
            var playerId = field[x][y]
            if (playerId && playerId.getName() == "DeadBody") {
                looted[playerId] = true
            }
        })

        // Shoot any nearby chumps
        if (context.getBullets() > 0) {
            var shootableIterator = context.shootablePlayers().iterator();
            while (shootableIterator.hasNext()) {
                var shootable = shootableIterator.next()
                if (chumps[shootable.getName()]) return new Shoot(shootable)
            }
        }

        // Helper function - everyone loves closures
        function moveTowards(x, y) {
            var tryMove = Move.inDirection(
                    x - Constants.CENTRE_OF_VISION,
                    y - Constants.CENTRE_OF_VISION
            )
            if (!(field[Constants.CENTRE_OF_VISION + tryMove.x][Constants.CENTRE_OF_VISION + tryMove.y])) {
                return tryMove
            } else {
                // If your path is blocked, take a random move
                return Move.randomMove()
            }
        }

        // Loot
        var bestX, bestY, bestDist = Infinity
        range(Constants.VISION_RANGE, Constants.CENTRE_OF_VISION).forEach(function(p) {
            var x = p[0], y = p[1]
            var playerId = field[x][y]
            if (playerId
                    && playerId.getName() == "DeadBody"
                    && !looted[playerId]
                    && dist(x, y) < bestDist) {
                bestDist = dist(x,y)
                bestX = x
                bestY = y
            }
        })

        if (bestDist < Infinity) {
            return moveTowards(bestX, bestY)
        }
        else return Move.SOUTH
    }
}

PlayerRegistry.registerPlayer("JohnNash", new JohnNash())