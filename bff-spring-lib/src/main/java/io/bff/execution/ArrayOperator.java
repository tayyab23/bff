package io.bff.execution;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public sealed interface ArrayOperator {

    Object apply(List<?> list);

    // ── Operators ──

    record CollectAll() implements ArrayOperator {
        public Object apply(List<?> list) { return new ArrayList<>(list); }
    }

    record IndexAccess(int index) implements ArrayOperator {
        public Object apply(List<?> list) {
            int i = index < 0 ? list.size() + index : index;
            return (i >= 0 && i < list.size()) ? list.get(i) : null;
        }
    }

    record Slice(Integer start, Integer end) implements ArrayOperator {
        public Object apply(List<?> list) {
            int s = resolveIndex(start, 0, list.size());
            int e = resolveIndex(end, list.size(), list.size());
            return s >= e ? List.of() : new ArrayList<>(list.subList(s, e));
        }
        private int resolveIndex(Integer val, int fallback, int size) {
            if (val == null) return fallback;
            int v = val < 0 ? size + val : val;
            return Math.max(0, Math.min(v, size));
        }
    }

    record Filter(String field, CompareOp op, Object value) implements ArrayOperator {
        public Object apply(List<?> list) {
            return list.stream().filter(item -> matches(item)).collect(Collectors.toList());
        }
        boolean matches(Object item) {
            Object actual = resolveField(item, field);
            return op.test(actual, value);
        }
    }

    record InFilter(String field, Set<Object> values) implements ArrayOperator {
        public Object apply(List<?> list) {
            return list.stream()
                    .filter(item -> {
                        Object actual = resolveField(item, field);
                        Object coerced = coerceForComparison(actual);
                        return values.stream().anyMatch(v -> Objects.equals(coerced, coerceForComparison(v)));
                    })
                    .collect(Collectors.toList());
        }
    }

    record RegexFilter(String field, Pattern pattern) implements ArrayOperator {
        public Object apply(List<?> list) {
            return list.stream()
                    .filter(item -> {
                        Object actual = resolveField(item, field);
                        return actual != null && pattern.matcher(actual.toString()).matches();
                    })
                    .collect(Collectors.toList());
        }
    }

    record ExistenceCheck(String field, boolean shouldExist) implements ArrayOperator {
        public Object apply(List<?> list) {
            return list.stream()
                    .filter(item -> {
                        Object actual = resolveField(item, field);
                        return shouldExist ? actual != null : actual == null;
                    })
                    .collect(Collectors.toList());
        }
    }

    record CompoundFilter(List<ArrayOperator> filters, LogicOp logic) implements ArrayOperator {
        public Object apply(List<?> list) {
            return list.stream()
                    .filter(item -> logic == LogicOp.AND
                            ? filters.stream().allMatch(f -> elementMatches(f, item))
                            : filters.stream().anyMatch(f -> elementMatches(f, item)))
                    .collect(Collectors.toList());
        }
        private boolean elementMatches(ArrayOperator f, Object item) {
            if (f instanceof Filter fi) return fi.matches(item);
            if (f instanceof InFilter inf) { var r = inf.apply(List.of(item)); return r instanceof List<?> l && !l.isEmpty(); }
            if (f instanceof RegexFilter rf) { var r = rf.apply(List.of(item)); return r instanceof List<?> l && !l.isEmpty(); }
            if (f instanceof ExistenceCheck ec) { var r = ec.apply(List.of(item)); return r instanceof List<?> l && !l.isEmpty(); }
            return false;
        }
    }

    enum LogicOp { AND, OR }

    // ── Comparison ──

    enum CompareOp {
        EQ  { boolean test(Object a, Object b) { return Objects.equals(coerceForComparison(a), coerceForComparison(b)); } },
        NEQ { boolean test(Object a, Object b) { return !Objects.equals(coerceForComparison(a), coerceForComparison(b)); } },
        GT  { boolean test(Object a, Object b) { return cmp(a, b) > 0; } },
        GTE { boolean test(Object a, Object b) { return cmp(a, b) >= 0; } },
        LT  { boolean test(Object a, Object b) { return cmp(a, b) < 0; } },
        LTE { boolean test(Object a, Object b) { return cmp(a, b) <= 0; } };

        abstract boolean test(Object actual, Object expected);

        private static int cmp(Object a, Object b) {
            Double da = toDouble(a), db = toDouble(b);
            if (da == null || db == null) return 0;
            return da.compareTo(db);
        }

        private static Double toDouble(Object v) {
            if (v instanceof Number n) return n.doubleValue();
            if (v instanceof String s) { try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; } }
            return null;
        }
    }

    // ── Shared helpers ──

    @SuppressWarnings("unchecked")
    static Object resolveField(Object item, String field) {
        if (!(item instanceof Map)) return null;
        Object current = item;
        for (String part : field.split("\\.")) {
            if (current instanceof Map<?, ?> map) current = map.get(part);
            else return null;
        }
        return current;
    }

    static Object coerceForComparison(Object v) {
        if (v instanceof Integer i) return i.doubleValue();
        if (v instanceof Long l) return l.doubleValue();
        if (v instanceof Float f) return f.doubleValue();
        return v;
    }

    // ── Parser ──

    static ArrayOperator parse(String bracket) {
        String inner = bracket.substring(1, bracket.length() - 1);
        if (inner.equals("*")) return new CollectAll();
        if (inner.startsWith("?")) return parseFilterExpr(inner.substring(1));
        if (inner.contains(":")) return parseSlice(inner);
        return new IndexAccess(Integer.parseInt(inner.trim()));
    }

    private static ArrayOperator parseFilterExpr(String expr) {
        // Compound: split on && or ||
        if (expr.contains("&&") || expr.contains("||")) {
            LogicOp logic = expr.contains("&&") ? LogicOp.AND : LogicOp.OR;
            String delimiter = logic == LogicOp.AND ? "&&" : "\\|\\|";
            String[] parts = expr.split(delimiter);
            List<ArrayOperator> filters = Arrays.stream(parts)
                    .map(String::trim)
                    .map(ArrayOperator::parseSingleFilter)
                    .toList();
            return new CompoundFilter(filters, logic);
        }
        return parseSingleFilter(expr);
    }

    private static ArrayOperator parseSingleFilter(String expr) {
        // Existence: "field exists" or "field missing"
        if (expr.endsWith(" exists")) return new ExistenceCheck(expr.substring(0, expr.length() - 7).trim(), true);
        if (expr.endsWith(" missing")) return new ExistenceCheck(expr.substring(0, expr.length() - 8).trim(), false);

        // In: "field in (a,b,c)"
        int inIdx = expr.indexOf(" in (");
        if (inIdx > 0 && expr.endsWith(")")) {
            String field = expr.substring(0, inIdx).trim();
            String valuesPart = expr.substring(inIdx + 5, expr.length() - 1);
            Set<Object> values = Arrays.stream(valuesPart.split(","))
                    .map(String::trim)
                    .map(ArrayOperator::parseValue)
                    .collect(Collectors.toSet());
            return new InFilter(field, values);
        }

        // Regex: "field==REG(...)"
        String[][] ops = {{">=", "GTE"}, {"<=", "LTE"}, {"!=", "NEQ"}, {">", "GT"}, {"<", "LT"}, {"==", "EQ"}};
        for (String[] op : ops) {
            int idx = expr.indexOf(op[0]);
            if (idx > 0) {
                String field = expr.substring(0, idx).trim();
                String raw = expr.substring(idx + op[0].length()).trim();
                if (raw.startsWith("REG(") && raw.endsWith(")")) {
                    String pattern = raw.substring(4, raw.length() - 1);
                    return new RegexFilter(field, Pattern.compile(pattern));
                }
                return new Filter(field, CompareOp.valueOf(op[1]), parseValue(raw));
            }
        }
        throw new IllegalArgumentException("Invalid filter expression: " + expr);
    }

    private static ArrayOperator parseSlice(String s) {
        String[] parts = s.split(":", -1);
        Integer start = parts[0].trim().isEmpty() ? null : Integer.parseInt(parts[0].trim());
        Integer end = parts[1].trim().isEmpty() ? null : Integer.parseInt(parts[1].trim());
        return new Slice(start, end);
    }

    static Object parseValue(String raw) {
        if ("null".equals(raw)) return null;
        if ("true".equals(raw)) return Boolean.TRUE;
        if ("false".equals(raw)) return Boolean.FALSE;
        if (raw.matches("^-?\\d+(\\.\\d+)?$")) return Double.parseDouble(raw);
        return raw;
    }
}
