#pragma once
#include "ast.h"
#include "lexer.h"

#include <memory>
#include <string>
#include <vector>

class Parser {
public:
    Parser(Lexer& lexer, const std::string& filename);

    std::unique_ptr<Program> parse();
    bool                     hadErrors() const { return m_errorCount > 0; }

private:
    Lexer&      m_lexer;
    std::string m_filename;
    int         m_errorCount = 0;

    Token peek() const { return m_lexer.peek(); }
    Token advance() { return m_lexer.next(); }
    bool  atEnd() const { return peek().tokenType == TokenType::EndOfFile; }
    bool  check(TokenType t) const { return peek().tokenType == t; }
    bool  isTypeKeyword() const;
    bool  isAssignOperator(TokenType t) const;
    bool  binaryOperator(TokenType t, int& precedence, BinOp& op) const;

    void        error(const Position& position, const std::string& message);
    bool        expect(TokenType type, const char* what);
    std::string expectIdentifier(const char* what);
    std::string describeToken(const Token& token) const;
    void        synchronizeStatement();
    void        synchronizeTopLevel();

    std::unique_ptr<Program> parseProgram();
    void                     parseImportDeclaration(Program& program);
    void                     parseGlobalDeclaration(Program& program);
    void                     parseFieldRest(Program& program, Type type, const std::string& name);
    void                     parseMethodRest(Program& program, bool isVoid, TypeName returnType,
                                             const std::string& name);
    std::vector<Parameter>   parseFormals();
    std::unique_ptr<Block>   parseBlock();

    std::unique_ptr<VariableDeclaration> parseVariableDeclaration();

    std::unique_ptr<Statement> parseStatement();
    std::unique_ptr<Statement> parseIfStatement();
    std::unique_ptr<Statement> parseForStatement();
    std::unique_ptr<Statement> parseWhileStatement();
    std::unique_ptr<Statement> parseReturnStatement();
    std::unique_ptr<Statement> parseSimpleStatement();
    void                       parseForUpdate(Update& update);

    std::unique_ptr<Expression>     parseExpression(int minPrecedence = 0);
    std::unique_ptr<Expression>     parseUnaryExpression();
    std::unique_ptr<Expression>     parsePostfixExpression();
    std::unique_ptr<Expression>     parseExternArgument();
    std::unique_ptr<CallExpression> parseCallRest(const std::string& name);
    Location                        parseLocation();
    TypeName                        parseType();
    AssignOp                        parseAssignOp();
};
