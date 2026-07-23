package com.lfgtavora.poketcapi.poketcg_api.service;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ArrayDeque;

public final class QueryParser {

    private QueryParser() {
    }

    public static Optional<Expression> parse(String query, String defaultField) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }

        List<Token> tokens = tokenize(query);
        Parser parser = new Parser(tokens, defaultField);
        Expression expression = parser.parseExpression();
        if (parser.hasRemainingTokens()) {
            throw new IllegalArgumentException("Invalid query syntax near: " + parser.peekRaw());
        }
        return Optional.ofNullable(expression);
    }

    private static List<Token> tokenize(String query) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < query.length(); i++) {
            char ch = query.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
                current.append(ch);
                continue;
            }

            if (!inQuotes && (ch == '(' || ch == ')')) {
                flushToken(current, tokens);
                tokens.add(new Token(ch == '(' ? TokenType.LPAREN : TokenType.RPAREN, String.valueOf(ch)));
                continue;
            }

            if (!inQuotes && Character.isWhitespace(ch)) {
                flushToken(current, tokens);
                continue;
            }

            current.append(ch);
        }

        flushToken(current, tokens);
        return tokens;
    }

    private static void flushToken(StringBuilder current, List<Token> tokens) {
        if (current.isEmpty()) {
            return;
        }
        String raw = current.toString();
        String upper = raw.toUpperCase(Locale.ROOT);
        TokenType type = switch (upper) {
            case "AND" -> TokenType.AND;
            case "OR" -> TokenType.OR;
            case "NOT" -> TokenType.NOT;
            default -> TokenType.TERM;
        };
        tokens.add(new Token(type, raw));
        current.setLength(0);
    }

    private static String unquote(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() > 1) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static Term parseTerm(String raw, String defaultField) {
        int colonPos = raw.indexOf(':');
        if (colonPos <= 0) {
            return new Term(defaultField, unquote(raw));
        }
        String field = raw.substring(0, colonPos).trim();
        String value = raw.substring(colonPos + 1).trim();
        if (field.isBlank() || value.isBlank()) {
            throw new IllegalArgumentException("Invalid term: " + raw);
        }
        return new Term(field, unquote(value));
    }

    public sealed interface Expression permits Term, And, Or, Not {
    }

    public record Term(String field, String value) implements Expression {
    }

    public record And(Expression left, Expression right) implements Expression {
    }

    public record Or(Expression left, Expression right) implements Expression {
    }

    public record Not(Expression expression) implements Expression {
    }

    private enum TokenType {
        TERM, AND, OR, NOT, LPAREN, RPAREN
    }

    private record Token(TokenType type, String raw) {
    }

    private static final class Parser {
        private final Deque<Token> queue;
        private final String defaultField;

        private Parser(List<Token> tokens, String defaultField) {
            this.queue = new ArrayDeque<>(tokens);
            this.defaultField = defaultField;
        }

        private Expression parseExpression() {
            return parseOr();
        }

        private Expression parseOr() {
            Expression left = parseAnd();
            while (peekType() == TokenType.OR) {
                queue.pollFirst();
                Expression right = parseAnd();
                left = new Or(left, right);
            }
            return left;
        }

        private Expression parseAnd() {
            Expression left = parseNot();
            while (true) {
                TokenType next = peekType();
                if (next == TokenType.AND) {
                    queue.pollFirst();
                    Expression right = parseNot();
                    left = new And(left, right);
                    continue;
                }

                // Implicit AND: term term / term ( ... ) / ) term
                if (next == TokenType.TERM || next == TokenType.LPAREN || next == TokenType.NOT) {
                    Expression right = parseNot();
                    left = new And(left, right);
                    continue;
                }
                break;
            }
            return left;
        }

        private Expression parseNot() {
            if (peekType() == TokenType.NOT) {
                queue.pollFirst();
                return new Not(parseNot());
            }
            return parsePrimary();
        }

        private Expression parsePrimary() {
            Token token = queue.pollFirst();
            if (token == null) {
                throw new IllegalArgumentException("Unexpected end of query");
            }

            if (token.type() == TokenType.TERM) {
                return parseTerm(token.raw(), defaultField);
            }

            if (token.type() == TokenType.LPAREN) {
                Expression inner = parseExpression();
                Token closing = queue.pollFirst();
                if (closing == null || closing.type() != TokenType.RPAREN) {
                    throw new IllegalArgumentException("Missing closing parenthesis");
                }
                return inner;
            }

            throw new IllegalArgumentException("Unexpected token: " + token.raw());
        }

        private TokenType peekType() {
            Token token = queue.peekFirst();
            return token == null ? null : token.type();
        }

        private boolean hasRemainingTokens() {
            return !queue.isEmpty();
        }

        private String peekRaw() {
            Token token = queue.peekFirst();
            return token == null ? "<end>" : token.raw();
        }
    }
}
