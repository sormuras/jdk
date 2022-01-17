package javatoolportal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * A data structure for {@code _the.MODULE-NAME-server.conf} files.
 *
 * @param arg0      the initial argument starting with {@code --server:conf=}
 * @param portfile  the regular file used by the Unix Domain Socket infrastructure
 * @param servercmd the entire platform-specific command spawning a new server process
 * @param args      the remainder of the arguments
 */
record Configuration(String arg0, Path portfile, List<String> servercmd, String[] args) {
    /**
     * Read {@code --server:conf=} option from an array of arguments at index {@code 0}.
     *
     * @param args the array of arguments to read in
     * @return a configuration object
     */
    static Configuration of(String... args) {
        var configurationMap = new HashMap<String, String>();
        var configurationFile = Path.of(args[0].substring("--server:conf=".length()));
        try {
            Files.readAllLines(configurationFile).forEach(parseLineInto(configurationMap));
        } catch (Exception exception) {
            throw new RuntimeException("Reading lines failed: " + configurationFile);
        }
        var portfile = Path.of(configurationMap.get("portfile"));
        var servercmd = List.of(configurationMap.get("servercmd").split(" "));
        var remainder = new String[args.length - 1];
        System.arraycopy(args, 1, remainder, 0, remainder.length);
        return new Configuration(args[0], portfile, servercmd, remainder);
    }

    private static Consumer<String> parseLineInto(HashMap<String, String> map) {
        return line -> {
            var separator = line.indexOf('=');
            var key = line.substring(0, separator).strip();
            var value = line.substring(separator + 1).strip();
            map.put(key, value);
        };
    }
}
