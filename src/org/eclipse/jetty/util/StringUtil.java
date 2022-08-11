//
// ========================================================================
// Copyright (c) 1995-2022 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fast String Utilities.
 *
 * These string utilities provide both convenience methods and
 * performance improvements over most standard library versions. The
 * main aim of the optimizations is to avoid object creation unless
 * absolutely required.
 */
public class StringUtil {
    /**
     * Replace chars within string.
     * <p>
     * Fast replacement for {@code java.lang.String#}{@link String#replace(char, char)}
     * </p>
     *
     * @param str the input string
     * @param find the char to look for
     * @param with the char to replace with
     * @return the now replaced string
     */
    public static String replace(String str, char find, char with)
    {
        if (str == null)
            return null;

        if (find == with)
            return str;

        int c = 0;
        int idx = str.indexOf(find, c);
        if (idx == -1)
        {
            return str;
        }
        char[] chars = str.toCharArray();
        int len = chars.length;
        for (int i = idx; i < len; i++)
        {
            if (chars[i] == find)
                chars[i] = with;
        }
        return String.valueOf(chars);
    }

    /**
     * Replace substrings within string.
     * <p>
     * Fast replacement for {@code java.lang.String#}{@link String#replace(CharSequence, CharSequence)}
     * </p>
     *
     * @param s the input string
     * @param sub the string to look for
     * @param with the string to replace with
     * @return the now replaced string
     */
    public static String replace(String s, String sub, String with)
    {
        if (s == null)
            return null;

        int c = 0;
        int i = s.indexOf(sub, c);
        if (i == -1)
        {
            return s;
        }
        StringBuilder buf = new StringBuilder(s.length() + with.length());
        do
        {
            buf.append(s, c, i);
            buf.append(with);
            c = i + sub.length();
        }
        while ((i = s.indexOf(sub, c)) != -1);
        if (c < s.length())
        {
            buf.append(s.substring(c));
        }
        return buf.toString();
    }

    public static String toHexString(byte b)
    {
        return toHexString(new byte[]{b}, 0, 1);
    }

    public static String toHexString(byte[] b)
    {
        return toHexString(Objects.requireNonNull(b, "ByteBuffer cannot be null"), 0, b.length);
    }

    public static String toHexString(byte[] b, int offset, int length)
    {
        StringBuilder buf = new StringBuilder();
        for (int i = offset; i < offset + length; i++)
        {
            int bi = 0xff & b[i];
            int c = '0' + (bi / 16) % 16;
            if (c > '9')
                c = 'A' + (c - '0' - 10);
            buf.append((char)c);
            c = '0' + bi % 16;
            if (c > '9')
                c = 'a' + (c - '0' - 10);
            buf.append((char)c);
        }
        return buf.toString();
    }

    public static void toHex(byte b, Appendable buf)
    {
        try
        {
            int d = 0xf & ((0xF0 & b) >> 4);
            buf.append((char)((d > 9 ? ('A' - 10) : '0') + d));
            d = 0xf & b;
            buf.append((char)((d > 9 ? ('A' - 10) : '0') + d));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
