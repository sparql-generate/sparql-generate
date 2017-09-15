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
package com.github.thesmartenergy.sparql.generate.jena.locator;

import static com.github.thesmartenergy.sparql.generate.jena.locator.LocatorFileAccept.log;
import java.io.InputStream;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.system.stream.Locator;
import org.apache.jena.riot.system.stream.LocatorClassLoader;
import org.apache.jena.riot.system.stream.StreamManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author maxime.lefrancois
 */
public class LocatorClassLoaderAccept implements Locator
{
    static Logger log = LogManager.getLogger(LocatorClassLoaderAccept.class) ;

    private final ClassLoader classLoader ;
    public LocatorClassLoaderAccept(ClassLoader _classLoader)
    {
        classLoader =_classLoader ;
    }
    
    @Override
    public boolean equals( Object other )
    {
        return 
            other instanceof LocatorClassLoaderAccept 
            && classLoader == ((LocatorClassLoaderAccept) other).classLoader;
    }
    
    @Override
    public int hashCode()
        { return classLoader.hashCode(); }
    
    @Override
    public TypedInputStream open(String resourceName)
    {
        if ( classLoader == null )
            return null ;
        
        
        log.trace(resourceName);
        if (!resourceName.substring(0, 7).equals("accept:")) {
            resourceName = "accept:*/*:" + resourceName;
        }

        // get accept
        int index = resourceName.indexOf(":", 7);
        if (index == -1) {
            log.debug("not supported " + resourceName);
            return null;
        }
        resourceName = resourceName.substring(index + 1);
        
        InputStream in = classLoader.getResourceAsStream(resourceName) ;
        if ( in == null )
        {
            if ( StreamManager.logAllLookups && log.isTraceEnabled() )
                log.trace("Failed to open: "+resourceName) ;
            return null ;
        }
        
        if ( StreamManager.logAllLookups  && log.isTraceEnabled() )
            log.trace("Found: "+resourceName) ;
        
        ContentType ct = RDFLanguages.guessContentType(resourceName) ;
        // No sensible base URI.
        return new TypedInputStream(in, ct, null) ;
    }
    
    public ClassLoader getClassLoader()
    {
        return classLoader ;
    }

    @Override
    public String getName() { return "ClassLoaderLocatorAccept" ; }
    
}
