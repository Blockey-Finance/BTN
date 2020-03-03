# ECDSA in java
```java
ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
g.initialize(ecSpec, new SecureRandom());
KeyPair keypair = g.generateKeyPair();
PublicKey publicKey = keypair.getPublic();
PrivateKey privateKey = keypair.getPrivate(); 
```

# Merkle Patricia Trie
Account Model  

https://github.com/ethereum/wiki/wiki/Patricia-Tree  
