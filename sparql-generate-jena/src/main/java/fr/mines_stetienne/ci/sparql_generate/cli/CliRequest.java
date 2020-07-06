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
package fr.mines_stetienne.ci.sparql_generate.cli;

import org.apache.log4j.Level;

import fr.mines_stetienne.ci.sparql_generate.FileConfigurations;

/**
 * Extends the FileConfigurations with the log level as an object
 * 
 * 
 * @author Maxime Lefrançois http://maxime-lefrancois.info/
 */
public class CliRequest extends FileConfigurations {

	/**
	 * The log level as an object
	 */
    public Level logLevelObject;
}
