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

import com.adobe.qe.toughday.api.annotations.feeders.FeederSet;
import com.adobe.qe.toughday.api.core.NamedObjectImpl;
import com.adobe.qe.toughday.api.feeders.InputFeeder;

import java.util.Base64;

public class Base64DecoderFeeder extends NamedObjectImpl implements InputFeeder<String> {
    private InputFeeder<String> input;

    @Override
    public String get(Object... keys) throws Exception {
        String current = input.get();
        if(current == null) {
            return null;
        }
        return new String(Base64.getDecoder().decode(input.get()));
    }

    @Override
    public void init() throws Exception {

    }

    @FeederSet(desc = "The Base64 Encoded feeder")
    public void setInput(InputFeeder<String> input) {
        this.input = input;
    }
}
