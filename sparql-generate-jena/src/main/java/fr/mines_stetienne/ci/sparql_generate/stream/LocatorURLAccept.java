/*
 * Copyright 2020 MINES Saint-Étienne
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
package fr.mines_stetienne.ci.sparql_generate.stream;

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
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Same as LocatorURL but with content negociation capabilities.
 *
 * @author Maxime Lefrançois
 */
public class LocatorURLAccept extends LocatorAcceptBase {

	static Logger log = LoggerFactory.getLogger(LocatorURLAccept.class);

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
	public TypedInputStream open(LookUpRequest request) {
		String acceptHeader = request.getAccept();
		String source = request.getFilenameOrURI();
		try {
			URL url = new URL(source);
			URLConnection conn = (URLConnection) url.openConnection();
//            conn.setConnectTimeout(200);
//            conn.setReadTimeout(500);
			return openConnectionCheckRedirects(url, acceptHeader, conn);
		} catch (java.io.FileNotFoundException ex) {
			log.debug("File not found online: " + source);
			return null;
		} catch (MalformedURLException ex) {
			return null;
		} // IOExceptions that occur sometimes.
		catch (java.net.UnknownHostException ex) {
			log.debug("UnknownHostException " + source);
			return null;
		} catch (java.net.ConnectException ex) {
			log.debug("ConnectException " + source);
			return null;
		} catch (java.net.SocketException ex) {
			log.debug("SocketException " + source);
			return null;
		} catch (java.net.SocketTimeoutException ex) {
			log.debug("SocketTimeoutException: " + source + "  " + ex.getMessage());
			return null;
		} catch (IOException ex) {
			log.debug("I/O Exception opening URL: " + source + "  " + ex.getMessage());
			return null;
		}
	}

	private TypedInputStream openConnectionCheckRedirects(URL url, String acceptHeader, URLConnection c) throws IOException {
		boolean redir;
		int redirects = 0;
		String contentType = null;
		String contentEncoding = null;
		do {
			if (c instanceof HttpURLConnection) {
				((HttpURLConnection) c).setInstanceFollowRedirects(false);
			}
			String userInfo = url.getUserInfo();
			if (userInfo != null && !userInfo.isEmpty()) {
				String encodedUserInfo = new String(Base64.encodeBase64(userInfo.getBytes("UTF-8")));
				c.setRequestProperty("Authorization", "Basic " + encodedUserInfo);
			}
			c.setRequestProperty("Accept", acceptHeader);
			c.setRequestProperty("Accept-Charset", "utf-8,*");

			// We want to open the input stream before getting headers
			// because getHeaderField() et al swallow IOExceptions.
			InputStream in = new BufferedInputStream(new BOMInputStream(c.getInputStream()));
			contentType = c.getContentType();
			contentEncoding = c.getContentEncoding();
			redir = false;
			if (c instanceof HttpURLConnection) {
				HttpURLConnection http = (HttpURLConnection) c;
				int stat = http.getResponseCode();
				if (stat >= 300 && stat <= 307 && stat != 306 && stat != HttpURLConnection.HTTP_NOT_MODIFIED) {
					URL base = http.getURL();
					String loc = http.getHeaderField("Location");
					URL target = null;
					if (loc != null) {
						target = new URL(base, loc);
					}
					http.disconnect();
					// Redirection should be allowed only for HTTP and HTTPS
					// and should be limited to 5 redirections at most.
					if (target == null || !(target.getProtocol().equals("http") || target.getProtocol().equals("https"))
							|| c.getURL().getProtocol().equals("https") && target.getProtocol().equals("http")
							|| redirects >= 5) {
						throw new SecurityException("illegal URL redirect");
					}
					redir = true;
					c = target.openConnection();
					redirects++;
				} else {
					if (contentType == null) {
						contentType = "text/plain";
					}
					return new TypedInputStream(in, contentType, contentEncoding);
				}
			}
		} while (redir);
		return null;
	}

}
