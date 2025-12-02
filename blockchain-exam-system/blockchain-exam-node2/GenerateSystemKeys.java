import java.security.*;
import java.util.Base64;

public class GenerateSystemKeys {
    public static void main(String[] args) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();

        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        System.out.println("PUBLIC_KEY:");
        System.out.println(publicKey);
        System.out.println("\nPRIVATE_KEY:");
        System.out.println(privateKey);
    }
}
