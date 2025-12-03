/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.module;

import module java.base;

public final class ExternalModuleFinder implements ModuleFinder {
    private final Path file;
    private final Path folder;
    private final Properties links;
    private boolean loaded;

    public ExternalModuleFinder(Path file) {
        this.file = file;
        this.folder = file.getParent();
        this.links = new Properties();
        this.loaded = false;
    }

    @Override
    public Optional<ModuleReference> find(String name) {
        loadLinks();
        if (links.containsKey(name)) {
            retrieveModularJarFileFor(name);
            return ModuleFinder.of(folder).find(name);
        }
        return Optional.empty();
    }

    @Override
    public Set<ModuleReference> findAll() {
        loadLinks();
        return links.stringPropertyNames().stream()
                .map(this::find)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
    }

    private void loadLinks() {
        if (loaded) return;
        try {
            links.load(new StringReader(Files.readString(file)));
            links.stringPropertyNames().stream()
                    .filter(name -> name.startsWith("@"))
                    .forEach(links::remove);
        } catch (Exception exception) {
            throw new RuntimeException("Loading links failed for: " + file, exception);
        } finally {
            loaded = true;
        }
    }

    private void retrieveModularJarFileFor(String name) {
        var target = folder.resolve(name + ".jar");
        if (Files.exists(target)) return;
        var source = links.getProperty(name);
        if (source == null) throw new FindException("Module not linked: " + name);
        try (var stream = URI.create(source).toURL().openStream()) {
            Files.createDirectories(folder);
            Files.copy(stream, target);
        } catch (IOException cause) {
            try {
                Files.deleteIfExists(target);
            } catch (Exception ignore) {
            }
            throw new UncheckedIOException(cause);
        }
    }
}
