package com.sharethrough.sdk;

import java.util.Date;

class DateProvider implements Provider<Date> {
    @Override
    public Date get() {
        return new Date();
    }
}
