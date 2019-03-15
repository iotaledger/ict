package org.iota.ict.utils.crypto;

import org.iota.ict.utils.Trytes;
import org.junit.Assert;
import org.junit.Test;

public class MerkleTreeTest {

    // tree generation takes long -> use one tree for all tests
    private static final MerkleTree merkleTree = new MerkleTree(Trytes.randomSequenceOfLength(81), 3, 3);

    @Test
    public void testSignatureVerification() {
        String toSign = Trytes.randomSequenceOfLength(81);
        int randomIndex = (int)(Math.random()*Math.pow(2, merkleTree.getDepth()));
        MerkleTree.Signature signature = merkleTree.sign(randomIndex, toSign);

        String addressOfMerkleTree = merkleTree.getAddress();
        String addressOfSignature = signature.deriveAddress();

        Assert.assertEquals("Signature validation failed.", addressOfMerkleTree, addressOfSignature);
        Assert.assertEquals("Signature index derived incorrectly.", randomIndex, signature.deriveIndex());
    }

    @Test
    public void testSignatureFromTrytesConcatenatedWithMerklePath() {
        String toSign = Trytes.randomSequenceOfLength(81);
        int randomIndex = (int)(Math.random()*Math.pow(2, merkleTree.getDepth()));
        MerkleTree.Signature signature = merkleTree.sign(randomIndex, toSign);

        MerkleTree.Signature splitSignature = MerkleTree.Signature.fromTrytesConcatenatedWithMerklePath(signature.toString(), toSign);

        String addressOfMerkleTree = merkleTree.getAddress();
        String addressOfSplitSignature = splitSignature.deriveAddress();

        Assert.assertEquals("Concatenated signature split failed.", addressOfMerkleTree, addressOfSplitSignature);
    }
}