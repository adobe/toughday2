package com.adobe.qe.toughday.internal.core;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Timestamp {
    public static final String START_TIME = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm").format(new Date());

    private Timestamp() {}
}
