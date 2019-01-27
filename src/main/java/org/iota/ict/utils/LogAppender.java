package org.iota.ict.utils;

import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.Serializable;

@Plugin(name = "LogAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)

public class LogAppender extends AbstractAppender {

    public static final Object NOTIFY_SYNCHRONIZER = new Object();

    private static final int LOG_CAPACITY = 50;
    private static int writePosition = 0;
    private static LogEvent[] logs = new LogEvent[LOG_CAPACITY];

    protected LogAppender(String name, Filter filter, Layout<? extends Serializable> layout, final boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
    }

    @PluginFactory
    public static LogAppender createAppender(@PluginAttribute("name") String name, @PluginElement("Layout") Layout<? extends Serializable> layout, @PluginElement("Filter") Filter filter) {
        return new LogAppender(name, filter, layout, false);
    }

    @Override
    public synchronized void append(LogEvent event) {
        logs[(writePosition++)%LOG_CAPACITY] = event;
        synchronized (NOTIFY_SYNCHRONIZER) {
            NOTIFY_SYNCHRONIZER.notifyAll();
        }
    }

    public static LogEvent getLogEvent(int index) {
        if(index >= writePosition || index < writePosition-LOG_CAPACITY || index < 0)
            return null;
        return logs[index % LOG_CAPACITY];
    }

    public static int getIndexMin() {
        return Math.max(writePosition-LOG_CAPACITY, -1);
    }

    public static int getIndexMax() {
        return writePosition-1;
    }
}