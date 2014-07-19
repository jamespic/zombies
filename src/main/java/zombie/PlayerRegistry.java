package zombie;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.apache.commons.io.FilenameUtils;

public class PlayerRegistry {
    private PlayerRegistry() {}
    
    private static final ThreadLocal<Map<String, Player>> players =
        new ThreadLocal<Map<String, Player>>() {
            @Override
            protected Map<String, Player> initialValue() {
                return new HashMap<>();
            }
        };
    
    public static void registerPlayer(String name, Player player) {
        players.get().put(name, player);
    }
    
    public static void registerPlayerByClass(String className) {
        try {
            Class clazz = Class.forName(className);
            Player instance = (Player) clazz.newInstance();
            registerPlayer(clazz.getSimpleName(), instance);
        } catch (Exception ex) {
            Logger logger = Logger.getLogger(PlayerRegistry.class.getName());
            logger.log(Level.SEVERE, "Failed to instantiate " + className, ex);
        }
    }
    
    static void runJsr223Script(String scriptPath) {
        try {
            String extension = FilenameUtils.getExtension(scriptPath);
            ScriptEngineManager mgr = new ScriptEngineManager();
            ScriptEngine engine = mgr.getEngineByExtension(extension);
            InputStream script = PlayerRegistry.class.getResourceAsStream(scriptPath);
            Reader scriptReader = new InputStreamReader(script, StandardCharsets.UTF_8);
            engine.eval(scriptReader);
        } catch (Exception ex) {
            Logger.getLogger(PlayerRegistry.class.getName()).log(Level.SEVERE, "Could not execute "+ scriptPath, ex);
        }
    }
    
    public static boolean isDeadOrUndead(String player) {
        return players.get().get(player) instanceof Dead;
    }
    
    static Player getPlayer(String name) {
        return players.get().get(name);
    }
    
    static void clear() {
        players.get().clear();
    }
    
    static Map<String, Player> getPlayers() {
        return players.get();
    }
}
