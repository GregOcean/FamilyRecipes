package com.familyrecipes.util;

import jakarta.servlet.http.HttpServletRequest;

/** Only the local Nginx reverse proxy may supply the client address used for rate limits. */
public final class ClientAddress {
    private ClientAddress() {}
    public static String of(HttpServletRequest request) {
        String remote=request.getRemoteAddr();
        if (!java.util.Set.of("127.0.0.1","::1","0:0:0:0:0:0:0:1").contains(remote)) return remote;
        String forwarded=request.getHeader("X-Real-IP");
        if(forwarded==null || !forwarded.matches("[0-9a-fA-F:.]{3,45}"))return remote;
        try {return java.net.InetAddress.getByName(forwarded).getHostAddress();}
        catch(java.net.UnknownHostException e){return remote;}
    }
}
