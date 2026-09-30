package org.fakebelieve.tomcat;

import java.io.IOException;
import javax.servlet.ServletException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

/**
 * Preserve Request Values
 *
 * There are ways to configure Tomcat in which the server will modify request
 * values making the original values inaccessible later in the request, including
 * for logging. This valve will preserve the desired request values in request
 * attributes.
 *
 * In order for the valve to preserve the original values it needs to be configured
 * early in the server configuration. For example, Tomcat's RequestIpValve will change
 * many of the request values, so this valve should be placed before it.
 *
 * This valve can preserve
 * - any request header - for example "x-forwarded-for"
 * - the tomcat virtual host name - use "request-host-name"
 */
public class PreserveRequestValve extends ValveBase {

    public static final String REQUEST_HOST_NAME = "request-host-name";
    public static final String X_FORWARDED_FOR = "x-forwarded-for";

    private String prefix = "raw";
    private String names = REQUEST_HOST_NAME + "," + X_FORWARDED_FOR;

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setNames(String names) {
        this.names = names.toLowerCase();
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {

        for (String name : names.split(",")) {

            String rawValue = request.getHeader(name);
            if (rawValue != null) {
                // Store as a request attribute before RemoteIpValve strips it
                request.setAttribute(prefix + "-" + name, rawValue);
            } else if (name.equals(REQUEST_HOST_NAME)) {
                request.setAttribute(
                        prefix + "-" + REQUEST_HOST_NAME, request.getHost().getName());
            }
        }

        getNext().invoke(request, response);
    }
}
