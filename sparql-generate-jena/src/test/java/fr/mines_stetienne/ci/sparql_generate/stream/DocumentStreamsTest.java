/*
 * Copyright 2026 MINES Saint-Étienne
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

/**
 * Documents are read decompressed according to their extension, and in UTF-8.
 */
public class DocumentStreamsTest {

    private static final String TEXT = "{\"name\": \"Zoë\"}";

    private static String read(byte[] bytes, String name) throws IOException {
        try (InputStream in = DocumentStreams.open(new ByteArrayInputStream(bytes), name)) {
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }

    private static byte[] utf8() {
        return TEXT.getBytes(StandardCharsets.UTF_8);
    }

    private interface Writer {
        OutputStream wrap(OutputStream out) throws IOException;
    }

    private static byte[] compress(Writer w, byte[] content) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (OutputStream out = w.wrap(bytes)) {
            out.write(content);
        }
        return bytes.toByteArray();
    }

    private static byte[] tar(byte[] content) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(bytes)) {
            TarArchiveEntry e = new TarArchiveEntry("data.json");
            e.setSize(content.length);
            tar.putArchiveEntry(e);
            tar.write(content);
            tar.closeArchiveEntry();
        }
        return bytes.toByteArray();
    }

    @Test
    public void plain() throws IOException {
        assertEquals(TEXT, read(utf8(), "data.json"));
    }

    @Test
    public void gzip() throws IOException {
        assertEquals(TEXT, read(compress(GzipCompressorOutputStream::new, utf8()), "data.json.gz"));
    }

    @Test
    public void zip() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("data.json"));
            zip.write(utf8());
            zip.closeEntry();
        }
        assertEquals(TEXT, read(bytes.toByteArray(), "data.json.zip"));
    }

    @Test
    public void tarGzip() throws IOException {
        assertEquals(TEXT, read(compress(GzipCompressorOutputStream::new, tar(utf8())), "data.json.tar.gz"));
    }

    @Test
    public void tarXz() throws IOException {
        assertEquals(TEXT, read(compress(XZCompressorOutputStream::new, tar(utf8())), "data.json.tar.xz"));
    }

    @Test
    public void utf16WithByteOrderMark() throws IOException {
        byte[] le = ("﻿" + TEXT).getBytes(Charset.forName("UTF-16LE"));
        assertEquals(TEXT, read(le, "data.json"));
        byte[] be = ("﻿" + TEXT).getBytes(Charset.forName("UTF-16BE"));
        assertEquals(TEXT, read(be, "data.json"));
    }

    @Test
    public void utf8ByteOrderMarkIsNotContent() throws IOException {
        byte[] bom = ("﻿" + TEXT).getBytes(StandardCharsets.UTF_8);
        assertEquals(TEXT, read(bom, "data.json"));
    }
}
