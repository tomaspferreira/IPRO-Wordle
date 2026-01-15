public class Mathler {
    int length;

    Mathler(int length) {
        this.length = length;

        String equation = "";
        char[] operators = {'+', '-', '*', '/'};

        for (int i = 0; i < length; i++) {

            if (i % 3 == 2) {
                int r = (int) (Math.random() * operators.length);
                equation += operators[r];
            } else {
                equation += (int) (Math.random() * 10);
            }
        }
        IO.println(equation);
        int count_numbers = 0;
        boolean inNumber = false;

        for (int i = 0; i < equation.length(); i++) {
            char c = equation.charAt(i);

            if (Character.isDigit(c)) {
                if (!inNumber) {
                    count_numbers++;
                    inNumber = true;
                }
            } else {
                inNumber = false;
            }
        }
        int[] numbers = new int[count_numbers];
        for (int i = 0; i < equation.length(); i++) {
            char c = equation.charAt(i);
            if (Character.isDigit(c)) {
                if (Character.isDigit(c + 1)) {
                    if (Character.isDigit(c + 2)) {
                        for (int j = 0; j < numbers.length; j++) {
                            if (numbers[j] % 100 == 0) {
                                numbers[i] += (int) c;
                                break;
                            }
                        }
                    }
                } else {
                    for (int j = 0; j < numbers.length; j++) {
                        if (numbers[j] % 10 == 0) {
                            numbers[i] += (int) c;
                            break;
                        }
                    }
                }
            } else {
                for (int j = 0; j < numbers.length; j++) {
                    if (numbers[j] % 10 == 0) {
                        numbers[i] += (int) c;
                        break;
                    }
                }
            }
        }
        IO.println(numbers);
    }
}
