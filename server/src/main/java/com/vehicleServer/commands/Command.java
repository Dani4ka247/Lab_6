package com.vehicleServer.commands;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;

public interface Command {

    Response execute(Request request);

    String getDescription();
}