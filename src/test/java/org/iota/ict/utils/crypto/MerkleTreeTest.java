package org.iota.ict.utils.crypto;

import org.iota.ict.utils.Trytes;
import org.junit.Assert;
import org.junit.Test;

public class MerkleTreeTest {

    @Test
    public void testSignatureVerification() {
        MerkleTree merkleTree = new MerkleTree(Trytes.randomSequenceOfLength(81), 3);
        String toSign = Trytes.randomSequenceOfLength(81);
        int randomIndex = (int)(Math.random()*Math.pow(2, merkleTree.getDepth()));
        MerkleTree.Signature signature = merkleTree.sign(randomIndex, toSign);

        String addressOfMerkleTree = merkleTree.getAddress();
        String addressOfSignature = signature.deriveAddress();

        Assert.assertEquals("Signature validation failed.", addressOfMerkleTree, addressOfSignature);
    }

    @Test
    public void testSignatureFromTrytesConcatenatedWithMerklePath() {
        MerkleTree merkleTree = new MerkleTree(Trytes.randomSequenceOfLength(81), 3);
        String toSign = Trytes.randomSequenceOfLength(81);
        int randomIndex = (int)(Math.random()*Math.pow(2, merkleTree.getDepth()));
        MerkleTree.Signature signature = merkleTree.sign(randomIndex, toSign);

        MerkleTree.Signature splitSignature = MerkleTree.Signature.fromTrytesConcatenatedWithMerklePath(signature.toString(), toSign);

        String addressOfMerkleTree = merkleTree.getAddress();
        String addressOfSplitSignature = splitSignature.deriveAddress();

        Assert.assertEquals("Concatenated signature split failed.", addressOfMerkleTree, addressOfSplitSignature);
    }
}