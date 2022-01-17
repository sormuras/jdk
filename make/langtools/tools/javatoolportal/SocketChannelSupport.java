/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javatoolportal;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Static helpers reading from and writing onto a {@link SocketChannel}.
 */
class SocketChannelSupport {

    static int readInt(SocketChannel channel) throws Exception {
        var buffer = ByteBuffer.allocate(4);
        while (buffer.hasRemaining()) {
            if (channel.read(buffer) < 0) throw new RuntimeException();
        }
        return buffer.getInt(0); // no flip required
    }

    static List<String> readStrings(SocketChannel channel) throws Exception {
        var capacity = readInt(channel);
        var argsBuffer = ByteBuffer.allocate(capacity);
        while (argsBuffer.hasRemaining()) {
            if (channel.read(argsBuffer) < 0) throw new RuntimeException();
        }
        argsBuffer.flip();
        var size = argsBuffer.getInt();
        var list = new ArrayList<String>(size); // 4
        for (int i = 0; i < size; i++) {
            var bytes = new byte[argsBuffer.getInt()]; // 4 * n
            argsBuffer.get(bytes); // M * n
            list.add(new String(bytes));
        }
        return List.copyOf(list);
    }

    static void writeInt(SocketChannel channel, int i) throws Exception {
        var buffer = ByteBuffer.allocate(4);
        buffer.putInt(i);
        buffer.flip();
        channel.write(buffer);
    }

    static void writeStrings(SocketChannel channel, Iterable<String> strings) throws Exception {
        var list = new ArrayList<byte[]>();
        var size = 0;
        for (var string : strings) {
            var bytes = string.getBytes();
            list.add(bytes);
            size += bytes.length;
        }
        var capacity = 4 + 4 * list.size() + size;
        var buffer = ByteBuffer.allocate(capacity);
        buffer.putInt(list.size()); // 4
        for (var bytes : list) {
            buffer.putInt(bytes.length); // 4 * n
            buffer.put(bytes); // M * n
        }
        var data = buffer.flip();
        writeInt(channel, data.remaining());
        channel.write(data);
    }
}
