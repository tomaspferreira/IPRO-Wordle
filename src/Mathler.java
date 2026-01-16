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
        String.
    }
}
