package org.iota.ict.utils.crypto;

import org.iota.ict.utils.Trytes;

public class MerkleTree {

    private final SignatureScheme.PrivateKey[] privateKeys;
    private final SignatureScheme.PublicKey[] publicKeys;
    private final String[] nodes;
    private final int depth;

    public MerkleTree(String seed, int depth) {
        this.depth = depth;
        int width = (int)Math.pow(2, depth);
        privateKeys = new SignatureScheme.PrivateKey[width];
        publicKeys = new SignatureScheme.PublicKey[width];
        initKeys(seed, 3);
        nodes = calcNodes();
    }

    public String getAddress() {
        return nodes[nodes.length-1];
    }

    private String[] calcNodes() {
        String[] nodes = new String[privateKeys.length * 2 - 1];

        for(int i = 0; i < privateKeys.length; i++) {
            nodes[i] = publicKeys[i].getAddress();
        }

        for(int i = 0; i < nodes.length/2; i++) {
            nodes[i + privateKeys.length] = hashNodes(nodes[2*i], nodes[2*i+1]);
        }

        return nodes;
    }

    private void initKeys(String seed, int securityLevel) {
        for(int i = 0; i < privateKeys.length; i++) {
            privateKeys[i] = SignatureSchemeImplementation.derivePrivateKeyFromSeed(seed, i, securityLevel);
            publicKeys[i] = privateKeys[i].derivePublicKey();
        }
    }

    public Signature sign(int index, String toSign) {
        String[] merklePath = genMerklePath(index);
        return new Signature(privateKeys[index].sign(toSign).toString(), merklePath, toSign);
    }

    private String[] genMerklePath(int index) {
        if(index < 0 || index >= privateKeys.length)
            throw new IllegalArgumentException("index out of range");
        String[] merklePath = new String[depth];

        int layerOffset = 0;
        for(int layer = 0; layer < depth; layer++) {
            int ancestorIndexInLayer = (int)(index / Math.pow(2, layer));
            int ancestorIndex = layerOffset + ancestorIndexInLayer;
            int ancestorsSiblingIndex = ancestorIndex + (ancestorIndex%2==0 ? 1 : -1);
            merklePath[layer] = nodes[ancestorsSiblingIndex];
            layerOffset += (int)Math.pow(2, depth-layer);
        }

        return merklePath;
    }

    private static String hashNodes(String nodeA, String nodeB) {
        int comp = nodeA.compareTo(nodeB);
        return SignatureSchemeImplementation.hash((comp < 0 ? nodeA : nodeB) + (comp < 0 ? nodeB : nodeA));
    }

    public int getDepth() {
        return depth;
    }

    public static class Signature extends SignatureSchemeImplementation.Signature {

        private final String[] merklePath;
        private String address;

        public Signature(String trytes, String merklePath[], String signed) {
            super(trytes, signed);
            this.merklePath = merklePath;
        }

        public static Signature fromTrytesConcatenatedWithMerklePath(String trytesConcatenateWithMerklePath, String signed) {
            int trytesLength = signed.length() * 81;
            String trytes = trytesConcatenateWithMerklePath.substring(0, trytesLength);
            String[] merklePath = trytesConcatenateWithMerklePath.substring(trytes.length()).split("(?<=\\G.{81})");
            return new Signature(trytes, merklePath, signed);
        }

        @Override
        public String toString() {
            StringBuilder merklePathString = new StringBuilder();
            for(String merkleNode : merklePath)
                merklePathString.append(merkleNode);
            return super.toString() + merklePathString;
        }

        @Override
        public int length() {
            return super.length() + merklePath.length * SignatureSchemeImplementation.HASH_LENGTH;
        }

        @Override
        public String deriveAddress() {
            if(address == null) {
                String calcAddress = super.deriveAddress();
                for(String node : merklePath)
                    calcAddress = hashNodes(calcAddress, node);
                address = calcAddress;
            }
            return address;
        }
    }
}
