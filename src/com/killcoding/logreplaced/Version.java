package com.killcoding.logreplaced;

/**
 * Build version carrier for the JsPlus-LogReplaced runtime.
 *
 * The ant build (build.xml) stamps the real semver + build date into
 * VERSION before compilation and restores the BuildVersion placeholder
 * right after, so the source always stays in placeholder form.
 */
public class Version {

    public final static String VERSION = "${BuildVersion.VERSION}(${BuildVersion.BUILD_DATE})";

}
