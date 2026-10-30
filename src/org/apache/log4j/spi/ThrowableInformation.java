package org.apache.log4j.spi;

import java.io.Serializable;

import org.apache.log4j.Category;

/**
 * Log4j 1.2 {@code ThrowableInformation} - signature-compatible replacement.
 *
 * <p>Holds the throwable and renders its stack trace on demand.</p>
 */
public class ThrowableInformation implements Serializable {

    private static final long serialVersionUID = 1L;

    private transient Throwable throwable;
    private String[] rep;

    public ThrowableInformation(Throwable throwable) {
        this.throwable = throwable;
    }

    public ThrowableInformation(Throwable throwable, Category category) {
        this.throwable = throwable;
    }

    public ThrowableInformation(String[] rep) {
        this.rep = rep;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public synchronized String[] getThrowableStrRep() {
        if (rep == null) {
            rep = toArray();
        }
        return rep;
    }

    private String[] toArray() {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.flush();
        return sw.toString().split("\\r?\\n");
    }
}
