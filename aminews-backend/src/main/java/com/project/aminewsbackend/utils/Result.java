package com.project.aminewsbackend.utils;

public class Result {
    private int code;
    private String message;
    private Object data;

    public Result(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static Result success(Object data) {
        return new Result(200, "Success", data);
    }

    public static Result success(Object data, String message) {
        return new Result(200, message, data);
    }

    public static Result success(String message) {
        return new Result(200, message, null);
    }

    public static Result error(int code, String message) {
        return new Result(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }
}
