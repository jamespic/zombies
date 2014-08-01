package zombie;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.awt.Point;
import java.io.*;
import static zombie.Constants.*;
import static java.lang.Math.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.text.DateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.util.ResizableDoubleArray;
import org.apache.commons.math3.stat.descriptive.rank.Median;

public class Game {	
    private static final boolean DEBUG = true;
    private static final String[] COMPILED_PLAYERS = new String[] {
        "player.StandStill",
		"player.MoveRandomly",
        "player.Gunner",
        "player.EmoWolfWithAGun",
        "player.GordonFreeman",
        "player.HuddleWolf",
        "player.ZombieRightsActivist",
        "player.ZombieHater",
        "player.ThePriest",
        "player.Shotguneer",
        "player.Coward",
        "player.HideyTwitchy",
        "player.Waller",		
        "player.Vortigaunt",
        "player.Fox",
        "player.Sokie",
        "player.SuperCoward",
        "player.SunTzu",
        "player.SOS"
//        "example.ScalaExample"
    };
    private static final String[] JSR223_PLAYERS = new String[] {
//        "/js-example.js",
		  "/bee.py"
          ,"/john-nash.js"
//        "/rb-example.rb",
//        "/clj-example.clj"
    };
    private static final String[] FREGE_PLAYERS = new String[] {
//        "example.PureFregeExample",
//        "example.IOFregeExample",
//        "example.ContinuationFregeExample"
          "player.Cocoon"
    };
    private static final String[] COLORS = new String[] {
        "Red",
        "Green",
        "Chartreuse",
        "Blue",
        "DarkOrange",
        "Gold",
        "HotPink",
        "Sienna"
    };

	private static final String[] watchedPlayers = null;
  
    private int boardSize;
    private List<PlayerInfo> players = new ArrayList<>();
    private int playerIdCounter = 0;
    private Set<PlayerId> shootings = new HashSet<>();
    private Map<String, Integer> scores = new TreeMap<>();
    private int gameClock = 0;
    private final MersenneTwister rand = new MersenneTwister();
    
    public static void registerSystemPlayers() {
        PlayerRegistry.registerPlayer(Dead.DEADBODYNAME, Dead.DEADBODY);
        PlayerRegistry.registerPlayer(Dead.ZOMBIENAME, Dead.ZOMBIE);
    }
    
    public static void registerCompiledPlayers() {
        for (String player: COMPILED_PLAYERS) {
            PlayerRegistry.registerPlayerByClass(player);
        }
    }
    
    public static void registerJsr223Players() {
        for (String script: JSR223_PLAYERS) {
            PlayerRegistry.runJsr223Script(script);
        }
    }
	    
    public static void registerFregePlayers() {
        for (String className: FREGE_PLAYERS) {
            PlayerRegistry.registerFregePlayer(className);
        }
    }
	
    public void clearOutputDirectory() {
		File dir = new File("game-output");
		String[] allFiles = dir.list();
		for (String file : allFiles) {
			if (file.endsWith(".html")) {
				new File("game-output" + "/" + file).delete();
			}
		}
	}

    private Point randomPoint() {
        int x = rand.nextInt(boardSize);
        int y = rand.nextInt(boardSize);
        return new Point(x, y);
    }
    
    private int diffWrapping(int b, int a) {
        int d = b - a;
        while (d > boardSize / 2) d -= boardSize;
        while (d < -boardSize / 2) d += boardSize;
        return d;
    }
    
    private Point diffWrapping(Point b, Point a) {
        return new Point(diffWrapping(b.x, a.x), diffWrapping(b.y, a.y));
    }
    
    private Point findUnoccupiedSpace() {
        outer: while (true) {
            Point p = randomPoint();
            for (PlayerInfo info: players) {
                if (info.newPosition.equals(p)) continue outer;
            }
            return p;
        }
    }
    
    public void initializeBoard() {
        int livingPlayers = 0;
        for (Player player: PlayerRegistry.getPlayers().values()) {
            if (!(player instanceof Dead)) livingPlayers++;
        }
        int totalStartingPlayers = livingPlayers * PLAYERS_PER_SPECIES;
        double squaresNeeded = totalStartingPlayers / PLAYER_DENSITY;
        boardSize = max(VISION_WIDTH, (int) ceil(sqrt(squaresNeeded)));
        
        for (Map.Entry<String, Player> entry: PlayerRegistry.getPlayers().entrySet()) {
            Player player = entry.getValue();
            String name = entry.getKey();
            if (!(player instanceof Dead)) {
                for (int i = 0; i < PLAYERS_PER_SPECIES; i++) {
                    Point p = findUnoccupiedSpace();
                    PlayerInfo info =
                        new PlayerInfo(
                            name,
                            p,
                            player,
                            STARTING_BULLETS
                        );
                    players.add(info);
                            
                }
            }
        }
    }
    
    public void doTurn() {
        spawnAZombie();
        doZombieAttacks();
        doScoring();
        requestMoves();
        doShootings();
        doMovements();
        distributeBullets();
        gameClock++;
    }
    
    private void spawnAZombie() {
        Point position = randomPoint();
        for (PlayerInfo player: players) {
            if (player.newPosition.equals(position)) {
                player.turn();
                return;
            }
        }
        // No player currently at that position, so we spawn, rather than turn
        players.add(new PlayerInfo(Dead.ZOMBIENAME, position, Dead.ZOMBIE, 0));
    }
    
    private void requestMoves() {
        PlayerId[][] board = new PlayerId[boardSize][boardSize];
        for (PlayerInfo player: players) {
            int x = player.oldPosition.x;
            int y = player.oldPosition.y;
            board[x][y] = player.playerId();
        }
        
        Collections.shuffle(players); // Randomise play order
        for (PlayerInfo playerInfo: players) {
            try {
                PlayerContext context = playerInfo.generateContext(board);
                Set<PlayerId> shootablePlayers = context.shootablePlayers();
                Action action = playerInfo.player.doTurn(context);
                if (action instanceof Move) {
                    playerInfo.move((Move) action);
                } else if (action instanceof Shoot) {
                    if (playerInfo.bullets > 0) {
                        playerInfo.bullets--;
                        
                        PlayerId target = ((Shoot) action).getTarget();
                        if (shootablePlayers.contains(target)) {
                            shootings.add(target);
                        }
                    }
                }
            } catch (Exception ex) {
                Logger logger = Logger.getLogger(Game.class.getName());
                logger.log(Level.SEVERE, "Player " + playerInfo.name + " failed to move", ex);
            }
        }
    }

    private void doShootings() {
        for (PlayerInfo player: players) {
            if (shootings.contains(player.playerId())) player.die();
        }
        shootings.clear();
    }

    private void doMovements() {
        boolean conflicts = true;
        while (conflicts) {
            conflicts = false;
            
            Multimap<Point, PlayerInfo> newBoard = ArrayListMultimap.create();
            for (PlayerInfo player: players) {
                newBoard.put(player.newPosition, player);
            }
            
            for (Collection<PlayerInfo> group: newBoard.asMap().values()) {
                if (group.size() > 1) {
                    conflicts = true;
                    for (PlayerInfo player: group) {
                        player.goBack();
                    }
                }
            }
        }
        
        for (PlayerInfo player: players) {
            player.confirmMove(); // All conflicts have been settled, so we confirm the move
        }
    }

    private void distributeBullets() {
        Map<Point, PlayerInfo> board = new HashMap<>();
        for (PlayerInfo player: players) {
            board.put(player.newPosition, player);
        }
        
        for (PlayerInfo player: players) {
            if (player.player == Dead.DEADBODY) {
                List<PlayerInfo> nearbyPlayers = new ArrayList<>();
                
                for (int xOff = -1; xOff <= 1; xOff++) {
                    for (int yOff = -1; yOff <= 1; yOff++) {
                        int x = (boardSize + player.newPosition.x + xOff) % boardSize;
                        int y = (boardSize + player.newPosition.y + yOff) % boardSize;
                        PlayerInfo neighbour = board.get(new Point(x, y));
                        if (neighbour != null && !(neighbour.player instanceof Dead)) {
                            nearbyPlayers.add(neighbour);
                        }
                    }
                }
                
                if (!nearbyPlayers.isEmpty()) {
                    int bullets = player.bullets / nearbyPlayers.size();
                    player.bullets = 0;
                    for (PlayerInfo neighbour: nearbyPlayers) {
                        neighbour.bullets += bullets;
                    }
                }
            }
        }
    }

    private void doZombieAttacks() {
        Map<Point, PlayerInfo> board = new HashMap<>();
        for (PlayerInfo player: players) {
            board.put(player.newPosition, player);
        }
        
        List<Point> zombies = new ArrayList<>();
        for (PlayerInfo player: players) {
            if (player.player == Dead.ZOMBIE) {
                zombies.add(player.newPosition);
                
            }
        }
        
        for (Point zombieLoc: zombies) {
            for (int xOff = -1; xOff <= 1; xOff++) {
                    for (int yOff = -1; yOff <= 1; yOff++) {
                        int x = (boardSize + zombieLoc.x + xOff) % boardSize;
                        int y = (boardSize + zombieLoc.y + yOff) % boardSize;
                        PlayerInfo neighbour = board.get(new Point(x, y));
                        if (neighbour != null && !(neighbour.player instanceof Dead)) {
                            neighbour.turn();
                        }
                    }
                }
        }
    }

    private void doScoring() {
        if (DEBUG) {
            
            PlayerInfo[][] board = new PlayerInfo[boardSize][boardSize];
            for (PlayerInfo player: players) {
                board[player.newPosition.y][player.newPosition.x] = player;
            }		
            Path outputFile = Paths.get("game-output", gameClock + ".html");
            try (Writer writer = new FileWriter(outputFile.toFile())) {
                writer.append("<!DOCTYPE html>\n");
                writer.append("<html>\n");
                writer.append("  <head>\n");
                writer.append("    <title>Game Stage " + gameClock + "</title>\n");
                writer.append("  </head>\n");
				writer.append("  <script>\n");
				writer.append("  document.onkeydown = function(evt) {\n");
				writer.append("    evt = evt || window.event;\n");
				writer.append("    switch (evt.keyCode) {\n");
				writer.append("      case 37:\n");
				writer.append("        document.location.href = \""+(gameClock - 1)+".html\"; return false;\n");
				writer.append("      case 39:\n");
				writer.append("        document.location.href = \""+(gameClock + 1)+".html\"; return false;\n");
				writer.append("    }\n");
				writer.append("  };\n");
				writer.append("  </script>\n");
                writer.append("  <link rel=\"stylesheet\" href=\"style.css\"/>\n");
                writer.append("  <body>\n");
                if (gameClock > 0) {
                    writer.append("<a href=\"" + (gameClock - 1) + ".html\">Prev</a>\n");
                }
                writer.append("<a href=\"" + (gameClock + 1) + ".html\">Next</a>");
                writer.append("<pre>");
                for (PlayerInfo[] row: board) {
                    for (PlayerInfo player: row) {
                        if (player == null) {
                            writer.append('.');
                        } else {
                            switch(player.name) {
                                case Dead.DEADBODYNAME:
                                    writer.append("<span class=\"X\">D</span>");
                                    break;
                                case Dead.ZOMBIENAME:
                                    writer.append("<span class=\"X\">Z</span>");
                                    break;
                                default:
                                    char initial = player.name.charAt(0);
                                    String title = player.name + ": " + player.bullets + " bullets";
                                    char cssClass = (char) ('a' + (player.name.hashCode() & 7));
                                    writer.append("<span class=\"" + cssClass + "\" title=\"" + title + "\">" + initial + "</span>");
                                    break;
                                    
                            }
                        }
                    }
					
					if(watchedPlayers != null) {					
						writer.append("  ");
						for (PlayerInfo player: row) {
							if (player == null) {
								writer.append('.');
							} else {
								boolean contains = false;
								for(String name : watchedPlayers) {
									if(name.equals(player.name))
									{
										contains = true;
										break;
									}
								}
								if(contains) {																
									String title = player.name + ": " + player.bullets + " bullets";
									char initial = player.name.charAt(0);
									writer.append("<span class=\"X\" title=\"" + title + "\">" + initial + "</span>");	
								} else {
									writer.append('.');
								}                          
							}
						}
					}
					

                    writer.append("\n");
                }
                writer.append("</pre>\n");
                writer.append("</body></html>");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
        for (PlayerInfo player: players) {
            if (!(player.player instanceof Dead)) scores.put(player.name, gameClock);
        }
    }
    
    public boolean playersLeft() {
        int livingPlayers = 0;
        for (PlayerInfo player: players) {
            if (!(player.player instanceof Dead)) {
                livingPlayers++;
            }
        }
        return livingPlayers > 0;
    }
    
    public Map<String, Integer> getScores() {
        return Collections.unmodifiableMap(scores);
    }

    class PlayerInfo {
        public String name;
        public int id = playerIdCounter++;
        public Point newPosition;
        public Player player;
        public int bullets;
        public Point oldPosition;

        public PlayerInfo(String name, Point position, Player player, int bullets) {
            this.name = name;
            this.newPosition = position;
            this.player = player;
            this.bullets = bullets;
            this.oldPosition = position;
        }
        
        public void goBack() {
            newPosition = oldPosition;
        }
        
        public void confirmMove() {
            oldPosition = newPosition;
        }
        
        public void move(Move move) {
            int newx = (boardSize + newPosition.x + move.x) % boardSize;
            int newy = (boardSize + newPosition.y + move.y) % boardSize;
            newPosition = new Point(newx, newy);
        }
        
        public void die() {
            name = Dead.DEADBODYNAME;
            id = playerIdCounter++; // New identity
            player = Dead.DEADBODY;
            newPosition = oldPosition; // Cancel planned move
        }
        
        public void turn() {
            name = Dead.ZOMBIENAME;
            id = playerIdCounter++; // New identity
            player = Dead.ZOMBIE;
            bullets = 0; // Zombies lose all bullets on death
        }
        
        public PlayerId playerId() {
            return new PlayerId(name, id);
        }
        
        public PlayerContext generateContext(PlayerId[][] board) {
            PlayerId[][] playField = new PlayerId[VISION_WIDTH][VISION_WIDTH];
            for (int xOff = -VISION_RANGE; xOff <= VISION_RANGE; xOff++) {
                for (int yOff = -VISION_RANGE; yOff <= VISION_RANGE; yOff++) {
                    int xInBoard = (boardSize + oldPosition.x + xOff) % boardSize;
                    int yInBoard = (boardSize + oldPosition.y + yOff) % boardSize;
                    playField[xOff + CENTRE_OF_VISION][yOff + CENTRE_OF_VISION] =
                            board[xInBoard][yInBoard];
                }
            }
            return new PlayerContext(playerId(), bullets, gameClock, playField, oldPosition.x, oldPosition.y, boardSize);
        }
    }
    
    public static void main(String[] args) {
        registerAllPlayers();
        
        Game game = new Game();
        
        game.initializeBoard();
		
        if(DEBUG) game.clearOutputDirectory();

        while(game.playersLeft()) game.doTurn();
        
        // Output scores
        try (FileWriter gameLog = new FileWriter("game-output/game.log", true)) {
            gameLog.append("#Run at " + DateFormat.getDateTimeInstance().format(new Date()) + "#\n");
            for (Map.Entry<String, Integer> entry: game.getScores().entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
                gameLog.append(entry.getKey() + ": " + entry.getValue() + "\n");
            }
            gameLog.append('\n');
        } catch (IOException ex) {
            ex.printStackTrace();
            for (Map.Entry<String, Integer> entry: game.getScores().entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        }
        
        // Output aggregate scores for runs in log
        try (BufferedReader gameLog = new BufferedReader(new FileReader("game-output/game.log"))) {
            Map<String, ResizableDoubleArray> scores = new TreeMap<>();
            String line;
            while ((line = gameLog.readLine()) != null) {
                if (line.contains(": ")) {
                    String[] splitLine = line.split(": ");
                    String name = splitLine[0];
                    double score = Double.valueOf(splitLine[1]);
                    if (scores.containsKey(name)) {
                        scores.get(name).addElement(score);
                    } else {
                        scores.put(name, new ResizableDoubleArray(new double[] {score}));
                    }
                }
            }
            System.out.println();
            System.out.println("Aggregate Scores");
            for (Map.Entry<String, ResizableDoubleArray> entry: scores.entrySet()) {
                Median median = new Median();
                double medianScore = median.evaluate(entry.getValue().getElements());
                System.out.println(entry.getKey() + ": " + medianScore);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    static void registerAllPlayers() {
        registerSystemPlayers();
        registerCompiledPlayers();
        registerJsr223Players();
        registerFregePlayers();
    }
}
