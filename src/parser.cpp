#include "parser.hpp"
#include <map>
#include <stdexcept>
#include <utility>

namespace {
class Parser {
    const std::vector<Token>& tokens;
    size_t cursor = 0;

    const Token& here() const { return tokens.at(cursor); }
    std::string take() { return tokens.at(cursor++).text; }
    bool accept(const std::string& text) {
        if (here().text != text) return false;
        ++cursor;
        return true;
    }
    [[noreturn]] void expected(const std::string& what) const {
        auto actual = here().kind == "EOF" ? "end of file" : "'" + here().text + "'";
        throw std::runtime_error(here().position() + ": expected " + what + ", got " + actual);
    }
    void require(const std::string& text) {
        if (!accept(text)) expected("'" + text + "'");
    }
    std::string identifier() {
        if (here().kind != "IDENTIFIER") expected("identifier");
        return take();
    }
    bool atType() const {
        return here().text == "int" || here().text == "bool" || here().text == "long";
    }
    Node fields(std::string type, std::string name) {
        Node result{"fields " + type};
        do {
            Node field{name};
            if (accept("[")) {
                if (here().kind != "INTLITERAL") expected("integer array size");
                field.children.push_back({take()});
                require("]");
            }
            result.children.push_back(std::move(field));
            if (!accept(",")) break;
            name = identifier();
        } while (true);
        require(";");
        return result;
    }
    Node method(std::string type, std::string name) {
        Node params{"parameters"};
        if (!accept(")")) {
            do {
                if (!atType()) expected("parameter type");
                auto paramType = take();
                params.children.push_back({paramType + " " + identifier()});
            } while (accept(","));
            require(")");
        }
        Node result{"method " + type + " " + name};
        result.children.push_back(std::move(params));
        result.children.push_back(block());
        return result;
    }
    Node block() {
        require("{");
        Node result{"block"};
        while (atType()) {
            auto type = take();
            result.children.push_back(fields(type, identifier()));
        }
        while (!accept("}")) result.children.push_back(statement());
        return result;
    }
    Node location(std::string name) {
        Node result{"location " + name};
        if (accept("[")) {
            result.children.push_back(expression());
            require("]");
        }
        return result;
    }
    Node update(std::string name) {
        auto lhs = location(name);
        auto op = here().text;
        if (op != "++" && op != "--" && op != "=" && op != "+=" && op != "-=" &&
            op != "*=" && op != "/=" && op != "%=") expected("assignment or increment");
        take();
        Node result{op};
        result.children.push_back(std::move(lhs));
        if (op != "++" && op != "--") result.children.push_back(expression());
        return result;
    }
    Node call(std::string name) {
        Node result{"call " + name};
        if (!accept(")")) {
            do {
                if (here().kind == "STRINGLITERAL") result.children.push_back({take()});
                else result.children.push_back(expression());
            } while (accept(","));
            require(")");
        }
        return result;
    }
    Node statement() {
        if (here().text == "if" || here().text == "while") {
            Node result{take()};
            require("(");
            result.children.push_back(expression());
            require(")");
            result.children.push_back(block());
            if (result.label == "if" && accept("else")) result.children.push_back(block());
            return result;
        }
        if (accept("for")) {
            require("(");
            Node init{"="};
            init.children.push_back({"location " + identifier()});
            require("=");
            init.children.push_back(expression());
            require(";");
            Node result{"for"};
            result.children.push_back(std::move(init));
            result.children.push_back(expression());
            require(";");
            result.children.push_back(update(identifier()));
            require(")");
            result.children.push_back(block());
            return result;
        }
        Node result{""};
        if (here().kind == "IDENTIFIER") {
            auto name = identifier();
            result = accept("(") ? call(name) : update(name);
        } else if (accept("return")) {
            result.label = "return";
            if (here().text != ";") result.children.push_back(expression());
        } else if (here().text == "break" || here().text == "continue") {
            result.label = take();
        } else {
            expected("statement or '}'");
        }
        require(";");
        return result;
    }
    Node prefix() {
        if (here().text == "-" || here().text == "!") {
            Node result{take()};
            result.children.push_back(prefix());
            return result;
        }
        if (accept("(")) {
            auto value = expression();
            require(")");
            return value;
        }
        if (here().text == "int" || here().text == "long" || here().text == "len") {
            Node result{take()};
            require("(");
            if (result.label == "len") result.children.push_back({identifier()});
            else result.children.push_back(expression());
            require(")");
            return result;
        }
        if (here().kind == "IDENTIFIER") {
            auto name = take();
            return accept("(") ? call(name) : location(name);
        }
        if (here().kind == "INTLITERAL" || here().kind == "LONGLITERAL" ||
            here().kind == "BOOLEANLITERAL" || here().kind == "CHARLITERAL") return {take()};
        expected("expression");
    }
    Node expression(int minimum = 1) {
        static const std::map<std::string, int> precedence = {
            {"||",1},{"&&",2},{"==",3},{"!=",3},{"<",4},{"<=",4},{">",4},{">=",4},
            {"+",5},{"-",5},{"*",6},{"/",6},{"%",6}
        };
        Node left = prefix();
        while (true) {
            auto op = precedence.find(here().text);
            if (op == precedence.end() || op->second < minimum) return left;
            Node binary{take()};
            binary.children.push_back(std::move(left));
            binary.children.push_back(expression(op->second + 1));
            left = std::move(binary);
        }
    }
public:
    explicit Parser(const std::vector<Token>& input) : tokens(input) {}
    Node program() {
        Node result{"program"};
        while (accept("import")) {
            result.children.push_back({"import " + identifier()});
            require(";");
        }
        bool methodsStarted = false;
        while (here().kind != "EOF") {
            if (!atType() && here().text != "void") expected("declaration");
            auto type = take();
            auto name = identifier();
            if (accept("(")) {
                methodsStarted = true;
                result.children.push_back(method(type, name));
            } else {
                if (methodsStarted || type == "void") expected("'('");
                result.children.push_back(fields(type, name));
            }
        }
        return result;
    }
};
}

Node parse(const std::vector<Token>& tokens) {
    return Parser(tokens).program();
}
