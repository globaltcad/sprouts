package util;

import java.util.function.Supplier;

/**
 *  A utility class making testing async code
 *  easier in Sprouts
 */
public class Wait {

    public static boolean until(Supplier<Boolean> condition, long timeout) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout) {
            if (condition.get()) {
                return true;
            }
        }
        return false;
    }

}
