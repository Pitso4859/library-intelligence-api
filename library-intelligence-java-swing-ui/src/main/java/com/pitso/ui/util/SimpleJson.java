package com.pitso.ui.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal JSON parser/writer so the desktop client stays dependency-free. */
public final class SimpleJson {
    private SimpleJson() {}

    public static Object parse(String json) {
        if (json == null) return null;
        Parser parser = new Parser(json);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.isEnd()) {
            throw new IllegalArgumentException("Unexpected content at position " + parser.position());
        }
        return value;
    }

    public static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        writeValue(out, value);
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String s) {
            out.append('"').append(escape(s)).append('"');
        } else if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) out.append(',');
                first = false;
                out.append('"').append(escape(String.valueOf(entry.getKey()))).append('"').append(':');
                writeValue(out, entry.getValue());
            }
            out.append('}');
        } else if (value instanceof Iterable<?> iterable) {
            out.append('[');
            boolean first = true;
            for (Object item : iterable) {
                if (!first) out.append(',');
                first = false;
                writeValue(out, item);
            }
            out.append(']');
        } else {
            out.append('"').append(escape(String.valueOf(value))).append('"');
        }
    }

    private static String escape(String text) {
        StringBuilder out = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.toString();
    }

    private static final class Parser {
        private final String input;
        private int pos;

        private Parser(String input) {
            this.input = input;
        }

        private int position() { return pos; }
        private boolean isEnd() { return pos >= input.length(); }
        private void skipWhitespace() {
            while (!isEnd() && Character.isWhitespace(input.charAt(pos))) pos++;
        }

        private Object parseValue() {
            skipWhitespace();
            if (isEnd()) throw error("Unexpected end of JSON");
            char c = input.charAt(pos);
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (c == '-' || Character.isDigit(c)) yield parseNumber();
                    throw error("Unexpected character '" + c + "'");
                }
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                map.put(key, parseValue());
                skipWhitespace();
                if (peek('}')) {
                    pos++;
                    return map;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                pos++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    pos++;
                    return list;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (!isEnd()) {
                char c = input.charAt(pos++);
                if (c == '"') return out.toString();
                if (c == '\\') {
                    if (isEnd()) throw error("Unterminated escape sequence");
                    char e = input.charAt(pos++);
                    switch (e) {
                        case '"' -> out.append('"');
                        case '\\' -> out.append('\\');
                        case '/' -> out.append('/');
                        case 'b' -> out.append('\b');
                        case 'f' -> out.append('\f');
                        case 'n' -> out.append('\n');
                        case 'r' -> out.append('\r');
                        case 't' -> out.append('\t');
                        case 'u' -> {
                            if (pos + 4 > input.length()) throw error("Invalid unicode escape");
                            String hex = input.substring(pos, pos + 4);
                            pos += 4;
                            out.append((char) Integer.parseInt(hex, 16));
                        }
                        default -> throw error("Invalid escape sequence: \\" + e);
                    }
                } else {
                    out.append(c);
                }
            }
            throw error("Unterminated string");
        }

        private Object parseNumber() {
            int start = pos;
            if (peek('-')) pos++;
            while (!isEnd() && Character.isDigit(input.charAt(pos))) pos++;
            boolean decimal = false;
            if (!isEnd() && input.charAt(pos) == '.') {
                decimal = true;
                pos++;
                while (!isEnd() && Character.isDigit(input.charAt(pos))) pos++;
            }
            if (!isEnd() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
                decimal = true;
                pos++;
                if (!isEnd() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) pos++;
                while (!isEnd() && Character.isDigit(input.charAt(pos))) pos++;
            }
            String number = input.substring(start, pos);
            return decimal ? Double.parseDouble(number) : Long.parseLong(number);
        }

        private Object parseLiteral(String literal, Object value) {
            if (!input.startsWith(literal, pos)) throw error("Expected " + literal);
            pos += literal.length();
            return value;
        }

        private void expect(char expected) {
            skipWhitespace();
            if (isEnd() || input.charAt(pos) != expected) {
                throw error("Expected '" + expected + "'");
            }
            pos++;
        }

        private boolean peek(char c) {
            return !isEnd() && input.charAt(pos) == c;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at position " + pos);
        }
    }
}
