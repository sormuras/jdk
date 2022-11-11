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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

record JavaToolClient(Configuration configuration) {

    int run() throws Exception {
        ensurePortalIsRunning();

        var portfile = configuration.portfile();
        if (Files.notExists(portfile)) throw new AssertionError("[JavaToolClient] Port file not found: " + portfile);
        var port = Integer.parseInt(Files.readString(portfile));

        var out = System.out;
        var err = System.err;

        var localhost = InetAddress.getByName(null);
        var socketAddress = new InetSocketAddress(localhost, port);

        try (var channel = SocketChannel.open()) {
            var connected = channel.connect(socketAddress);
            if (!connected) throw new RuntimeException("[JavaToolClient] Connect failed: " + socketAddress);
            // Send arguments
            SocketChannelSupport.writeStrings(channel, List.of(configuration.args()));
            // Read status code
            var status = SocketChannelSupport.readInt(channel);
            // Read strings
            var strings = SocketChannelSupport.readStrings(channel);
            out.print(strings.get(0));
            err.print(strings.get(1));
            out.flush();
            err.flush();
            return status;
        }
    }

    void ensurePortalIsRunning() throws Exception {
        var portfile = configuration.portfile();
        if (Files.exists(portfile)) return;
        var starting = portfile.resolveSibling("server.port.starting");
        try {
            Files.createFile(starting);
            var command = new ArrayList<>(configuration.servercmd());
            command.add(1, "-XX:StartFlightRecording:filename=../build/recording.jfr,dumponexit=true,maxsize=1GB");
            command.add(configuration.arg0());            
            var builder = new ProcessBuilder(command).inheritIO();
            var process = builder.start();
            System.out.println("[JavaToolClient] Starting Java Tool Portal... " + process.info());
            pollForFileBeingCreated(portfile);
            Files.delete(starting);
        } catch (FileAlreadyExistsException exception) {
            System.out.println("[JavaToolClient] Waiting for Java Tool Portal coming up...");
            pollForFileBeingCreated(portfile);
        }
    }

    void pollForFileBeingCreated(Path file) throws Exception {
        var counter = 0;
        while (Files.notExists(file)) {
            Thread.sleep(333);
            counter++;
            if (counter > 10) throw new java.io.FileNotFoundException(file.toString());
        }
    }
}
