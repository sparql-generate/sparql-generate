/*
 * Copyright 2016 ITEA 12004 SEAS Project.
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

package org.w3id.sparql.generate.selector;

import org.apache.jena.query.QueryBuildException;

/** Extension factory that instantiates a class each time. */ 

class SelectorFactoryAuto implements SelectorFactory
{
    Class<?> extClass ;
    
    SelectorFactoryAuto(Class<?> xClass)
    {
        extClass = xClass ;
    }
    
    @Override
    public Selector create(String uri)
    {
        try
        {
            return (Selector)extClass.newInstance() ;
        } catch (Exception e)
        {
            throw new QueryBuildException("Can't instantiate selector for "+uri, e) ;
        } 
    }
}
