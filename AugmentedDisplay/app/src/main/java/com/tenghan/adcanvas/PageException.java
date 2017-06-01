package com.tenghan.adcanvas;

/**
 * Created by hanteng on 2017-06-01.
 */

public class PageException extends Exception{
    public PageException(){
        super();
    }

    public PageException(String message){
        super(message);
    }

    public PageException(String message, Throwable cause){
        super(message, cause);
    }

    public PageException(Throwable cause)
    {
        super(cause);
    }
}
