var zombie = Packages.zombie
zombie.PlayerRegistry.registerPlayer(
  "js-example",
  new zombie.Player({doTurn: function(context) {return zombie.Move.STAY}})
)

