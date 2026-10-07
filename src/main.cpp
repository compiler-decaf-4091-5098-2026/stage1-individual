#include "lexer.h"
#include "parser.h"

#include <iostream>
#include <stdexcept>
#include <string>

void printUsage(const char* prog) {
    std::cerr << "Usage: " << prog << " -t <phase> <source-file>\n"
              << "Phases:\n"
              << "  scan  - run the lexer and print tokens\n"
              << "  parse - run the parser (syntax check)\n";
}

void printLexerErrors(Lexer& lexer) {
    for (const LexerError& err : lexer.errors()) {
        std::cerr << lexer.filename() << ':' << err.position.line << ':' << err.position.column
                  << ": " << err.message << '\n';
    }
    lexer.clearErrors();
}

void printToken(const Token& token) {
    std::cout << token.position.line << ' ' << tokenToString(token) << '\n';
}

int runScan(const std::string& filename) {
    Lexer lexer(filename);
    bool  hadErrors = false;
    while (true) {
        Token token = lexer.next();
        if (lexer.hasErrors()) {
            hadErrors = true;
        }
        printLexerErrors(lexer);
        if (token.tokenType == TokenType::EndOfFile) {
            break;
        }
        printToken(token);
    }
    return hadErrors ? 1 : 0;
}

int runParse(const std::string& filename) {
    Lexer  lexer(filename);
    Parser parser(lexer, filename);

    std::unique_ptr<Program> program = parser.parse();
    (void)program;

    bool hadLexicalErrors = lexer.hasErrors();
    printLexerErrors(lexer);
    return (parser.hadErrors() || hadLexicalErrors) ? 1 : 0;
}

int main(int argc, char** argv) {
    std::string phase;
    std::string filename;

    for (int i = 1; i < argc; ++i) {
        std::string arg = argv[i];
        if (arg == "-t") {
            if (i + 1 >= argc) {
                std::cerr << "Error: -t requires an argument\n";
                printUsage(argv[0]);
                return 1;
            }
            phase = argv[++i];
        } else if (arg == "-h" || arg == "--help") {
            printUsage(argv[0]);
            return 0;
        } else if (!arg.empty() && arg[0] == '-') {
            std::cerr << "Error: unknown option '" << arg << "'\n";
            printUsage(argv[0]);
            return 1;
        } else {
            if (!filename.empty()) {
                std::cerr << "Error: multiple input files given\n";
                printUsage(argv[0]);
                return 1;
            }
            filename = arg;
        }
    }

    if (phase.empty()) {
        std::cerr << "Error: missing -t <phase>\n";
        printUsage(argv[0]);
        return 1;
    }
    if (filename.empty()) {
        std::cerr << "Error: missing input file\n";
        printUsage(argv[0]);
        return 1;
    }

    try {
        if (phase == "scan") {
            return runScan(filename);
        }
        if (phase == "parse") {
            return runParse(filename);
        }
        std::cerr << "Error: unknown phase '" << phase << "'\n";
        printUsage(argv[0]);
        return 1;
    } catch (const std::exception& e) {
        std::cerr << "Error: " << e.what() << '\n';
        return 1;
    }
}
