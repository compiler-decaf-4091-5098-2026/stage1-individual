package decaf;

import java.nio.file.Path;

/** Options available in stage 1. Later compilation targets are intentionally rejected. */
public record CommandLine(String target, Path input, Path output, boolean debug, boolean help) {
    public static final String USAGE = """
            Usage: ./run.sh [options] <file>
              -t, --target scan|parse  Stop after scanning or parsing (default: parse)
              -o, --output <file>      Write tokens or the parse tree to a file
              -d, --debug              Print the parse tree on successful parsing
              -O, --opt [all]          No optimizations exist in stage 1
              -h, --help               Show this help
            """;

    public static CommandLine parse(String[] args) {
        String target = "parse";
        Path input = null;
        Path output = null;
        boolean debug = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h", "--help" -> {
                    return new CommandLine(target, null, null, debug, true);
                }
                case "-d", "--debug" -> debug = true;
                case "-t", "--target" -> {
                    target = value(args, ++i, "--target");
                    if (!target.equals("scan") && !target.equals("parse")) {
                        throw new IllegalArgumentException("Stage 1 supports only scan and parse");
                    }
                }
                case "-o", "--output" -> output = Path.of(value(args, ++i, "--output"));
                case "-O", "--opt" -> {
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        String optimizations = args[++i];
                        if (!optimizations.equals("all")) {
                            throw new IllegalArgumentException("No optimizations exist in stage 1");
                        }
                    }
                }
                default -> {
                    if (args[i].startsWith("-")) {
                        throw new IllegalArgumentException("Unknown option: " + args[i]);
                    }
                    if (input != null) {
                        throw new IllegalArgumentException("Specify exactly one input file");
                    }
                    input = Path.of(args[i]);
                }
            }
        }
        if (input == null) {
            throw new IllegalArgumentException("Specify an input file");
        }
        return new CommandLine(target, input, output, debug, false);
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length || args[index].startsWith("-")) {
            throw new IllegalArgumentException("Missing value for " + option);
        }
        return args[index];
    }
}
