#include <iostream>
#include <string>
#include <vector>
#include "scanner.h"
#include "parser.h"
#include "utils/cli.h"

int main(int argc, char** argv) {
    std::string mode, path;
    for (int i = 1; i < argc; i++) {
        std::string a = argv[i];
        if (a == "-t" && i + 1 < argc) mode = argv[++i];
        else path = a;
    }
    if (path.empty() || (mode != "scan" && mode != "parse")) {
        std::cerr << "usage: decaf -t {scan|parse} <file>\n";
        return 1;
    }

    std::string src;
    try {
        src = cli::readFile(path);
    } catch (const std::exception& e) {
        std::cerr << e.what() << "\n";
        return 1;
    }

    Scanner sc(src, path);
    std::vector<Token> toks;
    while (true) {
        Token t = sc.next();
        if (t.type == TokType::END) { toks.push_back(t); break; }
        if (t.type == TokType::ERROR) continue;
        toks.push_back(t);
    }
    
    for (const auto& e : sc.errors) std::cerr << e << "\n";

    if (mode == "scan") {
        for (const auto& t : toks) {
            if (t.type == TokType::END) break;
            std::string tn = Scanner::typeName(t.type);
            std::cout << t.line << " ";
            if (!tn.empty()) std::cout << tn << " ";
            std::cout << t.text << "\n";
        }
        return sc.errors.empty() ? 0 : 1;
    }

    Parser ps(std::move(toks), path);
    ps.parseProgram();
    return (ps.errors || !sc.errors.empty()) ? 1 : 0;
}