/*
 * Copyright 2017 École des Mines de Saint-Étienne.
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
package fr.emse.ci.sparqlext.stream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AccessControlException;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.lib.IRILib;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.util.FileUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Same as LocatorFile but with content negociation capabilities.
 *
 * @author maxime.lefrancois
 */
public class LocatorFileAccept extends LocatorAcceptBase {
    // Implementation note:
    // Java7: Path.resolve may provide an answer from the intricies of MS Windows

    static Logger log = LoggerFactory.getLogger(LocatorFileAccept.class);
    private final String thisDir;
    private final String thisDirLogStr;

    /**
     * Create a LocatorFile without a specific working directory. Relative file
     * names are relative to the working directory of the JVM.
     */
    public LocatorFileAccept() {
        this(null);
    }

    /**
     * Create a LocatorFile that uses the argument as it's working directory.
     * <p>
     * The working directory should be a UNIX style file name, (relative or
     * absolute), not a URI.
     * <p>
     * For MS Window, if asked to {@link #open} a file name with a drive letter,
     * the code assumes it is not relative to the working directory of this
     * {@code LocatorFile}.
     */
    public LocatorFileAccept(String dir) {
        if (dir != null) {
            if (dir.endsWith("/") || dir.endsWith(java.io.File.separator)) {
                dir = dir.substring(0, dir.length() - 1);
            }
            thisDirLogStr = " [" + dir + "]";
        } else {
            thisDirLogStr = "";
        }
        thisDir = dir;
    }

    /**
     * Processing the filename for file: or relative filename and return a
     * filename suitable for file operations.
     */
    public String toFileName(String filenameIRI) {
        // Do not use directly : it will ignore the directory. 
        //IRILib.filenameToIRI

        String scheme = FileUtils.getScheme(filenameIRI);
        String fn = filenameIRI;
        // Windows : C:\\ is not a scheme name!
        if (scheme != null) {
            if (scheme.length() == 1) {
                // Not perfect for MS Windows but if thisDir is set then
                // the main use case is resolving relative (no drive)
                // filenames against thisDir. Treat the presence of a
                // drive letter as making this a JVM relative filename. 
                return fn;
            } else if (scheme.length() > 1) {
                if (!scheme.equalsIgnoreCase("file")) // Not file: IRI
                {
                    return null;
                }
                fn = IRILib.IRIToFilename(filenameIRI);
                // fall through
            }
        }
        // fn is the file name to use.
        return absolute(fn);
    }

    /**
     * Make a filename (no URI scheme, no windows drive) absolute if there is a
     * setting for directory name thisDir
     */
    private String absolute(String fn) {
        if (thisDir != null && !fn.startsWith("/") && !fn.startsWith(File.separator)) {
            fn = thisDir + File.separator + fn;
        }
        return fn;
    }

    public String getThisDir() {
        return thisDir;
    }

    public boolean hasCurrentDir() {
        return thisDir != null;
    }

    public boolean exists(String fileIRI) {
        String fn = toFileName(fileIRI);
        if (fn == null) {
            return false;
        }

        return exists$(fn);
    }

    private boolean exists$(String fn) {
        if (fn.equals("-")) {
            return true;
        }
        return new File(fn).exists();
    }

    /**
     * Open anything that looks a bit like a file name
     */
    @Override
    public TypedInputStream open(LookUpRequest request) {
        String filenameIRI = request.getFilenameOrURI();
        if(filenameIRI.startsWith("http") || filenameIRI.startsWith("coap")) {
            return null;
        }
        String fn = toFileName(filenameIRI);
        if (fn == null) {
            log.debug("Cannot find a proper filename: " + filenameIRI);
            return null;
        }

        try {
            if (!exists$(fn)) {
                return null;
            }
        } catch (AccessControlException e) {
            log.debug("Security problem testing for file", e);
            return null;
        }

        try {
            InputStream in = IO.openFileEx(fn);
            try {
                String ct = Files.probeContentType(Paths.get(new File(filenameIRI).getName()));
                return new TypedInputStream(in, ContentType.create(ct), filenameIRI);
            } catch (Exception ex) {
                log.trace("Error while trying to probe content type for " + filenameIRI + ": " + ex.getMessage());
                return new TypedInputStream(in, (ContentType) null, filenameIRI);
            } 
        } catch (IOException ioEx) {
            // Includes FileNotFoundException
            // We already tested whether the file exists or not.
            log.debug("File unreadable (but exists): " + fn + " Exception: " + ioEx.getMessage());
            return null;
        }
    }

    @Override
    public String getName() {
        String tmp = "LocatorFileAccept";
        if (thisDir != null) {
            tmp = tmp + "(" + thisDir + ")";
        }
        return tmp;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((thisDir == null) ? 0 : thisDir.hashCode());
        result = prime * result + ((thisDirLogStr == null) ? 0 : thisDirLogStr.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LocatorFileAccept other = (LocatorFileAccept) obj;
        if (thisDir == null) {
            if (other.thisDir != null) {
                return false;
            }
        } else if (!thisDir.equals(other.thisDir)) {
            return false;
        }
        if (thisDirLogStr == null) {
            if (other.thisDirLogStr != null) {
                return false;
            }
        } else if (!thisDirLogStr.equals(other.thisDirLogStr)) {
            return false;
        }
        return true;
    }
}
