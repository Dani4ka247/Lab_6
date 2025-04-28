package com.vehicle.commands;

import com.vehicle.network.Request;
import com.vehicle.network.Response;

public interface Command {

    Response execute(Request request);

    String getDescription();
}