#include "parser.h"

#include <iostream>

Parser::Parser(Lexer& lexer, const std::string& filename)
    : m_lexer(lexer)
    , m_filename(filename) {
}

bool Parser::isTypeKeyword() const {
    TokenType t = peek().tokenType;
    return t == TokenType::KeywordInt || t == TokenType::KeywordLong || t == TokenType::KeywordBool;
}

bool Parser::isAssignOperator(TokenType t) const {
    return t == TokenType::Assign || t == TokenType::PlusAssign || t == TokenType::MinusAssign ||
           t == TokenType::StarAssign || t == TokenType::SlashAssign ||
           t == TokenType::PercentAssign;
}

bool Parser::binaryOperator(TokenType t, int& precedence, BinOp& op) const {
    switch (t) {
        case TokenType::Star:
            precedence = 7;
            op         = BinOp::Mul;
            return true;
        case TokenType::Slash:
            precedence = 7;
            op         = BinOp::Div;
            return true;
        case TokenType::Percent:
            precedence = 7;
            op         = BinOp::Rem;
            return true;
        case TokenType::Plus:
            precedence = 6;
            op         = BinOp::Add;
            return true;
        case TokenType::Minus:
            precedence = 6;
            op         = BinOp::Sub;
            return true;
        case TokenType::Less:
            precedence = 5;
            op         = BinOp::Less;
            return true;
        case TokenType::LessEqual:
            precedence = 5;
            op         = BinOp::LessEqual;
            return true;
        case TokenType::Greater:
            precedence = 5;
            op         = BinOp::Greater;
            return true;
        case TokenType::GreaterEqual:
            precedence = 5;
            op         = BinOp::GreaterEqual;
            return true;
        case TokenType::Equal:
            precedence = 4;
            op         = BinOp::Equal;
            return true;
        case TokenType::NotEqual:
            precedence = 4;
            op         = BinOp::NotEqual;
            return true;
        case TokenType::LogicalAnd:
            precedence = 3;
            op         = BinOp::LogicalAnd;
            return true;
        case TokenType::LogicalOr:
            precedence = 2;
            op         = BinOp::LogicalOr;
            return true;
        default:
            return false;
    }
}

std::string Parser::describeToken(const Token& token) const {
    if (!token.data.empty()) {
        return token.data;
    }
    return tokenTypeName(token.tokenType);
}

void Parser::error(const Position& position, const std::string& message) {
    std::cerr << m_filename << ':' << position.line << ':' << position.column
              << ": syntax error: " << message << '\n';
    ++m_errorCount;
}

bool Parser::expect(TokenType type, const char* what) {
    if (check(type)) {
        advance();
        return true;
    }
    error(peek().position,
          std::string("expected ") + what + " but found '" + describeToken(peek()) + "'");
    return false;
}

std::string Parser::expectIdentifier(const char* what) {
    if (check(TokenType::Identifier)) {
        std::string name = peek().data;
        advance();
        return name;
    }
    error(peek().position,
          std::string("expected ") + what + " but found '" + describeToken(peek()) + "'");
    return "";
}

void Parser::synchronizeStatement() {
    while (!atEnd()) {
        if (check(TokenType::Semicolon)) {
            advance();
            return;
        }
        if (check(TokenType::RBrace)) {
            return;
        }
        advance();
    }
}

void Parser::synchronizeTopLevel() {
    while (!atEnd()) {
        TokenType t = peek().tokenType;
        if (t == TokenType::KeywordImport || t == TokenType::KeywordVoid || isTypeKeyword()) {
            return;
        }
        advance();
    }
}

std::unique_ptr<Program> Parser::parse() {
    std::unique_ptr<Program> program = std::make_unique<Program>();
    while (!atEnd()) {
        TokenType t = peek().tokenType;
        if (t == TokenType::KeywordImport) {
            parseImportDeclaration(*program);
        } else if (t == TokenType::KeywordVoid || isTypeKeyword()) {
            parseGlobalDeclaration(*program);
        } else {
            error(peek().position,
                  std::string("expected a declaration (import, field or method) but found '") +
                      describeToken(peek()) + "'");
            synchronizeTopLevel();
        }
    }
    return program;
}

void Parser::parseImportDeclaration(Program& program) {
    Position start = peek().position;
    advance();  // 'import'
    std::string name = expectIdentifier("identifier after 'import'");
    if (name.empty()) {
        error(start, "malformed import declaration");
    }
    program.imports.push_back(name);
    expect(TokenType::Semicolon, "';' after import declaration");
}

void Parser::parseGlobalDeclaration(Program& program) {
    bool     isVoid     = false;
    TypeName returnType = TypeName::Int;

    if (check(TokenType::KeywordVoid)) {
        isVoid = true;
        advance();
    } else {
        returnType = parseType();
    }

    std::string name = expectIdentifier("identifier after type");
    if (name.empty()) {
        synchronizeTopLevel();
        return;
    }

    if (check(TokenType::LParen)) {
        advance();
        parseMethodRest(program, isVoid, returnType, name);
    } else {
        if (isVoid) {
            error(peek().position, "'void' is only allowed for method declarations");
        }
        Type type;
        type.name = returnType;
        parseFieldRest(program, type, name);
    }
}

void Parser::parseFieldRest(Program& program, Type type, const std::string& name) {
    FieldDeclaration field;
    field.type = type;
    field.name = name;

    if (check(TokenType::LBracket)) {
        advance();
        if (check(TokenType::IntLiteral)) {
            field.type.isArray   = true;
            field.type.arraySize = peek().data;
            advance();
        } else {
            error(peek().position, "expected integer literal as array size");
        }
        if (!expect(TokenType::RBracket, "']' after array size")) {
            synchronizeTopLevel();
            return;
        }
    }
    expect(TokenType::Semicolon, "';' after field declaration");
    program.fields.push_back(field);
}

void Parser::parseMethodRest(Program& program, bool isVoid, TypeName returnType,
                             const std::string& name) {
    MethodDeclaration method;
    method.isVoid     = isVoid;
    method.returnType = Type{returnType, false, ""};
    method.name       = name;

    method.parameters = parseFormals();
    if (!expect(TokenType::RParen, "')' after parameter list")) {
        synchronizeTopLevel();
        return;
    }
    method.body = parseBlock();
    if (!method.body) {
        error(peek().position, "expected method body block");
        return;
    }
    program.methods.push_back(std::move(method));
}

std::vector<Parameter> Parser::parseFormals() {
    std::vector<Parameter> parameters;
    while (!check(TokenType::RParen) && !atEnd()) {
        TypeName    t    = parseType();
        std::string name = expectIdentifier("parameter name");
        parameters.push_back({Type{t, false, ""}, name});
        if (check(TokenType::Comma)) {
            advance();
        } else {
            break;
        }
    }
    return parameters;
}

std::unique_ptr<Block> Parser::parseBlock() {
    auto block = std::make_unique<Block>();
    if (!expect(TokenType::LBrace, "'{'")) {
        synchronizeStatement();
        return block;
    }

    while (!check(TokenType::RBrace) && !atEnd()) {
        if (isTypeKeyword()) {
            auto varDecl = parseVariableDeclaration();
            if (varDecl) {
                block->variables.push_back(std::move(*varDecl));
            }
        } else {
            auto statement = parseStatement();
            if (statement) {
                block->statements.push_back(std::move(statement));
            }
        }
    }

    expect(TokenType::RBrace, "'}'");
    return block;
}

std::unique_ptr<VariableDeclaration> Parser::parseVariableDeclaration() {
    auto varDecl = std::make_unique<VariableDeclaration>();

    varDecl->type.name = parseType();
    varDecl->name      = expectIdentifier("variable name");

    if (check(TokenType::LBracket)) {
        advance();
        if (check(TokenType::IntLiteral)) {
            varDecl->type.isArray   = true;
            varDecl->type.arraySize = peek().data;
            advance();
        } else {
            error(peek().position, "expected integer literal as array size");
        }
        if (!expect(TokenType::RBracket, "']' after array size")) {
            synchronizeStatement();
            return nullptr;
        }
    }

    if (!expect(TokenType::Semicolon, "';' after variable declaration")) {
        synchronizeStatement();
        return nullptr;
    }
    return varDecl;
}

std::unique_ptr<Statement> Parser::parseStatement() {
    switch (peek().tokenType) {
        case TokenType::KeywordIf:
            return parseIfStatement();
        case TokenType::KeywordFor:
            return parseForStatement();
        case TokenType::KeywordWhile:
            return parseWhileStatement();
        case TokenType::KeywordReturn:
            return parseReturnStatement();
        case TokenType::KeywordBreak:
            advance();
            expect(TokenType::Semicolon, "';' after 'break'");
            return std::make_unique<BreakStatement>();
        case TokenType::KeywordContinue:
            advance();
            expect(TokenType::Semicolon, "';' after 'continue'");
            return std::make_unique<ContinueStatement>();
        default:
            return parseSimpleStatement();
    }
}

std::unique_ptr<Statement> Parser::parseIfStatement() {
    auto stmt = std::make_unique<IfStatement>();

    advance();
    if (!expect(TokenType::LParen, "'(' after 'if'")) {
        synchronizeStatement();
        return nullptr;
    }
    stmt->condition = parseExpression();
    if (!stmt->condition) {
        synchronizeStatement();
        return nullptr;
    }
    if (!expect(TokenType::RParen, "')' after 'if' condition")) {
        synchronizeStatement();
        return nullptr;
    }

    stmt->thenBlock = parseBlock();
    if (check(TokenType::KeywordElse)) {
        advance();
        stmt->elseBlock = parseBlock();
    }
    return stmt;
}

std::unique_ptr<Statement> Parser::parseForStatement() {
    auto stmt = std::make_unique<ForStatement>();

    advance();
    if (!expect(TokenType::LParen, "'(' after 'for'")) {
        synchronizeStatement();
        return nullptr;
    }

    if (check(TokenType::Identifier)) {
        stmt->index = peek().data;
        advance();
    } else {
        error(peek().position,
              std::string("expected loop variable but found '") + describeToken(peek()) + "'");
        synchronizeStatement();
        return nullptr;
    }

    if (!expect(TokenType::Assign, "'=' after loop variable")) {
        synchronizeStatement();
        return nullptr;
    }
    stmt->init = parseExpression();
    if (!stmt->init) {
        synchronizeStatement();
        return nullptr;
    }
    if (!expect(TokenType::Semicolon, "';' after initial expression of 'for'")) {
        synchronizeStatement();
        return nullptr;
    }

    stmt->condition = parseExpression();
    if (!stmt->condition) {
        synchronizeStatement();
        return nullptr;
    }
    if (!expect(TokenType::Semicolon, "';' after condition of 'for'")) {
        synchronizeStatement();
        return nullptr;
    }

    parseForUpdate(stmt->update);
    if (!expect(TokenType::RParen, "')' after for-update")) {
        synchronizeStatement();
        return nullptr;
    }

    stmt->body = parseBlock();
    return stmt;
}

std::unique_ptr<Statement> Parser::parseWhileStatement() {
    auto stmt = std::make_unique<WhileStatement>();

    advance();
    if (!expect(TokenType::LParen, "'(' after 'while'")) {
        synchronizeStatement();
        return nullptr;
    }
    stmt->condition = parseExpression();
    if (!stmt->condition) {
        synchronizeStatement();
        return nullptr;
    }
    if (!expect(TokenType::RParen, "')' after 'while' condition")) {
        synchronizeStatement();
        return nullptr;
    }
    stmt->body = parseBlock();
    return stmt;
}

std::unique_ptr<Statement> Parser::parseReturnStatement() {
    auto stmt = std::make_unique<ReturnStatement>();

    advance();
    if (!check(TokenType::Semicolon)) {
        stmt->value = parseExpression();
        if (!stmt->value) {
            synchronizeStatement();
            return nullptr;
        }
    }
    if (!expect(TokenType::Semicolon, "';' after 'return' statement")) {
        synchronizeStatement();
        return nullptr;
    }
    return stmt;
}

void Parser::parseForUpdate(Update& update) {
    if (!check(TokenType::Identifier)) {
        error(peek().position,
              std::string("expected for-update (assignment or increment) but found '") +
                  describeToken(peek()) + "'");
        return;
    }
    update.location = parseLocation();

    if (check(TokenType::Increment) || check(TokenType::Decrement)) {
        update.increment = true;
        update.isUp      = check(TokenType::Increment);
        advance();
        return;
    }
    if (isAssignOperator(peek().tokenType)) {
        update.op    = parseAssignOp();
        update.value = parseExpression();
        if (!update.value) {
            return;
        }
        return;
    }
    error(peek().position,
          std::string("expected assignment operator or increment in for-update but found '") +
              describeToken(peek()) + "'");
}

std::unique_ptr<Statement> Parser::parseSimpleStatement() {
    if (!check(TokenType::Identifier)) {
        error(peek().position,
              std::string("expected a statement but found '") + describeToken(peek()) + "'");
        synchronizeStatement();
        return nullptr;
    }

    std::string name = peek().data;
    advance();

    if (check(TokenType::LParen)) {
        auto call = parseCallRest(name);
        if (!call) {
            synchronizeStatement();
            return nullptr;
        }
        if (!expect(TokenType::Semicolon, "';' after method call")) {
            synchronizeStatement();
            return nullptr;
        }
        auto stmt       = std::make_unique<CallStatement>();
        stmt->name      = call->name;
        stmt->arguments = std::move(call->arguments);
        return stmt;
    }

    Location loc;
    loc.name = name;
    if (check(TokenType::LBracket)) {
        advance();
        loc.index = parseExpression();
        if (!loc.index) {
            synchronizeStatement();
            return nullptr;
        }
        if (!expect(TokenType::RBracket, "']' after index expression")) {
            synchronizeStatement();
            return nullptr;
        }
    }

    if (check(TokenType::Increment) || check(TokenType::Decrement)) {
        bool isUp = check(TokenType::Increment);
        advance();
        if (!expect(TokenType::Semicolon, "';' after increment")) {
            synchronizeStatement();
            return nullptr;
        }
        auto stmt       = std::make_unique<IncrementStatement>();
        stmt->target    = std::move(loc);
        stmt->increment = isUp;
        return stmt;
    }

    if (isAssignOperator(peek().tokenType)) {
        auto stmt    = std::make_unique<AssignStatement>();
        stmt->target = std::move(loc);
        stmt->op     = parseAssignOp();
        stmt->value  = parseExpression();
        if (!stmt->value) {
            synchronizeStatement();
            return nullptr;
        }
        if (!expect(TokenType::Semicolon, "';' after assignment")) {
            synchronizeStatement();
            return nullptr;
        }
        return stmt;
    }

    error(peek().position,
          std::string("expected '(', assignment operator or increment but found '") +
              describeToken(peek()) + "'");
    synchronizeStatement();
    return nullptr;
}

std::unique_ptr<Expression> Parser::parseExpression(int minPrecedence) {
    std::unique_ptr<Expression> left = parseUnaryExpression();
    if (!left) {
        return nullptr;
    }

    while (true) {
        TokenType tokenType = peek().tokenType;
        int       precedence;
        BinOp     op;
        if (!binaryOperator(tokenType, precedence, op)) {
            break;
        }
        if (precedence < minPrecedence) {
            break;
        }
        advance();
        std::unique_ptr<Expression> right = parseExpression(precedence + 1);
        if (!right) {
            if (!atEnd()) {
                advance();
            }
            break;
        }
        auto bin   = std::make_unique<BinaryExpression>();
        bin->op    = op;
        bin->left  = std::move(left);
        bin->right = std::move(right);
        left       = std::move(bin);
    }
    return left;
}

std::unique_ptr<Expression> Parser::parseUnaryExpression() {
    Token tk = peek();

    if (tk.tokenType == TokenType::Minus) {
        advance();
        auto operand = parseUnaryExpression();
        if (!operand) return nullptr;
        auto expr     = std::make_unique<UnaryExpression>();
        expr->op      = UnaryOp::Negate;
        expr->operand = std::move(operand);
        return expr;
    }

    if (tk.tokenType == TokenType::LogicalNot) {
        advance();
        auto operand = parseUnaryExpression();
        if (!operand) return nullptr;
        auto expr     = std::make_unique<UnaryExpression>();
        expr->op      = UnaryOp::LogicalNot;
        expr->operand = std::move(operand);
        return expr;
    }

    if (tk.tokenType == TokenType::KeywordInt || tk.tokenType == TokenType::KeywordLong) {
        bool castToInt = tk.tokenType == TokenType::KeywordInt;
        advance();
        if (!expect(TokenType::LParen, "'(' after cast operator")) {
            return nullptr;
        }
        auto argument = parseExpression();
        if (!argument) {
            return nullptr;
        }
        if (!expect(TokenType::RParen, "')' in cast expression")) {
            return nullptr;
        }
        auto expr      = std::make_unique<CastExpression>();
        expr->type     = castToInt ? TypeName::Int : TypeName::Long;
        expr->argument = std::move(argument);
        return expr;
    }

    if (tk.tokenType == TokenType::KeywordLen) {
        advance();
        if (!expect(TokenType::LParen, "'(' after 'len'")) {
            return nullptr;
        }
        auto expr  = std::make_unique<LenExpression>();
        expr->name = expectIdentifier("array name after 'len('");
        if (!expect(TokenType::RParen, "')' after 'len' argument")) {
            return nullptr;
        }
        return expr;
    }

    return parsePostfixExpression();
}

std::unique_ptr<Expression> Parser::parsePostfixExpression() {
    Token tk = peek();

    switch (tk.tokenType) {
        case TokenType::IntLiteral: {
            advance();
            auto expr = std::make_unique<IntExpression>();
            expr->raw = tk.data;
            return expr;
        }
        case TokenType::LongLiteral: {
            advance();
            auto expr = std::make_unique<LongExpression>();
            expr->raw = tk.data;
            return expr;
        }
        case TokenType::BoolLiteral: {
            advance();
            auto expr   = std::make_unique<BoolExpression>();
            expr->value = (tk.data == "true");
            return expr;
        }
        case TokenType::CharLiteral: {
            advance();
            auto expr = std::make_unique<CharExpression>();
            expr->raw = tk.data;
            return expr;
        }
        case TokenType::Identifier: {
            advance();
            if (check(TokenType::LParen)) {
                return parseCallRest(tk.data);
            }
            auto expr           = std::make_unique<LocationExpression>();
            expr->location.name = tk.data;
            if (check(TokenType::LBracket)) {
                advance();
                expr->location.index = parseExpression();
                if (!expr->location.index) {
                    return nullptr;
                }
                if (!expect(TokenType::RBracket, "']' after index expression")) {
                    return nullptr;
                }
            }
            return expr;
        }
        case TokenType::LParen: {
            advance();
            auto expr = parseExpression();
            if (!expr) {
                return nullptr;
            }
            if (!expect(TokenType::RParen, "')'")) {
                return nullptr;
            }
            return expr;
        }
        default:
            error(tk.position,
                  std::string("expected an expression but found '") + describeToken(tk) + "'");
            return nullptr;
    }
}

std::unique_ptr<Expression> Parser::parseExternArgument() {
    Token tk = peek();
    if (tk.tokenType == TokenType::StringLiteral) {
        advance();
        auto expr = std::make_unique<StringExpression>();
        expr->raw = tk.data;
        return expr;
    }
    return parseExpression();
}

std::unique_ptr<CallExpression> Parser::parseCallRest(const std::string& name) {
    auto call  = std::make_unique<CallExpression>();
    call->name = name;

    expect(TokenType::LParen, "'('");
    if (!check(TokenType::RParen)) {
        auto arg = parseExternArgument();
        if (arg) {
            call->arguments.push_back(std::move(arg));
        }
        while (check(TokenType::Comma)) {
            advance();
            auto nextArg = parseExternArgument();
            if (nextArg) {
                call->arguments.push_back(std::move(nextArg));
            }
        }
    }
    if (!expect(TokenType::RParen, "')' after arguments")) {
        return nullptr;
    }
    return call;
}

Location Parser::parseLocation() {
    Location loc;
    if (check(TokenType::Identifier)) {
        loc.name = peek().data;
        advance();
    } else {
        error(peek().position,
              std::string("expected an identifier but found '") + describeToken(peek()) + "'");
        return loc;
    }

    if (check(TokenType::LBracket)) {
        advance();
        loc.index = parseExpression();
        if (!loc.index) {
            return loc;
        }
        if (!expect(TokenType::RBracket, "']' after index expression")) {
            loc.index.reset();
        }
    }
    return loc;
}

TypeName Parser::parseType() {
    Position  p = peek().position;
    TokenType t = peek().tokenType;
    advance();
    switch (t) {
        case TokenType::KeywordInt:
            return TypeName::Int;
        case TokenType::KeywordLong:
            return TypeName::Long;
        case TokenType::KeywordBool:
            return TypeName::Bool;
        default:
            error(p, std::string("expected a type (int, long or bool) but found '") +
                         describeToken(peek()) + "'");
            return TypeName::Int;
    }
}

AssignOp Parser::parseAssignOp() {
    TokenType t = peek().tokenType;
    advance();
    switch (t) {
        case TokenType::PlusAssign:
            return AssignOp::PlusAssign;
        case TokenType::MinusAssign:
            return AssignOp::MinusAssign;
        case TokenType::StarAssign:
            return AssignOp::StarAssign;
        case TokenType::SlashAssign:
            return AssignOp::SlashAssign;
        case TokenType::PercentAssign:
            return AssignOp::PercentAssign;
        default:
            return AssignOp::Assign;
    }
}
