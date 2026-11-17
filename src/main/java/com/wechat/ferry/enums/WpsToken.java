package com.wechat.ferry.enums;

import org.springframework.stereotype.Component;

@Component
public class WpsToken {

    public String getWpsToken() {
        //    public static void main(String[] args) {
        //        System.out.println(getWpsToken());
        //    }
        return getToken();
    }

    private String getToken() {
        return System.getenv("Wps_Token");
    }

}
