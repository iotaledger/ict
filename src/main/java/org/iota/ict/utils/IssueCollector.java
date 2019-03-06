package org.iota.ict.utils;

import java.util.LinkedList;
import java.util.List;

/**
 * Collects unexpected exceptions and errors thrown during runtime. Makes it simpler to diagnose any issues.
 * */
public final class IssueCollector {

    private static final int MAX_AMOUNT_OF_THROWABLES_TO_PRINT = 10;
    private static final int MAX_AMOUNT_OF_THROWABLES_BEFORE_SHUTDOWN = 10;
    protected static final List<Incident> incidents = new LinkedList<>();

    public static void collect(Throwable t) {
        incidents.add(new Incident(t));

        if (incidents.size() > MAX_AMOUNT_OF_THROWABLES_BEFORE_SHUTDOWN) {
            log();
            System.exit(0);
        }
    }

    public static int amountOfIndicidents() {
        return incidents.size();
    }

    public static void log() {
        // use system.err instead of logger in case there is an exception with logging
        if (incidents.size() > 0) {
            System.err.println("***** ERROR REPORT *****");
            System.err.println("This is a list of critical incidents which occurred during runtime.");
            System.err.println("Please create an issue on https://github.com/iotaledger/ict or report it in #ict-discussion in IOTA Discord.");

            int amountPrinted = 0;
            for (Incident incident : incidents) {
                System.err.println();
                if (amountPrinted++ > MAX_AMOUNT_OF_THROWABLES_TO_PRINT) {
                    System.err.println("And " + (incidents.size() - MAX_AMOUNT_OF_THROWABLES_TO_PRINT) + " more incidents ...");
                    break;
                }
                incident.log();
            }

            System.err.println();
            System.err.println("************************");
        }
    }


    static class Incident {
        private final Throwable throwable;
        private final long timestamp;

        private Incident(Throwable throwable) {
            this.throwable = throwable;
            this.timestamp = System.currentTimeMillis();
        }

        private void log() {
            System.err.println("[" + timestamp + "]");
            throwable.printStackTrace();
        }
    }

}
