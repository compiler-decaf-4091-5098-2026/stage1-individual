#include "lexer.hpp"
#include <fstream>
#include <iostream>
#include <iterator>
#include <stdexcept>

int main(int argc, char** argv) {
    try {
        if (argc != 4 || std::string(argv[1]) != "-t" || std::string(argv[2]) != "scan")
            throw std::runtime_error("usage: decaf -t scan FILE");
        std::ifstream input(argv[3], std::ios::binary);
        if (!input) throw std::runtime_error("cannot open input file");
        std::string source{std::istreambuf_iterator<char>(input), {}};
        for (const auto& token : scan(source)) {
            if (token.kind == "EOF") break;
            std::cout << token.line << ' ';
            if (!token.kind.empty()) std::cout << token.kind << ' ';
            std::cout << token.text << '\n';
        }
    } catch (const std::exception& error) {
        std::cerr << error.what() << '\n';
        return 1;
    }
}
