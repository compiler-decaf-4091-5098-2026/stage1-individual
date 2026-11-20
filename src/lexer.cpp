#include "lexer.hpp"
#include <cctype>
#include <set>
#include <stdexcept>
#include <string_view>

std::string Token::position() const {
    return std::to_string(line) + ":" + std::to_string(column);
}

std::vector<Token> scan(const std::string& s) {
    static const std::set<std::string> keywords = {
        "int","long","bool","void","import","if","else","while","for",
        "return","break","continue","len"
    };
    static const std::set<std::string> pairs = {
        "++","--","+=","-=","*=","/=","%=","==","!=","<=",">=","&&","||"
    };
    std::vector<Token> tokens;
    size_t i = 0;
    int line = 1, column = 1;
    auto peek = [&](size_t offset = 0) -> unsigned char {
        return i + offset < s.size() ? s[i + offset] : 0;
    };
    auto advance = [&] {
        char c = s[i++];
        if (c == '\r' || (c == '\n' && (i < 2 || s[i-2] != '\r'))) ++line;
        if (c == '\r' || c == '\n') column = 1;
        else ++column;
    };
    while (i < s.size()) {
        size_t start = i;
        Token t{"", "", line, column};
        auto fail = [&](const char* reason) {
            throw std::runtime_error(t.position() + ": " + reason);
        };
        unsigned char c = peek();
        if (std::string_view(" \t\r\n\f").find(c) != std::string_view::npos) {
            advance();
            continue;
        }
        if (s.compare(i, 2, "//") == 0) {
            while (i < s.size() && peek() != '\r' && peek() != '\n') advance();
            continue;
        }
        if (s.compare(i, 2, "/*") == 0) {
            advance();
            advance();
            while (i < s.size() && s.compare(i, 2, "*/") != 0) advance();
            if (i == s.size()) fail("unterminated block comment");
            advance();
            advance();
            continue;
        }
        if (std::isalpha(c) || c == '_') {
            do { advance(); } while (std::isalnum(peek()) || peek() == '_');
            auto word = s.substr(start, i - start);
            if (word == "true" || word == "false") t.kind = "BOOLEANLITERAL";
            else if (!keywords.count(word)) t.kind = "IDENTIFIER";
        } else if (std::isdigit(c)) {
            if (s.compare(i, 2, "0x") == 0) {
                advance();
                advance();
                if (!std::isxdigit(peek())) fail("expected hexadecimal digit");
                while (std::isxdigit(peek())) advance();
            } else {
                while (std::isdigit(peek())) advance();
            }
            t.kind = "INTLITERAL";
            if (peek() == 'L') {
                advance();
                t.kind = "LONGLITERAL";
            }
        } else if (c == '\'' || c == '"') {
            advance();
            size_t length = 0;
            while (i < s.size() && peek() != c) {
                if (peek() == '\\') {
                    advance();
                    if (i == s.size() || std::string_view("\\\"'tnrf").find(peek()) == std::string_view::npos)
                        fail("invalid escape sequence");
                } else if (peek() < 32 || peek() > 126 || peek() == '\'' || peek() == '"') {
                    fail("invalid character in literal");
                }
                advance();
                ++length;
            }
            if (i == s.size()) fail("unterminated literal");
            if (c == '\'' && length != 1) fail("expected a single character");
            advance();
            t.kind = c == '\'' ? "CHARLITERAL" : "STRINGLITERAL";
        } else if (pairs.count(s.substr(i, 2))) {
            advance();
            advance();
        } else if (std::string_view("(){}[],;+-*/%=<>!").find(c) != std::string_view::npos) {
            advance();
        } else {
            fail("unexpected character");
        }
        t.text = s.substr(start, i - start);
        tokens.push_back(t);
    }
    tokens.push_back({"EOF", "", line, column});
    return tokens;
}
