package ec.com.sidesoft.custom.reports.ad_process;

import java.util.Random;

public class GenerateRandomCode {

  public static String generate_code() {
    Random aleatorio = new Random();
    String abecedario = "ABCDEFGHIJKLMNOPQRSTVWXYZ";
    String cadena = ""; // Inicializamos la Variable//
    int m = 0, pos = 0, num;
    while (m < 1) {
      pos = (int) (aleatorio.nextDouble() * abecedario.length() - 1 + 0);

      num = (int) (aleatorio.nextDouble() * 9999 + 100);
      cadena = cadena + abecedario.charAt(pos) + num;
      pos = (int) (aleatorio.nextDouble() * abecedario.length() - 1 + 0);
      cadena = cadena + abecedario.charAt(pos);

      // System.out.println("Tu codigo es: " + cadena);
      // cadena = "";
      m++;
    }
    return cadena;
  }

}