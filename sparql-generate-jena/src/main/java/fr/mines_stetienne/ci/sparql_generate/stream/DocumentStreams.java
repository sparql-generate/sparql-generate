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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.io.input.ReaderInputStream;

/**
 * The content of a document file: decompressed according to the extension of
 * its name, and in UTF-8.
 * <ul>
 * <li>{@code .gz}, {@code .bz2}, {@code .xz}: the decompressed content;</li>
 * <li>{@code .zip}, {@code .tar}, {@code .tar.gz} ({@code .tgz}),
 * {@code .tar.bz2}, {@code .tar.xz} ({@code .txz}): the content of the first
 * file of the archive;</li>
 * <li>a byte order mark (UTF-8, UTF-16, UTF-32) is not part of the content,
 * and a UTF-16 or UTF-32 content is transcoded to UTF-8.</li>
 * </ul>
 *
 * @since 2.4.2
 */
public final class DocumentStreams {

    private DocumentStreams() {
    }

    /**
     * @param in the bytes of the file
     * @param name the name of the file (its extension gives the compression)
     * @return the content, in UTF-8
     * @throws IOException if the content cannot be read
     */
    public static InputStream open(InputStream in, String name) throws IOException {
        return unicode(decompress(in, name));
    }

    /**
     * @param in the bytes of the file
     * @param name the name of the file
     * @return the decompressed content, or {@code in} if the name has no
     * known compression extension
     * @throws IOException if the archive cannot be read
     */
    public static InputStream decompress(InputStream in, String name) throws IOException {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        int q = n.indexOf('?');
        if (q >= 0) {
            n = n.substring(0, q);
        }
        if (n.endsWith(".zip")) {
            ZipInputStream zip = new ZipInputStream(in);
            ZipEntry e = zip.getNextEntry();
            while (e != null && e.isDirectory()) {
                e = zip.getNextEntry();
            }
            if (e == null) {
                throw new IOException("Empty zip archive: " + name);
            }
            return zip;
        }
        if (n.endsWith(".tar.gz") || n.endsWith(".tgz")) {
            return firstTarEntry(new GzipCompressorInputStream(in, true), name);
        }
        if (n.endsWith(".tar.xz") || n.endsWith(".txz")) {
            return firstTarEntry(new XZCompressorInputStream(in, true), name);
        }
        if (n.endsWith(".tar.bz2")) {
            return firstTarEntry(new BZip2CompressorInputStream(in, true), name);
        }
        if (n.endsWith(".tar")) {
            return firstTarEntry(in, name);
        }
        if (n.endsWith(".gz")) {
            return new GzipCompressorInputStream(in, true);
        }
        if (n.endsWith(".xz")) {
            return new XZCompressorInputStream(in, true);
        }
        if (n.endsWith(".bz2")) {
            return new BZip2CompressorInputStream(in, true);
        }
        return in;
    }

    private static InputStream firstTarEntry(InputStream in, String name) throws IOException {
        TarArchiveInputStream tar = new TarArchiveInputStream(in);
        TarArchiveEntry e = tar.getNextEntry();
        while (e != null && !e.isFile()) {
            e = tar.getNextEntry();
        }
        if (e == null) {
            throw new IOException("No file in tar archive: " + name);
        }
        return tar;
    }

    /**
     * @param in a content
     * @return the content without byte order mark, transcoded to UTF-8 when
     * the byte order mark is a UTF-16 or UTF-32 one
     * @throws IOException if the content cannot be read
     */
    @SuppressWarnings("deprecation")
    public static InputStream unicode(InputStream in) throws IOException {
        BOMInputStream bom = new BOMInputStream(in, false, ByteOrderMark.UTF_8,
                ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_32BE,
                ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE);
        ByteOrderMark mark = bom.getBOM();
        if (mark == null || mark.equals(ByteOrderMark.UTF_8)) {
            return bom;
        }
        return new ReaderInputStream(new InputStreamReader(bom, mark.getCharsetName()),
                StandardCharsets.UTF_8);
    }
}
