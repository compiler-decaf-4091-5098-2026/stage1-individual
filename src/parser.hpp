#pragma once
#include "lexer.hpp"

struct Node {
    std::string label;
    std::vector<Node> children{};
};

Node parse(const std::vector<Token>& tokens);
