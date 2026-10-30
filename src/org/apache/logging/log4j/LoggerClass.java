package org.apache.logging.log4j;

import org.apache.logging.log4j.message.EntryMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.Supplier;

/**
 * Log4j2 {@code Logger} implementation backed by the JsPlus engine.
 *
 * <p>Generated from the official javap signature dump by
 * {@code build/tools/genLog4j2.py}; edit the generator, not this file.
 * Providers ({@link Supplier}, {@link MessageSupplier}) are evaluated eagerly
 * (decision D6) so that no logging call is silently dropped.</p>
 */
public class LoggerClass extends org.slf4j.LoggerClass implements Logger {

    private static final EntryMessage NOOP_ENTRY = new SimpleEntryMessage();

    public LoggerClass(Class clazz) {
        super(clazz);
    }

    public LoggerClass(String clazzName) {
        super(clazzName);
    }

    /* ------------------------------------------------------------------
     * Engine plumbing.
     * ------------------------------------------------------------------ */

    private static String levelName(Level level) {
        return level == null ? "INFO" : level.name();
    }

    private static Exception asException(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        if (throwable instanceof Exception) {
            return (Exception) throwable;
        }
        return new RuntimeException(throwable);
    }

    private static String text(Object value) {
        return value == null ? "null" : value.toString();
    }

    private static String messageText(Message message) {
        return message == null ? "null" : message.getFormattedMessage();
    }

    private static String messageSupplierText(MessageSupplier supplier) {
        return supplier == null ? "null" : messageText(supplier.get());
    }

    private static String supplierText(Supplier<?> supplier) {
        return supplier == null ? "null" : text(supplier.get());
    }

    private static String format(String pattern, Object[] args) {
        if (pattern == null) {
            return "null";
        }
        if (args == null || args.length == 0) {
            return pattern;
        }
        StringBuilder sb = new StringBuilder(pattern.length() + 32);
        int argIndex = 0;
        int cursor = 0;
        while (cursor < pattern.length()) {
            int hit = pattern.indexOf("{}", cursor);
            if (hit < 0 || argIndex >= args.length) {
                sb.append(pattern.substring(cursor));
                break;
            }
            sb.append(pattern, cursor, hit);
            sb.append(text(args[argIndex++]));
            cursor = hit + 2;
        }
        return sb.toString();
    }

    private static String formatSuppliers(String pattern, Supplier<?>[] suppliers) {
        if (suppliers == null || suppliers.length == 0) {
            return pattern;
        }
        Object[] values = new Object[suppliers.length];
        for (int i = 0; i < suppliers.length; i++) {
            values[i] = suppliers[i] == null ? null : suppliers[i].get();
        }
        return format(pattern, values);
    }

    private static String stackText(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        throwable.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }

    /** Emit one event at the given level; the marker becomes a message prefix. */
    private void emit(String level, Marker marker, String message, Throwable throwable) {
        String msg = marker == null ? message : marker.getName() + " - " + message;
        com.killcoding.log.Logger engine = engine();
        Exception ex = asException(throwable);
        if ("FATAL".equals(level) || "ERROR".equals(level)) {
            if (ex != null) {
                engine.error(msg, ex);
            } else {
                engine.error(msg);
            }
        } else if ("WARN".equals(level)) {
            if (ex != null) {
                engine.warn(msg, ex);
            } else {
                engine.warn(msg);
            }
        } else if ("INFO".equals(level)) {
            if (ex != null) {
                engine.info(msg, ex);
            } else {
                engine.info(msg);
            }
        } else if ("DEBUG".equals(level)) {
            if (ex != null) {
                engine.debug(msg, ex);
            } else {
                engine.debug(msg);
            }
        } else {
            engine.mark(msg);
            if (throwable != null) {
                engine.mark(stackText(throwable));
            }
        }
    }

    private boolean isEnabledFor(String level) {
        if ("TRACE".equals(level)) {
            return isDebugEnabled();
        }
        if ("DEBUG".equals(level)) {
            return isDebugEnabled();
        }
        if ("INFO".equals(level)) {
            return isInfoEnabled();
        }
        if ("WARN".equals(level)) {
            return isWarnEnabled();
        }
        return isErrorEnabled();
    }


    @Override
    public void catching(org.apache.logging.log4j.Level p0, java.lang.Throwable p1) {
        emit(levelName(p0), null, "Catching", p1);
    }

    @Override
    public void catching(java.lang.Throwable p0) {
        emit("ERROR", null, "Catching", p0);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.message.Message p1) {
        emit("DEBUG", p0, messageText(p1), null);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.message.Message p1, java.lang.Throwable p2) {
        emit("DEBUG", p0, messageText(p1), p2);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.MessageSupplier p1) {
        emit("DEBUG", p0, messageSupplierText(p1), null);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.MessageSupplier p1, java.lang.Throwable p2) {
        emit("DEBUG", p0, messageSupplierText(p1), p2);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, java.lang.CharSequence p1) {
        emit("DEBUG", p0, text(p1), null);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, java.lang.CharSequence p1, java.lang.Throwable p2) {
        emit("DEBUG", p0, text(p1), p2);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, java.lang.Object p1) {
        emit("DEBUG", p0, text(p1), null);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, java.lang.Object p1, java.lang.Throwable p2) {
        emit("DEBUG", p0, text(p1), p2);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, java.lang.String p1) {
        emit("DEBUG", p0, text(p1), null);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object... p2) {
        emit("DEBUG", p0, format(p1, p2), null);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, java.lang.String p1, org.apache.logging.log4j.util.Supplier<?>... p2) {
        emit("DEBUG", p0, formatSuppliers(p1, p2), null);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Throwable p2) {
        emit("DEBUG", p0, text(p1), p2);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.Supplier<?> p1) {
        emit("DEBUG", p0, supplierText(p1), null);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.Supplier<?> p1, java.lang.Throwable p2) {
        emit("DEBUG", p0, supplierText(p1), p2);
    }

    @Override
    public void debug(org.apache.logging.log4j.message.Message p0) {
        emit("DEBUG", null, messageText(p0), null);
    }

    @Override
    public void debug(org.apache.logging.log4j.message.Message p0, java.lang.Throwable p1) {
        emit("DEBUG", null, messageText(p0), p1);
    }

    @Override
    public void debug(org.apache.logging.log4j.util.MessageSupplier p0) {
        emit("DEBUG", null, messageSupplierText(p0), null);
    }

    @Override
    public void debug(org.apache.logging.log4j.util.MessageSupplier p0, java.lang.Throwable p1) {
        emit("DEBUG", null, messageSupplierText(p0), p1);
    }

    @Override
    public void debug(java.lang.CharSequence p0) {
        emit("DEBUG", null, text(p0), null);
    }

    @Override
    public void debug(java.lang.CharSequence p0, java.lang.Throwable p1) {
        emit("DEBUG", null, text(p0), p1);
    }

    @Override
    public void debug(java.lang.Object p0) {
        emit("DEBUG", null, text(p0), null);
    }

    @Override
    public void debug(java.lang.Object p0, java.lang.Throwable p1) {
        emit("DEBUG", null, text(p0), p1);
    }

    @Override
    public void debug(java.lang.String p0) {
        emit("DEBUG", null, text(p0), null);
    }

    @Override
    public void debug(java.lang.String p0, java.lang.Object... p1) {
        emit("DEBUG", null, format(p0, p1), null);
    }

    @Override
    public void debug(java.lang.String p0, org.apache.logging.log4j.util.Supplier<?>... p1) {
        emit("DEBUG", null, formatSuppliers(p0, p1), null);
    }

    @Override
    public void debug(java.lang.String p0, java.lang.Throwable p1) {
        emit("DEBUG", null, text(p0), p1);
    }

    @Override
    public void debug(org.apache.logging.log4j.util.Supplier<?> p0) {
        emit("DEBUG", null, supplierText(p0), null);
    }

    @Override
    public void debug(org.apache.logging.log4j.util.Supplier<?> p0, java.lang.Throwable p1) {
        emit("DEBUG", null, supplierText(p0), p1);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2) {
        emit("DEBUG", p0, format(p1, new Object[]{p2}), null);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3) {
        emit("DEBUG", p0, format(p1, new Object[]{p2, p3}), null);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4) {
        emit("DEBUG", p0, format(p1, new Object[]{p2, p3, p4}), null);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5) {
        emit("DEBUG", p0, format(p1, new Object[]{p2, p3, p4, p5}), null);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6) {
        emit("DEBUG", p0, format(p1, new Object[]{p2, p3, p4, p5, p6}), null);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7) {
        emit("DEBUG", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7}), null);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8) {
        emit("DEBUG", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8}), null);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9) {
        emit("DEBUG", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9}), null);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10) {
        emit("DEBUG", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9, p10}), null);
    }

    @Override
    public void debug(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10, java.lang.Object p11) {
        emit("DEBUG", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9, p10, p11}), null);
    }

    @Override
    public void debug(java.lang.String p0, java.lang.Object p1) {
        emit("DEBUG", null, format(p0, new Object[]{p1}), null);
    }

    @Override
    public void debug(java.lang.String p0, java.lang.Object p1, java.lang.Object p2) {
        emit("DEBUG", null, format(p0, new Object[]{p1, p2}), null);
    }

    @Override
    public void debug(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3) {
        emit("DEBUG", null, format(p0, new Object[]{p1, p2, p3}), null);
    }

    @Override
    public void debug(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4) {
        emit("DEBUG", null, format(p0, new Object[]{p1, p2, p3, p4}), null);
    }

    @Override
    public void debug(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5) {
        emit("DEBUG", null, format(p0, new Object[]{p1, p2, p3, p4, p5}), null);
    }

    @Override
    public void debug(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6) {
        emit("DEBUG", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6}), null);
    }

    @Override
    public void debug(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7) {
        emit("DEBUG", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7}), null);
    }

    @Override
    public void debug(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8) {
        emit("DEBUG", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7, p8}), null);
    }

    @Override
    public void debug(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9) {
        emit("DEBUG", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7, p8, p9}), null);
    }

    @Override
    public void debug(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10) {
        emit("DEBUG", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7, p8, p9, p10}), null);
    }

    @Override
    public void entry() {
        emit("TRACE", null, "Entering", null);
    }

    @Override
    public void entry(java.lang.Object... p0) {
        emit("TRACE", null, "Entering", null);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.message.Message p1) {
        emit("ERROR", p0, messageText(p1), null);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.message.Message p1, java.lang.Throwable p2) {
        emit("ERROR", p0, messageText(p1), p2);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.MessageSupplier p1) {
        emit("ERROR", p0, messageSupplierText(p1), null);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.MessageSupplier p1, java.lang.Throwable p2) {
        emit("ERROR", p0, messageSupplierText(p1), p2);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, java.lang.CharSequence p1) {
        emit("ERROR", p0, text(p1), null);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, java.lang.CharSequence p1, java.lang.Throwable p2) {
        emit("ERROR", p0, text(p1), p2);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, java.lang.Object p1) {
        emit("ERROR", p0, text(p1), null);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, java.lang.Object p1, java.lang.Throwable p2) {
        emit("ERROR", p0, text(p1), p2);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, java.lang.String p1) {
        emit("ERROR", p0, text(p1), null);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object... p2) {
        emit("ERROR", p0, format(p1, p2), null);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, java.lang.String p1, org.apache.logging.log4j.util.Supplier<?>... p2) {
        emit("ERROR", p0, formatSuppliers(p1, p2), null);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Throwable p2) {
        emit("ERROR", p0, text(p1), p2);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.Supplier<?> p1) {
        emit("ERROR", p0, supplierText(p1), null);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.Supplier<?> p1, java.lang.Throwable p2) {
        emit("ERROR", p0, supplierText(p1), p2);
    }

    @Override
    public void error(org.apache.logging.log4j.message.Message p0) {
        emit("ERROR", null, messageText(p0), null);
    }

    @Override
    public void error(org.apache.logging.log4j.message.Message p0, java.lang.Throwable p1) {
        emit("ERROR", null, messageText(p0), p1);
    }

    @Override
    public void error(org.apache.logging.log4j.util.MessageSupplier p0) {
        emit("ERROR", null, messageSupplierText(p0), null);
    }

    @Override
    public void error(org.apache.logging.log4j.util.MessageSupplier p0, java.lang.Throwable p1) {
        emit("ERROR", null, messageSupplierText(p0), p1);
    }

    @Override
    public void error(java.lang.CharSequence p0) {
        emit("ERROR", null, text(p0), null);
    }

    @Override
    public void error(java.lang.CharSequence p0, java.lang.Throwable p1) {
        emit("ERROR", null, text(p0), p1);
    }

    @Override
    public void error(java.lang.Object p0) {
        emit("ERROR", null, text(p0), null);
    }

    @Override
    public void error(java.lang.Object p0, java.lang.Throwable p1) {
        emit("ERROR", null, text(p0), p1);
    }

    @Override
    public void error(java.lang.String p0) {
        emit("ERROR", null, text(p0), null);
    }

    @Override
    public void error(java.lang.String p0, java.lang.Object... p1) {
        emit("ERROR", null, format(p0, p1), null);
    }

    @Override
    public void error(java.lang.String p0, org.apache.logging.log4j.util.Supplier<?>... p1) {
        emit("ERROR", null, formatSuppliers(p0, p1), null);
    }

    @Override
    public void error(java.lang.String p0, java.lang.Throwable p1) {
        emit("ERROR", null, text(p0), p1);
    }

    @Override
    public void error(org.apache.logging.log4j.util.Supplier<?> p0) {
        emit("ERROR", null, supplierText(p0), null);
    }

    @Override
    public void error(org.apache.logging.log4j.util.Supplier<?> p0, java.lang.Throwable p1) {
        emit("ERROR", null, supplierText(p0), p1);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2) {
        emit("ERROR", p0, format(p1, new Object[]{p2}), null);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3) {
        emit("ERROR", p0, format(p1, new Object[]{p2, p3}), null);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4) {
        emit("ERROR", p0, format(p1, new Object[]{p2, p3, p4}), null);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5) {
        emit("ERROR", p0, format(p1, new Object[]{p2, p3, p4, p5}), null);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6) {
        emit("ERROR", p0, format(p1, new Object[]{p2, p3, p4, p5, p6}), null);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7) {
        emit("ERROR", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7}), null);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8) {
        emit("ERROR", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8}), null);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9) {
        emit("ERROR", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9}), null);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10) {
        emit("ERROR", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9, p10}), null);
    }

    @Override
    public void error(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10, java.lang.Object p11) {
        emit("ERROR", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9, p10, p11}), null);
    }

    @Override
    public void error(java.lang.String p0, java.lang.Object p1) {
        emit("ERROR", null, format(p0, new Object[]{p1}), null);
    }

    @Override
    public void error(java.lang.String p0, java.lang.Object p1, java.lang.Object p2) {
        emit("ERROR", null, format(p0, new Object[]{p1, p2}), null);
    }

    @Override
    public void error(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3) {
        emit("ERROR", null, format(p0, new Object[]{p1, p2, p3}), null);
    }

    @Override
    public void error(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4) {
        emit("ERROR", null, format(p0, new Object[]{p1, p2, p3, p4}), null);
    }

    @Override
    public void error(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5) {
        emit("ERROR", null, format(p0, new Object[]{p1, p2, p3, p4, p5}), null);
    }

    @Override
    public void error(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6) {
        emit("ERROR", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6}), null);
    }

    @Override
    public void error(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7) {
        emit("ERROR", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7}), null);
    }

    @Override
    public void error(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8) {
        emit("ERROR", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7, p8}), null);
    }

    @Override
    public void error(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9) {
        emit("ERROR", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7, p8, p9}), null);
    }

    @Override
    public void error(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10) {
        emit("ERROR", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7, p8, p9, p10}), null);
    }

    @Override
    public void exit() {
        emit("TRACE", null, "Exiting", null);
    }

    @Override
    public <R> R exit(R p0) {
        emit("TRACE", null, "Exiting", null);
        return p0;
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.message.Message p1) {
        emit("FATAL", p0, messageText(p1), null);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.message.Message p1, java.lang.Throwable p2) {
        emit("FATAL", p0, messageText(p1), p2);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.MessageSupplier p1) {
        emit("FATAL", p0, messageSupplierText(p1), null);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.MessageSupplier p1, java.lang.Throwable p2) {
        emit("FATAL", p0, messageSupplierText(p1), p2);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, java.lang.CharSequence p1) {
        emit("FATAL", p0, text(p1), null);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, java.lang.CharSequence p1, java.lang.Throwable p2) {
        emit("FATAL", p0, text(p1), p2);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, java.lang.Object p1) {
        emit("FATAL", p0, text(p1), null);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, java.lang.Object p1, java.lang.Throwable p2) {
        emit("FATAL", p0, text(p1), p2);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, java.lang.String p1) {
        emit("FATAL", p0, text(p1), null);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object... p2) {
        emit("FATAL", p0, format(p1, p2), null);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, java.lang.String p1, org.apache.logging.log4j.util.Supplier<?>... p2) {
        emit("FATAL", p0, formatSuppliers(p1, p2), null);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Throwable p2) {
        emit("FATAL", p0, text(p1), p2);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.Supplier<?> p1) {
        emit("FATAL", p0, supplierText(p1), null);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.Supplier<?> p1, java.lang.Throwable p2) {
        emit("FATAL", p0, supplierText(p1), p2);
    }

    @Override
    public void fatal(org.apache.logging.log4j.message.Message p0) {
        emit("FATAL", null, messageText(p0), null);
    }

    @Override
    public void fatal(org.apache.logging.log4j.message.Message p0, java.lang.Throwable p1) {
        emit("FATAL", null, messageText(p0), p1);
    }

    @Override
    public void fatal(org.apache.logging.log4j.util.MessageSupplier p0) {
        emit("FATAL", null, messageSupplierText(p0), null);
    }

    @Override
    public void fatal(org.apache.logging.log4j.util.MessageSupplier p0, java.lang.Throwable p1) {
        emit("FATAL", null, messageSupplierText(p0), p1);
    }

    @Override
    public void fatal(java.lang.CharSequence p0) {
        emit("FATAL", null, text(p0), null);
    }

    @Override
    public void fatal(java.lang.CharSequence p0, java.lang.Throwable p1) {
        emit("FATAL", null, text(p0), p1);
    }

    @Override
    public void fatal(java.lang.Object p0) {
        emit("FATAL", null, text(p0), null);
    }

    @Override
    public void fatal(java.lang.Object p0, java.lang.Throwable p1) {
        emit("FATAL", null, text(p0), p1);
    }

    @Override
    public void fatal(java.lang.String p0) {
        emit("FATAL", null, text(p0), null);
    }

    @Override
    public void fatal(java.lang.String p0, java.lang.Object... p1) {
        emit("FATAL", null, format(p0, p1), null);
    }

    @Override
    public void fatal(java.lang.String p0, org.apache.logging.log4j.util.Supplier<?>... p1) {
        emit("FATAL", null, formatSuppliers(p0, p1), null);
    }

    @Override
    public void fatal(java.lang.String p0, java.lang.Throwable p1) {
        emit("FATAL", null, text(p0), p1);
    }

    @Override
    public void fatal(org.apache.logging.log4j.util.Supplier<?> p0) {
        emit("FATAL", null, supplierText(p0), null);
    }

    @Override
    public void fatal(org.apache.logging.log4j.util.Supplier<?> p0, java.lang.Throwable p1) {
        emit("FATAL", null, supplierText(p0), p1);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2) {
        emit("FATAL", p0, format(p1, new Object[]{p2}), null);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3) {
        emit("FATAL", p0, format(p1, new Object[]{p2, p3}), null);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4) {
        emit("FATAL", p0, format(p1, new Object[]{p2, p3, p4}), null);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5) {
        emit("FATAL", p0, format(p1, new Object[]{p2, p3, p4, p5}), null);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6) {
        emit("FATAL", p0, format(p1, new Object[]{p2, p3, p4, p5, p6}), null);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7) {
        emit("FATAL", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7}), null);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8) {
        emit("FATAL", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8}), null);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9) {
        emit("FATAL", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9}), null);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10) {
        emit("FATAL", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9, p10}), null);
    }

    @Override
    public void fatal(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10, java.lang.Object p11) {
        emit("FATAL", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9, p10, p11}), null);
    }

    @Override
    public void fatal(java.lang.String p0, java.lang.Object p1) {
        emit("FATAL", null, format(p0, new Object[]{p1}), null);
    }

    @Override
    public void fatal(java.lang.String p0, java.lang.Object p1, java.lang.Object p2) {
        emit("FATAL", null, format(p0, new Object[]{p1, p2}), null);
    }

    @Override
    public void fatal(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3) {
        emit("FATAL", null, format(p0, new Object[]{p1, p2, p3}), null);
    }

    @Override
    public void fatal(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4) {
        emit("FATAL", null, format(p0, new Object[]{p1, p2, p3, p4}), null);
    }

    @Override
    public void fatal(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5) {
        emit("FATAL", null, format(p0, new Object[]{p1, p2, p3, p4, p5}), null);
    }

    @Override
    public void fatal(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6) {
        emit("FATAL", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6}), null);
    }

    @Override
    public void fatal(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7) {
        emit("FATAL", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7}), null);
    }

    @Override
    public void fatal(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8) {
        emit("FATAL", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7, p8}), null);
    }

    @Override
    public void fatal(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9) {
        emit("FATAL", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7, p8, p9}), null);
    }

    @Override
    public void fatal(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10) {
        emit("FATAL", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7, p8, p9, p10}), null);
    }

    @Override
    public org.apache.logging.log4j.Level getLevel() {
        return Level.INFO;
    }

    @Override
    public <MF extends org.apache.logging.log4j.message.MessageFactory> MF getMessageFactory() {
        return null;
    }

    @Override
    public org.apache.logging.log4j.message.FlowMessageFactory getFlowMessageFactory() {
        return null;
    }

    @Override
    public java.lang.String getName() {
        return super.getName();
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.message.Message p1) {
        emit("INFO", p0, messageText(p1), null);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.message.Message p1, java.lang.Throwable p2) {
        emit("INFO", p0, messageText(p1), p2);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.MessageSupplier p1) {
        emit("INFO", p0, messageSupplierText(p1), null);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.MessageSupplier p1, java.lang.Throwable p2) {
        emit("INFO", p0, messageSupplierText(p1), p2);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, java.lang.CharSequence p1) {
        emit("INFO", p0, text(p1), null);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, java.lang.CharSequence p1, java.lang.Throwable p2) {
        emit("INFO", p0, text(p1), p2);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, java.lang.Object p1) {
        emit("INFO", p0, text(p1), null);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, java.lang.Object p1, java.lang.Throwable p2) {
        emit("INFO", p0, text(p1), p2);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, java.lang.String p1) {
        emit("INFO", p0, text(p1), null);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object... p2) {
        emit("INFO", p0, format(p1, p2), null);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, java.lang.String p1, org.apache.logging.log4j.util.Supplier<?>... p2) {
        emit("INFO", p0, formatSuppliers(p1, p2), null);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Throwable p2) {
        emit("INFO", p0, text(p1), p2);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.Supplier<?> p1) {
        emit("INFO", p0, supplierText(p1), null);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.Supplier<?> p1, java.lang.Throwable p2) {
        emit("INFO", p0, supplierText(p1), p2);
    }

    @Override
    public void info(org.apache.logging.log4j.message.Message p0) {
        emit("INFO", null, messageText(p0), null);
    }

    @Override
    public void info(org.apache.logging.log4j.message.Message p0, java.lang.Throwable p1) {
        emit("INFO", null, messageText(p0), p1);
    }

    @Override
    public void info(org.apache.logging.log4j.util.MessageSupplier p0) {
        emit("INFO", null, messageSupplierText(p0), null);
    }

    @Override
    public void info(org.apache.logging.log4j.util.MessageSupplier p0, java.lang.Throwable p1) {
        emit("INFO", null, messageSupplierText(p0), p1);
    }

    @Override
    public void info(java.lang.CharSequence p0) {
        emit("INFO", null, text(p0), null);
    }

    @Override
    public void info(java.lang.CharSequence p0, java.lang.Throwable p1) {
        emit("INFO", null, text(p0), p1);
    }

    @Override
    public void info(java.lang.Object p0) {
        emit("INFO", null, text(p0), null);
    }

    @Override
    public void info(java.lang.Object p0, java.lang.Throwable p1) {
        emit("INFO", null, text(p0), p1);
    }

    @Override
    public void info(java.lang.String p0) {
        emit("INFO", null, text(p0), null);
    }

    @Override
    public void info(java.lang.String p0, java.lang.Object... p1) {
        emit("INFO", null, format(p0, p1), null);
    }

    @Override
    public void info(java.lang.String p0, org.apache.logging.log4j.util.Supplier<?>... p1) {
        emit("INFO", null, formatSuppliers(p0, p1), null);
    }

    @Override
    public void info(java.lang.String p0, java.lang.Throwable p1) {
        emit("INFO", null, text(p0), p1);
    }

    @Override
    public void info(org.apache.logging.log4j.util.Supplier<?> p0) {
        emit("INFO", null, supplierText(p0), null);
    }

    @Override
    public void info(org.apache.logging.log4j.util.Supplier<?> p0, java.lang.Throwable p1) {
        emit("INFO", null, supplierText(p0), p1);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2) {
        emit("INFO", p0, format(p1, new Object[]{p2}), null);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3) {
        emit("INFO", p0, format(p1, new Object[]{p2, p3}), null);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4) {
        emit("INFO", p0, format(p1, new Object[]{p2, p3, p4}), null);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5) {
        emit("INFO", p0, format(p1, new Object[]{p2, p3, p4, p5}), null);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6) {
        emit("INFO", p0, format(p1, new Object[]{p2, p3, p4, p5, p6}), null);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7) {
        emit("INFO", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7}), null);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8) {
        emit("INFO", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8}), null);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9) {
        emit("INFO", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9}), null);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10) {
        emit("INFO", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9, p10}), null);
    }

    @Override
    public void info(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10, java.lang.Object p11) {
        emit("INFO", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9, p10, p11}), null);
    }

    @Override
    public void info(java.lang.String p0, java.lang.Object p1) {
        emit("INFO", null, format(p0, new Object[]{p1}), null);
    }

    @Override
    public void info(java.lang.String p0, java.lang.Object p1, java.lang.Object p2) {
        emit("INFO", null, format(p0, new Object[]{p1, p2}), null);
    }

    @Override
    public void info(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3) {
        emit("INFO", null, format(p0, new Object[]{p1, p2, p3}), null);
    }

    @Override
    public void info(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4) {
        emit("INFO", null, format(p0, new Object[]{p1, p2, p3, p4}), null);
    }

    @Override
    public void info(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5) {
        emit("INFO", null, format(p0, new Object[]{p1, p2, p3, p4, p5}), null);
    }

    @Override
    public void info(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6) {
        emit("INFO", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6}), null);
    }

    @Override
    public void info(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7) {
        emit("INFO", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7}), null);
    }

    @Override
    public void info(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8) {
        emit("INFO", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7, p8}), null);
    }

    @Override
    public void info(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9) {
        emit("INFO", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7, p8, p9}), null);
    }

    @Override
    public void info(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10) {
        emit("INFO", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7, p8, p9, p10}), null);
    }

    @Override
    public boolean isDebugEnabled() {
        return super.isDebugEnabled();
    }

    @Override
    public boolean isDebugEnabled(org.apache.logging.log4j.Marker p0) {
        return super.isDebugEnabled();
    }

    @Override
    public boolean isEnabled(org.apache.logging.log4j.Level p0) {
        return isEnabledFor(levelName(p0));
    }

    @Override
    public boolean isEnabled(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1) {
        return isEnabledFor(levelName(p0));
    }

    @Override
    public boolean isErrorEnabled() {
        return super.isErrorEnabled();
    }

    @Override
    public boolean isErrorEnabled(org.apache.logging.log4j.Marker p0) {
        return super.isErrorEnabled();
    }

    @Override
    public boolean isFatalEnabled() {
        return super.isErrorEnabled();
    }

    @Override
    public boolean isFatalEnabled(org.apache.logging.log4j.Marker p0) {
        return super.isErrorEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return super.isInfoEnabled();
    }

    @Override
    public boolean isInfoEnabled(org.apache.logging.log4j.Marker p0) {
        return super.isInfoEnabled();
    }

    @Override
    public boolean isTraceEnabled() {
        return super.isTraceEnabled();
    }

    @Override
    public boolean isTraceEnabled(org.apache.logging.log4j.Marker p0) {
        return super.isTraceEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return super.isWarnEnabled();
    }

    @Override
    public boolean isWarnEnabled(org.apache.logging.log4j.Marker p0) {
        return super.isWarnEnabled();
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, org.apache.logging.log4j.message.Message p2) {
        emit(levelName(p0), p1, messageText(p2), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, org.apache.logging.log4j.message.Message p2, java.lang.Throwable p3) {
        emit(levelName(p0), p1, messageText(p2), p3);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, org.apache.logging.log4j.util.MessageSupplier p2) {
        emit(levelName(p0), p1, messageSupplierText(p2), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, org.apache.logging.log4j.util.MessageSupplier p2, java.lang.Throwable p3) {
        emit(levelName(p0), p1, messageSupplierText(p2), p3);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, java.lang.CharSequence p2) {
        emit(levelName(p0), p1, text(p2), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, java.lang.CharSequence p2, java.lang.Throwable p3) {
        emit(levelName(p0), p1, text(p2), p3);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, java.lang.Object p2) {
        emit(levelName(p0), p1, text(p2), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, java.lang.Object p2, java.lang.Throwable p3) {
        emit(levelName(p0), p1, text(p2), p3);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, java.lang.String p2) {
        emit(levelName(p0), p1, text(p2), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, java.lang.String p2, java.lang.Object... p3) {
        emit(levelName(p0), p1, format(p2, p3), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, java.lang.String p2, org.apache.logging.log4j.util.Supplier<?>... p3) {
        emit(levelName(p0), p1, formatSuppliers(p2, p3), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, java.lang.String p2, java.lang.Throwable p3) {
        emit(levelName(p0), p1, text(p2), p3);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, org.apache.logging.log4j.util.Supplier<?> p2) {
        emit(levelName(p0), p1, supplierText(p2), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, org.apache.logging.log4j.util.Supplier<?> p2, java.lang.Throwable p3) {
        emit(levelName(p0), p1, supplierText(p2), p3);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.message.Message p1) {
        emit(levelName(p0), null, messageText(p1), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.message.Message p1, java.lang.Throwable p2) {
        emit(levelName(p0), null, messageText(p1), p2);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.util.MessageSupplier p1) {
        emit(levelName(p0), null, messageSupplierText(p1), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.util.MessageSupplier p1, java.lang.Throwable p2) {
        emit(levelName(p0), null, messageSupplierText(p1), p2);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, java.lang.CharSequence p1) {
        emit(levelName(p0), null, text(p1), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, java.lang.CharSequence p1, java.lang.Throwable p2) {
        emit(levelName(p0), null, text(p1), p2);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, java.lang.Object p1) {
        emit(levelName(p0), null, text(p1), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, java.lang.Object p1, java.lang.Throwable p2) {
        emit(levelName(p0), null, text(p1), p2);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, java.lang.String p1) {
        emit(levelName(p0), null, text(p1), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, java.lang.String p1, java.lang.Object... p2) {
        emit(levelName(p0), null, format(p1, p2), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, java.lang.String p1, org.apache.logging.log4j.util.Supplier<?>... p2) {
        emit(levelName(p0), null, formatSuppliers(p1, p2), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, java.lang.String p1, java.lang.Throwable p2) {
        emit(levelName(p0), null, text(p1), p2);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.util.Supplier<?> p1) {
        emit(levelName(p0), null, supplierText(p1), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.util.Supplier<?> p1, java.lang.Throwable p2) {
        emit(levelName(p0), null, supplierText(p1), p2);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, java.lang.String p2, java.lang.Object p3) {
        emit(levelName(p0), p1, format(p2, new Object[]{p3}), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, java.lang.String p2, java.lang.Object p3, java.lang.Object p4) {
        emit(levelName(p0), p1, format(p2, new Object[]{p3, p4}), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, java.lang.String p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5) {
        emit(levelName(p0), p1, format(p2, new Object[]{p3, p4, p5}), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, java.lang.String p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6) {
        emit(levelName(p0), p1, format(p2, new Object[]{p3, p4, p5, p6}), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, java.lang.String p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7) {
        emit(levelName(p0), p1, format(p2, new Object[]{p3, p4, p5, p6, p7}), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, java.lang.String p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8) {
        emit(levelName(p0), p1, format(p2, new Object[]{p3, p4, p5, p6, p7, p8}), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, java.lang.String p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9) {
        emit(levelName(p0), p1, format(p2, new Object[]{p3, p4, p5, p6, p7, p8, p9}), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, java.lang.String p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10) {
        emit(levelName(p0), p1, format(p2, new Object[]{p3, p4, p5, p6, p7, p8, p9, p10}), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, java.lang.String p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10, java.lang.Object p11) {
        emit(levelName(p0), p1, format(p2, new Object[]{p3, p4, p5, p6, p7, p8, p9, p10, p11}), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, java.lang.String p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10, java.lang.Object p11, java.lang.Object p12) {
        emit(levelName(p0), p1, format(p2, new Object[]{p3, p4, p5, p6, p7, p8, p9, p10, p11, p12}), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, java.lang.String p1, java.lang.Object p2) {
        emit(levelName(p0), null, format(p1, new Object[]{p2}), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3) {
        emit(levelName(p0), null, format(p1, new Object[]{p2, p3}), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4) {
        emit(levelName(p0), null, format(p1, new Object[]{p2, p3, p4}), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5) {
        emit(levelName(p0), null, format(p1, new Object[]{p2, p3, p4, p5}), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6) {
        emit(levelName(p0), null, format(p1, new Object[]{p2, p3, p4, p5, p6}), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7) {
        emit(levelName(p0), null, format(p1, new Object[]{p2, p3, p4, p5, p6, p7}), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8) {
        emit(levelName(p0), null, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8}), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9) {
        emit(levelName(p0), null, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9}), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10) {
        emit(levelName(p0), null, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9, p10}), null);
    }

    @Override
    public void log(org.apache.logging.log4j.Level p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10, java.lang.Object p11) {
        emit(levelName(p0), null, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9, p10, p11}), null);
    }

    @Override
    public void printf(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, java.lang.String p2, java.lang.Object... p3) {
        emit(levelName(p0), p1, String.format(p2, p3), null);
    }

    @Override
    public void printf(org.apache.logging.log4j.Level p0, java.lang.String p1, java.lang.Object... p2) {
        emit(levelName(p0), null, String.format(p1, p2), null);
    }

    @Override
    public <T extends java.lang.Throwable> T throwing(org.apache.logging.log4j.Level p0, T p1) {
        emit(levelName(p0), null, "Throwing", p1);
        return p1;
    }

    @Override
    public <T extends java.lang.Throwable> T throwing(T p0) {
        emit("ERROR", null, "Throwing", p0);
        return p0;
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.message.Message p1) {
        emit("TRACE", p0, messageText(p1), null);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.message.Message p1, java.lang.Throwable p2) {
        emit("TRACE", p0, messageText(p1), p2);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.MessageSupplier p1) {
        emit("TRACE", p0, messageSupplierText(p1), null);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.MessageSupplier p1, java.lang.Throwable p2) {
        emit("TRACE", p0, messageSupplierText(p1), p2);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, java.lang.CharSequence p1) {
        emit("TRACE", p0, text(p1), null);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, java.lang.CharSequence p1, java.lang.Throwable p2) {
        emit("TRACE", p0, text(p1), p2);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, java.lang.Object p1) {
        emit("TRACE", p0, text(p1), null);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, java.lang.Object p1, java.lang.Throwable p2) {
        emit("TRACE", p0, text(p1), p2);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, java.lang.String p1) {
        emit("TRACE", p0, text(p1), null);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object... p2) {
        emit("TRACE", p0, format(p1, p2), null);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, java.lang.String p1, org.apache.logging.log4j.util.Supplier<?>... p2) {
        emit("TRACE", p0, formatSuppliers(p1, p2), null);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Throwable p2) {
        emit("TRACE", p0, text(p1), p2);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.Supplier<?> p1) {
        emit("TRACE", p0, supplierText(p1), null);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.Supplier<?> p1, java.lang.Throwable p2) {
        emit("TRACE", p0, supplierText(p1), p2);
    }

    @Override
    public void trace(org.apache.logging.log4j.message.Message p0) {
        emit("TRACE", null, messageText(p0), null);
    }

    @Override
    public void trace(org.apache.logging.log4j.message.Message p0, java.lang.Throwable p1) {
        emit("TRACE", null, messageText(p0), p1);
    }

    @Override
    public void trace(org.apache.logging.log4j.util.MessageSupplier p0) {
        emit("TRACE", null, messageSupplierText(p0), null);
    }

    @Override
    public void trace(org.apache.logging.log4j.util.MessageSupplier p0, java.lang.Throwable p1) {
        emit("TRACE", null, messageSupplierText(p0), p1);
    }

    @Override
    public void trace(java.lang.CharSequence p0) {
        emit("TRACE", null, text(p0), null);
    }

    @Override
    public void trace(java.lang.CharSequence p0, java.lang.Throwable p1) {
        emit("TRACE", null, text(p0), p1);
    }

    @Override
    public void trace(java.lang.Object p0) {
        emit("TRACE", null, text(p0), null);
    }

    @Override
    public void trace(java.lang.Object p0, java.lang.Throwable p1) {
        emit("TRACE", null, text(p0), p1);
    }

    @Override
    public void trace(java.lang.String p0) {
        emit("TRACE", null, text(p0), null);
    }

    @Override
    public void trace(java.lang.String p0, java.lang.Object... p1) {
        emit("TRACE", null, format(p0, p1), null);
    }

    @Override
    public void trace(java.lang.String p0, org.apache.logging.log4j.util.Supplier<?>... p1) {
        emit("TRACE", null, formatSuppliers(p0, p1), null);
    }

    @Override
    public void trace(java.lang.String p0, java.lang.Throwable p1) {
        emit("TRACE", null, text(p0), p1);
    }

    @Override
    public void trace(org.apache.logging.log4j.util.Supplier<?> p0) {
        emit("TRACE", null, supplierText(p0), null);
    }

    @Override
    public void trace(org.apache.logging.log4j.util.Supplier<?> p0, java.lang.Throwable p1) {
        emit("TRACE", null, supplierText(p0), p1);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2) {
        emit("TRACE", p0, format(p1, new Object[]{p2}), null);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3) {
        emit("TRACE", p0, format(p1, new Object[]{p2, p3}), null);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4) {
        emit("TRACE", p0, format(p1, new Object[]{p2, p3, p4}), null);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5) {
        emit("TRACE", p0, format(p1, new Object[]{p2, p3, p4, p5}), null);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6) {
        emit("TRACE", p0, format(p1, new Object[]{p2, p3, p4, p5, p6}), null);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7) {
        emit("TRACE", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7}), null);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8) {
        emit("TRACE", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8}), null);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9) {
        emit("TRACE", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9}), null);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10) {
        emit("TRACE", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9, p10}), null);
    }

    @Override
    public void trace(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10, java.lang.Object p11) {
        emit("TRACE", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9, p10, p11}), null);
    }

    @Override
    public void trace(java.lang.String p0, java.lang.Object p1) {
        emit("TRACE", null, format(p0, new Object[]{p1}), null);
    }

    @Override
    public void trace(java.lang.String p0, java.lang.Object p1, java.lang.Object p2) {
        emit("TRACE", null, format(p0, new Object[]{p1, p2}), null);
    }

    @Override
    public void trace(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3) {
        emit("TRACE", null, format(p0, new Object[]{p1, p2, p3}), null);
    }

    @Override
    public void trace(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4) {
        emit("TRACE", null, format(p0, new Object[]{p1, p2, p3, p4}), null);
    }

    @Override
    public void trace(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5) {
        emit("TRACE", null, format(p0, new Object[]{p1, p2, p3, p4, p5}), null);
    }

    @Override
    public void trace(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6) {
        emit("TRACE", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6}), null);
    }

    @Override
    public void trace(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7) {
        emit("TRACE", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7}), null);
    }

    @Override
    public void trace(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8) {
        emit("TRACE", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7, p8}), null);
    }

    @Override
    public void trace(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9) {
        emit("TRACE", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7, p8, p9}), null);
    }

    @Override
    public void trace(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10) {
        emit("TRACE", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7, p8, p9, p10}), null);
    }

    @Override
    public org.apache.logging.log4j.message.EntryMessage traceEntry() {
        return NOOP_ENTRY;
    }

    @Override
    public org.apache.logging.log4j.message.EntryMessage traceEntry(java.lang.String p0, java.lang.Object... p1) {
        return NOOP_ENTRY;
    }

    @Override
    public org.apache.logging.log4j.message.EntryMessage traceEntry(org.apache.logging.log4j.util.Supplier<?>... p0) {
        return NOOP_ENTRY;
    }

    @Override
    public org.apache.logging.log4j.message.EntryMessage traceEntry(java.lang.String p0, org.apache.logging.log4j.util.Supplier<?>... p1) {
        return NOOP_ENTRY;
    }

    @Override
    public org.apache.logging.log4j.message.EntryMessage traceEntry(org.apache.logging.log4j.message.Message p0) {
        return NOOP_ENTRY;
    }

    @Override
    public void traceExit() {
        emit("TRACE", null, "Exiting", null);
    }

    @Override
    public <R> R traceExit(R p0) {
        emit("TRACE", null, "Exiting", null);
        return p0;
    }

    @Override
    public <R> R traceExit(java.lang.String p0, R p1) {
        emit("TRACE", null, text(p0), null);
        return p1;
    }

    @Override
    public void traceExit(org.apache.logging.log4j.message.EntryMessage p0) {
        emit("TRACE", null, "Exiting", null);
    }

    @Override
    public <R> R traceExit(org.apache.logging.log4j.message.EntryMessage p0, R p1) {
        emit("TRACE", null, "Exiting", null);
        return p1;
    }

    @Override
    public <R> R traceExit(org.apache.logging.log4j.message.Message p0, R p1) {
        emit("TRACE", null, "Exiting", null);
        return p1;
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.message.Message p1) {
        emit("WARN", p0, messageText(p1), null);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.message.Message p1, java.lang.Throwable p2) {
        emit("WARN", p0, messageText(p1), p2);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.MessageSupplier p1) {
        emit("WARN", p0, messageSupplierText(p1), null);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.MessageSupplier p1, java.lang.Throwable p2) {
        emit("WARN", p0, messageSupplierText(p1), p2);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, java.lang.CharSequence p1) {
        emit("WARN", p0, text(p1), null);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, java.lang.CharSequence p1, java.lang.Throwable p2) {
        emit("WARN", p0, text(p1), p2);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, java.lang.Object p1) {
        emit("WARN", p0, text(p1), null);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, java.lang.Object p1, java.lang.Throwable p2) {
        emit("WARN", p0, text(p1), p2);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, java.lang.String p1) {
        emit("WARN", p0, text(p1), null);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object... p2) {
        emit("WARN", p0, format(p1, p2), null);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, java.lang.String p1, org.apache.logging.log4j.util.Supplier<?>... p2) {
        emit("WARN", p0, formatSuppliers(p1, p2), null);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Throwable p2) {
        emit("WARN", p0, text(p1), p2);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.Supplier<?> p1) {
        emit("WARN", p0, supplierText(p1), null);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, org.apache.logging.log4j.util.Supplier<?> p1, java.lang.Throwable p2) {
        emit("WARN", p0, supplierText(p1), p2);
    }

    @Override
    public void warn(org.apache.logging.log4j.message.Message p0) {
        emit("WARN", null, messageText(p0), null);
    }

    @Override
    public void warn(org.apache.logging.log4j.message.Message p0, java.lang.Throwable p1) {
        emit("WARN", null, messageText(p0), p1);
    }

    @Override
    public void warn(org.apache.logging.log4j.util.MessageSupplier p0) {
        emit("WARN", null, messageSupplierText(p0), null);
    }

    @Override
    public void warn(org.apache.logging.log4j.util.MessageSupplier p0, java.lang.Throwable p1) {
        emit("WARN", null, messageSupplierText(p0), p1);
    }

    @Override
    public void warn(java.lang.CharSequence p0) {
        emit("WARN", null, text(p0), null);
    }

    @Override
    public void warn(java.lang.CharSequence p0, java.lang.Throwable p1) {
        emit("WARN", null, text(p0), p1);
    }

    @Override
    public void warn(java.lang.Object p0) {
        emit("WARN", null, text(p0), null);
    }

    @Override
    public void warn(java.lang.Object p0, java.lang.Throwable p1) {
        emit("WARN", null, text(p0), p1);
    }

    @Override
    public void warn(java.lang.String p0) {
        emit("WARN", null, text(p0), null);
    }

    @Override
    public void warn(java.lang.String p0, java.lang.Object... p1) {
        emit("WARN", null, format(p0, p1), null);
    }

    @Override
    public void warn(java.lang.String p0, org.apache.logging.log4j.util.Supplier<?>... p1) {
        emit("WARN", null, formatSuppliers(p0, p1), null);
    }

    @Override
    public void warn(java.lang.String p0, java.lang.Throwable p1) {
        emit("WARN", null, text(p0), p1);
    }

    @Override
    public void warn(org.apache.logging.log4j.util.Supplier<?> p0) {
        emit("WARN", null, supplierText(p0), null);
    }

    @Override
    public void warn(org.apache.logging.log4j.util.Supplier<?> p0, java.lang.Throwable p1) {
        emit("WARN", null, supplierText(p0), p1);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2) {
        emit("WARN", p0, format(p1, new Object[]{p2}), null);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3) {
        emit("WARN", p0, format(p1, new Object[]{p2, p3}), null);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4) {
        emit("WARN", p0, format(p1, new Object[]{p2, p3, p4}), null);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5) {
        emit("WARN", p0, format(p1, new Object[]{p2, p3, p4, p5}), null);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6) {
        emit("WARN", p0, format(p1, new Object[]{p2, p3, p4, p5, p6}), null);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7) {
        emit("WARN", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7}), null);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8) {
        emit("WARN", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8}), null);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9) {
        emit("WARN", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9}), null);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10) {
        emit("WARN", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9, p10}), null);
    }

    @Override
    public void warn(org.apache.logging.log4j.Marker p0, java.lang.String p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10, java.lang.Object p11) {
        emit("WARN", p0, format(p1, new Object[]{p2, p3, p4, p5, p6, p7, p8, p9, p10, p11}), null);
    }

    @Override
    public void warn(java.lang.String p0, java.lang.Object p1) {
        emit("WARN", null, format(p0, new Object[]{p1}), null);
    }

    @Override
    public void warn(java.lang.String p0, java.lang.Object p1, java.lang.Object p2) {
        emit("WARN", null, format(p0, new Object[]{p1, p2}), null);
    }

    @Override
    public void warn(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3) {
        emit("WARN", null, format(p0, new Object[]{p1, p2, p3}), null);
    }

    @Override
    public void warn(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4) {
        emit("WARN", null, format(p0, new Object[]{p1, p2, p3, p4}), null);
    }

    @Override
    public void warn(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5) {
        emit("WARN", null, format(p0, new Object[]{p1, p2, p3, p4, p5}), null);
    }

    @Override
    public void warn(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6) {
        emit("WARN", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6}), null);
    }

    @Override
    public void warn(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7) {
        emit("WARN", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7}), null);
    }

    @Override
    public void warn(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8) {
        emit("WARN", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7, p8}), null);
    }

    @Override
    public void warn(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9) {
        emit("WARN", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7, p8, p9}), null);
    }

    @Override
    public void warn(java.lang.String p0, java.lang.Object p1, java.lang.Object p2, java.lang.Object p3, java.lang.Object p4, java.lang.Object p5, java.lang.Object p6, java.lang.Object p7, java.lang.Object p8, java.lang.Object p9, java.lang.Object p10) {
        emit("WARN", null, format(p0, new Object[]{p1, p2, p3, p4, p5, p6, p7, p8, p9, p10}), null);
    }

    @Override
    public void logMessage(org.apache.logging.log4j.Level p0, org.apache.logging.log4j.Marker p1, java.lang.String p2, java.lang.StackTraceElement p3, org.apache.logging.log4j.message.Message p4, java.lang.Throwable p5) {
        emit(levelName(p0), p1, messageText(p4), p5);
    }

    @Override
    public org.apache.logging.log4j.LogBuilder atTrace() {
        return new SimpleLogBuilder("TRACE", null);
    }

    @Override
    public org.apache.logging.log4j.LogBuilder atDebug() {
        return new SimpleLogBuilder("DEBUG", null);
    }

    @Override
    public org.apache.logging.log4j.LogBuilder atInfo() {
        return new SimpleLogBuilder("INFO", null);
    }

    @Override
    public org.apache.logging.log4j.LogBuilder atWarn() {
        return new SimpleLogBuilder("WARN", null);
    }

    @Override
    public org.apache.logging.log4j.LogBuilder atError() {
        return new SimpleLogBuilder("ERROR", null);
    }

    @Override
    public org.apache.logging.log4j.LogBuilder atFatal() {
        return new SimpleLogBuilder("FATAL", null);
    }

    @Override
    public org.apache.logging.log4j.LogBuilder always() {
        return new SimpleLogBuilder("INFO", null);
    }

    @Override
    public org.apache.logging.log4j.LogBuilder atLevel(org.apache.logging.log4j.Level p0) {
        return new SimpleLogBuilder(levelName(p0), null);
    }

    /* ------------------------------------------------------------------
     * Fluent builder returned by atTrace()/atDebug()/.../atLevel().
     * ------------------------------------------------------------------ */

    private final class SimpleLogBuilder implements LogBuilder {

        private final String level;
        private Marker marker;
        private Throwable throwable;
        private String pendingMessage;
        private final java.util.List<Object> pendingArguments = new java.util.ArrayList<Object>();
        private final java.util.List<String[]> pendingKeyValues = new java.util.ArrayList<String[]>();
        private String slf4jMarkerPrefix = "";

        SimpleLogBuilder(String level, Marker marker) {
            this.level = level;
            this.marker = marker;
        }

        @Override
        public LogBuilder withMarker(Marker newMarker) {
            this.marker = newMarker;
            return this;
        }

        @Override
        public LogBuilder withThrowable(Throwable newThrowable) {
            this.throwable = newThrowable;
            return this;
        }

        @Override
        public LogBuilder setCause(Throwable cause) {
            this.throwable = cause;
            return this;
        }

        @Override
        public LogBuilder addMarker(org.slf4j.Marker slf4jMarker) {
            if (slf4jMarker != null) {
                slf4jMarkerPrefix += slf4jMarker.getName() + " - ";
            }
            return this;
        }

        @Override
        public LogBuilder addArgument(Object p) {
            this.pendingArguments.add(p);
            return this;
        }

        @Override
        public LogBuilder addArgument(java.util.function.Supplier<?> objectSupplier) {
            this.pendingArguments.add(objectSupplier == null ? null : objectSupplier.get());
            return this;
        }

        @Override
        public LogBuilder addKeyValue(String key, Object value) {
            this.pendingKeyValues.add(new String[] {key, text(value)});
            return this;
        }

        @Override
        public LogBuilder addKeyValue(String key, java.util.function.Supplier<Object> valueSupplier) {
            this.pendingKeyValues.add(new String[] {key, text(valueSupplier == null ? null : valueSupplier.get())});
            return this;
        }

        @Override
        public LogBuilder setMessage(String message) {
            this.pendingMessage = message;
            return this;
        }

        @Override
        public LogBuilder setMessage(java.util.function.Supplier<String> messageSupplier) {
            this.pendingMessage = messageSupplier == null ? null : messageSupplier.get();
            return this;
        }

        @Override
        public void log(java.util.function.Supplier<String> messageSupplier) {
            emit2(text(messageSupplier == null ? null : messageSupplier.get()), throwable);
        }

        @Override
        public LogBuilder withLocation() {
            return this;
        }

        @Override
        public LogBuilder withLocation(StackTraceElement location) {
            return this;
        }

        @Override
        public void log(CharSequence message) {
            emit2(text(message), throwable);
        }

        @Override
        public void log(String message) {
            emit2(text(message), throwable);
        }

        @Override
        public void log(String message, Object... params) {
            emit2(format(message, params), throwable);
        }

        @Override
        public void log(String message, Supplier<?>... paramSuppliers) {
            emit2(formatSuppliers(message, paramSuppliers), throwable);
        }

        @Override
        public void log(Message message) {
            emit2(messageText(message), throwable);
        }

        @Override
        public void log(Supplier<Message> messageSupplier) {
            emit2(messageText(messageSupplier == null ? null : messageSupplier.get()), throwable);
        }

        @Override
        public Message logAndGet(Supplier<Message> messageSupplier) {
            Message message = messageSupplier == null ? null : messageSupplier.get();
            emit2(messageText(message), throwable);
            return message;
        }

        @Override
        public void log(Object message) {
            emit2(text(message), throwable);
        }

        @Override
        public void log(String message, Object p0) {
            emit2(format(message, new Object[] {p0}), throwable);
        }

        @Override
        public void log(String message, Object p0, Object p1) {
            emit2(format(message, new Object[] {p0, p1}), throwable);
        }

        @Override
        public void log(String message, Object p0, Object p1, Object p2) {
            emit2(format(message, new Object[] {p0, p1, p2}), throwable);
        }

        @Override
        public void log(String message, Object p0, Object p1, Object p2, Object p3) {
            emit2(format(message, new Object[] {p0, p1, p2, p3}), throwable);
        }

        @Override
        public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
            emit2(format(message, new Object[] {p0, p1, p2, p3, p4}), throwable);
        }

        @Override
        public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
            emit2(format(message, new Object[] {p0, p1, p2, p3, p4, p5}), throwable);
        }

        @Override
        public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
            emit2(format(message, new Object[] {p0, p1, p2, p3, p4, p5, p6}), throwable);
        }

        @Override
        public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
            emit2(format(message, new Object[] {p0, p1, p2, p3, p4, p5, p6, p7}), throwable);
        }

        @Override
        public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
            emit2(format(message, new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8}), throwable);
        }

        @Override
        public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
            emit2(format(message, new Object[] {p0, p1, p2, p3, p4, p5, p6, p7, p8, p9}), throwable);
        }

        @Override
        public void log() {
            if (pendingMessage != null) {
                emit2(keyValuePrefix() + format(pendingMessage, pendingArguments.toArray()), throwable);
            } else {
                emit2("", throwable);
            }
        }

        private String keyValuePrefix() {
            if (pendingKeyValues.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder(32);
            for (String[] kv : pendingKeyValues) {
                sb.append(kv[0]).append('=').append(kv[1]).append(' ');
            }
            return sb.toString();
        }

        private void emit2(String message, Throwable t) {
            emit(level, marker, slf4jMarkerPrefix + message, t);
        }
    }

    /** No-op flow message used by traceEntry(). */
    private static final class SimpleEntryMessage implements EntryMessage {

        @Override
        public String getFormattedMessage() {
            return "";
        }

        @Override
        public String getFormat() {
            return "";
        }

        @Override
        public Object[] getParameters() {
            return new Object[0];
        }

        @Override
        public Throwable getThrowable() {
            return null;
        }

        @Override
        public String getText() {
            return "";
        }

        @Override
        public Message getMessage() {
            return this;
        }
    }
}

