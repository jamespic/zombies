#Save The Last Bullet For Yourself#

Suddenly zombies appear! OH NOES!

In this king-of-the-hill challenge, you must create a bot to survive the zombie apocalypse. Or at least, hold out for as long as possible.

At the start of the game, 50 instances of each entry will be placed randomly in a large [toroidal][1] play area - that is, it appears to be square, but wraps around. The size of the play area will vary depending on the number of entries, but initially 6% of squares will be occupied. Each competitor starts with 3 bullets.

At the beginning of each turn, a zombie will rise from the ground at a random location, destroying whatever was above it. Any player who is next to a zombie at the start of their turn will become a zombie.

For each living player, their code will then be called. It will receive a [PlayerContext][2] object, containing information on their current status, and their surroundings. Each player can see for 8 squares in any direction.

The player must choose to either move (staying still is a valid movement), by returning a `Move`, or shoot a nearby person or zombie, by returning a `Shoot`. Your gun has a maximum range of 5 squares. Since you are within your gun's range you *can* shoot yourself, provided you have bullets left. If two players shoot each other, they both die.

If two players attempt to move onto the same square, they will fail, and will both return to the square they started from. If there are still conflicts, this will be repeated until there are no conflicts, which may mean everyone is back where they started.

If a player dies from a gunshot, their dead body will remain, and forms a permanent barrier. Any bullets they were carrying remain on their person, and can be scavenged by players in adjacent squares. If there are multiple players occupying the squares adjacent to a dead body, then the bullets will be shared between them, but any remainder will be lost.

If a player becomes a zombie, then their bullets are lost. Zombies will mindlessly walk towards the nearest living player.

Entries are scored on how long their longest-surviving player survives.

##Entries##

A control program is available at https://github.com/jamespic/zombies. Simply clone it, and run `mvn compile exec:java`.

To be eligible, entries must be written in a JVM language, must be portable, and must be possible to build from Maven with no special set-up. This is to ensure that competitors do not need to install multiple run-time environments to test their bots against competitors.

Sample entries are currently available in the following languages:

- [Java 7][3] - see also [a more complex example][4], and [the code for zombies][5]
- [Scala 2.11.1][6]
- [Javascript (via Rhino)][7]
- [Python (via Jython 2.7 beta 2)][8]
- [Ruby (via JRuby 1.7.13)][9]
- [Clojure 1.5.1][10]
- [Frege][11] (a bit like Haskell - [here's another example][12])

If you would like to compete in a language that is not listed, you can post a comment requesting it, and I will investigate the possibility of integrating your chosen language into the control program. Or, if you are impatient, you can submit a pull request to the control program.

Only one instance (in the Java sense of the word) will be created for each entry. This Java instance will be called multiple times per turn - once for each surviving player.

##API##

<!-- language: lang-java -->

    package zombie

    // You implement this. Your entry should be in package `player`
    interface Player {
        Action doTurn(PlayerContext context)
    }

    // These already exist
    class PlayerContext {
        // A square array, showing the area around you, with you at the centre
        // playFields is indexed by x from West to East, then y from North to South
        PlayerId[][] getPlayField()
        int getBullets() // Current bullets available
        int getGameClock() // Current turn number
        PlayerId getId() // Id of the current player instance
        int getX() // Your current x co-ordinate
        int getY() // Your current y co-ordinate
        Set<PlayerId> shootablePlayers() // A helper function that identifies players in range.
    }

    class PlayerId {
        String getName() // The name of the entrant that owns this player
        int getNumber() // A unique number, assigned to this player
    }

    // Don't implement this. Use either `Move` or `Shoot`
    interface Action {}

    enum Move implements Action {
        NORTHWEST, NORTH, NORTHEAST,
        EAST, STAY, WEST,
        SOUTHEAST, SOUTH, SOUTHWEST;
        static move randomMove();
    }

    class Shoot implements Action {
        Shoot(PlayerId target);
    }

##Additional Rules##

Each entry must have a unique name, in order to work correctly with the control program.

Entries should not attempt to tamper with other entrants, or with the control program, or otherwise take advantage of the run-time environment to "break the fourth wall", and gain an advantage that would not be available in a "real" zombie apocalypse.

Communication between players is allowed.

The winner is the entrant whose bot has the highest score in a test I will run on the 3rd of August 2014.

##Results##

On 21st July, the results were as follows:

    Entrant         Round 1 Round 2 Round 3 Round 4 Round 5 Median
    GordonFreeman   289     167     215     146     237     215
    Gunner          227     136     182     131     182     182
    StandStill      288     173     202     143     113     173
    HuddleWolf      158     168     275     139     158     158
    EmoWolfWithAGun 0       0       0       0       0       0

The control program was run for 5 rounds, and the entries were ranked according to their median score.

You can watch a run of the last battle [here][13]


  [1]: http://en.wikipedia.org/wiki/Torus#Topology
  [2]: https://github.com/jamespic/zombies/blob/master/src/main/java/zombie/PlayerContext.java
  [3]: https://github.com/jamespic/zombies/blob/master/src/main/java/player/StandStill.java
  [4]: https://github.com/jamespic/zombies/blob/master/src/main/java/player/Gunner.java
  [5]: https://github.com/jamespic/zombies/blob/master/src/main/java/zombie/Dead.java#L14
  [6]: https://github.com/jamespic/zombies/blob/master/src/main/scala/example/ScalaExample.scala
  [7]: https://github.com/jamespic/zombies/blob/master/src/main/resources/js-example.js
  [8]: https://github.com/jamespic/zombies/blob/master/src/main/resources/py-example.py
  [9]: https://github.com/jamespic/zombies/blob/master/src/main/resources/rb-example.rb
  [10]: https://github.com/jamespic/zombies/blob/master/src/main/resources/clj-example.clj
  [11]: https://github.com/jamespic/zombies/blob/master/src/main/frege/PureFregeExample.fr
  [12]: https://github.com/jamespic/zombies/blob/master/src/main/frege/IOFregeExample.fr
  [13]: http://jamespic.github.io/zombies/2014-07-21/0.html