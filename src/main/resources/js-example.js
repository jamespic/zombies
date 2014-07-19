with (Packages.zombie) {
  PlayerRegistry.registerPlayer(
    "js-example",
    new Player({doTurn: function(context) {return Move.STAY}})
  )
}

