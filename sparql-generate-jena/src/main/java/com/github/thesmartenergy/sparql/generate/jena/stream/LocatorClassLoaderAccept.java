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

import static com.github.thesmartenergy.sparql.generate.jena.stream.LocatorDirectoryAccept.log;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.atlas.web.TypedInputStream;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 *
 * @author maxime.lefrancois
 */
public class LocatorClassLoaderAccept extends LocatorAcceptBase {

    static Logger log = LoggerFactory.getLogger(LocatorClassLoaderAccept.class);

    private final ClassLoader classLoader;

    public LocatorClassLoaderAccept(ClassLoader _classLoader) {
        classLoader = _classLoader;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LocatorClassLoaderAccept
                && classLoader == ((LocatorClassLoaderAccept) other).classLoader;
    }

    @Override
    public int hashCode() {
        return classLoader.hashCode();
    }

    @Override
    public TypedInputStream open(LookUpRequest request) {
        if (classLoader == null) {
            return null;
        }
        String resourceName = request.getFilenameOrURI();
        InputStream in = classLoader.getResourceAsStream(resourceName);
        if (in == null) {
            return null;
        }
        
        try {
            String ct = Files.probeContentType(Paths.get(new File(resourceName).getName()));
            return new TypedInputStream(in, ContentType.create(ct), resourceName);
        } catch (IOException ex) {
                log.trace("Error while trying to probe content type for " + resourceName + ": " + ex.getMessage());
            return new TypedInputStream(in, (String) null);
        } 
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public String getName() {
        return "ClassLoaderLocatorAccept";
    }

}
