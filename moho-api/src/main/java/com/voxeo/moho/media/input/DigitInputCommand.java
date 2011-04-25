/**
 * Copyright 2010 Voxeo Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho.media.input;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

public class DigitInputCommand extends InputCommand {

    private static final URI DIGIT_GRAMMAR = URI.create("digits:" + encode("1,2,3,4,5,6,7,8,9,0,*,#"));

    public DigitInputCommand(char digit) {
        super(new Grammar(URI.create("digits:" + encode(Character.toString(digit)))));
    }

    public DigitInputCommand() {
        super(new Grammar(DIGIT_GRAMMAR));
    }

    private static String encode(String url) {
        try {
            return URLEncoder.encode(url, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

}
