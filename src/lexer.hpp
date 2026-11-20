#pragma once
#include <string>
#include <vector>

struct Token {
    std::string kind, text;
    int line, column;
    std::string position() const;
};

std::vector<Token> scan(const std::string& source);
