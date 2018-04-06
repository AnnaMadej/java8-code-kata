package stream.api;

import common.test.tool.annotation.Difficult;
import common.test.tool.annotation.Easy;
import common.test.tool.dataset.ClassicOnlineStore;
import common.test.tool.entity.Customer;
import common.test.tool.entity.Item;
import common.test.tool.util.CollectorImpl;

import org.junit.Test;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class Exercise9Test extends ClassicOnlineStore {

    @Easy @Test
    public void simplestStringJoin() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a String with comma separated names shown in the assertion.
         * The collector will be used by serial stream.
         */
        Supplier<List<String>> supplier = ArrayList::new;
        BiConsumer<List<String>, String> accumulator = List::add;
        BinaryOperator<List<String>> combiner = null;
        Function<List<String>, String> finisher = (list) -> list.stream().collect(Collectors.joining(","));

        Collector<String, ?, String> toCsv =
            new CollectorImpl<>(supplier, accumulator, combiner, finisher, Collections.emptySet());
        String nameAsCsv = customerList.stream().map(Customer::getName).collect(toCsv);
        assertThat(nameAsCsv, is("Joe,Steven,Patrick,Diana,Chris,Kathy,Alice,Andrew,Martin,Amy"));
    }

    @Difficult @Test
    public void mapKeyedByItems() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a {@link Map} with keys as item and
         * values as {@link Set} of customers who are wanting to buy that item.
         * The collector will be used by parallel stream.
         */
        Supplier<Map<String, Set<String>>> supplier = HashMap::new;

        BiConsumer<Map<String, Set<String>>, Customer> accumulator = (map, cus) -> {
            cus.getWantToBuy().stream()
                              .map(Item::getName)
                              .forEach(str-> map.merge(str, Stream.of(cus.getName()).collect(Collectors.toSet()), (set1, set2)-> {
                                  set1.addAll(set2);
                                  return set1;}));
        };
        BinaryOperator<Map<String,Set<String>>> combiner = (m1, m2)-> {
            m2.forEach((item, set)-> m1.merge(item, set, (s1,s2)->{
                s1.addAll(s2);
                return s1;
            }));
            return m1;
        };
        Function<Map<String,Set<String>>, Map<String, Set<String>>> finisher = null;

        Collector<Customer, Map<String, Set<String>>, Map<String, Set<String>>> toItemAsKey =
            new CollectorImpl<>(supplier, accumulator, combiner, finisher, EnumSet.of(
                Collector.Characteristics.CONCURRENT,
                Collector.Characteristics.IDENTITY_FINISH));
        Map<String, Set<String>> itemMap = customerList.stream().parallel().collect(toItemAsKey);
        assertThat(itemMap.get("plane"), containsInAnyOrder("Chris"));
        assertThat(itemMap.get("onion"), containsInAnyOrder("Patrick", "Amy"));
        assertThat(itemMap.get("ice cream"), containsInAnyOrder("Patrick", "Steven"));
        assertThat(itemMap.get("earphone"), containsInAnyOrder("Steven"));
        assertThat(itemMap.get("plate"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("fork"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("cable"), containsInAnyOrder("Diana", "Steven"));
        assertThat(itemMap.get("desk"), containsInAnyOrder("Alice"));
    }

    @Difficult @Test
    public void bitList2BitString() {
        String bitList = "22-24,9,42-44,11,4,46,14-17,5,2,38-40,33,50,48";

        /**
         * Create a {@link String} of "n"th bit ON.
         * for example
         * "3" will be "001"
         * "1,3,5" will be "10101"
         * "1-3" will be "111"
         * "7,1-3,5" will be "1110101"
         */

        Supplier<Set<Integer>> supplier = HashSet::new;

        BiConsumer<Set<Integer>, String> accumulator = ((set, s) -> {
            List<Integer> split = Arrays.stream(s.split("-")).map(Integer::valueOf).collect(Collectors.toList());
            set.addAll((split.size()<2) ? split :
            Stream.iterate(split.get(0), nr -> ++nr)
                  .limit(split.get(1)-split.get(0)+1).collect(Collectors.toList()));
            BinaryOperator<Set<Integer>> combiner = null;
        });

        BinaryOperator<Set<Integer>> combiner = null;

        Function<Set<Integer>, String> finisher = set->{
            Integer max = set.stream().max(Comparator.comparingInt(anInt -> anInt)).get();
            List<String> stringList= Stream.generate(()->"0").limit(max).collect(Collectors.toList());
            set.forEach(integer -> stringList.set(integer-1, "1"));
            return stringList.stream().collect(Collectors.joining(""));
        };

        Collector<String, Set<Integer>, String> toBitString = new CollectorImpl<>(supplier, accumulator, combiner, finisher, Collections.emptySet());
        String bitString = Arrays.stream(bitList.split(",")).collect(toBitString);
        assertThat(bitString, is("01011000101001111000011100000000100001110111010101")

        );
    }
}
