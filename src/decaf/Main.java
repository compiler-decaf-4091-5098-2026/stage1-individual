package decaf;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        System.exit(run(args, new PrintWriter(System.out, true), new PrintWriter(System.err, true)));
    }

    public static int run(String[] args, PrintWriter stdout, PrintWriter stderr) {
        String filename = null, output = null, target = "parse";
        boolean debug = false;
        try {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "-h", "--help" -> {
                        stdout.println("Usage: run filename [-t scan|parse] [-o output] [-d] [-O [all]]");
                        return 0;
                    }
                    case "-t", "--target" -> target = value(args, ++i, arg);
                    case "-o", "--output" -> output = value(args, ++i, arg);
                    case "-d", "--debug" -> debug = true;
                    case "-O", "--opt" -> {
                        if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                            String optimizations = args[++i];
                            if (!optimizations.equals("all"))
                                throw new IllegalArgumentException("no named optimizations are supported at stage 1");
                        }
                    }
                    default -> {
                        if (arg.startsWith("-")) throw new IllegalArgumentException("unknown option: " + arg);
                        if (filename != null) throw new IllegalArgumentException("exactly one input filename is required");
                        filename = arg;
                    }
                }
            }
            if (filename == null) throw new IllegalArgumentException("input filename is required");
            if (!target.equals("scan") && !target.equals("parse"))
                throw new IllegalArgumentException("stage 1 supports only scan and parse targets");
            Path inputPath = Path.of(filename);
            if (output != null) {
                Path outputPath = Path.of(output);
                if (inputPath.toAbsolutePath().normalize().equals(outputPath.toAbsolutePath().normalize())
                    || Files.exists(outputPath) && Files.isSameFile(inputPath, outputPath))
                    throw new IllegalArgumentException("output file must differ from input file");
            }
            String source = Files.readString(inputPath, StandardCharsets.UTF_8);
            Diagnostics diagnostics = new Diagnostics(filename, stderr);
            if (output == null) return compile(source, target, debug, diagnostics, stdout, stderr);
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Path.of(output), StandardCharsets.UTF_8))) {
                int result = compile(source, target, debug, diagnostics, writer, stderr);
                if (writer.checkError()) throw new IOException("could not write output file");
                return result;
            }
        } catch (IllegalArgumentException | IOException exception) {
            stderr.println("error: " + exception.getMessage());
            stderr.flush();
            return 2;
        }
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length || args[index].startsWith("-"))
            throw new IllegalArgumentException("missing value for " + option);
        return args[index];
    }

    private static int compile(String source, String target, boolean debug, Diagnostics diagnostics,
                               PrintWriter output, PrintWriter stderr) {
        Lexer lexer = new Lexer(source, diagnostics);
        ArrayList<Token> tokens = new ArrayList<>();
        while (true) {
            Token token = lexer.next();
            tokens.add(token);
            if (token.kind().equals("EOF")) break;
            if (target.equals("scan")) {
                output.println(token.listing());
                output.flush();
            }
        }
        if (target.equals("parse") && diagnostics.count() == 0) {
            Parser parser = new Parser(tokens, diagnostics);
            parser.parse();
        }
        if (debug) {
            stderr.printf("debug: %d tokens, %d error(s)%n", tokens.size() - 1, diagnostics.count());
        }
        output.flush(); stderr.flush();
        return diagnostics.count() == 0 ? 0 : 1;
    }
}
