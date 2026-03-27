package com.blikeng.job.executor.payloads;

public record EncryptionResultPayload(
        String algorithm,
        byte[] cipherBytes,
        String iv,
        String key
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EncryptionResultPayload(String algorithm1, byte[] bytes, String iv1, String key1))) return false;

        return java.util.Objects.equals(algorithm, algorithm1)
                && java.util.Arrays.equals(cipherBytes, bytes)
                && java.util.Objects.equals(iv, iv1)
                && java.util.Objects.equals(key, key1);
    }

    @Override
    public int hashCode() {
        int result = java.util.Objects.hash(algorithm, iv, key);
        result = 31 * result + java.util.Arrays.hashCode(cipherBytes);
        return result;
    }

    @Override
    public String toString() {
        return "EncryptedPayload[" +
                "algorithm=" + algorithm +
                ", cipherBytes.length=" + (cipherBytes != null ? cipherBytes.length : 0) +
                ", iv=" + iv +
                ", key=***" +
                ']';
    }
}
