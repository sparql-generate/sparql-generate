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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.web.TypedInputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LocatorFileAcceptTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	/**
	 * A UTF-8 byte order mark is not part of the document: left in, it makes
	 * XML and JSON parsers fail and prefixes the first CSV header.
	 */
	@Test
	public void byteOrderMarkIsRemoved() throws Exception {
		File file = folder.newFile("doc.xml");
		Files.write(file.toPath(), "﻿<a/>".getBytes(StandardCharsets.UTF_8));
		LocatorFileAccept locator = new LocatorFileAccept(folder.getRoot().getAbsolutePath());
		try (TypedInputStream tin = locator.open(new LookUpRequest("doc.xml"))) {
			assertEquals("<a/>", IOUtils.toString((InputStream) tin, StandardCharsets.UTF_8));
		}
	}

	@Test
	public void contentWithoutByteOrderMarkIsUnchanged() throws Exception {
		File file = folder.newFile("doc.txt");
		Files.write(file.toPath(), "abc".getBytes(StandardCharsets.UTF_8));
		LocatorFileAccept locator = new LocatorFileAccept(folder.getRoot().getAbsolutePath());
		try (TypedInputStream tin = locator.open(new LookUpRequest("doc.txt"))) {
			assertEquals("abc", IOUtils.toString((InputStream) tin, StandardCharsets.UTF_8));
		}
	}
}
