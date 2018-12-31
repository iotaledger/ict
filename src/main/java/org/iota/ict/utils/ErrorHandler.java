package org.iota.ict.utils;

import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public final class ErrorHandler {

    private static List<ThrowableLog> logs = new LinkedList<>();

    public static void handleWarning(Logger logger, Throwable throwable, String message) {
        logger.warn(message + " (" + throwable.getMessage() + ")");
        logs.add(new ThrowableLog(throwable));
    }

    public static void handleError(Logger logger, Throwable throwable, String message) {
        logger.error(message, throwable);
        logs.add(new ThrowableLog(throwable));
    }

    public static void dump(File dir) {
        dir.mkdirs();
        File logFile = new File(dir, "error_" + System.currentTimeMillis() + ".log");
        // TODO write to File
    }

    private static class ThrowableLog {
        private Throwable throwable;
        private long timestamp;

        ThrowableLog(Throwable throwable) {
            this.throwable = throwable;
            this.timestamp = System.currentTimeMillis();
        }
    }
}