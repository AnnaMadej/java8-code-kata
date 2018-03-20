package stream.api;

import common.test.tool.annotation.Difficult;
import common.test.tool.annotation.Easy;
import common.test.tool.dataset.ClassicOnlineStore;
import common.test.tool.entity.Customer;
import common.test.tool.util.CollectorImpl;

import org.junit.Test;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

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
        Supplier<StringBuilder> supplier = StringBuilder::new;
        BiConsumer<StringBuilder, String> accumulator = (sb, str)-> sb.append(str).append(",");
        BinaryOperator<StringBuilder> combiner = StringBuilder::append;
        Function<StringBuilder, String> finisher = (sb)->{
            sb.deleteCharAt(sb.length()-1);
            return sb.toString();
        };

        Collector<String, StringBuilder, String> toCsv =
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
        BiConsumer<Map<String, Set<String>>, Customer> accumulator =
                (map, cus) -> cus.getWantToBuy().forEach(itm -> {
                  map.putIfAbsent(itm.getName(), new HashSet<>());
                  map.get(itm.getName()).add(cus.getName());
        });
        BinaryOperator<Map<String, Set<String>>> combiner = (map1, map2) -> {
            map2.forEach((str, set)-> map1.merge(str, set, (set1, set2)-> {
                set1.addAll(set2);
                return set1;
            }));
            return map1;
        };
        Function<Map<String, Set<String>>, Map<String, Set<String>>> finisher = (map)->map;

        Collector<Customer, ?, Map<String, Set<String>>> toItemAsKey =
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
        Supplier<StringBuilder> supplier = StringBuilder::new;
        BiConsumer<StringBuilder, String > accumulator = (stringBuilder, inputString)-> {
            int start;
            int stop;
            if(inputString.contains("-")){
                String[] parts = inputString.split("-");
                start=Integer.valueOf(parts[0]);
                stop=Integer.valueOf(parts[1]);
            }else start=stop=Integer.valueOf(inputString);
            if(stringBuilder.length()<stop){
                for (int i = stringBuilder.length(); i<stop;i++ )
                    stringBuilder.append("0");
            }
            for(int i = start; i<=stop; i++){
                stringBuilder.setCharAt(i-1, '1');
            }
        };
        BinaryOperator<StringBuilder> combiner = null;
        Function<StringBuilder, String> finisher = StringBuilder::toString;

        Collector<String, ?, String> toBitString =
                new CollectorImpl<>(supplier, accumulator, combiner, finisher, Collections.emptySet());

        String bitString = Arrays.stream(bitList.split(",")).collect(toBitString);
        assertThat(bitString, is("01011000101001111000011100000000100001110111010101")

        );
    }
}
