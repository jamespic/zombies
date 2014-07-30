from zombie import Player, Move, Shoot, PlayerRegistry, Constants

import math

MID = Constants.CENTRE_OF_VISION
BOARDSIZE = 1
sign = lambda x: (1, -1)[x<0]
Bees = {}
Shot = set()
MoveTo = set()

def isEnemy(player):
    if not player:
        return False
    return {
        'Bee': False,
        'DeadBody': False,       
        'StandStill': False}.get(player.getName(), True)

def isZombie(player):
    if not player:
        return False
    return player.getName() is "Zombie"

def distance(x1, x2, y1, y2):
    return math.max(math.abs(x1 - x2), math.abs(y1 - y2))

class Bee(Player):  
    Queen = None
    QueenBeeX = QueenBeeY = X = Y = ID = 0
    LastTurn = -1   

    def doTurn(self, context):  
        self.ID = context.getId().getNumber()
        self.X = context.getX()
        self.Y = context.getY()
		BOARDSIZE = context.getBoardSize()
        turn = context.getGameClock()       
        if turn is not Bee.LastTurn:
            Bee.LastTurn = turn
            MoveTo.clear()
        Bees[self.ID] = turn # Report In
        self.setQueenBee(turn)                  
        action = self.sting(context)
        if action:
            return action
        return self.moveToQueenBee(context)     

    def setQueenBee(self, turn):
        if Bee.Queen not in Bees:
            # Long live the queen!
            Bee.Queen = self.ID
            Bee.QueenBeeX = self.X
            Bee.QueenBeeY = self.Y
        else:
            queenTurn = Bees[Bee.Queen]
            if queenTurn < turn - 1:
                # The queen is dead!
                Bee.Queen = self.ID
                Bee.QueenBeeX = self.X
                Bee.QueenBeeY = self.Y

    def moveToQueenBee(self, context):
        if self.ID == Bee.Queen:
            return Move.STAY
        field = context.getPlayField()
		
		distX = Bee.QueenBeeX - self.X
		distY = Bee.QueenBeeY - self.Y
		if abs(distX) > BOARDSIZE / 2:
			flipX = True
			
		
        deltaX = sign(Bee.QueenBeeX - self.X)
        deltaY = sign(Bee.QueenBeeY - self.Y)       
        x = MID + deltaX;
        y = MID + deltaY;
        if not field[x][y]:
            point = (self.X+deltaX,self.Y+deltaY)
            if point not in MoveTo:
                MoveTo.add(point)
                return Move.inDirection(deltaX,deltaY)  
        return Move.randomMove()

    def sting(self, context):
        bullets = context.getBullets()
        if not bullets:
            return          
        players = context.shootablePlayers()
        if not players:
            return
        field = context.getPlayField()
        bestZombie = None
        bestDist = 100;
        for x in range(MID - 3, MID + 3):
            for y in range(MID - 3, MID + 3):
                zombie = field[x][y]
                if zombie and isZombie(zombie) and zombie not in Shot:
                    dist = distance(MID,MID,x,y)
                    if dist < bestDist:
                        bestDist = dist
                        bestZombie = zombie

        if bestZombie:
            Shot.add(zombie)
            return Shoot(zombie)        

        for player in players:          
            if isEnemy(player) and player not in Shot:
                Shot.add(player)
                return Shoot(player)



PlayerRegistry.registerPlayer("Bee", Bee())
