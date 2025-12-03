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
