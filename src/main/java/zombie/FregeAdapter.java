package zombie;

import frege.prelude.PreludeBase.TST;
import frege.runtime.Delayed;
import frege.runtime.Lambda;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import org.python.tests.props.PropShadow;

public class FregeAdapter implements Player {
    private static final String METHOD_NAME = "doTurn";
    private Method method;
    private Object methodTarget = null;
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
            Object rawResult = method.invoke(methodTarget, context);
            Object forcedResult = Delayed.forced(rawResult);
            if (forcedResult instanceof Action) {
                // Simple case of pure function
                return (Action) forcedResult;
            } else if (forcedResult instanceof Lambda){
                // Must be IO Action
                Lambda ioMonad = (Lambda) forcedResult;
                Object monadResult = TST.performUnsafe(ioMonad);
                Object forcedMonadResult = Delayed.forced(monadResult);
                return (Action) forcedMonadResult;
            } else {
                // Must be continuation.
                // Unfortunately, compile order means we can't see TContinue at compile time,
                // so we have to use reflection
                Object continuation = forcedResult;
                Class continueClass = forcedResult.getClass();
                Field resultField = continueClass.getField("mem$result");
                Field andThenField = continueClass.getField("mem$andThen");
                Action result = (Action) Delayed.forced(resultField.get(continuation));
                methodTarget = (Lambda) Delayed.forced(andThenField.get(continuation));
                method = lambdaApplyMethod();
                return result;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private Method lambdaApplyMethod() throws Exception {
        return Lambda.class.getMethod("apply", Object.class);
    }
}
