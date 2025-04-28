package com.vehicle.network;

import java.io.Serializable;
import java.util.List;

/**
 * Класс для передачи ответов от сервера клиенту
 */
public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String message;
    private final List<Serializable> data;
    private final Exception exception;


    public Response(boolean success, String message,
                    List<Serializable> data, Exception exception) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.exception = exception;
    }


    public Response(boolean success, String message) {
        this(success, message, null, null);
    }

    public Response(boolean success, String message, List<Serializable> data) {
        this(success, message, data, null);
    }

    public Response(Exception exception) {
        this(false, "Server error: " + exception.getMessage(), null, exception);
    }


    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<Serializable> getData() {
        return data;
    }

    public Exception getException() {
        return exception;
    }


    public boolean hasData() {
        return data != null && !data.isEmpty();
    }

    @Override
    public String toString() {
        return "Response{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", dataSize=" + (data != null ? data.size() : 0) +
                ", exception=" + (exception != null ? exception.getClass().getSimpleName() : "null") +
                '}';
    }


    public static Response success(String message) {
        return new Response(true, message);
    }

    public static Response success(String message, List<Serializable> data) {
        return new Response(true, message, data);
    }

    public static Response error(String message) {
        return new Response(false, message);
    }

    public static Response serverError(Exception e) {
        return new Response(e);
    }
}