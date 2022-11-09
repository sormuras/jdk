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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.*;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.ArrayDeque;
import java.util.List;
import java.util.spi.ToolProvider;

class JavaToolPortal {

    public static void main(String... args) {
        var portal = new JavaToolPortal();
        var out = new PrintWriter(System.out, true);
        var err = new PrintWriter(System.err, true);
        System.exit(portal.run(out, err, args));
    }

    public int run(PrintWriter out, PrintWriter err, String... args) {
        out.println("Portal started in directory: " + Path.of("").toAbsolutePath());
        if (args.length != 1) {
            err.println("Usage: JavaToolPortal SOCKET-PATH");
            return 1;
        }
        try {
            var socketPath = Path.of(args[0]).toAbsolutePath();
            var localhost = InetAddress.getByName(null);
            var socketAddress = new InetSocketAddress(localhost, 0);
            try {
                var server = new Server(out, err, socketAddress);
                var serverSocketChannel = server.bind();
                Files.writeString(socketPath, "" + serverSocketChannel.socket().getLocalPort());
                Files.deleteIfExists(socketPath.resolveSibling("server.port.starting"));
                out.println("Portal bound to: " + serverSocketChannel.socket());
                new Thread(new Stopper(serverSocketChannel, socketPath.getParent())).start();
                Runtime.getRuntime()
                        .addShutdownHook(new Thread(new Closer(serverSocketChannel, socketPath)));
                server.serve(serverSocketChannel); // accept pending and future connections
                return 0;
            } catch (BindException exception) {
                err.println(exception);
                return 2;
            } finally {
                Files.deleteIfExists(socketPath);
            }
        } catch (Exception exception) {
            exception.printStackTrace(err);
            return 3;
        }
    }

    record Stopper(ServerSocketChannel serverSocketChannel, Path directory) implements Runnable {
        public void run() {
            try {
                var watchService
                        = FileSystems.getDefault().newWatchService();

                directory.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);

                WatchKey key;
                while ((key = watchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        var file = directory.resolve(event.context().toString());
                        if ("server.port.stop".equals(file.getFileName().toString())) {
                            serverSocketChannel.close();
                            Files.deleteIfExists(file);
                        }
                    }
                    key.reset();
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    record Closer(ServerSocketChannel serverSocketChannel, Path socketPath) implements Runnable {
        @Override
        public void run() {
            try {
                serverSocketChannel.close();
                Files.deleteIfExists(socketPath);
            } catch (Exception exception) {
                // ignore
            }
        }
    }

    record Server(PrintWriter out, PrintWriter err, SocketAddress socketAddress) {

        ServerSocketChannel bind() throws Exception {
            var serverChannel = ServerSocketChannel.open();
            serverChannel.bind(socketAddress);
            return serverChannel;
        }

        void serve(ServerSocketChannel serverSocketChannel) throws Exception {
            int counter = 0;
            int timeouts = 0;
            try {
                serverSocketChannel.socket().setSoTimeout(60 * 1000);
                while (serverSocketChannel.isOpen()) {
                    try {
                        var channel = serverSocketChannel.accept();
                        counter++;
                        timeouts = 0;
                        new Thread(new Handler(out, err, channel)).start();
                    } catch (SocketTimeoutException timeout) {
                        timeouts++;
                        if (timeouts >= 3) serverSocketChannel.close();
                    }
                }
            } catch (AsynchronousCloseException exception) {
                // fall-through
            }
            out.printf("Portal closed after handling %d request(s)%n", counter);
        }
    }

    record Handler(PrintWriter out, PrintWriter err, SocketChannel channel) implements Runnable {

        @Override
        public void run() {
            try (channel) {
                // Read arguments
                var arguments = new ArrayDeque<>(SocketChannelSupport.readStrings(channel));

                // Run tool
                var tool = arguments.removeFirst();
                var args = arguments.toArray(String[]::new);
                var normal = new StringWriter();
                var errors = new StringWriter();
                var code =
                        ToolProvider.findFirst(tool)
                                .orElseThrow()
                                .run(new PrintWriter(normal, true), new PrintWriter(errors, true), args);

                // Write status code and output streams
                SocketChannelSupport.writeInt(channel, code); // 4
                SocketChannelSupport.writeStrings(channel, List.of(normal.toString(), errors.toString()));
            } catch (Exception exception) {
                err.println(exception);
            }
        }
    }
}
