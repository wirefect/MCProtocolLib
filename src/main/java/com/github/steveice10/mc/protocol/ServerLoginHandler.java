package com.github.steveice10.mc.protocol;

import com.github.steveice10.packetlib.Session;

public interface ServerLoginHandler {

    void loggedIn(Session session);

}
