#include "parser.hpp"
#include <fstream>
#include <iostream>
#include <iterator>
#include <stdexcept>

void printTree(std::ostream& out, const Node& node, size_t depth = 0) {
    out << std::string(depth * 2, ' ') << node.label << '\n';
    for (const auto& child : node.children) printTree(out, child, depth + 1);
}

int main(int argc, char** argv) {
    std::string inputName, outputName, target = "parse";
    bool debug = false;
    try {
        for (int i = 1; i < argc; ++i) {
            std::string arg = argv[i];
            auto argument = [&]() -> std::string {
                if (i + 1 == argc) throw std::runtime_error("missing argument for " + arg);
                return argv[++i];
            };
            if (arg == "-t" || arg == "--target") target = argument();
            else if (arg == "-o" || arg == "--output") outputName = argument();
            else if (arg == "-d" || arg == "--debug") debug = true;
            else if (arg == "-O" || arg == "--opt") argument();
            else if (arg == "-h" || arg == "--help") {
                std::cout << "Usage: decaf [-t scan|parse] [-d] [-o OUTPUT] FILE\n";
                return 0;
            } else if (!arg.empty() && arg.front() == '-') {
                throw std::runtime_error("unknown option " + arg);
            } else if (inputName.empty()) inputName = arg;
            else throw std::runtime_error("expected exactly one input file");
        }
        if (inputName.empty()) throw std::runtime_error("missing input file");
        if (target != "scan" && target != "parse") throw std::runtime_error("unknown target " + target);
        std::ifstream input(inputName, std::ios::binary);
        if (!input) throw std::runtime_error("cannot open input file");
        std::string source{std::istreambuf_iterator<char>(input), {}};
        auto tokens = scan(source);
        std::ofstream file;
        if (!outputName.empty()) {
            file.open(outputName);
            if (!file) throw std::runtime_error("cannot open output " + outputName);
        }
        std::ostream& out = file.is_open() ? static_cast<std::ostream&>(file) : std::cout;
        if (target == "parse") {
            auto tree = parse(tokens);
            if (debug) printTree(out, tree);
        } else {
            for (const auto& token : tokens) {
                if (token.kind == "EOF") break;
                out << token.line << ' ';
                if (!token.kind.empty()) out << token.kind << ' ';
                out << token.text << '\n';
            }
        }
        out.flush();
        if (!out) throw std::runtime_error("output write failed");
    } catch (const std::exception& error) {
        if (!inputName.empty()) std::cerr << inputName << ":";
        std::cerr << error.what() << '\n';
        return 1;
    }
}
