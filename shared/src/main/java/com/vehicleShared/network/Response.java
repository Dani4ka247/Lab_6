package com.vehicleShared.network;

import java.io.Serializable;
import java.util.List;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;
    private final boolean success;
    private final String message;
    private final boolean requiresVehicle;
    private final List<Serializable> data;
    private final Exception exception;

    public Response(boolean success, String message, boolean requiresVehicle, List<Serializable> data, Exception exception) {
        this.success = success;
        this.message = message;
        this.requiresVehicle = requiresVehicle;
        this.data = data;
        this.exception = exception;
    }

    public Response(boolean success, String message, boolean requiresVehicle) {
        this(success, message, requiresVehicle, null, null);
    }

    public Response(boolean success, String message) {
        this(success, message, false);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public boolean requiresVehicle() {
        return requiresVehicle;
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
        return "Response{success=" + success + ", message='" + message + "', requiresVehicle=" + requiresVehicle + ", dataSize=" + (data != null ? data.size() : 0) + ", exception=" + (exception != null ? exception.getClass().getSimpleName() : "null") + '}';
    }

    public static Response success(String message) {
        return new Response(true, message, false);
    }

    public static Response success(String message, boolean requiresVehicle) {
        return new Response(true, message, requiresVehicle);
    }

    public static Response success(String message, List<Serializable> data) {
        return new Response(true, message, false, data, null);
    }

    public static Response error(String message) {
        return new Response(false, message, false);
    }

    public static Response serverError(Exception e) {
        return new Response(false, "Server error: " + e.getMessage(), false, null, e);
    }
}