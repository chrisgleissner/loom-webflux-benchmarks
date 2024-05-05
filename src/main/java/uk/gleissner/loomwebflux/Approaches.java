package uk.gleissner.loomwebflux;

public class Approaches {

    private Approaches() {
        throw new UnsupportedOperationException();
    }

    public static final String PLATFORM_TOMCAT = "platform-tomcat";
    public static final String LOOM_TOMCAT = "loom-tomcat";
    public static final String LOOM_NETTY = "loom-netty";
    public static final String WEBFLUX_NETTY = "webflux-netty";
}
