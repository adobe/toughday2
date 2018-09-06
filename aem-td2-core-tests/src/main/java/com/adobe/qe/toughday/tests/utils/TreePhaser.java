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
package com.adobe.qe.toughday.tests.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phaser to syncronize creation of a tree when a new level needs to be created
 */
public class TreePhaser extends Phaser {
    public Thread mon;

    public TreePhaser() {
        super();
        this.monitor();
    }

    private int base = Integer.parseInt(DEFAULT_BASE);

    public static final String DEFAULT_BASE = "10";

    public final AtomicInteger nextChildPerLevel = new AtomicInteger(0);

    private int maxChildrenPerLevel = base;

    protected boolean onAdvance(int phase, int registeredParties) {
        // Increase the level
        // reset counter for level
        this.nextChildPerLevel.set(0);
        maxChildrenPerLevel = maxChildrenPerLevel * base;
        // Return false, never terminate phaser.
        /*if (LOG.isDebugEnabled()) LOG.debug("onAdvance. phase=%d registeredParties=%d tid=%d",
                phase, registeredParties, Thread.currentThread().getId());*/
        return false;
    }

    public int getNextNode() {
        int childNumber = this.nextChildPerLevel.getAndIncrement();
        if (childNumber >= maxChildrenPerLevel) {
            //if (LOG.isDebugEnabled()) LOG.debug("Waiting for sync. tid = " + Thread.currentThread().getId());
            this.arriveAndAwaitAdvance();
            return getNextNode();
        }
        return childNumber;
    }

    public int getLevel() {
        return this.getPhase() + 1;
    }

    public void monitor() {
        this.mon = new Thread() {
            @Override
            public void run() {
                do {
                    if (nextChildPerLevel.get() >= maxChildrenPerLevel ) {
                        arriveAndAwaitAdvance();
                    } else {
                        try {
                            sleep(100);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                } while (true);
            }
        };
        this.register();
        mon.start();
    }

    public void setBase(int base) {
        this.maxChildrenPerLevel = base;
        this.base = base;
    }

    public int getBase() {
        return base;
    }

    public static String computeParentPath(int nextChild, int level, int base, String title, String prefix) {
        prefix = (null != prefix) ? prefix : "/";
        prefix = prefix.endsWith("/") ? prefix : prefix + "/";
        if (level == 1) {
            return prefix;
        }

        String path = Integer.toString(nextChild / base, base);
        path = StringUtils.leftPad(path, level-1, "0");
        path = StringUtils.stripStart(path.replace("", "/" + title), "/");
        path = prefix + StringUtils.stripEnd(path, title);
        return path;
    }

    public static String computeParentPath(int nextChild, int level, int base, String title) {
        return computeParentPath(nextChild, level, base, title, "/");
    }

    public static String computeNodeName(int nextChild, int base, String title) {
        return title + Integer.toString(nextChild % base, base);
    }
}
