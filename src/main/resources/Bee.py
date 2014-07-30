from zombie import Player, Move, Shoot, PlayerRegistry, Constants

friends = ['Bee','Waller','DeadBody','ThePriest','StandStill','Vortigaunt','EmoWolfWithAGun']
MID = Constants.CENTRE_OF_VISION
sign = lambda x: (1, -1)[x<0]
isZombie = lambda player: player and player.getName() is "Zombie"
isEnemy = lambda player: player and player.getName() not in friends
distance = lambda x1,y1,x2,y2: max(abs(x1 - x2), abs(y1 - y2))
Bees = {}
Shot = set()
MoveTo = set()	
	
class Bee(Player):	
	Queen = None
	QueenBeePosition = None
	X = Y = ID = 0
	LastTurn = -1	
	
	def doTurn(self, context):	
		self.ID = context.getId().getNumber()
		self.X = context.getX()
		self.Y = context.getY()	
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
			print "New Queen"
			Bee.Queen = self.ID
			Bee.QueenBeePosition = (self.X, self.Y)		
			
	def moveToQueenBee(self, context):
		if self.ID is Bee.Queen:
			return Move.STAY;
		
		dist = distance(Bee.QueenBeePosition[0], Bee.QueenBeePosition[1], self.X, self.Y)
		if dist < 3:
			return Move.randomMove();
		
		signX = sign(Bee.QueenBeePosition[0] - self.X)
		signY = sign(Bee.QueenBeePosition[1] - self.Y)	
		field = context.getPlayField()
		for (deltaX, deltaY) in [(signX,signY),(signX,0),(0,signY),(signX,-signY),(-signX,signY)]:
			if not field[MID + deltaX][MID + deltaY]:				
				point = frozenset([self.X+deltaX,self.Y+deltaY])			
				if point not in MoveTo:
					MoveTo.add(point)					
					return Move.inDirection(deltaX,deltaY)	
		return Move.STAY
		
	def sting(self, context):		
		if not context.getBullets():
			return		
		field = context.getPlayField()
		bestZombie,bestPlayer = None,None
		bestZombieDist,bestDist = 4,5	
		for x in range(MID - 5, MID + 5):
			for y in range(MID - 5, MID + 5):
				player = field[x][y]
				if player:
					dist = distance(MID,MID,x,y)
					if isZombie(player) and dist < bestZombieDist and player not in Shot:	
						bestZombieDist = dist
						bestZombie = player
					if isEnemy(player) and dist < bestDist and player not in Shot:	
						bestDist = dist
						bestPlayer = player
		
		if bestZombie:
			Shot.add(bestZombie)
			return Shoot(bestZombie)		
		
		if bestPlayer:
			Shot.add(bestPlayer)
			return Shoot(bestPlayer)		
			
PlayerRegistry.registerPlayer("Bee", Bee())