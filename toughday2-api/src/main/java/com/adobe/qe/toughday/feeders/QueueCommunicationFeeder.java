/*
Copyright 2018 Adobe. All rights reserved.
This file is licensed to you under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License. You may obtain a copy
of the License at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under
the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
OF ANY KIND, either express or implied. See the License for the specific language
governing permissions and limitations under the License.
*/
package com.adobe.qe.toughday.feeders;

import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.api.core.NamedObjectImpl;
import com.adobe.qe.toughday.api.feeders.InputFeeder;
import com.adobe.qe.toughday.api.feeders.OutputFeeder;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class QueueCommunicationFeeder extends NamedObjectImpl implements InputFeeder<String>, OutputFeeder<String> {
    private ConcurrentLinkedQueue<String> channel = new ConcurrentLinkedQueue<>();
    private ReadWriteLock rwLock = new ReentrantReadWriteLock(); //used to prevent any thread from entering into a live lock

    private int maxSize;

    @Override
    public String get() throws Exception {
        rwLock.readLock().lock();
        try {
            return channel.poll();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void push(String item) throws Exception {
        if(channel.size() > maxSize) {
            rwLock.writeLock().lock();
            try {
                while (channel.size() > maxSize) {
                    channel.poll();
                }
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        rwLock.readLock().lock();
        try {
            channel.add(item);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void init() throws Exception {
        channel = new ConcurrentLinkedQueue<>();
    }

    @ConfigArgSet(required = false, defaultValue = "100000",desc = "The max size of the queue. When this size is achieved, items from the beginning of the queue get discarded.")
    public void setMaxSize(String maxSize) {
        this.maxSize = Integer.parseInt(maxSize);
    }

    @ConfigArgGet
    public int getMaxSize() {
        return this.maxSize;
    }
}
