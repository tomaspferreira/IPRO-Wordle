public class Mathler {
    int length;

    Mathler(int length) {
        this.length = length;

        int result = 0;
        int[] numbers = new int[length];
        char[] eq_ops = new char[length - 1];
        char[] operators = {'+', '-', '*', '/'};
        do {
            String equation = "";
            numbers[0] = 1 + (int)(Math.random() * 100);
            int running = numbers[0];

            for (int i = 0; i < eq_ops.length; i++) {
                char op = operators[(int)(Math.random() * operators.length)];
                eq_ops[i] = op;

                if (op == '+' || op == '-') {
                    numbers[i + 1] = 1 + (int)(Math.random() * 199);
                    running = numbers[i + 1];
                }
                else if (op == '*') {
                    int factor = 2 + (int)(Math.random() * 10);
                    numbers[i + 1] = factor;
                    running *= factor;
                }
                else {
                    int[] divisors = new int[20];
                    int count = 0;

                    for (int d = 2; d <= 10; d++) {
                        if (running % d == 0) {
                            divisors[count++] = d;
                        }
                    }
                    int divisor = 0;
                    if (count == 0) {
                        divisor = 1;
                    } else {
                        divisor = divisors[(int)(Math.random() * count)];
                    }
                    numbers[i + 1] = divisor;
                    running /= divisor;
                }
            }
            for (int i = 0; i < numbers.length; i++) {
                equation += numbers[i];
                if (i < eq_ops.length) {
                    equation += eq_ops[i];
                }
            }

            int[] numbers2 = new int[numbers.length];
            char[] ops2 = new char[eq_ops.length];

            int nrCount = 0;
            int oCount = 0;

            numbers2[nrCount++] = numbers[0];

            for (int i = 0; i < eq_ops.length; i++) {
                char op = eq_ops[i];
                int right = numbers[i + 1];

                if (op == '*' || op == '/') {
                    int left = numbers2[nrCount - 1];
                    if (op == '*') {
                        numbers2[nrCount - 1] = left * right;
                    } else {
                        numbers2[nrCount - 1] = left / right;
                    }
                } else {
                    ops2[oCount++] = op;
                    numbers2[nrCount++] = right;
                }
            }

            result = numbers2[0];
            for (int i = 0; i < oCount; i++) {
                if (ops2[i] == '+') {
                    result += numbers2[i + 1];
                }
                else {
                    result -= numbers2[i + 1];
                }
            }
            IO.println(equation);
        } while (result < 0);
        IO.println(result);
    }
}