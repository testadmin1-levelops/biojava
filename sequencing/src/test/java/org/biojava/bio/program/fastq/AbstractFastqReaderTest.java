/*
 *                    BioJava development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the individual
 * authors.  These should be listed in @author doc comments.
 *
 * For more information on the BioJava project and its aims,
 * or to join the biojava-l mailing list, visit the home page
 * at:
 *
 *      http://www.biojava.org/
 *
 */
package org.biojava.bio.program.fastq;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.net.URL;

import junit.framework.TestCase;

/**
 * Abstract unit test for implementations of FastqReader.
 */
abstract class AbstractFastqReaderTest
    extends TestCase
{
    /** Array of example files that should throw IOExceptions. */
    static final String[] ERROR_EXAMPLES = new String[]
        {
            "error_diff_ids.fastq",
            "error_double_qual.fastq",
            "error_double_seq.fastq",
            "error_long_qual.fastq",
            "error_no_qual.fastq",
            "error_qual_del.fastq",
            "error_qual_escape.fastq",
            "error_qual_null.fastq",
            "error_qual_space.fastq",
            "error_qual_tab.fastq",
            "error_qual_unit_sep.fastq",
            "error_qual_vtab.fastq",
            "error_short_qual.fastq",
            "error_spaces.fastq",
            "error_tabs.fastq",
            "error_trunc_at_plus.fastq",
            "error_trunc_at_qual.fastq",
            "error_trunc_at_seq.fastq",
            "error_trunc_in_plus.fastq",
            "error_trunc_in_qual.fastq",
            "error_trunc_in_seq.fastq",
            "error_trunc_in_title.fastq"
        };

    /**
     * Create and return a new FASTQ formatted sequence suitable for testing.
     *
     * @return a new FASTQ formatted sequence suitable for testing.
     */
    protected abstract Fastq createFastq();

    /**
     * Create and return a new instance of an implementation of FastqReader to test.
     *
     * @return a new instnace of an implementation of FastqReader to test
     */
    protected abstract FastqReader createFastqReader();

    /**
     * Create and return a new instance of an implementation of FastqWriter to test round-tripping.
     *
     * @return a new instance of an implementation of FastqWriter to test round-tripping.
     */
    protected abstract FastqWriter createFastqWriter();

    public void testCreateFastq()
    {
        Fastq fastq = createFastq();
        assertNotNull(fastq);
    }

    public void testCreateFastqReader()
    {
        FastqReader reader = createFastqReader();
        assertNotNull(reader);
    }

    public void testCreateFastqWriter()
    {
        FastqWriter writer = createFastqWriter();
        assertNotNull(writer);
    }

    public void testReadFile() throws Exception
    {
        FastqReader reader = createFastqReader();
        try
        {
            reader.read((File) null);
            fail("read((File) null) expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }
        try
        {
            File noSuchFile = new File("no such file");
            reader.read(noSuchFile);
            fail("read(no such file) expected IOException");
        }
        catch (IOException e)
        {
            // expected
        }
    }

    public void testReadEmptyFile() throws Exception
    {
        FastqReader reader = createFastqReader();
        File empty = File.createTempFile("abstractFastqReaderTest", null);
        Iterable<Fastq> iterable = reader.read(empty);
        assertNotNull(iterable);
        int count = 0;
        for (Fastq f : iterable)
        {
            assertNotNull(f);
            count++;
        }
        assertEquals(0, count);
    }

    public void testReadRoundTripSingleFile() throws Exception
    {
        FastqReader reader = createFastqReader();
        File single = File.createTempFile("abstractFastqReaderTest", null);
        Fastq fastq = createFastq();
        FastqWriter writer = createFastqWriter();
        writer.write(single, fastq);
        Iterable<Fastq> iterable = reader.read(single);
        assertNotNull(iterable);
        int count = 0;
        for (Fastq f : iterable)
        {
            assertNotNull(f);
            count++;
        }
        assertEquals(1, count);
    }

    public void testReadRoundTripMultipleFile() throws Exception
    {
        FastqReader reader = createFastqReader();
        File multiple = File.createTempFile("abstractFastqReaderTest", null);
        Fastq fastq0 = createFastq();
        Fastq fastq1 = createFastq();
        Fastq fastq2 = createFastq();
        FastqWriter writer = createFastqWriter();
        writer.write(multiple, fastq0, fastq1, fastq2);
        Iterable<Fastq> iterable = reader.read(multiple);
        assertNotNull(iterable);
        int count = 0;
        for (Fastq f : iterable)
        {
            assertNotNull(f);
            count++;
        }
        assertEquals(3, count);
    }

    public void testReadURL() throws Exception
    {
        FastqReader reader = createFastqReader();
        try
        {
            reader.read((URL) null);
            fail("read((URL) null) expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }
        try
        {
            URL noSuchURL = new URL("file:///no such url");
            reader.read(noSuchURL);
            fail("read(no such URL) expected IOException");
        }
        catch (IOException e)
        {
            // expected
        }
    }

    public void testReadEmptyURL() throws Exception
    {
        FastqReader reader = createFastqReader();
        URL empty = getClass().getResource("empty.fastq");
        Iterable<Fastq> iterable = reader.read(empty);
        assertNotNull(iterable);
        int count = 0;
        for (Fastq f : iterable)
        {
            assertNotNull(f);
            count++;
        }
        assertEquals(0, count);
    }

    public void testReadInputStream() throws Exception
    {
        FastqReader reader = createFastqReader();
        try
        {
            reader.read((InputStream) null);
            fail("read((InputStream) null) expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }
    }

    public void testReadEmptyInputStream() throws Exception
    {
        FastqReader reader = createFastqReader();
        InputStream empty = getClass().getResourceAsStream("empty.fastq");
        Iterable<Fastq> iterable = reader.read(empty);
        assertNotNull(iterable);
        int count = 0;
        for (Fastq f : iterable)
        {
            assertNotNull(f);
            count++;
        }
        assertEquals(0, count);
        empty.close();
    }

    public void testWrappedSequence() throws Exception
    {
        FastqReader reader = createFastqReader();
        InputStream wrappedSequence = getClass().getResourceAsStream("wrapped-sequence.fastq");
        Iterable<Fastq> iterable = reader.read(wrappedSequence);
        assertNotNull(iterable);
        int count = 0;
        for (Fastq f : iterable)
        {
            assertNotNull(f);
            assertEquals("ACTG", f.getSequence());
            count++;
        }
        assertEquals(1, count);
        wrappedSequence.close();
    }

    public void testWrappedQuality() throws Exception
    {
        FastqReader reader = createFastqReader();
        InputStream wrappedQuality = getClass().getResourceAsStream("wrapped-quality.fastq");
        Iterable<Fastq> iterable = reader.read(wrappedQuality);
        assertNotNull(iterable);
        int count = 0;
        for (Fastq f : iterable)
        {
            assertNotNull(f);
            assertEquals("ZZZZ", f.getQuality());
            count++;
        }
        assertEquals(1, count);
        wrappedQuality.close();
    }

    public void testMultipleWrappedQuality() throws Exception
    {
        FastqReader reader = createFastqReader();
        InputStream wrappedQuality = getClass().getResourceAsStream("multiple-wrapped-quality.fastq");
        Iterable<Fastq> iterable = reader.read(wrappedQuality);
        assertNotNull(iterable);
        int count = 0;
        for (Fastq f : iterable)
        {
            assertNotNull(f);
            assertEquals("ZZZZ", f.getQuality());
            count++;
        }
        assertEquals(4, count);
        wrappedQuality.close();
    }

    public void testErrorExamples() throws Exception
    {
        FastqReader reader = createFastqReader();
        for (String errorExample : ERROR_EXAMPLES)
        {
            InputStream inputStream = getClass().getResourceAsStream(errorExample);
            try
            {
                reader.read(inputStream);
                fail("error example " + errorExample + " expected IOException");
            }
            catch (IOException e)
            {
                // expected
            }
            finally
            {
                if (inputStream != null)
                {
                    try
                    {
                        inputStream.close();
                    }
                    catch (IOException e)
                    {
                        // ignore
                    }
                }
            }
        }
    }
}