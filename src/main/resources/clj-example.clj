(zombie.PlayerRegistry/registerPlayer 
  "clj-example"
  (reify zombie.Player
    (doTurn [this context] zombie.Move/STAY)
  )
)