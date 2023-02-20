/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 * @test
 * @bug 8304400
 * @summary Test multi-file source-code launcher
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.launcher
 *          jdk.compiler/com.sun.tools.javac.main
 * @run junit MultiFileSourceLauncherTests
 */

import java.nio.file.*;
import java.util.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

class MultiFileSourceLauncherTests {
    @Test
    void testHelloWorldInTwoCompilationUnits(@TempDir Path base) throws Exception {
        var hello = Files.writeString(base.resolve("Hello.java"),
                """
                interface Hello {
                  static void main(String... args) {
                    System.out.println("Hello " + new World("Noname"));
                  }
                }
                """);
        var world = Files.writeString(base.resolve("World.java"),
                """
                record World(String name) {}
                """);

        // TODO Capture and check output
        var launcher = new com.sun.tools.javac.launcher.Main(System.out);
        launcher.run(
                new String[0],
                List.of(hello.toString()).toArray(String[]::new));
    }

    @Test
    void testHelloWorldMissingSecondUnit(@TempDir Path base) throws Exception {
        var hello = Files.writeString(base.resolve("Hello.java"),
                """
                interface Hello {
                  static void main(String... args) {
                    System.out.println("Hello " + new World("Noname"));
                  }
                }
                """);

        // TODO Capture and check output
        var launcher = new com.sun.tools.javac.launcher.Main(System.out);
        Assertions.assertThrows(
                com.sun.tools.javac.launcher.Main.Fault.class,
                () -> launcher.run(
                    new String[0],
                    List.of(hello.toString()).toArray(String[]::new)));
    }
}
