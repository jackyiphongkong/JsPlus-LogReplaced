package org.apache.log4j.spi;

import java.io.Serializable;

/**
 * Log4j 1.2 {@code LocationInfo} - signature-compatible replacement.
 *
 * <p>Location capture is not performed by the replacement layer: the engine
 * does not record call sites, so every instance reports {@link #NA}.</p>
 */
public class LocationInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String NA = "?";

    public static final LocationInfo NA_LOCATION_INFO =
            new LocationInfo(NA, NA, NA, NA);

    transient String lineNumber;
    transient String fileName;
    transient String className;
    transient String methodName;
    public String fullInfo;

    public LocationInfo(Throwable t, String fqnOfCallingClass) {
        this(NA, NA, NA, NA);
    }

    public LocationInfo(String file, String classname, String method, String line) {
        this.fileName = file;
        this.className = classname;
        this.methodName = method;
        this.lineNumber = line;
        this.fullInfo = classname + "." + method + "(" + file + ":" + line + ")";
    }

    public String getClassName() {
        return className;
    }

    public String getFileName() {
        return fileName;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public String getMethodName() {
        return methodName;
    }
}
