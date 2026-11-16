package decaf;

import decaf.parser.DecafLexer;
import decaf.parser.DecafParser;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class FrontendTest {
    @TempDir
    Path temp;

    record Result(int status, String out, String err) {}

    private Result run(String... args) {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        int status = DecafCompiler.run(args,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Result(status, out.toString(StandardCharsets.UTF_8),
                err.toString(StandardCharsets.UTF_8));
    }

    private Path source(String text) throws Exception {
        Path file = Files.createTempFile(temp, "source-", ".decaf");
        Files.writeString(file, text);
        return file;
    }

    @TestFactory
    Stream<DynamicTest> examples() throws Exception {
        List<Path> paths;
        try (var files = Files.list(Path.of("examples"))) {
            paths = files.filter(p -> p.toString().endsWith(".decaf")).sorted().toList();
        }
        return paths.stream().map(path -> DynamicTest.dynamicTest(path.toString(), () -> {
            boolean invalid = path.getFileName().toString().startsWith("invalid");
            Result result = run(path.toString());
            assertEquals(invalid ? 1 : 0, result.status(), result.err());
            assertEquals("", result.out());
            if (invalid) {
                assertTrue(result.err().contains(path + ":"));
            } else {
                assertEquals("", result.err());
            }
        }));
    }

    @TestFactory
    Stream<DynamicTest> lexicalErrorsFailEvenInScanMode() {
        return Stream.of("@", "0x", "\"bad\\q\"", "\"unterminated", "\"tail\\",
                "\"line\nbreak\"", "\"apostrophe'\"", "\"кириллица\"", "'ab'", "''",
                "'\\q'", "'tail\\", "'unterminated", "/*", "/**", "/***", "/* text *")
                .map(text -> DynamicTest.dynamicTest(text, () -> {
                    Result result = run("-t", "scan", source(text).toString());
                    assertEquals(1, result.status(), result.out());
                    assertFalse(result.err().isEmpty());
                    assertEquals("", result.out());
                }));
    }

    @TestFactory
    Stream<DynamicTest> syntacticallyInvalidPrograms() {
        return Stream.of(
                "int x = 1;",
                "void main() { if (true) return; }",
                "void main() { {} }",
                "void main() { int x[1L]; }",
                "void main() { int x[2][3]; }",
                "void main(int a[]) {}",
                "void main() { int x; x = len(x + 1); }",
                "void main() { int x; x = \"text\"; }",
                "void main() { int x; x = x++; }",
                "void main() { int x; x = 1 + ; }",
                "void main() { int x; ++x; }",
                "void main() { for (;;) {} }",
                "void main() {} import printf;",
                "void main() {} int x;")
                .map(text -> DynamicTest.dynamicTest(text, () ->
                        assertEquals(1, run(source(text).toString()).status())));
    }

    @TestFactory
    Stream<DynamicTest> commentsAndUnboundedNumbers() {
        return Stream.of("/**/", "/***/", "/****/", "/* * / ** */", "/* /* nested opener */",
                "// last line", "void main() {} // last line",
                "void main() { int x; x = 123456789123456789123456789; }",
                "void main() { int x; x = -!true; }")
                .map(text -> DynamicTest.dynamicTest(text, () ->
                        assertEquals(0, run(source(text).toString()).status())));
    }

    @Test
    void scannerUsesMaximalMunchAndSkipsComments() throws Exception {
        Path input = source("if IF intfortrue true false 0xAf 123L 0xFFL += ++ / /**/ // comment");
        Result result = run("--target", "scan", input.toString());
        assertEquals(0, result.status(), result.err());
        List<String> types = result.out().lines().map(line -> line.split(" ", 3)[1]).toList();
        assertEquals(List.of("IF", "ID", "ID", "TRUE", "FALSE", "INT_LITERAL",
                "LONG_LITERAL", "LONG_LITERAL", "PLUS_ASSIGN", "INCREMENT", "SLASH"), types);
        assertTrue(result.out().startsWith("1:1 IF if\n"));
    }

    @Test
    void parseModeIsSilentUnlessDebugOrOutputWasRequested() throws Exception {
        Path input = source("void main() {}");
        assertEquals(new Result(0, "", ""), run(input.toString()));
        Result debug = run("--debug", input.toString());
        assertEquals(0, debug.status());
        assertTrue(debug.out().startsWith("(program "));
        assertTrue(run("-d", input.toString()).out().contains("methodDecl"));
        Path output = temp.resolve("tree.txt");
        Result file = run(input.toString(), "-o", output.toString());
        assertEquals(new Result(0, "", ""), file);
        assertEquals(debug.out(), Files.readString(output));
    }

    @Test
    void scannerDoesNotParseAndCanWriteToFile() throws Exception {
        Path input = source("int"); // A valid token, but not a complete declaration.
        Result scan = run("-t", "scan", input.toString());
        assertEquals(0, scan.status());
        assertEquals(1, run("-t", "parse", input.toString()).status());
        Path output = temp.resolve("tokens.txt");
        assertEquals(0, run("-t", "scan", "--output", output.toString(), input.toString()).status());
        assertEquals(scan.out(), Files.readString(output));
    }

    @Test
    void failuresDoNotOverwriteAnOutputFile() throws Exception {
        Path output = temp.resolve("output.txt");
        Files.writeString(output, "keep");
        for (String text : List.of("void main() {", "void main() {} @")) {
            Result result = run(source(text).toString(), "-o", output.toString());
            assertEquals(1, result.status());
            assertEquals("keep", Files.readString(output));
            assertEquals("", result.out());
        }
    }

    @Test
    void reportsIoErrors() throws Exception {
        assertEquals(1, run(temp.resolve("missing.decaf").toString()).status());
        assertEquals(1, run(source("").toString(), "-o", temp.toString()).status());
    }

    @Test
    void validatesOptions() throws Exception {
        String input = source("").toString();
        assertEquals(0, run("--help").status());
        assertEquals(2, run().status());
        assertEquals(2, run("--unknown").status());
        assertEquals(2, run(input, input).status());
        assertEquals(2, run(input, "-o").status());
        assertEquals(2, run(input, "-t").status());
        assertEquals(2, run(input, "-t", "assembly").status());
        assertEquals(2, run("\u0000").status());
        assertEquals(2, run(input, "-O", "cse").status());
        assertEquals(0, run(input, "-O").status());
        assertEquals(0, run("-O", "all", input).status());
    }

    @Test
    void treePreservesPrecedenceAndLeftToRightOperandOrder() {
        DecafLexer lexer = new DecafLexer(CharStreams.fromString("1 + 2 * 3 < 8 == true && !false || false"));
        DecafParser parser = new DecafParser(new CommonTokenStream(lexer));
        var or = parser.expr().logicalOrExpr();
        assertEquals(0, parser.getNumberOfSyntaxErrors());
        assertEquals(2, or.logicalAndExpr().size());
        var and = or.logicalAndExpr(0);
        assertEquals(2, and.equalityExpr().size());
        var equality = and.equalityExpr(0);
        assertEquals(2, equality.relationalExpr().size());
        var relation = equality.relationalExpr(0);
        assertEquals(2, relation.additiveExpr().size());
        var sum = relation.additiveExpr(0);
        assertEquals("1", sum.term(0).getText());
        assertEquals("2*3", sum.term(1).getText());
        assertEquals("!false", and.equalityExpr(1).getText());

        parser = new DecafParser(new CommonTokenStream(new DecafLexer(CharStreams.fromString("9-3-1"))));
        var subtraction = parser.expr().logicalOrExpr().logicalAndExpr(0)
                .equalityExpr(0).relationalExpr(0).additiveExpr(0);
        assertEquals(List.of("9", "3", "1"), subtraction.term().stream().map(t -> t.getText()).toList());
    }
}
