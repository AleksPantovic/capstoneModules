package com.asioso.firstspirit.translatormodule.controller;

import de.espirit.firstspirit.access.BaseContext;

public class BaseController {
    private final BaseContext baseContext ;

    public BaseController(BaseContext baseContext){
        this.baseContext = baseContext;
    }

    public BaseContext getBaseContext() {
        return this.baseContext;
    }
}
