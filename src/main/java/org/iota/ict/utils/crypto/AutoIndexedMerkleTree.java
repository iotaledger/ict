package org.iota.ict.utils.crypto;

public class AutoIndexedMerkleTree extends MerkleTree {

    protected int index;

    public AutoIndexedMerkleTree(String seed, int securityLevel, int depth) {
        super(seed, securityLevel, depth);
        this.index = 0;
    }

    public AutoIndexedMerkleTree(String seed, int securityLevel, int depth, int startIndex) {
        super(seed, securityLevel, depth);
        this.index = startIndex;
    }

    @Override
    public Signature sign(int index, String toSign) {
        throw new RuntimeException("Please use the other sign() function without the 'index' parameter.");
    }

    public Signature sign(String toSign) {
        return super.sign(index++, toSign);
    }

    public int getIndex() {
        return index;
    }
}
