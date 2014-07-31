from zombie import Player, Move, Shoot, PlayerRegistry, Constants

friends = ['Bee','Waller','DeadBody','ThePriest','StandStill','Vortigaunt','EmoWolfWithAGun']
MID = Constants.CENTRE_OF_VISION
BOARDSIZE = 1
sign = lambda x: (1, -1)[x<0]
isZombie = lambda player: player and player.getName() is "Zombie"
isEnemy = lambda player: player and player.getName() not in friends
isWall = lambda player: player and (player.getName() is "DeadBody" or player.getName() is "StandStill")
distance = lambda x1,y1,x2,y2: max(distance1d(x1,x2), distance1d(y1,y2))
distance1d = lambda x1,x2: min(abs(x1-x2), BOARDSIZE - abs(x1-x2))
Bees = {}
Shot = set()
MoveTo = set()  

def getDirection(x1, x2):  
    diff = x1 - x2  
    if abs(diff) > (BOARDSIZE // 2):
        return sign(diff)
    return -sign(diff)

class Bee(Player):  
    Queen = None
    QueenBeePosition = None
    X = Y = ID = 0
    LastTurn = -1   

    def doTurn(self, context): 
        global BOARDSIZE
        self.ID = context.getId().getNumber()
        self.X = context.getX()
        self.Y = context.getY() 
        BOARDSIZE = context.getBoardSize()    
        self.setQueenBee(context.getGameClock())                    
        action = self.sting(context)
        if action:
            return action
        return self.moveToQueenBee(context)     

    def setQueenBee(self, turn):
        if turn != Bee.LastTurn:
            Bee.LastTurn = turn     
            MoveTo.clear() # Clear the move set on new turn
        Bees[self.ID] = turn # Report In        
        if not Bee.Queen or (Bee.Queen and Bees[Bee.Queen] < turn - 1):
            Bee.Queen = self.ID
            Bee.QueenBeePosition = (self.X, self.Y)     

    def moveToQueenBee(self, context):
        if self.ID == Bee.Queen:
            return Move.randomMove()

        dist = distance(Bee.QueenBeePosition[0], Bee.QueenBeePosition[1], self.X, self.Y)
        if dist < 4:
            return Move.randomMove()

        signX = getDirection(self.X, Bee.QueenBeePosition[0])      
        signY = getDirection(self.Y, Bee.QueenBeePosition[1])      
        walls = 0
        field = context.getPlayField()
        for (deltaX, deltaY) in [(signX,signY),(signX,0),(0,signY),(signX,-signY),(-signX,signY)]:
            player = field[MID + deltaX][MID + deltaY]
            if isWall(player):
                walls += 1
            if not player:               
                point = frozenset([self.X+deltaX,self.Y+deltaY])            
                if point not in MoveTo:
                    MoveTo.add(point)                   
                    return Move.inDirection(deltaX,deltaY)
        if walls > 2:
            return Move.randomMove()
        return Move.STAY

    def sting(self, context):      

        if context.getBullets() < 1:
            return      
        field = context.getPlayField()
        closestZombie,closestPlayer = None,None
        closestZombieDist,bestDist = 3,5   
        for x in range(MID - 5, MID + 5):
            for y in range(MID - 5, MID + 5):
                player = field[x][y]
                if player and not isWall(player) and player not in Shot:
                    dist = distance(MID,MID,x,y)
                    if isZombie(player) and dist < closestZombieDist:   
                        closestZombieDist = dist
                        closestZombie = player
                    elif isEnemy(player) and dist < bestDist: 
                        bestDist = dist
                        closestPlayer = player

        if closestZombie:
            Shot.add(closestZombie)
            return Shoot(closestZombie)        

        if closestPlayer:
            Shot.add(closestPlayer)
            return Shoot(closestPlayer)        

PlayerRegistry.registerPlayer("Bee", Bee())