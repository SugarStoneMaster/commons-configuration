/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.configuration2.plist;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * JUnit test class that exercises the expandBuff(boolean wrapAround) method
 * of the SimpleCharStream class.
 */
public class TestSimpleCharStream {

    private SimpleCharStream charStream;

    @BeforeEach
    public void setUp() {
        // We'll create a fresh SimpleCharStream instance for each test.
        charStream = new SimpleCharStream(new StringReader("dummy content"), 1, 1, 5);
    }

    /**
     * Test ExpandBuff(false) scenario (the "no wrap-around" branch).
     * In this path, the code copies [tokenBegin..bufsize-tokenBegin)
     * from old arrays to new ones, then updates bufpos -= tokenBegin.
     */
    @Test
    public void testExpandBuffNoWrapAround() {
        // Setup initial buffer-related fields.
        // We'll pretend we have a small buffer of size=5 and used indices:
        charStream.bufsize = 5;
        charStream.available = 5;    // often charStream.available = bufsize initially
        charStream.tokenBegin = 2;   // this means valid data starts from index 2
        charStream.bufpos = 4;       // current position

        // Initialize the buffers to track them after expansion
        charStream.buffer   = new char[]   {'a', 'b', 'c', 'd', 'e'};
        charStream.bufline  = new int[]    { 10, 11, 12, 13, 14 };
        charStream.bufcolumn= new int[]    { 20, 21, 22, 23, 24 };

        // We'll call expandBuff(false) -> new arrays of length 5+2048
        charStream.ExpandBuff(false);

        // Verify the new buffers have length = old bufsize + 2048
        assertEquals(5 + 2048, charStream.buffer.length,
                "Buffer length should be 5 + 2048 after expansion (no wrap).");
        assertEquals(5 + 2048, charStream.bufline.length,
                "bufline length should also be expanded.");
        assertEquals(5 + 2048, charStream.bufcolumn.length,
                "bufcolumn length should also be expanded.");

        // The relevant portion [tokenBegin..bufsize) from the old array
        // should now appear at [0..(bufsize - tokenBegin)) in the new array
        // so [2..5) => length=3 => should be new array indices [0..3).
        // old char buffer was [a, b, c, d, e]
        // we copy c,d,e => [0,1,2] in the new buffer
        assertEquals('c', charStream.buffer[0], "Should have copied old buffer[2] => new[0]");
        assertEquals('d', charStream.buffer[1], "Should have copied old buffer[3] => new[1]");
        assertEquals('e', charStream.buffer[2], "Should have copied old buffer[4] => new[2]");

        // Check lines, columns similarly
        assertEquals(12, charStream.bufline[0], "bufline old[2] => new[0]");
        assertEquals(13, charStream.bufline[1], "bufline old[3] => new[1]");
        assertEquals(14, charStream.bufline[2], "bufline old[4] => new[2]");

        assertEquals(22, charStream.bufcolumn[0], "bufcolumn old[2] => new[0]");
        assertEquals(23, charStream.bufcolumn[1], "bufcolumn old[3] => new[1]");
        assertEquals(24, charStream.bufcolumn[2], "bufcolumn old[4] => new[2]");

        // According to code, maxNextCharInd = (bufpos -= tokenBegin)
        // old bufpos=4, tokenBegin=2 => new bufpos=2 => also new maxNextCharInd=2
        assertEquals(2, charStream.bufpos,
                "bufpos should have been decreased by tokenBegin => 4-2=2.");
        assertEquals(charStream.bufpos, charStream.maxNextCharInd,
                "maxNextCharInd should match the new bufpos in no-wrap scenario.");

        // The code sets tokenBegin=0 at the end
        assertEquals(0, charStream.tokenBegin,
                "After expansion, tokenBegin should be reset to 0.");
        // bufsize is also increased by 2048 => 5+2048 => 2053
        assertEquals(5 + 2048, charStream.bufsize,
                "bufsize should now be old bufsize + 2048 => 2053");
        // available is set to bufsize
        assertEquals(charStream.bufsize, charStream.available,
                "available should be updated to new bufsize.");
    }

    /**
     * Test ExpandBuff(true) scenario (the "wrap-around" branch).
     * This is used when the buffer is used in a circular manner.
     */
    @Test
    public void testExpandBuffWrapAround() {
        // Setup initial fields:
        charStream.bufsize   = 5;
        charStream.available = 5;
        charStream.tokenBegin= 1;
        charStream.bufpos    = 2;
        // We'll imagine that the buffer has valid data from tokenBegin..end
        // and also from start..bufpos in a wrap scenario.

        // Original arrays:
        charStream.buffer   = new char[]   {'A', 'B', 'C', 'D', 'E'};
        charStream.bufline  = new int[]    {  1,  2,  3,  4,  5 };
        charStream.bufcolumn= new int[]    { 10, 20, 30, 40, 50 };

        // In wrap-around, the code copies [tokenBegin..end) to new[0..(bufsize-tokenBegin))
        // and [0..bufpos) to new[(bufsize-tokenBegin)..(bufsize-tokenBegin+bufpos)].
        // Then updates bufpos += (bufsize - tokenBegin).

        charStream.ExpandBuff(true);

        // Check new buffer lengths
        int expectedLen = 5 + 2048;
        assertEquals(expectedLen, charStream.buffer.length);
        assertEquals(expectedLen, charStream.bufline.length);
        assertEquals(expectedLen, charStream.bufcolumn.length);

        // old tokenBegin=1, old bufsize=5 => (bufsize - tokenBegin)=4
        // so we copy old buffer[1..4] => new[0..3] => (4 elements)
        // then old buffer[0..2) => new[4..6)
        // but note: old bufpos=2 => that means we copy old buffer[0..2) => length=2 => new[4..6)? The code does that.
        // Then bufpos += (bufsize - tokenBegin) => new bufpos=2+(5-1)=6 => maxNextCharInd=6

        // Let's verify the first portion new[0..(bufsize - tokenBegin)=4)
        // old buffer[1], [2], [3], [4] => 'B','C','D','E'
        assertEquals('B', charStream.buffer[0]);
        assertEquals('C', charStream.buffer[1]);
        assertEquals('D', charStream.buffer[2]);
        assertEquals('E', charStream.buffer[3]);

        // Next portion => old buffer[0..bufpos=2) => length=2 => 'A', 'B'?
        // Wait, we must re-check logic: old buffer[0..2) => indices 0,1 => 'A','B'.
        // That gets placed at new indices [4..4+2=6). => new[4], new[5]
        assertEquals('A', charStream.buffer[4]);
        assertEquals('B', charStream.buffer[5]);

        // Now confirm line and column arrays similarly
        // first portion => old line[1..4] => new[0..3], old col[1..4] => new[0..3]
        assertEquals(2, charStream.bufline[0]);
        assertEquals(3, charStream.bufline[1]);
        assertEquals(4, charStream.bufline[2]);
        assertEquals(5, charStream.bufline[3]);

        assertEquals(20, charStream.bufcolumn[0]);
        assertEquals(30, charStream.bufcolumn[1]);
        assertEquals(40, charStream.bufcolumn[2]);
        assertEquals(50, charStream.bufcolumn[3]);

        // second portion => old line[0..2) => new line[4..6), old col[0..2) => new col[4..6)
        // i.e. line[0], line[1] => new line[4], line[5]
        assertEquals(1, charStream.bufline[4]);
        assertEquals(2, charStream.bufline[5]);

        assertEquals(10, charStream.bufcolumn[4]);
        assertEquals(20, charStream.bufcolumn[5]);

        // Now check the new bufpos => old was 2 => plus (bufsize - tokenBegin)=4 => new is 6
        assertEquals(6, charStream.bufpos);
        assertEquals(6, charStream.maxNextCharInd);

        // tokenBegin => 0
        assertEquals(0, charStream.tokenBegin);

        // bufsize => old + 2048 => 2053
        assertEquals(expectedLen, charStream.bufsize);
        assertEquals(expectedLen, charStream.available);
    }

    /**
     * (Optional) Test if an exception is thrown in the arraycopy code or something
     * => it goes to the catch block, which rethrows as Error(t.getMessage()).
     * It's tricky to cause a real arraycopy error unless we set negative indexes or something.
     * That might require an artificially corrupted state.
     */
    @Test
    public void testExpandBuffArrayCopyError() {
        // We'll artificially create a scenario with negative tokenBegin or something to provoke an error
        charStream.bufsize = 5;
        charStream.tokenBegin = -1; // invalid => arraycopy likely triggers an ArrayIndexOutOfBounds
        assertThrows(Error.class, () -> {
            charStream.ExpandBuff(false);
        }, "Should throw new Error(...) if arraycopy fails with negative tokenBegin.");
    }

    /**
     * 1) If maxNextCharInd < available, we skip the entire if block
     *    and directly do read(...). We'll simulate a successful read
     *    (i != -1), so we do maxNextCharInd += i.
     */
    @Test
    public void testFillBuffNoIfBlock() throws IOException {
        // Setup:
        charStream.maxNextCharInd = 2;
        charStream.available = 5;  // so maxNextCharInd < available => skip the big if
        // We can optionally check that fillBuff tries reading from inputStream

        // We'll override inputStream with a fake that returns 2 bytes read
        charStream.inputStream = new StringReader("abc") {
            // We'll simulate read(char[] cbuf, int off, int len)
            // The default StringReader doesn't directly do partial reads,
            // but let's rely on the real StringReader for simplicity
        };

        // Call FillBuff()
        charStream.FillBuff();

        // We expect that the big if-block is skipped, we read up to 'available - maxNextCharInd' = 3 =>
        // If the read was partial or full depends on the actual data in "abc".
        // Typically, i might be 3 or 2.
        // The key is that no branch changed 'available' or 'bufsize' etc.
        // We check that maxNextCharInd > 2 if data read
        assertTrue(charStream.maxNextCharInd > 2, "We expect some data to be read, increasing maxNextCharInd.");
    }

    /**
     * 2) If read(...) returns -1, we close the stream and throw IOException.
     *    Ensure that scenario is covered.
     */
    @Test
    public void testFillBuffReadReturnsMinusOne() {
        // Force maxNextCharInd == available to go into the if-block,
        // but let's choose a path that doesn't do ExpandBuff => e.g. available > tokenBegin => sets available=bufsize
        charStream.maxNextCharInd = 5;
        charStream.available      = 5; // triggers if (maxNextCharInd==available)
        charStream.bufsize        = 5;
        charStream.tokenBegin     = 0; // so (available>tokenBegin) => available=bufsize => still 5

        // Then it goes beyond the if-block to read => we want read(...) to return -1
        charStream.inputStream = new Reader() {
            @Override
            public int read(char[] cbuf, int off, int len) {
                return -1; // simulate EOF
            }
            @Override
            public void close() {
                // we can track if it's called, but let's assume no error
            }
        };

        // we expect an IOException
        assertThrows(IOException.class, () -> charStream.FillBuff(),
                "If read(...) returns -1, method should throw IOException after closing inputStream.");
    }

    /**
     * 3) If an IOException is thrown while reading,
     *    we hit the catch => --bufpos; backup(0); if tokenBegin==-1 => tokenBegin=bufpos => throw e.
     */
    @Test
    public void testFillBuffCatchIOException() {
        // Make sure maxNextCharInd < available or not? Actually, let's do maxNextCharInd<available
        // so we skip the big if block => we do read => it throws IOException => we catch => do the final stuff
        charStream.maxNextCharInd = 0;
        charStream.available      = 5; // skip if block

        // We'll set some initial positions
        charStream.bufpos = 2;
        charStream.tokenBegin = -1; // so we can check that logic if (tokenBegin==-1) => tokenBegin=bufpos

        // Provide an inputStream that throws an IOException
        charStream.inputStream = new Reader() {
            public int read(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("Simulated I/O error");
            }
            public void close() {}
        };

        // We expect the code to catch => do --bufpos => backup(0) => tokenBegin=2 => rethrow
        IOException ex = assertThrows(IOException.class, () -> {
            charStream.FillBuff();
        }, "Should rethrow the same IOException after the catch block modifications.");

        assertEquals("Simulated I/O error", ex.getMessage());
        // Check that we did --bufpos => from 2 => 1
        assertEquals(1, charStream.bufpos, "bufpos should have decremented by 1 in the catch block.");

        // tokenBegin was -1 => after catch, set to bufpos => now 1
        assertEquals(1, charStream.tokenBegin,
                "If tokenBegin was -1, we set tokenBegin=bufpos in the catch block.");

        // backup(0) was called => if there's a side effect or we can check it, do so
        // for now we assume it's tested or doesn't matter
    }







    /**
     * 7) else if (available> tokenBegin) => sets available=bufsize
     *    We'll do maxNextCharInd=available => but available < bufsize => no => that won't hold
     *    Actually, let's re-check: we want available != bufsize,
     *    so the first 'if(available==bufsize)' is false,
     *    then 'else if(available>tokenBegin)' => sets available=bufsize
     */
    @Test
    public void testFillBuffBranchAvailableGreaterThanTokenBegin() throws IOException {
        // so maxNextCharInd==available => let's pick => 3
        charStream.maxNextCharInd = 3;
        charStream.available      = 3;  // ensures the big if triggers
        charStream.bufsize        = 5;  // but available != bufsize => skip that sub-branch
        charStream.tokenBegin     = 2;  // => 3 > 2 => triggers => available=bufsize => 5

        // We'll not read anything => set inputStream minimal
        charStream.inputStream = new StringReader("some data");

        charStream.FillBuff();

        assertEquals(5, charStream.available,
                "Should set available=bufsize => 5, because available>tokenBegin");
    }




}
