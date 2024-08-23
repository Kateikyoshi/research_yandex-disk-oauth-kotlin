package jp.warau.bakari;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Less verbose way to instantiate a logger
 */
public class LoggerInitializer {

    public static Logger initLogger(Object clazz) {
        return LoggerFactory.getLogger(clazz.getClass());
    }
}
