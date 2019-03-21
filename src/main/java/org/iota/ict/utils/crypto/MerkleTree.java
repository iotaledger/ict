package org.iota.ict.utils.crypto;

public class MerkleTree {

    private final int securityLevel;
    MerkleInnerNode root;

    public MerkleTree(String seed, int securityLevel, int depth) {
        this.securityLevel = securityLevel;
        this.root = new MerkleInnerNode(seed, 0, securityLevel, depth);
    }

    public int getSecurityLevel() {
        return securityLevel;
    }

    public int getDepth() {
        return root.getDepth();
    }

    public String getAddress() {
        return root.getHash();
    }

    public Signature sign(int index, String toSign) {
        if(index < 0 || index >= Math.pow(2, root.depth))
            throw new IllegalArgumentException("index: " + index + " not in interval [0,"+(int)(Math.pow(2, root.depth)-1)+"]");
        String[] merklePath = new String[root.depth];
        root.writeMerklePath(merklePath, index);
        MerkleLeave leave = root.getLeave(index);
        SignatureScheme.Signature leaveSignature = leave.privateKey.sign(toSign);
        return new Signature(leaveSignature.toString(), merklePath, toSign);
    }

    private static String hashNodes(String nodeA, String nodeB) {
        int comp = nodeA.compareTo(nodeB);
        return SignatureSchemeImplementation.hash((comp < 0 ? nodeA : nodeB) + (comp < 0 ? nodeB : nodeA));
    }

    private interface MerkleNode {
        String getHash();
        int getDepth();
        MerkleLeave getLeave(int index);
        void writeMerklePath(String[] path, int index);
    }

    private static class MerkleInnerNode implements MerkleNode {

        private final int depth;
        private final String hash;

        private final MerkleNode childLeft;
        private final MerkleNode childRight;

        MerkleInnerNode(String seed, int indexOffset, int securityLevel, int depth) {
            this.depth = depth;
            int childWidth = (int)Math.pow(2, depth-1);
            MerkleNode childA = depth > 1 ? new MerkleInnerNode(seed, indexOffset, securityLevel, depth-1) : new MerkleLeave(seed, indexOffset, securityLevel);
            MerkleNode childB = depth > 1 ? new MerkleInnerNode(seed, indexOffset+childWidth, securityLevel, depth-1) : new MerkleLeave(seed, indexOffset+childWidth, securityLevel);
            assert childA.getDepth()+1 == depth;
            int comp = childA.getHash().compareTo(childB.getHash());
            childLeft = comp < 0 ? childA : childB;
            childRight = comp < 0 ? childB : childA;
            hash = SignatureSchemeImplementation.hash(childLeft.getHash() + childRight.getHash());
        }

        @Override
        public int getDepth() {
            return depth;
        }

        @Override
        public String getHash() {
            return hash;
        }

        @Override
        public MerkleLeave getLeave(int index) {
            int childWidth = (int)Math.pow(2, depth-1);
            return (index >= childWidth ? childLeft : childRight).getLeave(index%childWidth);
        }

        @Override
        public  void writeMerklePath(String[] path, int index) {
            int childWidth = (int)Math.pow(2, depth-1);
            path[depth-1] = (index >= childWidth ? childRight : childLeft).getHash();
            (index >= childWidth ? childLeft : childRight).writeMerklePath(path, index%childWidth);
        }
    }

    private static class MerkleLeave implements MerkleNode {

        private final SignatureScheme.PrivateKey privateKey;
        private final SignatureScheme.PublicKey publicKey;

        MerkleLeave(String seed, int index, int securityLevel) {
            this.privateKey = SignatureSchemeImplementation.derivePrivateKeyFromSeed(seed, index, securityLevel);
            this.publicKey = privateKey.derivePublicKey();
        }

        public int getDepth() {
            return 0;
        }

        public String getHash() {
            return publicKey.getAddress();
        }

        @Override
        public MerkleLeave getLeave(int index) {
            return this;
        }

        @Override
        public void writeMerklePath(String[] path, int index) {

        }
    }

    public static class Signature extends SignatureSchemeImplementation.Signature {

        private final String[] merklePath;
        private String address;
        private int index = -1;

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
                calcAddressAndIndex();
            }
            return address;
        }

        public int deriveIndex() {
            if(address == null) {
                calcAddressAndIndex();
            }
            return index;
        }

        private void calcAddressAndIndex() {
            int calcIndex = 0;
            String calcAddress = super.deriveAddress();
            for(int layer = 0; layer < merklePath.length; layer++) {
                String node = merklePath[layer];
                int comp = calcAddress.compareTo(node);
                calcAddress = hashNodes(calcAddress, node);
                calcIndex += (int)Math.pow(2, layer) * (comp > 0 ? 0 : 1);
            }
            index = calcIndex;
            address = calcAddress;
        }
    }
}
