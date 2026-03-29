package io.bff.execution;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ArrayOperatorTest {

    // Helper to build a list of maps simulating JSON array of objects
    static Map<String, Object> item(Object... kvs) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kvs.length; i += 2) m.put((String) kvs[i], kvs[i + 1]);
        return m;
    }

    static List<?> items = List.of(
        item("id", "inv-001", "amount", 249.99, "status", "UNPAID",   "active", true,  "description", "Pro Plan - March"),
        item("id", "inv-002", "amount", 349.95, "status", "OVERDUE",  "active", true,  "description", "Pro Plan - Feb"),
        item("id", "inv-003", "amount", 50.00,  "status", "PAID",     "active", false, "description", "Starter Plan"),
        item("id", "inv-004", "amount", 999.99, "status", "OVERDUE",  "active", true,  "description", "Enterprise Annual"),
        item("id", "inv-005", "amount", 100.00, "status", "UNPAID",   "active", true,  "description", "Add-on Storage")
    );

    // ── CollectAll ──

    @Nested class CollectAllTests {
        @Test void collectsAll() {
            var op = ArrayOperator.parse("[*]");
            var result = (List<?>) op.apply(items);
            assertEquals(5, result.size());
        }

        @Test void emptyList() {
            var result = (List<?>) new ArrayOperator.CollectAll().apply(List.of());
            assertTrue(result.isEmpty());
        }
    }

    // ── IndexAccess ──

    @Nested class IndexAccessTests {
        @Test void firstElement() {
            var op = ArrayOperator.parse("[0]");
            var result = (Map<?, ?>) op.apply(items);
            assertEquals("inv-001", result.get("id"));
        }

        @Test void lastByPositiveIndex() {
            var result = (Map<?, ?>) ArrayOperator.parse("[4]").apply(items);
            assertEquals("inv-005", result.get("id"));
        }

        @Test void negativeIndex() {
            var result = (Map<?, ?>) ArrayOperator.parse("[-1]").apply(items);
            assertEquals("inv-005", result.get("id"));
        }

        @Test void negativeIndexSecondFromEnd() {
            var result = (Map<?, ?>) ArrayOperator.parse("[-2]").apply(items);
            assertEquals("inv-004", result.get("id"));
        }

        @Test void outOfBoundsPositive() {
            assertNull(ArrayOperator.parse("[99]").apply(items));
        }

        @Test void outOfBoundsNegative() {
            assertNull(ArrayOperator.parse("[-99]").apply(items));
        }

        @Test void emptyList() {
            assertNull(ArrayOperator.parse("[0]").apply(List.of()));
        }
    }

    // ── Slice ──

    @Nested class SliceTests {
        @Test void fullRange() {
            var result = (List<?>) ArrayOperator.parse("[0:5]").apply(items);
            assertEquals(5, result.size());
        }

        @Test void firstTwo() {
            var result = (List<?>) ArrayOperator.parse("[0:2]").apply(items);
            assertEquals(2, result.size());
            assertEquals("inv-001", ((Map<?, ?>) result.get(0)).get("id"));
            assertEquals("inv-002", ((Map<?, ?>) result.get(1)).get("id"));
        }

        @Test void fromStart() {
            var result = (List<?>) ArrayOperator.parse("[:3]").apply(items);
            assertEquals(3, result.size());
        }

        @Test void toEnd() {
            var result = (List<?>) ArrayOperator.parse("[3:]").apply(items);
            assertEquals(2, result.size());
            assertEquals("inv-004", ((Map<?, ?>) result.get(0)).get("id"));
        }

        @Test void negativeStart() {
            var result = (List<?>) ArrayOperator.parse("[-2:]").apply(items);
            assertEquals(2, result.size());
            assertEquals("inv-004", ((Map<?, ?>) result.get(0)).get("id"));
            assertEquals("inv-005", ((Map<?, ?>) result.get(1)).get("id"));
        }

        @Test void negativeEnd() {
            var result = (List<?>) ArrayOperator.parse("[:-2]").apply(items);
            assertEquals(3, result.size());
        }

        @Test void emptySlice() {
            var result = (List<?>) ArrayOperator.parse("[3:3]").apply(items);
            assertTrue(result.isEmpty());
        }

        @Test void invertedRange() {
            var result = (List<?>) ArrayOperator.parse("[4:2]").apply(items);
            assertTrue(result.isEmpty());
        }

        @Test void beyondBounds() {
            var result = (List<?>) ArrayOperator.parse("[0:100]").apply(items);
            assertEquals(5, result.size());
        }

        @Test void emptyList() {
            var result = (List<?>) ArrayOperator.parse("[0:5]").apply(List.of());
            assertTrue(result.isEmpty());
        }
    }

    // ── Filter: Equality ──

    @Nested class FilterEqualityTests {
        @Test void stringEquals() {
            var result = (List<?>) ArrayOperator.parse("[?status==OVERDUE]").apply(items);
            assertEquals(2, result.size());
            assertEquals("inv-002", ((Map<?, ?>) result.get(0)).get("id"));
            assertEquals("inv-004", ((Map<?, ?>) result.get(1)).get("id"));
        }

        @Test void stringNotEquals() {
            var result = (List<?>) ArrayOperator.parse("[?status!=PAID]").apply(items);
            assertEquals(4, result.size());
        }

        @Test void noMatch() {
            var result = (List<?>) ArrayOperator.parse("[?status==CANCELLED]").apply(items);
            assertTrue(result.isEmpty());
        }

        @Test void allMatch() {
            // all items have an "id" field that's a string
            var result = (List<?>) ArrayOperator.parse("[?id!=NONEXISTENT]").apply(items);
            assertEquals(5, result.size());
        }
    }

    // ── Filter: Numeric Comparison ──

    @Nested class FilterNumericTests {
        @Test void greaterThan() {
            var result = (List<?>) ArrayOperator.parse("[?amount>300]").apply(items);
            assertEquals(2, result.size()); // 349.95, 999.99
        }

        @Test void greaterThanOrEqual() {
            var result = (List<?>) ArrayOperator.parse("[?amount>=249.99]").apply(items);
            assertEquals(3, result.size()); // 249.99, 349.95, 999.99
        }

        @Test void lessThan() {
            var result = (List<?>) ArrayOperator.parse("[?amount<100]").apply(items);
            assertEquals(1, result.size()); // 50.00
            assertEquals("inv-003", ((Map<?, ?>) result.get(0)).get("id"));
        }

        @Test void lessThanOrEqual() {
            var result = (List<?>) ArrayOperator.parse("[?amount<=100]").apply(items);
            assertEquals(2, result.size()); // 50.00, 100.00
        }

        @Test void exactNumericMatch() {
            var result = (List<?>) ArrayOperator.parse("[?amount==50.0]").apply(items);
            assertEquals(1, result.size());
            assertEquals("inv-003", ((Map<?, ?>) result.get(0)).get("id"));
        }
    }

    // ── Filter: Boolean ──

    @Nested class FilterBooleanTests {
        @Test void booleanTrue() {
            var result = (List<?>) ArrayOperator.parse("[?active==true]").apply(items);
            assertEquals(4, result.size());
        }

        @Test void booleanFalse() {
            var result = (List<?>) ArrayOperator.parse("[?active==false]").apply(items);
            assertEquals(1, result.size());
            assertEquals("inv-003", ((Map<?, ?>) result.get(0)).get("id"));
        }
    }

    // ── Filter: Null ──

    @Nested class FilterNullTests {
        @Test void equalsNull() {
            var withNull = List.of(
                item("id", "a", "coupon", null),
                item("id", "b", "coupon", "SAVE10"),
                item("id", "c")  // coupon field missing entirely
            );
            var result = (List<?>) ArrayOperator.parse("[?coupon==null]").apply(withNull);
            assertEquals(2, result.size()); // null + missing
        }

        @Test void notEqualsNull() {
            var withNull = List.of(
                item("id", "a", "coupon", null),
                item("id", "b", "coupon", "SAVE10"),
                item("id", "c")
            );
            var result = (List<?>) ArrayOperator.parse("[?coupon!=null]").apply(withNull);
            assertEquals(1, result.size());
            assertEquals("b", ((Map<?, ?>) result.get(0)).get("id"));
        }
    }

    // ── Existence ──

    @Nested class ExistenceTests {
        @Test void fieldExists() {
            var mixed = List.of(
                item("id", "a", "coupon", "SAVE10"),
                item("id", "b"),
                item("id", "c", "coupon", null),
                item("id", "d", "coupon", "FREE")
            );
            var result = (List<?>) ArrayOperator.parse("[?coupon exists]").apply(mixed);
            assertEquals(2, result.size()); // "a" and "d" — null doesn't count as exists
            assertEquals("a", ((Map<?, ?>) result.get(0)).get("id"));
            assertEquals("d", ((Map<?, ?>) result.get(1)).get("id"));
        }

        @Test void fieldMissing() {
            var mixed = List.of(
                item("id", "a", "coupon", "SAVE10"),
                item("id", "b"),
                item("id", "c", "coupon", null),
                item("id", "d", "coupon", "FREE")
            );
            var result = (List<?>) ArrayOperator.parse("[?coupon missing]").apply(mixed);
            assertEquals(2, result.size()); // "b" (absent) and "c" (null)
        }
    }

    // ── In ──

    @Nested class InFilterTests {
        @Test void stringSet() {
            var result = (List<?>) ArrayOperator.parse("[?status in (OVERDUE,UNPAID)]").apply(items);
            assertEquals(4, result.size());
        }

        @Test void singleValue() {
            var result = (List<?>) ArrayOperator.parse("[?status in (PAID)]").apply(items);
            assertEquals(1, result.size());
        }

        @Test void numericSet() {
            var result = (List<?>) ArrayOperator.parse("[?amount in (50.0,100.0)]").apply(items);
            assertEquals(2, result.size());
        }

        @Test void noMatch() {
            var result = (List<?>) ArrayOperator.parse("[?status in (CANCELLED,REFUNDED)]").apply(items);
            assertTrue(result.isEmpty());
        }
    }

    // ── Regex ──

    @Nested class RegexFilterTests {
        @Test void matchesPrefix() {
            var result = (List<?>) ArrayOperator.parse("[?description==REG(^Pro Plan.*)]").apply(items);
            assertEquals(2, result.size());
            assertEquals("inv-001", ((Map<?, ?>) result.get(0)).get("id"));
            assertEquals("inv-002", ((Map<?, ?>) result.get(1)).get("id"));
        }

        @Test void matchesSuffix() {
            var result = (List<?>) ArrayOperator.parse("[?description==REG(.*Storage$)]").apply(items);
            assertEquals(1, result.size());
            assertEquals("inv-005", ((Map<?, ?>) result.get(0)).get("id"));
        }

        @Test void matchesContains() {
            var result = (List<?>) ArrayOperator.parse("[?description==REG(.*Plan.*)]").apply(items);
            assertEquals(3, result.size());
        }

        @Test void noMatch() {
            var result = (List<?>) ArrayOperator.parse("[?description==REG(^Enterprise$)]").apply(items);
            assertTrue(result.isEmpty());
        }

        @Test void nullFieldSkipped() {
            var withNull = List.of(item("id", "a", "desc", null), item("id", "b", "desc", "hello"));
            var result = (List<?>) ArrayOperator.parse("[?desc==REG(.*llo)]").apply(withNull);
            assertEquals(1, result.size());
        }
    }

    // ── Compound AND ──

    @Nested class CompoundAndTests {
        @Test void twoConditions() {
            var result = (List<?>) ArrayOperator.parse("[?status==OVERDUE&&amount>500]").apply(items);
            assertEquals(1, result.size());
            assertEquals("inv-004", ((Map<?, ?>) result.get(0)).get("id"));
        }

        @Test void threeConditions() {
            var result = (List<?>) ArrayOperator.parse("[?status==OVERDUE&&amount>100&&active==true]").apply(items);
            assertEquals(2, result.size());
        }

        @Test void noMatch() {
            var result = (List<?>) ArrayOperator.parse("[?status==PAID&&amount>1000]").apply(items);
            assertTrue(result.isEmpty());
        }

        @Test void allMatch() {
            var result = (List<?>) ArrayOperator.parse("[?amount>0&&id!=NONE]").apply(items);
            assertEquals(5, result.size());
        }
    }

    // ── Compound OR ──

    @Nested class CompoundOrTests {
        @Test void eitherCondition() {
            var result = (List<?>) ArrayOperator.parse("[?status==PAID||status==OVERDUE]").apply(items);
            assertEquals(3, result.size()); // PAID(1) + OVERDUE(2)
        }

        @Test void oneMatches() {
            var result = (List<?>) ArrayOperator.parse("[?amount>9999||status==PAID]").apply(items);
            assertEquals(1, result.size());
            assertEquals("inv-003", ((Map<?, ?>) result.get(0)).get("id"));
        }

        @Test void neitherMatches() {
            var result = (List<?>) ArrayOperator.parse("[?status==CANCELLED||amount>10000]").apply(items);
            assertTrue(result.isEmpty());
        }
    }

    // ── Nested Field Access in Filters ──

    @Nested class NestedFieldTests {
        static List<?> nested = List.of(
            item("id", "a", "billing", Map.of("region", "US", "method", "CARD")),
            item("id", "b", "billing", Map.of("region", "EU", "method", "BANK")),
            item("id", "c", "billing", Map.of("region", "US", "method", "PAYPAL")),
            item("id", "d", "billing", Map.of("region", "APAC", "method", "CARD"))
        );

        @Test void nestedEquals() {
            var result = (List<?>) ArrayOperator.parse("[?billing.region==US]").apply(nested);
            assertEquals(2, result.size());
        }

        @Test void nestedNotEquals() {
            var result = (List<?>) ArrayOperator.parse("[?billing.region!=US]").apply(nested);
            assertEquals(2, result.size());
        }

        @Test void nestedCompound() {
            var result = (List<?>) ArrayOperator.parse("[?billing.region==US&&billing.method==CARD]").apply(nested);
            assertEquals(1, result.size());
            assertEquals("a", ((Map<?, ?>) result.get(0)).get("id"));
        }

        @Test void nestedIn() {
            var result = (List<?>) ArrayOperator.parse("[?billing.region in (US,EU)]").apply(nested);
            assertEquals(3, result.size());
        }

        @Test void missingNestedField() {
            var partial = List.of(item("id", "a", "billing", Map.of("region", "US")), item("id", "b"));
            var result = (List<?>) ArrayOperator.parse("[?billing.region==US]").apply(partial);
            assertEquals(1, result.size());
        }
    }

    // ── parseValue ──

    @Nested class ParseValueTests {
        @Test void parsesNull()    { assertNull(ArrayOperator.parseValue("null")); }
        @Test void parsesTrue()    { assertEquals(Boolean.TRUE, ArrayOperator.parseValue("true")); }
        @Test void parsesFalse()   { assertEquals(Boolean.FALSE, ArrayOperator.parseValue("false")); }
        @Test void parsesInt()     { assertEquals(42.0, ArrayOperator.parseValue("42")); }
        @Test void parsesDouble()  { assertEquals(3.14, ArrayOperator.parseValue("3.14")); }
        @Test void parsesNeg()     { assertEquals(-10.0, ArrayOperator.parseValue("-10")); }
        @Test void parsesString()  { assertEquals("hello", ArrayOperator.parseValue("hello")); }
    }

    // ── FieldMapper.extractPath integration with operators ──

    @Nested class ExtractPathTests {
        static Map<String, Object> body = Map.of("items", items);

        @Test void collectAllIds() {
            var result = (List<?>) FieldMapper.extractPath(body, "items[*].id");
            assertEquals(List.of("inv-001", "inv-002", "inv-003", "inv-004", "inv-005"), result);
        }

        @Test void filterThenPluck() {
            var result = (List<?>) FieldMapper.extractPath(body, "items[?status==OVERDUE].id");
            assertEquals(List.of("inv-002", "inv-004"), result);
        }

        @Test void sliceThenPluck() {
            var result = (List<?>) FieldMapper.extractPath(body, "items[:2].id");
            assertEquals(List.of("inv-001", "inv-002"), result);
        }

        @Test void indexAccess() {
            var result = FieldMapper.extractPath(body, "items[0].id");
            assertEquals("inv-001", result);
        }

        @Test void negativeIndex() {
            var result = FieldMapper.extractPath(body, "items[-1].id");
            assertEquals("inv-005", result);
        }

        @Test void collectWithoutPluck() {
            var result = (List<?>) FieldMapper.extractPath(body, "items[*]");
            assertEquals(5, result.size());
        }

        @Test void filterWithoutPluck() {
            var result = (List<?>) FieldMapper.extractPath(body, "items[?status==PAID]");
            assertEquals(1, result.size());
        }

        @Test void nonArrayPath() {
            var result = FieldMapper.extractPath(Map.of("name", "test"), "name[*]");
            assertNull(result);
        }

        @Test void missingPath() {
            var result = FieldMapper.extractPath(body, "nonexistent[*].id");
            assertNull(result);
        }

        @Test void emptyFilterResult() {
            var result = (List<?>) FieldMapper.extractPath(body, "items[?status==CANCELLED].id");
            assertTrue(result.isEmpty());
        }
    }
}
