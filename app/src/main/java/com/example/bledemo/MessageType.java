package com.example.bledemo;

/**
 * Created by Administrator on 2017/8/24.
 */

public class MessageType {
    public static final String CONNECT="1001";
    public String type;
    public Object data;
    public int position;

    public MessageType(String type, Object data) {
        this.type = type;
        this.data = data;
    }
    public MessageType(String type, Object data,int position) {
        this.type = type;
        this.data = data;
        this.position=position;
    }
}
