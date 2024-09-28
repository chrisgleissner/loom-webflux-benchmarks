package uk.gleissner.loomwebflux.config;

public class Profiles {
    public static final String NO_CACHE = "no-cache";
    public static final String POSTGRES = "postgres";

    public static final String REST_CLIENT_JDK = "rest-client-jdk";
    public static final String REST_CLIENT_APACHE5 = "rest-client-apache5";
    public static final String REST_CLIENT_REACTOR_NETTY = "rest-client-reactor-netty";

    public static final String ANY_REST_CLIENT_PROFILE_ACTIVE =
        REST_CLIENT_APACHE5 + " | "
            + REST_CLIENT_JDK + " | "
            + REST_CLIENT_REACTOR_NETTY;

    public static final String NO_REST_CLIENT_PROFILE_ACTIVE = "!(" + ANY_REST_CLIENT_PROFILE_ACTIVE + ")";
}
