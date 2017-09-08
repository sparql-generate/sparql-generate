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
package com.github.thesmartenergy.sparql.generate.jena.locator;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.riot.system.stream.Locator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class LocatorURLAccept implements Locator {

    static Logger log = LogManager.getLogger(LocatorURLAccept.class);
    

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
        return LocatorURLAccept.class.getSimpleName();
    }

    @Override
    public TypedInputStream open(String acceptURI) {
        log.trace(acceptURI);
        if (!acceptURI.substring(0, 7).equals("accept:")) {
            log.trace("not supported " + acceptURI);
            return null;
        }

        // get accept
        int index = acceptURI.indexOf(":", 7);
        if (index == -1) {
            log.trace("not supported " + acceptURI);
            return null;
        }
        String acceptHeader = acceptURI.substring(7, index);
        String source = acceptURI.substring(index + 1);

        try {
            URL url = new URL(source);
            URLConnection conn = (URLConnection) url.openConnection();
            String userInfo = url.getUserInfo();
            if (userInfo != null && !userInfo.isEmpty()) {
                String encodedUserInfo = new String(Base64.encodeBase64(userInfo.getBytes("UTF-8")));
                conn.setRequestProperty("Authorization", "Basic " + encodedUserInfo);
            }
            conn.setRequestProperty("Accept", acceptHeader);
            conn.setRequestProperty("Accept-Charset", "utf-8,*");
            conn.setDoInput(true);
            conn.setDoOutput(false);
            // Default is true.  See javadoc for HttpURLConnection
            ((HttpURLConnection)conn).setInstanceFollowRedirects(true) ;
            conn.connect();
            InputStream in = new BufferedInputStream(new BOMInputStream(conn.getInputStream()));

            log.trace("found: " + source + " " + conn.getContentType() + " " + conn.getContentEncoding());
            return new TypedInputStream(in, conn.getContentType(), conn.getContentEncoding());
        } catch (java.io.FileNotFoundException ex) {
            log.trace("not found: " + source, ex);
            return null;
        } catch (MalformedURLException ex) {
            log.trace("Malformed URL: " + source, ex);
            return null;
        } // IOExceptions that occur sometimes.
        catch (java.net.UnknownHostException ex) {
            log.trace("UnknownHostException " + source, ex);
            return null;
        } catch (java.net.ConnectException ex) {
            log.trace("ConnectException " + source, ex);
            return null;
        } catch (java.net.SocketException ex) {
            log.trace("SocketException " + source, ex);
            return null;
        }
        catch (IOException ex) {
            log.warn("I/O Exception opening URL: " + source + "  " + ex.getMessage(), ex);
            return null;
        }
    }

}
