/*
 * Copyright 2016 Ecole des Mines de Saint-Etienne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.thesmartenergy.sparql.generate.jena;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import org.apache.jena.util.Locator;
import org.apache.jena.util.TypedStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class LocatorURLAccept implements Locator {

    static Logger log = LoggerFactory.getLogger(LocatorURLAccept.class);
    final String acceptHeader;
    static final String[] schemeNames = {"http:", "https:"};    // Must be lower case and include the ":"

    public LocatorURLAccept() {
        this.acceptHeader = "*/*";
    }

    public LocatorURLAccept(final String acceptHeader) {
        this.acceptHeader = acceptHeader;
    }
    
    @Override
    public TypedStream open(String filenameOrURI) {
        if (!acceptByScheme(filenameOrURI)) {
            if (log.isTraceEnabled()) {
                log.trace("Not found : " + filenameOrURI);
            }
            return null;
        }

        try {
            URL url = new URL(filenameOrURI);
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("Accept", acceptHeader);
            conn.setRequestProperty("Accept-Charset", "utf-8,*");
            conn.setDoInput(true);
            conn.setDoOutput(false);
            // Default is true.  See javadoc for HttpURLConnection
            //((HttpURLConnection)conn).setInstanceFollowRedirects(true) ;
            conn.connect();
            InputStream in = new BufferedInputStream(conn.getInputStream());

            if (log.isTraceEnabled()) {
                log.trace("Found: " + filenameOrURI);
            }
            return new TypedStream(in, conn.getContentType(), conn.getContentEncoding());
        } catch (java.io.FileNotFoundException ex) {
            if (log.isTraceEnabled()) {
                log.trace("LocatorURLAccept: not found: " + filenameOrURI);
            }
            return null;
        } catch (MalformedURLException ex) {
            log.warn("Malformed URL: " + filenameOrURI);
            return null;
        } // IOExceptions that occur sometimes.
        catch (java.net.UnknownHostException ex) {
            if (log.isTraceEnabled()) {
                log.trace("LocatorURLAccept: not found (UnknownHostException): " + filenameOrURI);
            }
            return null;
        } catch (java.net.ConnectException ex) {
            if (log.isTraceEnabled()) {
                log.trace("LocatorURLAccept: not found (ConnectException): " + filenameOrURI);
            }
            return null;
        } catch (java.net.SocketException ex) {
            if (log.isTraceEnabled()) {
                log.trace("LocatorURLAccept: not found (SocketException): " + filenameOrURI);
            }
            return null;
        } // And IOExceptions we don't expect
        catch (IOException ex) {
            log.warn("I/O Exception opening URL: " + filenameOrURI + "  " + ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LocatorURLAccept;
    }

    @Override
    public int hashCode() {
        return LocatorURLAccept.class.hashCode();
    }

    @Override
    public String getName() {
        return "LocatorURLAccept";
    }

    private boolean acceptByScheme(String filenameOrURI) {
        String uriSchemeName = getScheme(filenameOrURI);
        if (uriSchemeName == null) {
            return false;
        }
        uriSchemeName = uriSchemeName.toLowerCase(Locale.ENGLISH);
        for (String schemeName : schemeNames) {
            if (uriSchemeName.equals(schemeName)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasScheme(String uri, String scheme) {
        String actualScheme = getScheme(uri);
        if (actualScheme == null) {
            return false;
        }
        return actualScheme.equalsIgnoreCase(scheme);
    }

    // Not perfect - but we support Java 1.3 (as of August 2004)
    private String getScheme(String uri) {
        int ch = uri.indexOf(':');
        if (ch < 0) {
            return null;
        }

        // Includes the : 
        return uri.substring(0, ch + 1);
    }

}
