package zombie;

import frege.prelude.PreludeBase.TST;
import frege.runtime.Delayed;
import frege.runtime.Lambda;
import java.lang.reflect.Method;
import org.python.tests.props.PropShadow;

public class FregeAdapter implements Player {
    private static final String METHOD_NAME = "doTurn";
    private Method method;
    public final String name;

    public FregeAdapter(String className) {
        try {
            Class clazz = Class.forName(className);
            name = clazz.getSimpleName();
            try {
                method = clazz.getMethod(METHOD_NAME, PlayerContext.class);
            } catch (NoSuchMethodException e) {
                method = clazz.getMethod(METHOD_NAME, Object.class);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Action doTurn(PlayerContext context) {
        try {
            Object rawResult = method.invoke(null, context);
            Object forcedResult = Delayed.forced(rawResult);
            if (forcedResult instanceof Action) return (Action) forcedResult;
            else {
                // Must be IO Action
                Lambda ioMonad = (Lambda) forcedResult;
                Object monadResult = TST.performUnsafe(ioMonad);
                Object forcedMonadResult = Delayed.forced(monadResult);
                return (Action) forcedMonadResult;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
