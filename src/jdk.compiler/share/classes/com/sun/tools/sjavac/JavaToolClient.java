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

package com.sun.tools.sjavac;

import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.spi.ToolProvider;

public class JavaToolClient {

    public static void main(String... args) throws Exception {
        var configuration = Configuration.of(args);

        if (configuration.args.length == 0) {
            JavaToolPortal.main(configuration.portfile().toString());
            return;
        }

        System.exit(run(configuration));
    }

    static int run(Configuration configuration) throws Exception {
        if (Files.notExists(configuration.portfile())) {
            var command = new ArrayList<String>(configuration.servercmd());
            command.add(configuration.arg0());
            var builder = new ProcessBuilder(command).inheritIO();

            var process = builder.start();
            Thread.sleep(1234);
        }
        if (Files.notExists(configuration.portfile())) {
            // Server not started, fall back to cold run in this VM ... or fail?
            return runCold(configuration);
        }
        return runHot(configuration);
    }

    static int runCold(Configuration configuration) {
        var out = System.out;
        var err = System.err;

        out.printf("[COLD] Run javac with %d args...%n", configuration.args().length);

        var javac = ToolProvider.findFirst("javac").orElseThrow();
        return javac.run(out, err, configuration.args());
    }

    static int runHot(Configuration configuration) throws Exception {
        var out = System.out;
        var err = System.err;

        out.printf("[HOT] Run javac with %d args...%n", configuration.args().length);

        var socketPath = configuration.portfile();
        var socketAddress = UnixDomainSocketAddress.of(socketPath);

        try (var channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
            var connected = channel.connect(socketAddress);
            if (!connected) throw new RuntimeException("Connect failed: " + socketAddress);
            // Prepare arguments
            var arguments = new ArrayList<String>();
            arguments.add("javac");
            arguments.addAll(List.of(configuration.args()));
            // Send arguments
            SocketChannelSupport.writeStrings(channel, arguments);
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

    record Configuration(String arg0, Path portfile, List<String> servercmd, String[] args) {
        static Configuration of(String... args) throws Exception {
            var configurationMap = new HashMap<String, String>();
            var configurationFile = Path.of(args[0].substring("--server:conf=".length()));
            Files.readAllLines(configurationFile).forEach(
                    line -> {
                        var separator = line.indexOf('=');
                        var key = line.substring(0, separator).strip();
                        var value = line.substring(separator + 1).strip();
                        configurationMap.put(key, value);
                    }
            );
            var portfile = Path.of(configurationMap.get("portfile"));
            var servercmd = List.of(configurationMap.get("servercmd").split(" "));
            var toolargs = new String[args.length - 1];
            System.arraycopy(args, 1, toolargs, 0, toolargs.length);
            return new Configuration(args[0], portfile, servercmd, toolargs);
        }
    }
}
