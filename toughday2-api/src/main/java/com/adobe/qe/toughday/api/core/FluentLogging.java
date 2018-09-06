/*
Copyright 2015 Adobe. All rights reserved.
This file is licensed to you under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License. You may obtain a copy
of the License at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under
the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
OF ANY KIND, either express or implied. See the License for the specific language
governing permissions and limitations under the License.
*/
package com.adobe.qe.toughday.api.core;


import com.adobe.qe.toughday.api.core.benchmark.signatures.Callable;
import com.adobe.qe.toughday.api.core.benchmark.signatures.VoidCallable;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class FluentLogging {
    private static class LogEntry {
        public final Level level;
        public final String message;
        public final Object[] objects;

        public LogEntry(Level level, String message, Object[] objects) {
            this.level = level;
            this.message = message;
            this.objects = objects;
        }
    }

    private static class ThrowableLogEntry extends LogEntry {
        public final boolean includeStackTrace;

        public ThrowableLogEntry(Level level, String message, Object[] objects) {
            super(level, message, objects);
            this.includeStackTrace = false;
        }

        public ThrowableLogEntry(Level level, String message, boolean includeStackTrace) {
            super(level, message, null);
            this.includeStackTrace = includeStackTrace;
        }
    }

    private Logger logger;

    private List<LogEntry>  before = new ArrayList<>();
    private List<LogEntry>  after = new ArrayList<>();
    private List<LogEntry>  success = new ArrayList<>();
    private List<ThrowableLogEntry>  throwable = new ArrayList<>();


    public FluentLogging(Logger logger) {
        this.logger = logger;
    }

    public static FluentLogging create(Logger logger) {
        return new FluentLogging(logger);
    }

    public FluentLogging before(Level level, String message, Object... objects) {
        before.add(new LogEntry(level, message, objects));
        return this;
    }

    public FluentLogging after(Level level, String message, Object... objects) {
        after.add(new LogEntry(level, message, objects));
        return this;
    }

    public FluentLogging onThrowable(Level level, String message, Object... objects) {
        throwable.add(new ThrowableLogEntry(level, message, objects));
        return this;
    }

    public FluentLogging onThrowable(Level level, String message, boolean includeStackTrace) {
        throwable.add(new ThrowableLogEntry(level, message, includeStackTrace));
        return this;
    }

    public FluentLogging onThrowable(Level level, String message) {
        return onThrowable(level, message, false);
    }

    public FluentLogging onSuccess(Level level, String message, Object... objects) {
        success.add(new LogEntry(level, message, objects));
        return this;
    }

    public <T> T run(Callable<T> callable) throws Throwable {
        for(LogEntry entry : before) {
            logger.log(entry.level, entry.message, entry.objects);
        }
        try {
            T result = callable.call();
            for(LogEntry entry : success) {
                logger.log(entry.level, entry.message, entry.objects);
            }
            return result;
        } catch (Throwable e) {
            for (ThrowableLogEntry entry : throwable) {
                if (entry.includeStackTrace) {
                    logger.log(entry.level, entry.message, e);
                } else {
                    logger.log(entry.level, entry.message, entry.objects);
                }
            }
            throw e;
        } finally {
            for (LogEntry entry : after) {
                logger.log(entry.level, entry.message, entry.objects);
            }
        }
    }

    public void run(VoidCallable callable) throws Throwable {
        run(() -> {
            callable.call();
            return null;
        });
    }
}
