#include "lexer.h"

#include <cstdio>

const char* tokenTypeName(TokenType type) {
    switch (type) {
        case TokenType::EndOfFile:
            return "EndOfFile";
        case TokenType::Invalid:
            return "Invalid";
        case TokenType::Identifier:
            return "Identifier";
        case TokenType::IntLiteral:
            return "IntLiteral";
        case TokenType::LongLiteral:
            return "LongLiteral";
        case TokenType::BoolLiteral:
            return "BoolLiteral";
        case TokenType::CharLiteral:
            return "CharLiteral";
        case TokenType::StringLiteral:
            return "StringLiteral";
        case TokenType::KeywordBool:
            return "bool";
        case TokenType::KeywordBreak:
            return "break";
        case TokenType::KeywordContinue:
            return "continue";
        case TokenType::KeywordElse:
            return "else";
        case TokenType::KeywordFor:
            return "for";
        case TokenType::KeywordIf:
            return "if";
        case TokenType::KeywordImport:
            return "import";
        case TokenType::KeywordInt:
            return "int";
        case TokenType::KeywordLen:
            return "len";
        case TokenType::KeywordLong:
            return "long";
        case TokenType::KeywordReturn:
            return "return";
        case TokenType::KeywordVoid:
            return "void";
        case TokenType::KeywordWhile:
            return "while";
        case TokenType::LBrace:
            return "{";
        case TokenType::RBrace:
            return "}";
        case TokenType::LParen:
            return "(";
        case TokenType::RParen:
            return ")";
        case TokenType::LBracket:
            return "[";
        case TokenType::RBracket:
            return "]";
        case TokenType::Semicolon:
            return ";";
        case TokenType::Comma:
            return ",";
        case TokenType::Assign:
            return "=";
        case TokenType::PlusAssign:
            return "+=";
        case TokenType::MinusAssign:
            return "-=";
        case TokenType::StarAssign:
            return "*=";
        case TokenType::SlashAssign:
            return "/=";
        case TokenType::PercentAssign:
            return "%=";
        case TokenType::Plus:
            return "+";
        case TokenType::Minus:
            return "-";
        case TokenType::Star:
            return "*";
        case TokenType::Slash:
            return "/";
        case TokenType::Percent:
            return "%";
        case TokenType::Increment:
            return "++";
        case TokenType::Decrement:
            return "--";
        case TokenType::Less:
            return "<";
        case TokenType::LessEqual:
            return "<=";
        case TokenType::Greater:
            return ">";
        case TokenType::GreaterEqual:
            return ">=";
        case TokenType::Equal:
            return "==";
        case TokenType::NotEqual:
            return "!=";
        case TokenType::LogicalAnd:
            return "&&";
        case TokenType::LogicalOr:
            return "||";
        case TokenType::LogicalNot:
            return "!";
    }
    return "<?>";
}

std::string tokenToString(const Token& token) {
    switch (token.tokenType) {
        case TokenType::Identifier:
            return "IDENTIFIER " + token.data;
        case TokenType::IntLiteral:
            return "INTLITERAL " + token.data;
        case TokenType::LongLiteral:
            return "LONGLITERAL " + token.data;
        case TokenType::BoolLiteral:
            return "BOOLEANLITERAL " + token.data;
        case TokenType::CharLiteral:
            return "CHARLITERAL " + token.data;
        case TokenType::StringLiteral:
            return "STRINGLITERAL " + token.data;
        default:
            return token.data;
    }
}

Lexer::Lexer(const std::string& filename)
    : m_filename(filename) {
    m_file.open(filename);
    if (!m_file) {
        throw std::runtime_error("could not open file: " + filename);
    }
}

void Lexer::recordError(const std::string& message, Position position) {
    m_errors.push_back({message, position});
}

Position Lexer::currentPosition() const {
    return {line, col};
}

Token Lexer::next() {
    if (m_hasLookahead) {
        m_hasLookahead = false;
        return m_lookahead;
    }
    return scanToken();
}

Token Lexer::peek() {
    if (!m_hasLookahead) {
        m_lookahead    = scanToken();
        m_hasLookahead = true;
    }
    return m_lookahead;
}

Token Lexer::scanToken() {
    skipWhitespace();

    Token token;
    token.position = currentPosition();

    int c = currentChar();
    if (c == EOF) {
        token.tokenType = TokenType::EndOfFile;
        return token;
    }

    if (std::isalpha(static_cast<unsigned char>(c)) || c == '_') {
        token.data      = readIdentifierOrKeyword();
        token.tokenType = classifyWord(token.data);
    } else if (std::isdigit(static_cast<unsigned char>(c))) {
        bool isLong     = false;
        token.data      = readNumber(isLong);
        token.tokenType = isLong ? TokenType::LongLiteral : TokenType::IntLiteral;
    } else if (c == '\'' || c == '"') {
        char quote = static_cast<char>(c);
        advance();
        int  elements   = 0;
        bool closed     = false;
        token.data      = readStringOrChar(quote, elements, closed, token.position);
        token.tokenType = (quote == '\'') ? TokenType::CharLiteral : TokenType::StringLiteral;
        if (quote == '\'' && closed && elements != 1) {
            recordError("character literal must contain exactly one character", token.position);
        }
    } else {
        token.data      = readOperatorOrPunctuation();
        token.tokenType = classifySymbol(token.data);
        if (token.tokenType == TokenType::Invalid) {
            if (token.data.size() == 1 && std::isprint(static_cast<unsigned char>(token.data[0]))) {
                recordError(std::string("invalid character '") + token.data + "'", token.position);
            } else {
                recordError(std::string("invalid token \"") + token.data + "\"", token.position);
            }
            return scanToken();
        }
    }

    return token;
}

TokenType Lexer::classifySymbol(const std::string& symbol) {
    if (symbol == "{") return TokenType::LBrace;
    if (symbol == "}") return TokenType::RBrace;
    if (symbol == "(") return TokenType::LParen;
    if (symbol == ")") return TokenType::RParen;
    if (symbol == "[") return TokenType::LBracket;
    if (symbol == "]") return TokenType::RBracket;
    if (symbol == ";") return TokenType::Semicolon;
    if (symbol == ",") return TokenType::Comma;
    if (symbol == "=") return TokenType::Assign;
    if (symbol == "+=") return TokenType::PlusAssign;
    if (symbol == "-=") return TokenType::MinusAssign;
    if (symbol == "*=") return TokenType::StarAssign;
    if (symbol == "/=") return TokenType::SlashAssign;
    if (symbol == "%=") return TokenType::PercentAssign;
    if (symbol == "+") return TokenType::Plus;
    if (symbol == "-") return TokenType::Minus;
    if (symbol == "*") return TokenType::Star;
    if (symbol == "/") return TokenType::Slash;
    if (symbol == "%") return TokenType::Percent;
    if (symbol == "++") return TokenType::Increment;
    if (symbol == "--") return TokenType::Decrement;
    if (symbol == "<") return TokenType::Less;
    if (symbol == "<=") return TokenType::LessEqual;
    if (symbol == ">") return TokenType::Greater;
    if (symbol == ">=") return TokenType::GreaterEqual;
    if (symbol == "==") return TokenType::Equal;
    if (symbol == "!=") return TokenType::NotEqual;
    if (symbol == "&&") return TokenType::LogicalAnd;
    if (symbol == "||") return TokenType::LogicalOr;
    if (symbol == "!") return TokenType::LogicalNot;
    return TokenType::Invalid;
}

TokenType Lexer::classifyWord(const std::string& word) {
    if (word == "bool") return TokenType::KeywordBool;
    if (word == "break") return TokenType::KeywordBreak;
    if (word == "continue") return TokenType::KeywordContinue;
    if (word == "else") return TokenType::KeywordElse;
    if (word == "false") return TokenType::BoolLiteral;
    if (word == "for") return TokenType::KeywordFor;
    if (word == "if") return TokenType::KeywordIf;
    if (word == "import") return TokenType::KeywordImport;
    if (word == "int") return TokenType::KeywordInt;
    if (word == "len") return TokenType::KeywordLen;
    if (word == "long") return TokenType::KeywordLong;
    if (word == "return") return TokenType::KeywordReturn;
    if (word == "true") return TokenType::BoolLiteral;
    if (word == "void") return TokenType::KeywordVoid;
    if (word == "while") return TokenType::KeywordWhile;
    return TokenType::Identifier;
}

void Lexer::skipWhitespace() {
    while (true) {
        // whitespace
        while (true) {
            int c = currentChar();
            if (c == EOF || !std::isspace(static_cast<unsigned char>(c))) {
                break;
            }
            advance();
        }

        // comments
        if (currentChar() == '/') {
            Position slash = currentPosition();
            advance();
            int nc = currentChar();
            if (nc == '/') {
                advance();
                while (true) {
                    int c = currentChar();
                    if (c == EOF || c == '\n') {
                        break;
                    }
                    advance();
                }
                continue;
            } else if (nc == '*') {
                advance();
                bool isClosed = false;
                while (true) {
                    int c = currentChar();
                    if (c == EOF) {
                        break;
                    }
                    if (c == '*') {
                        advance();
                        if (currentChar() == '/') {
                            advance();
                            isClosed = true;
                            break;
                        }
                    } else {
                        advance();
                    }
                }
                if (!isClosed) {
                    recordError("unterminated block comment", slash);
                }
                continue;
            } else {
                m_file.unget();
                --col;
                break;
            }
        } else {
            break;
        }
    }
}

std::string Lexer::readIdentifierOrKeyword() {
    std::string token;
    while (true) {
        int c = currentChar();
        if (c == EOF || (!std::isalnum(static_cast<unsigned char>(c)) && c != '_')) {
            break;
        }
        token.push_back(static_cast<char>(c));
        advance();
    }
    return token;
}

std::string Lexer::readStringOrChar(char quote, int& elementCount, bool& closed, Position start) {
    std::string raw;
    raw.push_back(quote);
    elementCount = 0;
    closed       = false;

    while (true) {
        int c = currentChar();
        if (c == EOF) {
            recordError("unterminated string or character literal", start);
            return raw;
        }
        if (c == quote) {
            raw.push_back(static_cast<char>(c));
            advance();
            closed = true;
            return raw;
        }
        if (c == '\n') {
            recordError("newline in string or character literal", start);
            return raw;
        }
        if (c == '\\') {
            raw.push_back('\\');
            advance();
            int esc = currentChar();
            switch (esc) {
                case EOF:
                    recordError("unterminated string or character literal", start);
                    return raw;
                case 'n':
                case 't':
                case 'r':
                case 'f':
                case '\\':
                case '\'':
                case '"':
                    break;
                default:
                    recordError("invalid escape sequence in literal", start);
                    break;
            }
            raw.push_back(static_cast<char>(esc));
            advance();
            ++elementCount;
        } else {
            if (c < 32 || c > 126) {
                recordError("invalid character in literal", start);
            }
            raw.push_back(static_cast<char>(c));
            advance();
            ++elementCount;
        }
    }
}

std::string Lexer::readNumber(bool& isLongLiteral) {
    std::string token;
    Position    start = currentPosition();
    isLongLiteral     = false;

    if (currentChar() == '0') {
        token.push_back('0');
        advance();
        if (currentChar() == 'x') {
            token.push_back('x');
            advance();
            bool hasHex = false;
            while (true) {
                int c = currentChar();
                if (c != EOF && std::isxdigit(static_cast<unsigned char>(c))) {
                    token.push_back(static_cast<char>(c));
                    advance();
                    hasHex = true;
                } else {
                    break;
                }
            }
            if (!hasHex) {
                recordError("malformed hexadecimal literal: missing digits after '0x'", start);
                return token;
            }
            if (currentChar() == 'L') {
                token.push_back('L');
                advance();
                isLongLiteral = true;
            }
            return token;
        }
    }

    while (true) {
        int c = currentChar();
        if (c != EOF && std::isdigit(static_cast<unsigned char>(c))) {
            token.push_back(static_cast<char>(c));
            advance();
        } else {
            break;
        }
    }

    if (currentChar() == 'L') {
        token.push_back('L');
        advance();
        isLongLiteral = true;
    }

    return token;
}

std::string Lexer::readOperatorOrPunctuation() {
    std::string token;
    int         c = currentChar();
    if (c == EOF) {
        return token;
    }

    token.push_back(static_cast<char>(c));
    advance();

    int nc = currentChar();
    if (nc != EOF) {
        static const char* twoCharOperators[] = {
            "==", "!=", "<=", ">=", "&&", "||", "+=", "-=", "*=", "/=", "%=", "++", "--"};
        std::string two = token + static_cast<char>(nc);
        for (const char* op : twoCharOperators) {
            if (two == op) {
                token = two;
                advance();
                break;
            }
        }
    }

    return token;
}

void Lexer::advance() {
    int c = m_file.get();
    if (c == EOF) return;

    ++col;
    if (c == '\n') {
        ++line;
        col = 1;
    }
}
