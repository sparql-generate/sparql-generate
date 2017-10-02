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
package com.github.thesmartenergy.sparql.generate.jena.stream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.atlas.web.TypedInputStream;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 *
 * @author maxime.lefrancois
 */
public class LocatorDirectoryAccept extends LocatorAcceptBase {
    // Implementation note:
    // Java7: Path.resolve may provide an answer from the intricies of MS Windows

    static Logger log = LoggerFactory.getLogger(LocatorDirectoryAccept.class);
    private final String base;
    private final File dir;

    public LocatorDirectoryAccept(String base, File dir) {
        Objects.requireNonNull(base, "the base must not be null");
        Objects.requireNonNull(dir, "the dir must not be null");
        if (!dir.exists()) {
            throw new NullPointerException("dir does not exist, it must be a valid directory");
        }
        if (!dir.isDirectory()) {
            throw new NullPointerException("dir exists but is not a directory. It must be a valid directory");
        }
        this.base = base;
        this.dir = dir;
    }

    /**
     * Open anything that looks a bit like a file name
     */
    @Override
    public TypedInputStream open(LookUpRequest request) {

        String filenameIRI = request.getFilenameOrURI();

        if (!filenameIRI.startsWith(base)) {
            log.debug("filename " + filenameIRI + " does not start with base " + base);
            return null;
        }

        String relativeIRI = filenameIRI.substring(base.length());

        File f = new File(dir, relativeIRI);
        try {
            InputStream is = new BOMInputStream(new FileInputStream(f));
            log.info("found " + filenameIRI);

            try {
                String ct = Files.probeContentType(Paths.get(f.getName()));
                return new TypedInputStream(is, ct, "UTF-8");
            } catch (IOException ex) {
                log.trace("Error while trying to probe content type for " + f + ": " + ex.getMessage());
                return new TypedInputStream(is, (ContentType) null, "UTF-8");
            }
        } catch (FileNotFoundException ex) {
            if (!f.exists()) {
                log.debug("Not found: " + f);
                return null;
            }
            if (f.isDirectory()) {
                log.debug("File should not be a directory: " + f);
                return null;
            }
            if (f.canRead()) {
                log.debug("File is not readable: " + f);
                return null;
            }
            log.debug("Not found: " + f);
            return null;
        }
    }

    @Override
    public String getName() {
        StringBuilder sb = new StringBuilder("LocatorDirectoryAccept");
        sb.append("(").append(dir).append(")");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 29;
        int result = 1;
        result = prime * result + dir.hashCode();
        result = prime * result + base.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(getClass() != obj.getClass())) {
            return false;
        }
        LocatorDirectoryAccept other = (LocatorDirectoryAccept) obj;
        if (!other.base.equals(this.base) || !other.dir.equals(this.dir)) {
            return false;
        }
        return true;
    }
}
