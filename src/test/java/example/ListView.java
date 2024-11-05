package example;

import sprouts.*;

public class ListView {

    public static void main(String[] args) {

        Vars<Integer> numbers = Vars.of(1, 2, 3);
        Vals<String> strings = numbers.view("n:null", "error", n -> "n:" + n);

        System.out.println(numbers);
        System.out.println(strings);

        System.out.println("remove: 2");
        numbers.removeAt(1);

        System.out.println(numbers);
        System.out.println(strings);

        System.out.println("add: 4");
        numbers.add(4);

        System.out.println(numbers);
        System.out.println(strings);

        System.out.println("setAt: 2, 5");
        numbers.setAt(2, 5);

        System.out.println(numbers);
        System.out.println(strings);

        System.out.println("at.set: 0, 6");
        numbers.at(0).set(From.ALL, 6);

        System.out.println(numbers);
        System.out.println(strings);

        System.out.println("at.set: 2, 7");
        numbers.at(2).set(From.ALL, 7);

        System.out.println(numbers);
        System.out.println(strings);
    }

    public static void test() {
        Vars<Integer> numbers = Vars.of(1, 2, 3);

        Var<Integer> singleNumber = numbers.at(1);
        Val<Integer> singleNumberView = numbers.at(1);

        System.out.println(singleNumber.get());
        System.out.println(singleNumberView.get());

        numbers.removeAt(1);

        System.out.println(singleNumber.get());
        System.out.println(singleNumberView.get());

        singleNumber.set(22);

        System.out.println(singleNumber.get());
        System.out.println(singleNumberView.get());
    }

}
